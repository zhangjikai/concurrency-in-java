package com.zhangjikai.basic;

/**
 * Created by zhangjikai on 17-3-20.
 */
public class PriorityDemo {

    public static void main(String[] args) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    System.out.println(Thread.currentThread().getName() + "(" + Thread.currentThread().getPriority() + ")"
                            + ", loop " + i);
                }
            }
        });

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    System.out.println(Thread.currentThread().getName() + "(" + Thread.currentThread().getPriority() + ")"
                            + ", loop " + i);
                }
            }
        });

        thread.setPriority(1);
        thread2.setPriority(10);
        thread.start();
        thread2.start();
    }
}
