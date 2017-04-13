# 队列同步器
<!--email_off-->

<!-- toc -->

## 前言
队列同步器 AbstractQueuedSynchronizer（以下简称 AQS），是用来构建锁或者其他同步组件的基础框架。它使用一个 int 成员变量来表示同步状态，通过 CAS 操作对同步状态进行修改，确保状态的改变是安全的。通过内置的 FIFO （First In First Out）队列来完成资源获取线程的排队工作。


## AQS 和 synchronized
在介绍 AQS 的使用之前，需要首先说明一点，AQS 同步和 synchronized 关键字同步（以下简称 synchronized 同步）是采用的两种不同的机制。首先看下 synchronized 同步，synchronized 关键字经过编译之后，会在同步块的前后分别形成 monitorenter 和 monitorexit 这两个字节码指令，这两个字节码需要关联到一个监视对象，当线程执行 monitorenter 指令时，需要首先获得获得监视对象的锁，这里监视对象锁就是进入同步块的凭证，只有获得了凭证才可以进入同步块，当线程离开同步块时，会执行 monitorexit 指令，释放对象锁。

在 AQS 同步中，使用一个 int 类型的变量 state 来表示当前同步块的状态。以独占式同步（一次只能有一个线程进程同步块）为例，state 的有效值有两个 0 和 1，其中 0 表示当前同步块中没有线程，1 表示同步块中已经有线程在执行。当线程要进入同步块时，需要首先判断 state 的值是否为 0，假设为 0，会尝试将 state 修改为 1，只有**修改成功**了之后，线程才可以进入同步块。注意上面提到的两个条件：
* state 为 0，证明当前同步块中没有线程在执行，所以当前线程可以尝试获得进入同步块的凭证，而这里的凭证就是是否成功将 state 修改为 1（在 synchronized 同步中，我们说的凭证是对象锁，但是对象锁的最终实现是否和我们现在说的方式类似，这里没有找到相关的资料）
* 成功将 state 修改为 1，通过使用 CAS 操作，我们可以确保即便有多个线程同时修改 state，也只有一个线程会修改成功。关于 CAS 的具体解释会在后面提到。

当线程离开同步块时，会修改 state 的值，将其设为 0，并唤醒等待的线程。所在在 AQS 同步中，我们说线程获得了锁，实际上是指线程成功修改了状态变量 state，而线程释放了锁，是指线程将状态变量置为了可修改的状态（在独占式同步中就是置为了 0），让其他线程可以再次尝试修改状态变量。在下面的表述中，我们说线程获得和释放了锁，就是上述含义， 这与 synchronized 同步中说的获得和释放锁的含义不同，需要区别理解。

## 基本使用
AQS 的设计是基于模板方法的，使用者需要继承 AQS 并重写指定的方法。在随后的使用中，AQS 中的模板方法会调用重写的方法。一般来说，我们需要重写的方法主要有下面 5 个：

| 方法名称 | 描述 |
|:--|:---|
| protected boolean tryAcquire(int) | 独占式获取锁，实现该方法需要查询当前状态并判断同步状态是否和预期值相同，然后使用 CAS 操作设置同步状态 |
| protected boolean tryRelease(int) | 独占式释放锁，等待获取锁的线程将有机会获取锁 |
| protected int tryAcquireShared(int) | 共享式获取锁，返回大于等于 0 的值，表示获取锁成功，反之获取失败 |
| protected boolean tryReleaseShared(int) | 共享式释放锁 |
| protected boolean isHeldExclusively() | 判断当前线程是否占有独占锁 |

在自定义的同步组件中，我们一般会调用 AQS 提供的模板方法。AQS 提供的模板方法基本上分为 3 类： 独占式获取与释放锁、共享式获取与释放锁以及查询同步队列中的等待线程情况。下面是相关的模板方法：

