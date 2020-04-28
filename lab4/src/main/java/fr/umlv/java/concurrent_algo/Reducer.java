package fr.umlv.java.concurrent_algo;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.function.BinaryOperator;
import java.util.function.IntBinaryOperator;

public class Reducer {
    public static int sum(int[] array) {
        /*
         * var sum = 0;
         * for(var value: array)
         *      sum += value;
         * return sum;
         */
        return reduce(array, 0, Integer::sum);
    }

    public static int parallelSum(int[] array) {
        return parallelReduceWithStream(array, 0, Integer::sum);
    }

    public static int max(int[] array) {
        /*
         * var max = Integer.MIN_VALUE;
         * for(var value: array)
         *     max = Math.max(max,value);
         * return max;
         */
        return reduce(array, Integer.MIN_VALUE, Math::max);
    }

    public static int reduce(int[] array, int initial, IntBinaryOperator op) {
        var acc = initial;
        for (var value : array) {
            acc = op.applyAsInt(acc, value);
        }
        return acc;
    }

    public static int reduceWithStream(int[] array, int initial, IntBinaryOperator op) {
        return Arrays.stream(array).reduce(initial, op);
    }

    public static int parallelReduceWithStream(int[] array, int initial, IntBinaryOperator op) {
        return Arrays.stream(array).parallel().reduce(initial, op);
    }

    public static int parallelReduceWithForkJoin (int[] array, int initial, IntBinaryOperator op) {
        var pool = ForkJoinPool.commonPool();
        return pool.invoke(new TaskRecur(array, 0, array.length, op, initial));
    }

    static class TaskRecur extends RecursiveTask<Integer> {

        final int[] data;
        final int start, end, initial;
        final IntBinaryOperator op;

        TaskRecur(int[] data, int start, int end, IntBinaryOperator op, int initial){
            this.data = data;
            this.start = start;
            this.end = end;
            this.initial = initial;
            this.op = op;
        }

        @Override
        protected Integer compute() {
            int sum = 0;
            if (end - start < 1024)
                return Arrays.stream(data, start, end).reduce(initial, op);
            var middle = (start + end) / 2;
            TaskRecur part1 = new TaskRecur(data, start, middle, op, initial), part2 = new TaskRecur(data, middle, end, op, initial);
            part1.fork();
            int r2 = part2.compute(), r1 = part1.join();
            return op.applyAsInt(r1, r2);
        }

    }

    public static void main (String[] args) {
        var random = new Random(0);
        int[] array = random.ints(1_000_000, 0, 1_000).toArray();
        System.out.println(sum(array));
        System.out.println(parallelSum(array));
        System.out.println(parallelReduceWithForkJoin(array, Integer.MIN_VALUE, Integer::sum));
        System.out.println(parallelReduceWithForkJoin(array, Integer.MIN_VALUE, Integer::max));
    }
}
