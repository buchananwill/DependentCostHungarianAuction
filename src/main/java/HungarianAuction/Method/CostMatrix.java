package HungarianAuction.Method;

import HungarianAuction.TaskElements.DoNothingTask;
import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.TaskElements.TaskRequest;
import HungarianAuction.WorkerElements.WorkerGrouping;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <ul>
 * <li>
 *      Has a single internal state: no parallel or historic data..
 * </li>
 * <li>
 *      Carries out one set of assignment decisions on the basis of the supplied data.
 * </li>
 * </ul>
 */
public class CostMatrix<T extends TaskSource<T, W>, W extends WorkerGrouping<T, W>> implements CostMatrixInterface<T, W> {
    public static final int NUMBER_OF_ARRAY_DIMENSIONS = 2;
    public static final int NEGATIVE_SEARCH_RESULT = -1;

    public static final int[] NEGATIVE_SEARCH_LOCATION = new int[]{NEGATIVE_SEARCH_RESULT, NEGATIVE_SEARCH_RESULT};
    public static final int ROW_INDEX = 0;
    public static final int COLUMN_INDEX = 1;
    private static final AtomicInteger matrixCounter = new AtomicInteger();
    private static final double NO_ASSIGNMENTS_MADE = -1;
    private final int id;
    private final double[][] costsWorkerTask;
    private final List<int[]> zeroLocations = new ArrayList<>();
    private final List<WorkerGrouping<T,W>> workers;
    private final List<TaskRequest<T,W>> tasks;
    private final boolean[] rowsCrossed;
    private final boolean[] columnsCrossed;

    private final boolean[][] starredValues;
    private final boolean[][] primedValues;

    private final int size;
    private final Set<Assignment<T,W>> assignedTasks = new HashSet<>();

    /**
     * Part of the <b>Init Matrix</b> process.
     * */
    public CostMatrix(List<WorkerGrouping<T, W>> workerGroupings, List<TaskRequest<T, W>> tasks) {
        if (workerGroupings.size() != tasks.size())
            throw new IllegalArgumentException(workerGroupings.size() + " worker groupings must equal " + tasks.size() + " tasks.");

        this.size = workerGroupings.size();

        this.workers = List.copyOf(workerGroupings);
        this.tasks = List.copyOf(tasks);

        costsWorkerTask = new double[size][size];
        rowsCrossed = new boolean[size];
        columnsCrossed = new boolean[size];
        starredValues = new boolean[size][size];
        primedValues = new boolean[size][size];

        for (int array = 0; array < size; array++) {
            double[] row = costsWorkerTask[array];
            Arrays.fill(row, Double.POSITIVE_INFINITY);
        }
        workers.forEach(this::addCostsToMatrix);
        id = matrixCounter.getAndIncrement();
    }

    private void uncrossAllRowsAndColumns() {
        Arrays.fill(rowsCrossed, false);
        Arrays.fill(columnsCrossed, false);
    }

    /**
     * Part of the <b>Init Matrix</b> process.
     * */
    private void addCostsToMatrix(WorkerGrouping<T,W> workerGrouping) {
        for (TaskRequest<T,W> task :
                tasks) {
            int workerIndex = workers.indexOf(workerGrouping);
            int taskIndex = tasks.indexOf(task);
            double cost = task.getCost(workerGrouping);
            costsWorkerTask[workerIndex][taskIndex] = cost; // INITIALIZATION
        }
    }

    /**
     * Part of the <b>Iterate Matrix</b> process.
     * */
    private CostMatrix(double[][] costsWorkerTask, List<WorkerGrouping<T, W>> workers, List<TaskRequest<T, W>> tasks, boolean[] rowsCrossed, boolean[] columnsCrossed, boolean[][] starredValues, boolean[][] primedValues, int size) {
        this.id = matrixCounter.getAndIncrement();
        this.costsWorkerTask = costsWorkerTask;
        this.workers = workers;
        this.tasks = tasks;
        this.rowsCrossed = rowsCrossed;
        this.columnsCrossed = columnsCrossed;
        this.starredValues = starredValues;
        this.primedValues = primedValues;
        this.size = size;
    }

