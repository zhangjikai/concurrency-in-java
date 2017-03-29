package com.zhangjikai.basic;

/**
 * Created by zhangjikai on 17-3-20.
 */
public class ThreadDemo extends Thread{
    @Override
    public void run() {
        System.out.println("I am in Thread Demo");

    }

    public static void main(String[] args) throws InterruptedException {
        Thread thread = new ThreadDemo();
        thread.start();

        System.out.println(thread.getState());
    }
}

