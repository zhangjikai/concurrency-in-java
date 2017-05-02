# synchronized
synchronized 是最常用的实现同步的手段，在 Java SE 1.6 以及之后的版本，对 synchronized 进行了优化，使 synchronized 整体的性能得到了很大的提升，下面看下 synchronized 的相关实现。

## 基础
下面是一个基本的使用示例：
```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Jikai Zhang on 2017/5/2.
 */
public class SynchronizedTest {

    public static int counter = 0;

    public synchronized static void increase() {
        for(int i = 0; i < 10000; i++) {
            counter++;
        }
    }

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(100);
        for(int i = 0; i < 10; i ++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    increase();
                }
            });
        }
        executor.shutdown();
        while (!executor.isTerminated()) {}
        System.out.println(counter);
    }
}
```
synchronized 代码执行之前，要首先锁住一个对象，具体为下面三种情况：
* 对于普通方法（非静态）方法，锁住的是当前实例对象
* 对于静态方法，锁住是当前类的 Class 对象
* 对于同步方法块，锁住的是 synchronized 括号中配置的对象。

synchronized 关键字经过编译之后，会在同步块的前后分别形成 monitorenter 和 monitorexit 这两个字节码指令，这两条指令都需要一个 reference 类型的参数来指明要锁定和解锁的对象。当执行 monitorenter 指令时，会首先尝试获取对象的锁，如果对象没被锁定，或者当前线程已经拥有了那个对象的锁，就把锁的计数器加 1，相应的，在执行 monitorexit 指令时会将锁的计数器减 1，当计数器为 0 时，锁就会被释放。若果获取锁失败，那么当前线程就要进入阻塞状态，直到另外一个线程释放锁。
