package com.zhangjikai.basic;

/**
 * Created by ZhangJikai on 2017/3/22.
 */
public class CurrentThreadDemo {

    public static void main(String[] args) {
        Thread thread = Thread.currentThread();
        System.out.println(thread.getId() + " " + thread.getName());
    }
}
