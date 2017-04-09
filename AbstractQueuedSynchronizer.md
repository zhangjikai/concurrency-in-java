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

自定义组件通过使用使用同步器提供的模板方法来实现自己的同步语义。


AQS 的主要使用方式是继承，子类同步器 AQS 继承并实现它指定的方法来管理同步状态。一般我们只需要定义线程能否获得锁的判断逻辑，而后续的处理（例如线程获取锁失败进入同步队列等待）则交给 AQS 来完成。一般来说，我们只需要重写下面几个方法



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
* [深入JVM锁机制2-Lock](http://blog.csdn.net/chen77716/article/details/6641477)

同步器依赖内部的同步队列（一个 FIFO）的双向队列来完成同步状态的管理，当前线程获取同步状态失败时，同步器会将当前线程以及等待状态等信息构造成一个节点（Node）并将其加入同步队列，同时会阻塞当前线程，当同步状态释放时，会把首节点中的线程唤醒，使其再次尝试获取同步状态。



## 参考文章

* [Java 并发编程的艺术](http://download.csdn.net/detail/u011898232/9548575)
* [Java Magic. Part 4: sun.misc.Unsafe](http://ifeve.com/sun-misc-unsafe/)
* [Java里的CompareAndSet(CAS)](http://www.blogjava.net/mstar/archive/2013/04/24/398351.html)
* [ReentrantLock的lock-unlock流程详解](http://blog.csdn.net/luonanqin/article/details/41871909)


<!--email_off-->
