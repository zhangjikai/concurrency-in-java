package com.zhangjikai.basic;

/**
 * Created by ZhangJikai on 2017/3/26.
 */
public class JoinDemo {
    public static class JoinThread implements Runnable{
        @Override
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("in JoinThread");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(new JoinThread());
        thread.start();
        System.out.println("in main thread: before thread.join()");
        thread.join();
        System.out.println("in main thread: after thread.join()");

    }
}
