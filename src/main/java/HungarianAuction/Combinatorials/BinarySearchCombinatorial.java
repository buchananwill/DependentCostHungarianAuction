package HungarianAuction.Combinatorials;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * NOTE: This implementation skips over the case of choosing no elements.
 */
public class BinarySearchCombinatorial<T> {
    protected final List<T> elements;

    protected final ExhaustiveBinaryCombinatorial combinatorialGenerator;

    public BinarySearchCombinatorial(Set<T> elements) {
        this.elements = List.copyOf(elements);
        combinatorialGenerator = new ExhaustiveBinaryCombinatorial(elements.size());
    }


    public boolean hasNext() {
        return combinatorialGenerator.hasNext();
    }

    @NotNull
    public Set<T> next() {
        if (!hasNext()) return new HashSet<>();
        long nextCombinationBinary = combinatorialGenerator.getNext();
        return convertBinaryToSet(nextCombinationBinary);
    }

    @NotNull
    protected Set<T> convertBinaryToSet(long nextCombinationBinary) {
        Set<T> nextCombination = new HashSet<>();
        for (int include = 0; include < elements.size(); include++) {
            if ((nextCombinationBinary & (1L << include)) != 0) nextCombination.add(elements.get(include));
        }
        return nextCombination;
    }

    public Set<T> getElementSet() {
        return Set.copyOf(elements);
    }


}
