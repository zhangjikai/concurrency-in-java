# ThreadLocal 模式



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
不论 value 值如何改变，都不会影响到其他线程，因为在每次调用 increase 方法时，都会创建一个 value 变量，该变量只对当前调用 increase 方法的线程可见。借助于这种思想，我们可以对每个线程创建一个共享变量的备份，该备份只对当前线程可见，那么修改该备份变量时就不会影响到其他的线程。一个简单的思路是使用 Map 存储每个变量的副本，将当前线程的 id 作为 key，备份变量作为 value 值，下面是一个实现：
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

* http://www.iteye.com/topic/103804
* http://www.jianshu.com/p/529c03d9b67e
* http://stackoverflow.com/questions/38994306/what-is-the-meaning-of-0x61c88647-constant-in-threadlocal-java
* http://jerrypeng.me/2013/06/thread-local-and-magical-0x61c88647/
