# Lock 框架

<!-- toc -->

## 示例
通过使用显式锁，我们可以更加灵活的来处理线程同步。同时使用显式锁可以实现 synchronized 所没有的功能，下面是使用显式锁的一个示例：
```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Jikai Zhang on 2017/4/4.
 */
public class ReentrantLockTest {
    private static int count = 1;
    private static Lock lock = new ReentrantLock();

    static class CustomThread implements Runnable {
        @Override
        public void run() {
            lock.lock();
            try {
                count++;
                Thread.sleep(1000);
                System.out.println(Thread.currentThread().toString() + ": " + count);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }

        }
    }

    public static void main(String[] args) {
        Thread thread = new Thread(new CustomThread());
        Thread thread2 = new Thread(new CustomThread());
        thread.start();
        thread2.start();
    }

}
```
## Lock 框架体系

![](/images/lock.jpg)
> 图片来自 https://my.oschina.net/xianggao/blog/88477

## 参考资料
* [Java 并发：Lock 框架详解](http://blog.csdn.net/justloveyou_/article/details/54972105)
* [Java Threads](http://cs.lmu.edu/~ray/notes/javathreading/)
* [Java Concurrency / Multithreading Tutorial](http://tutorials.jenkov.com/java-concurrency/index.html)
