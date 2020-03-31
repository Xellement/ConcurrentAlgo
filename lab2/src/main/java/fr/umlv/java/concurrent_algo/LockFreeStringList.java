package fr.umlv.java.concurrent_algo;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LockFreeStringList {
    static final class Entry {
        final String element;
        volatile Entry next;

        Entry(String element) {
            this.element = element;
        }
    }

    private final Entry head;
    private static final VarHandle NEXT_HANDLE, TAIL_HANDLE;
    private volatile Entry tail;

    static {
        var lookup = MethodHandles.lookup();
        try {
            NEXT_HANDLE = lookup.findVarHandle(Entry.class, "next", Entry.class);
            TAIL_HANDLE = lookup.findVarHandle(LockFreeStringList.class, "tail", Entry.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public LockFreeStringList() {
        head = new Entry(null);
        tail = head;
    }

    /*public void addLast(String element) {
        var entry = new Entry(Objects.requireNonNull(element));
        var current = head;
        while (true){
            var next = current.next;
            if (next == null) {
                if (NEXT_HANDLE.compareAndSet(current, null, entry))
                    return;
                next = current.next;
            }
            current = next;
        }
    }*/

    /*public void addLast(String element) {
        var entry = new Entry(Objects.requireNonNull(element));
        var oldTail = tail;
        var current = oldTail;
        while (true){
            var next = current.next;
            if (next == null) {
                if (NEXT_HANDLE.compareAndSet(current, null, entry)) {
                    TAIL_HANDLE.compareAndSet(this, oldTail, entry);
                    return;
                }
                next = tail;
            }
            current = next;
        }
    }*/

    public void addLast(String element) {
        var entry = new Entry(Objects.requireNonNull(element));
        var oldTail = tail;
        var current = oldTail;
        while (!NEXT_HANDLE.compareAndSet(current, null, entry))
            current = current.next;
        TAIL_HANDLE.compareAndSet(this, oldTail, entry);
    }

    public int size() {
        var count = new AtomicInteger();
        for (var e = head.next; e != null; e = e.next)
            count.incrementAndGet();
        return count.get();
    }

    private static Runnable createRunnable(LockFreeStringList list, int id) {
        return () -> {
            for (var j = 0; j < 10_000; j++) {
                list.addLast(id + " " + j);
            }
        };
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        var threadCount = 50;
        var list = new LockFreeStringList();
        var tasks = IntStream.range(0, threadCount)
                .mapToObj(id -> createRunnable(list, id))
                .map(Executors::callable)
                .collect(Collectors.toList());
        var executor = Executors.newFixedThreadPool(threadCount);
        var futures = executor.invokeAll(tasks);
        executor.shutdown();
        for(var future : futures) {
            future.get();
        }
        System.out.println(list.size());
    }
}