| 方法名称 | 描述 |
|:---|:---|
| void acquire(int) | 独占式获取锁，如果当前线程成功获取锁，那么方法就返回，否则会将当前线程放入同步队列等待。该方法会调用重写的 tryAcquire(int arg) 方法判断是否可以获得锁 |
| void acquireInterruptibly(int) | 和 acquire(int) 相同，但是该方法响应中断，当线程在同步队列中等待时，如果线程被中断，会抛出 InterruptedException 异常并返回。 |
| boolean tryAcquireNanos(int, long) | 在 acquireInterruptibly(int) 基础上添加了超时控制，同时支持中断和超时，当在指定时间内没有获得锁时，会返回 false，获取到了返回 true |
| void acquireShared(int) | 共享式获得锁，如果成功获得锁就返回，否则将当前线程放入同步队列等待，与独占式获取锁的不同是，同一时刻可以有多个线程获得共享锁，该方法调用 tryAcquireShared(int) |
| acquireSharedInterruptibly(int) | 与 acquireShared(int) 相同，该方法响应中断 |
| tryAcquireSharedNanos(int, long) | 在 acquireSharedInterruptibly(int) 基础上添加了超时控制 |
| boolean release(int) | 独占式释放锁，该方法会在释放锁后，将同步队列中的第一个节点包含的线程唤醒 |
| boolean releaseShared(int) | 共享式释放锁 |
| Collection<Thread> getQueuedThreads() | 获得同步队列中等待的线程集合 |

自定义组件通过使用使用同步器提供的模板方法来实现自己的同步语义。下面我们通过两个示例，看下如何借助于 AQS 来实现锁的同步语义。我们首先实现一个独占锁（排它锁），独占锁就是说在某个时刻内，只能有一个线程持有独占锁，只有持有锁的线程释放了独占锁，其他线程可以获取独占锁。下面是具体实现：
```java
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Created by Jikai Zhang on 2017/4/6.
 * <p>
 * 自定义独占锁
 */
public class Mutex implements Lock {

    // 通过继承 AQS，自定义同步器
    private static class Sync extends AbstractQueuedLongSynchronizer {

        // 当前线程是否被独占
        @Override
        protected boolean isHeldExclusively() {
            return getState() == 1;

        }

        // 尝试获得锁
        @Override
        protected boolean tryAcquire(long arg) {
            // 只有当 state 的值为 0，并且线程成功将 state 值修改为 1 之后，线程才可以获得独占锁
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;

        }

        @Override
        protected boolean tryRelease(long arg) {
            // state 为 0 说明当前同步块中没有锁了，无需释放
            if (getState() == 0) {
                throw new IllegalMonitorStateException();
            }
            // 将独占的线程设为 null
            setExclusiveOwnerThread(null);
            // 将状态变量的值设为 0，以便其他线程可以成功修改状态变量从而获得锁
            setState(0);
            return true;
        }

        Condition newCondition() {
            return new ConditionObject();
        }
    }

    // 将操作代理到 Sync 上
    private final Sync sync = new Sync();

    @Override
    public void lock() {
        sync.acquire(1);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(time));
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }


    public boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    public boolean isLocked() {
        return sync.isHeldExclusively();
    }

    public static void withoutMutex() throws InterruptedException {
        System.out.println("Without mutex: ");
        int threadCount = 2;
        final Thread threads[] = new Thread[threadCount];
        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 100000; j++) {
                        if (j % 20000 == 0) {
                            System.out.println("Thread-" + index + ": j =" + j);
                        }
                    }
                }
            });
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
    }


    public static void withMutex() {
        System.out.println("Without mutex: ");
        final Mutex mutex = new Mutex();
        int threadCount = 2;
        final Thread threads[] = new Thread[threadCount];
        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(new Runnable() {

                @Override
                public void run() {

                    mutex.lock();
                    try {
                        for (int j = 0; j < 100000; j++) {
                            if (j % 20000 == 0) {
                                System.out.println("Thread-" + index + ": j =" + j);
                            }
                        }
                    } finally {
                        mutex.unlock();
                    }
                }
            });
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        withoutMutex();
        System.out.println();
        withMutex();

    }
}
```
程序的运行结果如下面所示。我们看到使用了 Mutex 之后，线程 0 和线程 1 不会再交替执行，而是当一个线程执行完，另外一个线程再执行。
```
Without mutex:
Thread-0: j =0
Thread-1: j =0
Thread-0: j =20000
Thread-1: j =20000
Thread-0: j =40000
Thread-1: j =40000
Thread-0: j =60000
Thread-1: j =60000
Thread-1: j =80000
Thread-0: j =80000

With mutex:
Thread-0: j =0
Thread-0: j =20000
Thread-0: j =40000
Thread-0: j =60000
Thread-0: j =80000
Thread-1: j =0
Thread-1: j =20000
Thread-1: j =40000
Thread-1: j =60000
Thread-1: j =80000
```
下面在看一个共享锁的示例。在该示例中，我们定义两个共享资源，即同一时间内允许两个线程同时执行。我们将同步变量的初始状态 state 设为 2，当一个线程获取了共享锁之后，将 state 减 1，线程释放了共享锁后，将 state 加 1。状态的合法范围是 0、1 和 2，其中 0 表示已经资源已经用光了，此时线程再要获得共享锁就需要进入同步序列等待。下面是具体实现：
```java
import java.util.concurrent.TimeUnit;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Created by Jikai Zhang on 2017/4/9.
 * <p>
 * 自定义共享锁
 */
public class TwinsLock implements Lock {

    private static class Sync extends AbstractQueuedSynchronizer {

        public Sync(int resourceCount) {
            if (resourceCount <= 0) {
                throw new IllegalArgumentException("resourceCount must be larger than zero.");
            }
            // 设置可以共享的资源总数
            setState(resourceCount);
        }


        @Override
        protected int tryAcquireShared(int reduceCount) {
            // 使用尝试获得资源，如果成功修改了状态变量（获得了资源）或者资源的总量小于 0（没有资源了），则返回。
            for (; ; ) {
                int lastCount = getState();
                int newCount = lastCount - reduceCount;
                if (newCount < 0 || compareAndSetState(lastCount, newCount)) {
                    return newCount;
                }
            }
        }

        @Override
        protected boolean tryReleaseShared(int returnCount) {
            // 释放共享资源，因为可能有多个线程同时执行，所以需要使用 CAS 操作来修改资源总数。
            for (; ; ) {
                int lastCount = getState();
                int newCount = lastCount + returnCount;
                if (compareAndSetState(lastCount, newCount)) {
                    return true;
                }
            }
        }
    }

    // 定义两个共享资源，说明同一时间内可以有两个线程同时运行
    private final Sync sync = new Sync(2);

    @Override
    public void lock() {
        sync.acquireShared(1);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    @Override
    public boolean tryLock() {
        return sync.tryAcquireShared(1) >= 0;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(time));
    }

    @Override
    public void unlock() {
        sync.releaseShared(1);
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    public static void main(String[] args) {
        final Lock lock = new TwinsLock();
        int threadCounts = 10;
        Thread threads[] = new Thread[threadCounts];
        for (int i = 0; i < threadCounts; i++) {
            final int index = i;
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 5; i++) {
                        lock.lock();
                        try {
                            TimeUnit.SECONDS.sleep(1);
                            System.out.println(Thread.currentThread().getName());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            lock.unlock();
                        }

                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        for (int i = 0; i < threadCounts; i++) {
            threads[i].start();
        }
    }
}
```
运行程序，我们会发现程序每次都会同时打印两条语句，如下面的形式，证明同时有两个线程在执行。
```
Thread-0
Thread-1
Thread-3
Thread-2
Thread-8
Thread-4
Thread-3
Thread-6
```

