package com.zhangjikai.basic;

import java.util.concurrent.TimeUnit;

/**
 * Created by zhangjikai on 17-3-21.
 */
public class DaemonDemo {
    public static void main(String[] args) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("run finally in daemon thread.");
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
    }
}
