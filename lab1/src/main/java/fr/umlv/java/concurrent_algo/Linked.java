package fr.umlv.java.concurrent_algo;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class Linked<E> {
    private static class Entry<E> {
        private final E element;
        private final Entry<E> next;

        private Entry(E element, Entry<E> next) {
            this.element = element;
            this.next = next;
        }
    }

    private final AtomicReference<Entry<E>> head = new AtomicReference<>();

    public void addFirst(E element) {
        while (true) {
            Entry<E> tmp = head.get();
            if (head.compareAndSet(tmp, new Entry<>(element, tmp)))
                return;
        }
    }

    public int size() {
        var size = 0;
        for(var link = head.get(); link != null; link = link.next)
            size ++;
        return size;
    }

    public static void main(String[] args) throws InterruptedException {
        var list = new Linked<Integer>();
        var threads = new ArrayList<Thread>();
        for (var i = 0;i < 4; i++){
            var thread = new Thread(() -> {
                for (var j = 0;j< 100_000; j++)
                    list.addFirst(j);
            });
            thread.start();
            threads.add(thread);
        }
        for (var thread: threads)
            thread.join();
        System.out.println(list.size());
    }

}