## CAS 操作
CAS（Compare and Swap），比较并交换，通过利用底层硬件平台的特性，实现原子性操作。CAS 操作涉及到3个操作数，内存值 V，旧的期望值 A，需要修改的新值 B。当且仅当预期值 A 和 内存值 V 相同时，才将内存值 V 修改为 B，否则什么都不做。CAS 操作类似于执行了下面流程
```java
if(oldValue == memory[valueAddress]) {
    memory[valueAddress] = newValue;
}
```
在上面的流程中，其实涉及到了两个操作，比较以及替换，为了确保程序正确，需要确保这两个操作的原子性（也就是说确保这两个操作同时进行，中间不会有其他线程干扰）。现在的 CPU 中，提供了相关的底层 CAS 指令，即 CPU 底层指令确保了比较和交换两个操作作为一个原子操作进行（其实在这一点上还是有排他锁的. 只是比起用synchronized, 这里的排他时间要短的多.），Java 中的 CAS 函数是借助于底层的 CAS 指令来实现的。更多关于 CPU 底层实现的原理可以参考 [这篇文章](http://zl198751.iteye.com/blog/1848575)。我们来看下 Java 中对于 CAS 函数的定义：

```java
/**
 * Atomically update Java variable to x if it is currently
 * holding expected.
 * @return true if successful
 */
public final native boolean compareAndSwapObject(Object o, long offset, Object expected, Object x);

/**
 * Atomically update Java variable to x if it is currently
 * holding expected.
 * @return true if successful
 */
public final native boolean compareAndSwapInt(Object o, long offset, int expected, int x);

/**
 * Atomically update Java variable to x if it is currently
 * holding expected.
 * @return true if successful
 */
public final native boolean compareAndSwapLong(Object o, long offset, long expected, long x);
```
上面三个函数定义在 sun.misc.Unsafe 类中，使用该类可以进行一些底层的操作，例如直接操作原生内存，更多关于 Unsafe 类的文章可以参考 [这篇](http://ifeve.com/sun-misc-unsafe/)。以 compareAndSwapInt 为例，我们看下如何使用 CAS 函数：
```java
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Created by Jikai Zhang on 2017/4/8.
 */
public class CASIntTest {
    private volatile int count = 0;

    private static final Unsafe unsafe = getUnsafe();
    private static final long offset;

    // 获得 count 属性在 CASIntTest 中的偏移量（内存地址偏移）
    static {
        try {
            offset = unsafe.objectFieldOffset(CASIntTest.class.getDeclaredField("count"));
        } catch (NoSuchFieldException e) {
            throw new Error(e);
        }
    }
    // 通过反射的方式获得 Unsafe 类
    public static Unsafe getUnsafe() {
        Unsafe unsafe = null;
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return unsafe;
    }

    public void increment() {
        int previous = count;
        unsafe.compareAndSwapInt(this, offset, previous, previous + 1);
    }

    public static void main(String[] args) {
        CASIntTest casIntTest = new CASIntTest();
        casIntTest.increment();
        System.out.println(casIntTest.count);
    }
}
```
在 CASIntTest 类中，我们定义一个 count 变量，其中 `increment()` 方法是将 count 的值加 1，执行上面的程序，我们会看到输出结果也为 1。下面是将 count 加 1 的代码：
```java
int previous = count;
unsafe.compareAndSwapInt(this, offset, previous, previous + 1);
```
在没有线程竞争的条件下，该代码执行的结果是将 count 变量的值加 1（多个线程竞争可能会有线程执行失败），但是在 compareAndSwapInt 函数中，我们并没有传入 count 变量，那么函数是如何修改 count 变量值的呢？其实我们往 compareAndSwapInt 函数中传入了 count 变量在堆内存中的地址，函数直接修改了 count 变量所在内存区域。count 属性在堆内存中的地址是由 CASIntTest 实例的起始内存地址和 count 属性相对于起始内存的偏移量决定的。其中对象属性在对象中的偏移量通过 `objectFieldOffset` 函数获得，函数原型如下所示。该函数接受一个 Filed 类型的参数，返回该 Filed 属性在对象中的偏移量。

```java
/**
 * Report the location of a given static field, in conjunction with {@link
 * #staticFieldBase}.
 * Do not expect to perform any sort of arithmetic on this offset;
 * it is just a cookie which is passed to the unsafe heap memory accessors.
 *
 * Any given field will always have the same offset, and no two distinct
 * fields of the same class will ever have the same offset.
 *
 * As of 1.4.1, offsets for fields are represented as long values,
 * although the Sun JVM does not use the most significant 32 bits.
 * It is hard to imagine a JVM technology which needs more than
 * a few bits to encode an offset within a non-array object,
 * However, for consistency with other methods in this class,
 * this method reports its result as a long value.
 */
public native long objectFieldOffset(Field f);
```

下面我们再看一下 compareAndSwapInt 的函数原型。我们知道 CAS 操作需要知道 3 个信息：内存中的值，期望的旧值以及要修改的新值。通过前面的分析，我们知道通过 o 和 offset 我们可以确定属性在内存中的地址，也就是知道了属性在内存中的值。expected 对应期望的旧址，而 x 就是要修改的新值。

```java
public final native boolean compareAndSwapInt(Object o, long offset, int expected, int x);
```
compareAndSwapInt 函数首先比较一下 expected 是否和内存中的值相同，如果不同证明其他线程修改了属性值，那么就不会执行更新操作，但是程序如果就此返回了，似乎不太符合我们的期望，我们是希望程序可以执行更新操作的，如果其他线程先进行了更新，那么就在更新后的值的基础上进行修改，所以我们一般使用循环配合 CAS 函数，使程序在更新操作完成之后再返回，如下所示：
```java
long before = counter;
while (!unsafe.compareAndSwapLong(this, offset, before, before + 1)) {
    before = counter;
}
```

下面是使用 CAS 函数实现计数器的一个实例：
```java
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Created by Jikai Zhang on 2017/4/8.
 */
public class CASCounter {

    // 通过反射的方式获得 Unsafe 类
    public static Unsafe getUnsafe() {
        Unsafe unsafe = null;
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return unsafe;
    }

    private volatile long counter = 0;
    private static final long offset;
    private static final Unsafe unsafe = getUnsafe();

    static {
        try {
            offset = unsafe.objectFieldOffset(CASCounter.class.getDeclaredField("counter"));
        } catch (NoSuchFieldException e) {
            throw new Error(e);
        }
    }

    public void increment() {
        long before = counter;
        while (!unsafe.compareAndSwapLong(this, offset, before, before + 1)) {
            before = counter;
        }
    }

    public long getCounter() {
        return counter;
    }

    private static long intCounter = 0;

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 10;
        Thread threads[] = new Thread[threadCount];
        final CASCounter casCounter = new CASCounter();

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {

                    for (int i = 0; i < 10000; i++) {
                        casCounter.increment();
                        intCounter++;
                    }
                }
            });
            threads[i].start();
        }

        for(int i = 0; i < threadCount; i++) {
            threads[i].join();
        }
        System.out.printf("CASCounter is %d \nintCounter is %d\n", casCounter.getCounter(), intCounter);
    }
}
```

## 同步队列


同步器依赖内部的同步队列（一个 FIFO）的双向队列来完成同步状态的管理，当前线程获取同步状态失败时，同步器会将当前线程以及等待状态等信息构造成一个节点（Node）并将其加入同步队列，同时会阻塞当前线程，当同步状态释放时，会把首节点中的线程唤醒，使其再次尝试获取同步状态。同步队列的结构如下所示：

![](/images/同步队列.png)
> 图片来自 http://www.infoq.com/cn/articles/jdk1.8-abstractqueuedsynchronizer

Head 节点本身不保存等待线程的信息，它通过 next 变量指向第一个保存线程等待信息的节点（Node1）。当线程被唤醒之后，会删除 Head 节点，而唤醒线程所在的节点会设置为 Head 节点（Node1 被唤醒之后，Node1会被置为 Head 节点）。下面我们看下 JDK 中同步队列的实现。

### Node 类
首先看在节点所对应的 Node 类：
```java
static final class Node {

    /**
     * 标志是独占式模式还是共享模式
     */
    static final Node SHARED = new Node();
    static final Node EXCLUSIVE = null;

    /**
     * 线程等待状态的合法值
     */
    static final int CANCELLED = 1;
    static final int SIGNAL = -1;
    static final int CONDITION = -2;
    static final int PROPAGATE = -3;

    /**
     * 线程状态，合法值为上面 4 个值中的一个
     */
    volatile int waitStatus;

    /**
     * 当前节点的前置节点
     */
    volatile Node prev;

    /**
     * 当前节点的后置节点
     */
    volatile Node next;

    /**
     * 当前节点所关联的线程
     */
    volatile Thread thread;

    /**
     * 指向下一个在某个条件上等待的节点，或者指向 SHARE 节点，标明当前处于共享模式
     */
    Node nextWaiter;

    final boolean isShared() {
        return nextWaiter == SHARED;
    }

    final Node predecessor() throws NullPointerException {
        Node p = prev;
        if (p == null)
            throw new NullPointerException();
        else
            return p;
    }

    Node() { // Used to establish initial head or SHARED marker
    }

    Node(Thread thread, Node mode) { // Used by addWaiter
        this.nextWaiter = mode;
        this.thread = thread;
    }

    Node(Thread thread, int waitStatus) { // Used by Condition
        this.waitStatus = waitStatus;
        this.thread = thread;
    }
}
```
在 Node 类中定义了四种等待状态：
* CANCELED： 1，因为等待超时 （timeout）或者中断（interrupt），节点会被置为取消状态。处于取消状态的节点不会再去竞争锁，也就是说不会再被阻塞。节点会一直保持取消状态，而不会转换为其他状态。处于 CANCELED 的节点会被移出队列，被 GC 回收。
* SIGNAL： -1，标明当前的后继结点正在或者将要被阻塞（通过使用 LockSupport.pack 方法）。因为当前的节点被释放（release）或者被取消时（cancel）时，要唤醒它的后继结点（通过 LockSupport.unpark 方法）。
* CONDITION： -2，标明当前节点在条件队列中，因为等待某个条件而被阻塞。
* PROPAGATE： -3，在共享模式下，可以认为资源有多个，因此当前线程被唤醒之后，可能还有剩余的资源可以唤醒其他线程。该状态用来标明后续节点会传播唤醒的操作。需要注意的是只有头节点才可以设置为该状态（This is set (for head node only) in doReleaseShared to ensure propagation continues, even if other operations have since intervened.）。
* 0：新创建的节点会处于这种状态

### 独占锁的获取和释放
我们首先看下独占锁的获取和释放过程

**独占锁获取**

下面是获取独占锁的流程图：
![](images/获取独占锁.png)

我们通过 acquire 方法来获取独占锁，下面是方法定义
```java
public final void acquire(int arg) {
    // 首先尝试获取锁，如果获取失败，会先调用 addWaiter 方法将创建节点并追加到队列尾部
    // 然后调用 acquireQueued 阻塞或者循环尝试获取锁
    if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg)){
        // 在 acquireQueued 中，如果线程是因为中断而退出的阻塞状态会返回 true
        // 这里的 selfInterrupt 主要是为了恢复线程的中断状态
        selfInterrupt();
    }
}
```
acquire 会首先调用 tryAcquire 方法来获得锁，这个方法需要我们来实现，这个在前面已经提过了。如果没有获取锁，会调用 addWaiter 方法会创建一个和当前线程关联的节点追加到等待队列的尾部，我们调用 addWaiter 时传入的是 Node.EXCLUSIVE，表明当前是独占模式。下面是 addWaiter 的具体实现
```java
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    // tail 指向等待队列的尾节点
    Node pred = tail;
    // Try the fast path of enq; backup to full enq on failure
    if (pred != null) {
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    enq(node);
    return node;
}
```
addWaiter 方法会首先调用 if 方法，来判断能否成功将节点添加到队列尾部，如果添加失败，再调用 enq 方法（使用循环不断重试）进行添加，下面是 enq 方法的实现：
```java
private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        // 同步队列采用的懒初始化（lazily initialized）的方式，
        // 初始时 head 和 tail 都会被设置为 null，当一次被访问时
        // 才会创建 head 对象，并把尾指针指向 head。
        if (t == null) { // Must initialize
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}
```
addWaiter 仅仅是将节点加到了等待队列的末尾，并没有阻塞线程，线程阻塞的操作是在 acquireQueued 方法中完成的，下面是 acquireQueued 的实现：
```java
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            // 如果当前节点的前继节点是 head，就使用自旋（循环）的方式不断请求锁
            if (p == head && tryAcquire(arg)) {
                // 成功获得锁，将当前节点置为 head 节点，同时删除原 head 节点
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }

            // shouldParkAfterFailedAcquire 检查是否可以挂起线程，
            // 如果可以挂起进程，会调用 parkAndCheckInterrupt 挂起线程，
            // 如果 parkAndCheckInterrupt 返回 true，表明当前线程是因为中断而退出挂起状态的，
            // 所以要将 interrupted 设为 true，表明当前线程被中断过
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```
acquireQueued 会首先检查当前节点的前继节点是否为 head，如果为 head，将使用自旋的方式不断的请求锁，如果不是 head，则调用 shouldParkAfterFailedAcquire 查看是否应该挂起当前节点关联的线程，下面是 shouldParkAfterFailedAcquire 的实现：
```java
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    // 当前节点的前继节点的等待状态
    int ws = pred.waitStatus;
    // 如果前继节点的等待状态为 SIGNAL 我们就可以将当前节点对应的线程挂起
    if (ws == Node.SIGNAL)
        return true;
    if (ws > 0) {
        // ws 大于 0，表明当前线程的前继节点处于 CANCELED 的状态，
        // 所以我们需要从当前节点开始往前查找，直到找到第一个不为
        // CAECELED  状态的节点
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        pred.next = node;
    } else {
        /*
         * waitStatus must be 0 or PROPAGATE.  Indicate that we
         * need a signal, but don't park yet.  Caller will need to
         * retry to make sure it cannot acquire before parking.
         */
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
    }
    return false;
}
```
shouldParkAfterFailedAcquire 会检查前继节点的等待状态，如果前继节点状态为 SIGNAL，则可以将当前节点关联的线程挂起，如果不是 SIGNAL，会做一些其他的操作，在当前循环中不会挂起线程。如果确定了可以挂起线程，就调用 parkAndCheckInterrupt 方法对线程进行阻塞：
```java
private final boolean parkAndCheckInterrupt() {
    // 挂起当前线程
    LockSupport.park(this);
    // 可以通过调用 interrupt 方法使线程退出 park 状态，
    // 为了使线程在后面的循环中还可以响应中断，会重置线程的中断状态。
    // 这里使用 interrupted 会先返回线程当前的中断状态，然后将中断状态重置为 false，
    // 线程的中断状态会返回给上层调用函数，在线程获得锁后，
    // 如果发现线程曾被中断过，会将中断状态重新设为 true
    return Thread.interrupted();
}
```

**独占锁释放**

下面是释放独占锁的流程：
![](images/释放独占锁.png)

通过 release 方法，我们可以释放互斥锁。下面是 release 方法的实现：
```java
public final boolean release(int arg) {
    if (tryRelease(arg)) {
        Node h = head;
        // waitStatus 为 0，证明是初始化的空队或者后继结点已经被唤醒了
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);
        return true;
    }
    return false;
}
```
在独占模式下释放锁时，是没有其他线程竞争的，所以处理会简单一些。首先尝试释放锁，如果失败就直接返回（失败不是因为多线程竞争，而是线程本身就不拥有锁）。如果成功的话，会检查 h 的状态，然后调用 unparkSuccessor 方法来唤醒后续线程。下面是 unparkSuccessor 的实现：
```java
private void unparkSuccessor(Node node) {

    int ws = node.waitStatus;
    // 将 head 节点的状态置为 0，表明当前节点的后续节点已经被唤醒了，
    // 不需要再次唤醒，修改 ws 状态主要作用于 release 的判断
    if (ws < 0)
        compareAndSetWaitStatus(node, ws, 0);

    /*
     * Thread to unpark is held in successor, which is normally
     * just the next node.  But if cancelled or apparently null,
     * traverse backwards from tail to find the actual
     * non-cancelled successor.
     */
    Node s = node.next;
    if (s == null || s.waitStatus > 0) {
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0)
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread);
}
```
在 unparkSuccessor 方法中，如果发现头节点的后继结点为 null 或者处于 CANCELED 状态，会从尾部往前找（在节点存在的前提下，这样一定能找到）离头节点最近的需要唤醒的节点，然后唤醒该节点。



## 参考文章

* [Java 并发编程的艺术](http://download.csdn.net/detail/u011898232/9548575)
* [Java Magic. Part 4: sun.misc.Unsafe](http://ifeve.com/sun-misc-unsafe/)
* [Java里的CompareAndSet(CAS)](http://www.blogjava.net/mstar/archive/2013/04/24/398351.html)
* [ReentrantLock的lock-unlock流程详解](http://blog.csdn.net/luonanqin/article/details/41871909)
* [深入JVM锁机制2-Lock](http://blog.csdn.net/chen77716/article/details/6641477)
* [深度解析Java 8：JDK1.8 AbstractQueuedSynchronizer的实现分析（上）](http://www.infoq.com/cn/articles/jdk1.8-abstractqueuedsynchronizer)
* http://www.cnblogs.com/zhanjindong/p/java-concurrent-package-aqs-AbstractQueuedSynchronizer.html
* http://www.infoq.com/cn/articles/jdk1.8-abstractqueuedsynchronizer
* https://my.oschina.net/xianggao/blog/532709
* http://www.javarticles.com/2012/10/abstractqueuedsynchronizer-aqs.html

<!--email_off-->
