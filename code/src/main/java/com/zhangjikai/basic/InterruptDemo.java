package com.zhangjikai.basic;

import java.util.concurrent.TimeUnit;

/**
 * Created by ZhangJikai on 2017/3/23.
 */
public class InterruptDemo {
    public static void main(String[] args) throws InterruptedException {
        // testInterruptException();
        handleInterruptByHand();
    }

    public static void testInterrupt() throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int threshold = 10000;
                int index = 1;
                while (true) {
                    if(index++ % threshold == 0) {
                        System.out.println(index);
                        index = 1;
                    }
                }
            }
        });

        thread.start();
        TimeUnit.SECONDS.sleep(1);
        thread.interrupt();
    }

    public static void testInterruptException() throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    System.out.println("Thread is interrupted, and current stage is: " + Thread.currentThread().isInterrupted());
                    //e.printStackTrace();
                }
            }
        });
        thread.start();
        Thread.sleep(1000);
        thread.interrupt();
    }

    public static void handleInterruptException() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    System.out.println("Thread is interrupted, and current stage is: " + Thread.currentThread().isInterrupted());
                    Thread.currentThread().interrupt();
                    //e.printStackTrace();
                }
            }
        });
        thread.start();
        thread.interrupt();


    }

    public static void handleInterruptByHand() throws InterruptedException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int max = 10000000, index=0;
                while (!Thread.currentThread().isInterrupted()) {
                    index++;
                    if(index > max) {
                        index = 0;
                        System.out.println("doing task......");
                    }
                }
                System.out.println("finish task");

            }
        });
        thread.start();
        Thread.sleep(100);
        thread.interrupt();
    }

}

