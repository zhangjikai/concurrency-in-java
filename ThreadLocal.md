# ThreadLocal

<!-- toc -->

## 线程局部变量
在多线程环境下，之所以会有并发问题，就是因为不同的线程会同时访问同一个共享变量，例如下面的形式
```java
public class MultiThreadDemo {

    public static class Number {
        private int value = 0;

        public void increase() throws InterruptedException {
            value = 10;
            Thread.sleep(10);
            System.out.println("increase value: " + value);
        }

        public void decrease() throws InterruptedException {
            value = -10;
            Thread.sleep(10);
            System.out.println("decrease value: " + value);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        final Number number = new Number();
        Thread increaseThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    number.increase();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread decreaseThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    number.decrease();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        increaseThread.start();
        decreaseThread.start();
    }
}
```
在上面的代码中，increase 线程和 decrease 线程会操作同一个 number 中 value，那么输出的结果是不可预测的，因为当前线程修改变量之后但是还没输出的时候，变量有可能被另外一个变量修改，下面是一种可能的情况：
```
increase value: 10
decrease value: 10
```
一种解决方法是在 `increase()` 和 `decrease()` 方法上加上 synchronized 关键字进行同步，这种做法其实是将 value 的**赋值**和**打印**包装成了一个原子操作，也就是说两者要么同时进行，要不都不进行，中间不会有额外的操作。我们换个角度考虑问题，如果 value 只属于 increase 线程或者 decrease 线程，而不是被两个线程共享，那么也不会出现竞争问题。一种比较常见的形式就是局部（local）变量（这里排除局部变量引用指向共享对象的情况），如下所示：
```java
public void increase() throws InterruptedException {
    int value = 10;
    Thread.sleep(10);
    System.out.println("increase value: " + value);
}
```
不论 value 值如何改变，都不会影响到其他线程，因为在每次调用 increase 方法时，都会创建一个 value 变量，该变量只对当前调用 increase 方法的线程可见。借助于这种思想，我们可以对每个线程创建一个共享变量的副本，该副本只对当前线程可见（可以认为是线程私有的变量），那么修改该副本变量时就不会影响到其他的线程。一个简单的思路是使用 Map 存储每个变量的副本，将当前线程的 id 作为 key，副本变量作为 value 值，下面是一个实现：
```java
public class SimpleImpl {

    public static class CustomThreadLocal {
        private Map<Long, Integer> cacheMap = new HashMap<>();

        private int defaultValue ;

        public CustomThreadLocal(int value) {
            defaultValue = value;
        }

        public Integer get() {
            long id = Thread.currentThread().getId();
            if (cacheMap.containsKey(id)) {
                return cacheMap.get(id);
            }
            return defaultValue;
        }

        public void set(int value) {
            long id = Thread.currentThread().getId();
            cacheMap.put(id, value);
        }
    }

    public static class Number {
        private CustomThreadLocal value = new CustomThreadLocal(0);

        public void increase() throws InterruptedException {
            value.set(10);
            Thread.sleep(10);
            System.out.println("increase value: " + value.get());
        }

        public void decrease() throws InterruptedException {
            value.set(-10);
            Thread.sleep(10);
            System.out.println("decrease value: " + value.get());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        final Number number = new Number();
        Thread increaseThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    number.increase();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread decreaseThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    number.decrease();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        increaseThread.start();
        decreaseThread.start();
    }
}
```
但是上面的实现会存在下面的问题：
* 每个线程对应的副本变量的生命周期不是由线程决定的，而是由共享变量的生命周期决定的。在上面的例子中，即便线程执行完，只要 `number` 变量存在，线程的副本变量依然会存在（存放在 number 的 cacheMap 中）。但是作为特定线程的副本变量，该变量的生命周期应该由线程决定，线程消亡之后，该变量也应该被回收。
* 多个线程有可能会同时操作 cacheMap，需要对 cacheMap 进行同步处理。

为了解决上面的问题，我们换种思路，每个线程创建一个 Map，存放当前线程中副本变量，用变量在栈中的引用作为 key 值，下面是一个示例：
```java
public class SimpleImpl2 {

    public static class CommonThread extends Thread {
        Map<Integer, Integer> cacheMap = new HashMap<>();
    }

    public static class CustomThreadLocal {
        private int defaultValue;

        public CustomThreadLocal(int value) {
            defaultValue = value;
        }

        public Integer get() {
            Integer id = this.hashCode();
            Map<Integer, Integer> cacheMap = getMap();
            if (cacheMap.containsKey(id)) {
                return cacheMap.get(id);
            }
            return defaultValue;
        }

        public void set(int value) {
            Integer id = this.hashCode();
            Map<Integer, Integer> cacheMap = getMap();
            cacheMap.put(id, value);
        }

        public Map<Integer, Integer> getMap() {
            CommonThread thread = (CommonThread) Thread.currentThread();
            return thread.cacheMap;
        }
    }

    public static class Number {
        private CustomThreadLocal value = new CustomThreadLocal(0);

        public void increase() throws InterruptedException {
            value.set(10);
            Thread.sleep(10);
            System.out.println("increase value: " + value.get());
        }

        public void decrease() throws InterruptedException {
            value.set(-10);
            Thread.sleep(10);
            System.out.println("decrease value: " + value.get());
        }
    }


    public static void main(String[] args) throws InterruptedException {
        final Number number = new Number();
        Thread increaseThread = new CommonThread() {
            @Override
            public void run() {
                try {
                    number.increase();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        };

        Thread decreaseThread = new CommonThread() {
            @Override
            public void run() {
                try {
                    number.decrease();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        increaseThread.start();
        decreaseThread.start();
    }
}
```
在上面的实现中，当线程消亡之后，线程中 cacheMap 也会被回收，它当中存放的副本变量也会被全部回收，并且 cacheMap 是线程私有的，不会出现多个线程同时访问一个 cacheMap 的情况。在 Java 中，ThreadLocal 类的实现就是采用的这种思想，注意只是思想，实际的实现和上面的并不一样。

## 使用 ThreadLocal 的示例
Java 使用 ThreadLocal 类来实现线程局部变量模式，ThreadLocal 使用 set 和 get 方法设置和获取变量，下面是函数原型：
```java
public void set(T value);
public T get();
```
下面是使用 ThreadLocal 的一个完整示例：
```java
public class ThreadLocalDemo {
    private static ThreadLocal<Integer> threadLocal = new ThreadLocal<>();
    private static int value = 0;

    public static class ThreadLocalThread implements Runnable {
        @Override
        public void run() {
            threadLocal.set((int)(Math.random() * 100));
            value = (int) (Math.random() * 100);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.printf(Thread.currentThread().getName() + ": threadLocal=%d, value=%d\n", threadLocal.get(), value);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(new ThreadLocalThread());
        Thread thread2 = new Thread(new ThreadLocalThread());
        thread.start();
        thread2.start();
        thread.join();
        thread2.join();
    }
}
```
下面是一种可能的输出：
```
Thread-0: threadLocal=87, value=15
Thread-1: threadLocal=69, value=15
```
我们看到虽然 `threadLocal` 是静态变量，但是每个线程都有自己的值，不会受到其他线程的影响。

##

* http://www.iteye.com/topic/103804
* http://www.jianshu.com/p/529c03d9b67e
* http://stackoverflow.com/questions/38994306/what-is-the-meaning-of-0x61c88647-constant-in-threadlocal-java
* http://jerrypeng.me/2013/06/thread-local-and-magical-0x61c88647/
