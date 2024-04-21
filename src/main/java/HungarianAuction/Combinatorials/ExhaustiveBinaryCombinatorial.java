package HungarianAuction.Combinatorials;

public class ExhaustiveBinaryCombinatorial {

    private ExhaustiveNumericalCombinatorial numericalGenerator = null;
    private final int numberOfBits;
    public ExhaustiveBinaryCombinatorial(int numberOfBits) {
        this.numberOfBits = numberOfBits;
    }

    public boolean hasNext() {
        if (numericalGenerator==null) return true;
        return numericalGenerator.hasNext();
    }

    public long getNext() {
        if (!hasNext()) return -1;
        long nextCombinationBinary = 0;
        if (numericalGenerator == null) {
            numericalGenerator = new ExhaustiveNumericalCombinatorial(this.numberOfBits);
            return nextCombinationBinary;
        }
        int[] next = numericalGenerator.getNext();
        for (int i : next) {
            nextCombinationBinary = nextCombinationBinary | ((long) 1 << i);
        }
        return nextCombinationBinary;
    }
}
