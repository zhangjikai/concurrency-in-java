package com.zhangjikai.basic;

import java.util.concurrent.TimeUnit;

/**
 * Created by ZhangJikai on 2017/3/22.
 */
public class SleepDemo {
    public static void main(String[] args) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for(int i = 0; i < 10; i++) {
                        Thread.sleep(1000);
                        System.out.printf("I sleep %d second.\n", i + 1);

                    }
                } catch (InterruptedException e) {
                    System.out.println("I am interrupted.");
                    e.printStackTrace();
                }
            }
        });

        thread.start();
        try {
            TimeUnit.SECONDS.sleep(5);
            thread.interrupt();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
