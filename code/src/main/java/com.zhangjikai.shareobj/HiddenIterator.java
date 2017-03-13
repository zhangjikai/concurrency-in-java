package com.zhangjikai.shareobj;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Created by zhangjikai on 17-3-13.
 */
public class HiddenIterator {
    private final Set<Integer> set = new HashSet<>();

    public synchronized void add(Integer i) {
        set.add(i);
    }

    public synchronized void remove(Integer i) {
        set.remove(i);
    }

    public void addTenThings() {
        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            add(r.nextInt());
        }
        System.out.println("Debug: added ten elements to " + set);
    }

    public static void main(String[] args) {
        final HiddenIterator iterator = new HiddenIterator();

        new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++)
                    iterator.addTenThings();
            }
        }.start();

        for (int i = 0; i < 10; i++)
            iterator.addTenThings();
    }
}
