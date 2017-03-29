package com.zhangjikai.basic;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by ZhangJikai on 2017/3/26.
 */
public class ProducerConsumer {

    public static class Storage {
        private final int MAX_ITEM = 5;
        private Queue<Integer> queue = new LinkedList<>();

        public synchronized void put(Integer item) {
            while (queue.size() >= MAX_ITEM) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            queue.add(item);
            System.out.println("produce product: " + item);
            notifyAll();
        }

        public synchronized Integer take() {
            Integer item = 0;
            while (queue.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // slow down the consumer speed.
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            item = queue.poll();
            System.out.println("consume product: " + item);
            notifyAll();
            return item;
        }
    }

    public static class ProducerThread implements Runnable {
        private Storage storage;

        public ProducerThread(Storage storage) {
            this.storage = storage;
        }

        @Override
        public void run() {
            int num = 20;
            for (int i = 0; i < num; i++) {
                storage.put(i);
            }
        }
    }

    public static class ConsumerThread implements Runnable {

        private Storage storage;

        public ConsumerThread(Storage storage) {
            this.storage = storage;
        }

        @Override
        public void run() {
            int num = 20;
            for (int i = 0; i < num; i++) {
                storage.take();
            }
        }
    }

    public static void main(String[] args) {
        Storage storage = new Storage();
        Thread consumerThread = new Thread(new ConsumerThread(storage));
        Thread producerThread = new Thread(new ProducerThread(storage));
        consumerThread.start();
        producerThread.start();
    }
}
