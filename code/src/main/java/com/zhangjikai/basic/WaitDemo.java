package com.zhangjikai.basic;

/**
 * Created by ZhangJikai on 2017/3/26.
 */
public class WaitDemo {

    public static void main(String[] args) throws InterruptedException {
        testWaitNotify();
    }

    public static void testWaitNotify() throws InterruptedException {
        final Object obj = new Object();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (obj) {
                        obj.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("finish wait.");
            }
        });

        thread.start();
        Thread.sleep(1000);
        // 该方法获得是 obj 对象的锁
        synchronized (obj) {
            obj.notify();
        }
    }
}
