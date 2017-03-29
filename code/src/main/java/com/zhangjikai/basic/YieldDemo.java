package com.zhangjikai.basic;

/**
 * Created by Jikai Zhang on 2017/3/27.
 */
public class YieldDemo {


    public static class YieldThread implements Runnable {

        @Override
        public void run() {
            int num = 10;
            for (int i = 0; i < num; i++) {
                System.out.println("YieldThread: " + i);
                Thread.yield();
            }
        }
    }

    public static void main(String[] args) {
        Thread thread = new Thread(new YieldThread());

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                int num = 10;

                for (int i = 0; i < num; i++) {
                    System.out.println("CommonThread: " + i);
                }
            }
        });

        thread.start();
        thread2.start();
    }
}
