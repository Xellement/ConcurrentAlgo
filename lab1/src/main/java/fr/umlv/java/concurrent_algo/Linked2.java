package fr.umlv.java.concurrent_algo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class Linked2<E> {

    private static class Entry<E> {
        private final E element;
        private final Entry<E> next;

        private Entry(E element, Entry<E> next) {
            this.element = element;
            this.next = next;
        }
    }

    private Entry<E> head;
    private static final VarHandle VARHANDLE;

    static {
        var lookup = MethodHandles.lookup();
        try {
            VARHANDLE = lookup.findVarHandle(Linked2.class, "head", Entry.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public void addFirst(E element) {
        Objects.requireNonNull(element);
        while (true) {
            var currentHead = head;
            var newHead = new Entry<>(element, currentHead);
            if (VARHANDLE.compareAndSet(this, currentHead, newHead)) return;
        }
    }

    public int size() {
        var size = 0;
        for(var link = head; link != null; link = link.next)
            size ++;
        return size;
    }

    public static void main(String[] args) throws InterruptedException {
        var list = new Linked2<Integer>();
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
