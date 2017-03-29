package com.zhangjikai.basic;

/**
 * Created by zhangjikai on 17-3-13.
 */
public class RunnableDemo implements Runnable{
    @Override
    public void run() {
        System.out.println("I am in runnable Demo");
    }

    public static void main(String[] args) {
        Thread thread = new Thread(new RunnableDemo());
        thread.start();
    }
}