    /**
     * Escape hatch for unsolvable matrices.
     * */
    @Override
    public boolean checkRowsAndColumnsAreViable() {
        int arraysToCheck = this.size * 2;
        int[] viableDimensions = new int[arraysToCheck];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                if (costsWorkerTask[row][column] == Double.POSITIVE_INFINITY) {
                    if (++viableDimensions[row] == size) return false;
                    if (++viableDimensions[size + column] == size) return false;
                }
            }
        }

        return true;
    }

    @Override
    public int getId() {
        return id;
    }

    /**
     * Outcome retrieval
     * */
    @Override
    public double getAssignmentCost(Assignment<T,W> assignment) {
        int workerIndex = workers.indexOf(assignment.workerGrouping());
        int taskIndex = tasks.indexOf(assignment.task());
        if (workerIndex < 0 || taskIndex < 0) return -1D;
        return costsWorkerTask[workerIndex][taskIndex];
    }

    /**
     * Part of the <b>Iterate Matrix</b> process.
     * */
    @Override
    public CostMatrixInterface<T,W> cloneMatrix() {
        double[][] clonedCostsWorkerTasks = new double[this.size][this.size];
        for (int i = 0; i < this.costsWorkerTask.length; i++) {
            clonedCostsWorkerTasks[i] = costsWorkerTask[i].clone();
        }
        boolean[][] clonedStarredValues = new boolean[this.size][this.size];
        for (int i = 0; i < this.starredValues.length; i++) {
            clonedStarredValues[i] = this.starredValues[i].clone();
        }
        boolean[][] clonedPrimedValues = new boolean[this.size][this.size];
        for (int i = 0; i < this.primedValues.length; i++) {
            clonedPrimedValues[i] = this.primedValues[i].clone();
        }

        return new CostMatrix<>(
                clonedCostsWorkerTasks,
                this.workers,
                this.tasks,
                this.rowsCrossed.clone(),
                this.columnsCrossed.clone(),
                clonedStarredValues,
                clonedPrimedValues,
                this.size
        );
    }

    /**
     * Part of the <b>Init Matrix</b> process.
     * */
    @Override
    public void computeMarginalTaskCosts() {
//        For each COLUMN and ROW:
//              - find the minimum value
//              - subtract it from all the values


        normaliseDimension(Dimension.TASK);
        normaliseDimension(Dimension.WORKER);
        cacheZeroLocations();
    }

    /**
     * Part of <b>Loop 2</b>
     * */
    private void cacheZeroLocations() {
        for (int workerRow = 0; workerRow < size; workerRow++) {
            for (int taskColumn = 0; taskColumn < size; taskColumn++) {
                if (costsWorkerTask[workerRow][taskColumn] == 0) {
                    zeroLocations.add(new int[]{workerRow, taskColumn});
                }
            }
        }
    }

    /**
     * Part of the <b>Init Matrix</b> process.
     * */
    private void normaliseDimension(Dimension normalisedAxis) {
        int worker = 0;
        int task = 0;

        for (int outer = 0; outer < this.size; outer++) {
            double lowestValueChecked = Double.MAX_VALUE;
            if (normalisedAxis == Dimension.TASK) task = outer;
            else worker = outer;
            for (int inner = 0; inner < this.size; inner++) {
                if (normalisedAxis == Dimension.TASK) worker = inner;
                else task = inner;
                double nextValue = costsWorkerTask[worker][task];
                lowestValueChecked = Math.min(lowestValueChecked, nextValue);
            }
            for (int inner = 0; inner < this.size; inner++) {
                if (normalisedAxis == Dimension.TASK) worker = inner;
                else task = inner;
                double absoluteValue = costsWorkerTask[worker][task];
                costsWorkerTask[worker][task] = absoluteValue - lowestValueChecked; // MODIFICATION

            }
        }

    }

    /**
     * <b>Loop 2: </b> crossing out all the zero values.
     * <p><b>True</b> means the matrix is solved.</p>
     * <p><b>False</b> means the matrix needs to run Loop 1 to modify its values.</p>
     * */
    @Override
    public boolean applyMinimumCrossings() {
        if (zeroLocations.isEmpty()) {
            cacheZeroLocations();
        }
        uncrossAllRowsAndColumns();
        starSingleZeroColumns();

        if (countStarredValues() == size) {
            confirmStarredAssignments();
            return true;
        }

        boolean coveringAllZeros = true;
        int[] uncrossedZero = new int[NUMBER_OF_ARRAY_DIMENSIONS];
        while (coveringAllZeros) {
            crossStarredColumns();
            boolean columnSwappingLoop = true;
            while (columnSwappingLoop) {
                // Todo make this function more efficient
                uncrossedZero = findUnCrossedZero();
                if (uncrossedZero[ROW_INDEX] == NEGATIVE_SEARCH_RESULT) {
                    columnSwappingLoop = false;
                    coveringAllZeros = false;
                } else
                    columnSwappingLoop = uncrossColumnAndCrossRowInstead(uncrossedZero);
            }
            if (coveringAllZeros) {
                Deque<int[]> primeStarWalk = findPrimeStarWalk(uncrossedZero);
                applyPrimeStarWalk(primeStarWalk);
                uncrossAllRowsAndColumns();
                unPrimeAllValues();
            }
        }
        if (countStarredValues() == size) {
            confirmStarredAssignments();
            return true;
        }
        return false;
    }

    /**
     * <h3>Part of <b>Loop 1</b>.</h3>
     * <p style="padding: 4px">Limiting search space to the cached zero values provides a marginal performance gain.</p>
     * <p style="padding: 4px">The original method can be found in previous versions.</p>
     * */
    private void starSingleZeroColumns() {
        int[][] ints = new int[this.size][];

        zeroLocations.forEach(zero -> {
            int columnIndex = zero[COLUMN_INDEX];
            if (ints[columnIndex] == null) {
                ints[columnIndex] = zero;
            } else if (ints[columnIndex] == NEGATIVE_SEARCH_LOCATION) {

            } else {
                ints[columnIndex] = NEGATIVE_SEARCH_LOCATION;
            }
        });
        for (int[] rowColumnIndex : ints) {
            if (rowColumnIndex == NEGATIVE_SEARCH_LOCATION) continue;
            if (rowColumnIndex == null) continue;
            int rowIndex = rowColumnIndex[ROW_INDEX];
            int columnIndex = rowColumnIndex[COLUMN_INDEX];
            if (checkForStarInRow(rowIndex) == NEGATIVE_SEARCH_RESULT) {
                starredValues[rowIndex][columnIndex] = true;
            }
        }

    }

    /**
     * Part of <b>Loop 1</b>.
     * */
    private int countStarredValues() {
        int starCount = 0;
        for (int row = 0; row < size; row++) {
            int column = checkForStarInRow(row);
            if (column != NEGATIVE_SEARCH_RESULT) starCount++;
        }
        return starCount;
    }

    /**
     * Last operation in <b>Loop 1</b>, after the matrix is solved.
     * */
    private void confirmStarredAssignments() {
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                if (starredValues[row][column]) {
                    TaskRequest<T,W> task = tasks.get(column);
                    if (task instanceof DoNothingTask<T,W>) continue;
                    WorkerGrouping<T,W> worker = workers.get(row);
                    assignedTasks.add(new Assignment<>(task, worker));
                }
            }
        }
    }

    /**
     * Part of the Intersection between <b>Loop 1</b> and <b>Loop 2</b>.
     * */
    private void crossStarredColumns() {
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                if (starredValues[row][column]) columnsCrossed[column] = true;
            }
        }
    }

    /**
     * Part of the Intersection between <b>Loop 1</b> and <b>Loop 2</b>.
     * */
    private int[] findUnCrossedZero() {
        for (int[] zeroLocation : zeroLocations) {
            int workerRow = zeroLocation[Dimension.WORKER.ordinal()];
            int taskColumn = zeroLocation[Dimension.TASK.ordinal()];
            if (!rowsCrossed[workerRow] && !columnsCrossed[taskColumn]) {
                primedValues[workerRow][taskColumn] = true;
                return zeroLocation;
            }
        }

        return new int[]{NEGATIVE_SEARCH_RESULT, NEGATIVE_SEARCH_RESULT};
    }

    /**
     * <b>Loop 2.1</b>
     * */
    private boolean uncrossColumnAndCrossRowInstead(int[] primedZero) {
        int row = primedZero[ROW_INDEX];
        int column = checkForStarInRow(row);
        if (column != NEGATIVE_SEARCH_RESULT) {
            columnsCrossed[column] = false;
            rowsCrossed[row] = true;
            return true;
        } else return false;
    }

    /**
     * <b>Loop 2.2</b>
     * */
    private Deque<int[]> findPrimeStarWalk(int[] uncrossedZero) {
        Deque<int[]> walkList = new ArrayDeque<>();
        walkList.push(uncrossedZero);
        boolean continuingWalk = true;
        while (continuingWalk) {
            int[] primedZero = walkList.peek();
            if (primedZero == null)
                throw new IllegalArgumentException("Uncrossed zero cannot be null." + this);
            int[] nextStarredZero = findStarInColumn(primedZero);
            if (nextStarredZero[ROW_INDEX] == NEGATIVE_SEARCH_RESULT) continuingWalk = false;
            else {
                walkList.push(nextStarredZero);
                int[] nextPrimeZero = findPrimeInRow(nextStarredZero);
                walkList.push(nextPrimeZero);
            }
        }
        return walkList;
    }

    /**
     * Last part of <b>Loop 2</b>.
     * */
    private void applyPrimeStarWalk(Deque<int[]> primeStarWalk) {
        boolean prime = true;
        while (!primeStarWalk.isEmpty()) {
            int[] nextValue = primeStarWalk.poll();
            int row = nextValue[ROW_INDEX];
            int column = nextValue[COLUMN_INDEX];
            if (prime) {
                primedValues[row][column] = false;
                starredValues[row][column] = true;
                prime = false;
            } else {
                starredValues[row][column] = false;
                prime = true;
            }
        }
    }

    /**
     * Clean up after <b>Loop 2</b>
     * */
    private void unPrimeAllValues() {
        for (int row = 0; row < size; row++) {
            Arrays.fill(primedValues[row], false);
        }
    }

    /**
     * The intersection of <b>Loop 2.1</b> and <b>Loop 2.2</b>.
     * */
    private int checkForStarInRow(int row) {
        for (int column = 0; column < size; column++) {
            if (starredValues[row][column]) return column;
        }
        return NEGATIVE_SEARCH_RESULT;
    }

    /**
     * <b>Loop 2.2a</b>
     * */
    private int[] findStarInColumn(int[] primedColumn) {
        int column = primedColumn[COLUMN_INDEX];
        for (int row = 0; row < size; row++) {
            if (starredValues[row][column]) {
                return new int[]{row, column};
            }
        }
        return new int[]{NEGATIVE_SEARCH_RESULT, NEGATIVE_SEARCH_RESULT};
    }

    /**
     * <b>Loop 2.2b</b>
     * */
    private int[] findPrimeInRow(int[] starredZero) {
        int row = starredZero[ROW_INDEX];
        for (int column = 0; column < size; column++) {
            if (primedValues[row][column]) {
                return new int[]{row, column};
            }
        }
        return new int[]{NEGATIVE_SEARCH_RESULT, NEGATIVE_SEARCH_RESULT};
    }

    /**
     * <b>Loop 1</b>: modifying the costs towards a global optimum.
     * */
    @Override
    public boolean modifyCostsByLowestUncrossedValue() {
        zeroLocations.clear();
        double lowestUncrossedValue = Double.MAX_VALUE;
        for (int worker = 0; worker < this.size; worker++) {
            for (int task = 0; task < this.size; task++) {
                if (!rowsCrossed[worker] && !columnsCrossed[task])
                    lowestUncrossedValue = Math.min(lowestUncrossedValue, costsWorkerTask[worker][task]);
            }
        }
        boolean anyFiniteValuesUncrossed = false;
        for (int worker = 0; worker < this.size; worker++) {
            for (int task = 0; task < this.size; task++) {
                if (costsWorkerTask[worker][task] == Double.POSITIVE_INFINITY)
                    continue;
                int crossingsCoefficient = -1;
                if (rowsCrossed[worker]) crossingsCoefficient += 1;
                if (columnsCrossed[task]) crossingsCoefficient += 1;
                anyFiniteValuesUncrossed = anyFiniteValuesUncrossed || crossingsCoefficient != 0;
                double modifiedValue = costsWorkerTask[worker][task] + (crossingsCoefficient * lowestUncrossedValue);
                costsWorkerTask[worker][task] = modifiedValue; // MODIFICATION
                if (modifiedValue == 0) {
                    zeroLocations.add(new int[]{worker, task});
                }
            }
        }

        // TODO This line changed
        return lowestUncrossedValue != Double.MAX_VALUE && anyFiniteValuesUncrossed;
    }

    /**
     * Outcome retrieval
     * */
    @Override
    public Set<Assignment<T,W>> getAssignments() {
        return Collections.unmodifiableSet(assignedTasks);
    }

    /**
     * Part of the <b>Iterate Matrix</b> process.
     * */
    @Override
    public void overrideAssignmentCost(Assignment<T,W> invalidAssignment, double cost) {
        zeroLocations.clear();
        int taskIndex = tasks.indexOf(invalidAssignment.task());
        int workerIndex = workers.indexOf(invalidAssignment.workerGrouping());
        if (taskIndex < 0 || workerIndex < 0)
            return;
        costsWorkerTask[workerIndex][taskIndex] = cost; // MODIFICATION
    }

    /**
     * Diagnostic method to check the combinatorial iteration is generating different, progressively less optimal outcomes.
     * */
    @Override
    public double getSumOfAssignments() {
        if (assignedTasks.isEmpty()) return NO_ASSIGNMENTS_MADE;
        double sum = 0.0;
        for (Assignment<T,W> assignedTask : assignedTasks) {
            double trueAssignmentCost = getTrueAssignmentCost(assignedTask);
            sum += trueAssignmentCost;
        }
        return sum;
    }

    @Override
    public boolean anyNaN() {
        for (double[] doubles : costsWorkerTask) {
            for (double aDouble : doubles) {
                if (Double.isNaN(aDouble)) return true;
            }
        }
        return false;
    }

    /**
     * Part of the diagnostic method getSumOfAssignments()
     * */
    private double getTrueAssignmentCost(Assignment<T,W> assignment) {

        // TODO This line changed
        return assignment.task().getCost(assignment.workerGrouping());
    }

    enum Dimension {
        WORKER,
        TASK
    }
}
