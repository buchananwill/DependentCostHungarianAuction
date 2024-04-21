package HungarianAuction.Combinatorials;

public class IterativeNumericalCombinatorial {
        private int[] currentCombination;
        private int currentElementToIncrement;
        private final int elements;
        private final int combinationSize;

    public IterativeNumericalCombinatorial(int n, int r) {
        elements = n;
        currentElementToIncrement = (r-1); // Start by incrementing the right-most element.
        combinationSize = r;

    }

    public boolean hasNext() {
        if (currentCombination==null) return true;
        return nextElementToIncrement() >= 0;
    }


    private int nextElementToIncrement() {
        int r = currentCombination.length;
        int i = r-1;
        while (i >= 0 && currentCombination[i] == elements - r + i) {
            i--;
        }

        // All combinations have been generated
        if (i < 0) {
            return -1;
        }

        return i;
    }

    public int[] getNext() {
        if (currentCombination==null) {
            currentCombination = new int[combinationSize];

            // Initialize first combination
            for (int i = 0; i < combinationSize; i++) {
                currentCombination[i] = i;
            }

            return currentCombination.clone();
        } else if (hasNext()){
            // Update the pointer for next time.
            currentElementToIncrement = nextElementToIncrement();
        } else {
        // Do no maths if we've run out of combinations, but still return the array to avoid exceptions.
            return currentCombination.clone();
        }


        // Then modify our state ready for the next call.


        // Increment the rightmost element and reset subsequent elements
        currentCombination[currentElementToIncrement]++;
        for (int j = currentElementToIncrement + 1; j < currentCombination.length; j++) {
            currentCombination[j] = currentCombination[j - 1] + 1;
        }



        // Return the old array that we cached.
        return currentCombination.clone();

    }
}
