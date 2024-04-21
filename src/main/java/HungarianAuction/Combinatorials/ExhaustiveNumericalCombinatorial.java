package HungarianAuction.Combinatorials;

public class ExhaustiveNumericalCombinatorial {
    public int getElementsCount() {
        return elements;
    }

    private final int elements;

    public int getCurrentCombinationSize() {
        return currentCombinationSize;
    }

    private int currentCombinationSize;
    private IterativeNumericalCombinatorial currentCombinatorial;
    public ExhaustiveNumericalCombinatorial(int elements) {
        this.elements = elements;
        currentCombinationSize = 1;
        currentCombinatorial = new IterativeNumericalCombinatorial(elements, currentCombinationSize);
    }

    public ExhaustiveNumericalCombinatorial(int elements, int startingCombinationSize) {
        this.elements = elements;
                this.currentCombinationSize = startingCombinationSize;
                currentCombinatorial = new IterativeNumericalCombinatorial(elements, currentCombinationSize);
    }

    public boolean hasNext() {
        return currentCombinationSize <= elements;
    }

    public int[] getNext() {
        if (currentCombinatorial.hasNext()) {
            return currentCombinatorial.getNext();
        } else if (this.hasNext()){
            currentCombinationSize++;
            currentCombinatorial = new IterativeNumericalCombinatorial(elements, currentCombinationSize);
            return currentCombinatorial.getNext();
        } else return currentCombinatorial.getNext();
    }


}
