# 队列同步器
<!--email_off-->

<!-- toc -->

## 前言
队列同步器 AbstractQueuedSynchronizer（以下简称 AQS），是用来构建锁或者其他同步组件的基础框架。它使用一个 int 成员变量来表示同步状态，通过 CAS 操作对同步状态进行修改，确保状态的改变是安全的。通过内置的 FIFO （First In First Out）队列来完成资源获取线程的排队工作。

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

<!--email_off-->
