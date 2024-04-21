package HungarianAuction.Combinatorials;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * NOTE: This implementation skips over the case of choosing no elements.
 * Given a set of elements of type T, this algorithm will generate every subset of elements of minimum size 1.
 * To designate a particular order for the sets to be generated, pass a TreeSet as the constructor argument.
 */
public class BinSearchCombAdvanced<T> extends BinarySearchCombinatorial<T>{

    private final Set<Long> avoidSet = new HashSet<>();

    public BinSearchCombAdvanced(Set<T> elements) {
        super(elements);
    }


    @Override
    @NotNull
    public Set<T> next() {
        if (!hasNext()) return new HashSet<>();
        long nextCombinationBinary = combinatorialGenerator.getNext();
        while (nextContainsIgnoreSubset(nextCombinationBinary)) {
            if (!hasNext()) return new HashSet<>();
            nextCombinationBinary = combinatorialGenerator.getNext();
        }

        return this.convertBinaryToSet(nextCombinationBinary);
    }

    private boolean nextContainsIgnoreSubset(long nextCombinationBinary) {
        for (Long subsetToAvoid : avoidSet) {
            if ((nextCombinationBinary & subsetToAvoid) == subsetToAvoid) return true;
        }
        return false;
    }

    public void addAvoidSet(Set<T> elementSetToAvoid) {
        long binarySetExpression = 0;
        for (T element : elementSetToAvoid) {
            long indexOf = elements.indexOf(element);
            if (indexOf < 0) throw new IllegalArgumentException("Element: " + element + " not contained in this combinatorial.");
            binarySetExpression = binarySetExpression | (1L << indexOf);
        }
        avoidSet.add(binarySetExpression);
    }


}
