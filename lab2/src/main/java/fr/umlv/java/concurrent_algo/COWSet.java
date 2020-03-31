package fr.umlv.java.concurrent_algo;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public class COWSet<E> {
    private final E[][] hashArray;

    private static final Object[] EMPTY = new Object[0];
    private static int SIZE = 2_000;

    private static final VarHandle HASH_ARRAY_HANDLE;

    static {
        var lookup = MethodHandles.lookup();
        // HASH_ARRAY_HANDLE = lookup.findVarHandle(COWSet.class, "hashArray", Object[][].class);
        HASH_ARRAY_HANDLE = MethodHandles.arrayElementVarHandle(Object[][].class);
    }

    @SuppressWarnings("unchecked")
    public COWSet(int capacity) {
        var array = new Object[capacity][];
        Arrays.fill(array, EMPTY);
        this.hashArray = (E[][])array;
    }

    public boolean add(E element) {
        Objects.requireNonNull(element);
        var i = element.hashCode() % hashArray.length;
        while (true){
            var oldArray = (E[]) HASH_ARRAY_HANDLE.getVolatile(hashArray, i);
            for (E e : hashArray[i])
                if (element.equals(e))
                    return false;
            var newArray = Arrays.copyOf(oldArray, oldArray.length + 1);
            newArray[oldArray.length] = element;
            if (HASH_ARRAY_HANDLE.compareAndSet(hashArray, i, oldArray, newArray))
                return true;
        }
    }

    public void forEach(Consumer<? super E> consumer) {
        for(var i = 0; i < hashArray.length; i++) {
            var oldArray = (E[]) HASH_ARRAY_HANDLE.getVolatile(hashArray, i);
            for(var element: oldArray)
                consumer.accept(element);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        COWSet<Integer> set = new COWSet<>(SIZE/16);

        Thread t1 = new Thread(() ->{
            for (int i = 0; i < SIZE; i++)
                set.add(i);
        });
        t1.start();

        Thread t2 = new Thread(() ->{
            for (int i = 0; i < SIZE; i++)
                set.add(i);
        });
        t2.start();

        t1.join();
        t2.join();

        var list = new ArrayList<>();
        set.forEach(list::add);

        if((long) list.size() == SIZE)
            System.out.println("Only distinct elements");
        else
            System.out.println("Not only distinct elements");
    }

}
