package fr.umlv.structconc;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import static jdk.incubator.vector.VectorOperators.ADD;

public class Vectorized {

    private static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;

    public static int sumLoop(int[] array) {
        var sum = 0;
        for(var value: array) {
            sum += value;
        }
        return sum;
    }

    public static int sumReduceLane(int[] array){
        int sum = 0, i = 0;
        var limit = array.length - (array.length % SPECIES.length());
        for (; i < limit; i+= SPECIES.length()){
            var tmp = IntVector.fromArray(SPECIES, array, i);
            sum += tmp.reduceLanes(ADD);
        }
        for (; i < array.length; i++)
            sum += array[i];
        return sum;
    }

    public static int sumLanewise(int[] array){
        IntVector vec = IntVector.zero(SPECIES);
        int i = 0;
        var limit = array.length - (array.length % SPECIES.length());
        for (; i < limit; i+= SPECIES.length())
            vec = vec.lanewise(ADD, IntVector.fromArray(SPECIES, array, i));
        var sum = vec.reduceLanes(ADD);
        for (; i < array.length; i++)
            sum += array[i];
        return sum;
    }

}
