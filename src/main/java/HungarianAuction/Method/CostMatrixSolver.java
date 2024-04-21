package HungarianAuction.Method;

import HungarianAuction.TaskElements.DoNothingTask;
import HungarianAuction.TaskElements.TaskCost;
import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.Combinatorials.BinSearchCombAdvanced;
import HungarianAuction.TaskElements.TaskRequest;
import HungarianAuction.WorkerElements.WorkerGrouping;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <ul>
 * <li>
 *      Responsible for the decision of which worker to allocate to which task, on the basis of their stated fee.
 * </li>
 * <li>
 *      Designed to retain memory of:
 *      <ol>
 *      <li>
 *          Previous allocation decisions that may have proven to be unviable
 *      </li>
 *      <li>
 *          Which combinatorial sets have been disallowed during this search.
 *      </li>
 *      </ol>
 * </li>
 * <li>
 *      Continues its search until it can return a viable assignment or exhausts the entire combinatorial space.
 * </li>
 * </ul>
 */
public class CostMatrixSolver<T extends TaskSource<T, W>, W extends WorkerGrouping<T, W>> {

    private final CostMatrixInterface<T, W> templateCostMatrix;
    private final List<WorkerGrouping<T, W>> workers;
    private final List<TaskRequest<T, W>> taskRequests;
    private final int taskSize;
    private final Set<WorkerGrouping<T, W>> unassignedWorkerGroupings = new HashSet<>();
    private final Map<TaskRequest<T, W>, Set<WorkerGrouping<T, W>>> viableAssignmentMap = new HashMap<>();

    private final Set<TaskRequest<T, W>> unassignedTasks = new HashSet<>();
    private final Map<TaskRequest<T, W>, WorkerGrouping<T, W>> uniqueViableAssignments = new HashMap<>();

    /**
     * The output of the CostMatrixSolver: its main externally-readable field.
     */
    private final Set<Assignment<T, W>> confirmedAssignmentSet = new HashSet<>();
    private final Set<Set<Assignment<T, W>>> failedAssignmentBranches;
    int solvedMatrices = 0;
    private CostMatrixInterface<T, W> activeCostMatrix = null;
    private BinSearchCombAdvanced<Assignment<T, W>> activeCombinatorial = null;
    private Viability viability;


    /**
     * <h1> CostMatrixSolver</h1>
     * <p style="padding:12px">A stateful wrapper for solving an assignment batch via the Hungarian Method. Contains several enhancements to allow the method to be applied in a wider range of scenarios.</p>
     * <h2>Enhancements</h2>
     * <ul style="padding-left:8px">
     *     <li><em>Automatic normalisation of the matrix to square, via DoNothingTasks which mirror the real tasks received.</em></li>
     *     <li><strong>Does not allow fewer workers than tasks.</strong></li>
     *     <li><em>Viability check that allows the inclusion of infinity values in the costs. Triggers a handled failure.</em></li>
     *     <li><em>Can be solved iteratively, leading to progressively less optimal outcomes. </em></li>
     *     <li><strong>When the base assignment costs have changed externally, the CostMatrixSolver must be discarded.</strong></li>
     * </ul>
     */
    public CostMatrixSolver(List<WorkerGrouping<T, W>> workers, List<TaskRequest<T, W>> taskRequests, int taskSize) {
        this.workers = Collections.unmodifiableList(workers);
        this.taskRequests = Collections.unmodifiableList(taskRequests);
        this.taskSize = taskSize;
        this.failedAssignmentBranches = new TreeSet<>(getAssignmentSetComparator());
        unassignedWorkerGroupings.addAll(workers);
        unassignedTasks.addAll(taskRequests);

        // Call the data from the external interfaces.
        computeBaseCosts();

        // Build a template cost matrix with this data.
        this.templateCostMatrix = computeCostMatrix();

        // Clone the first matrix to solve from the template, if the viability checks pass.
        if (this.viability == Viability.LIVE && templateCostMatrix != null) {
            this.activeCostMatrix = templateCostMatrix.cloneMatrix();
        }
    }

    private Comparator<Set<Assignment<T, W>>> getAssignmentSetComparator() {
        return (set1, set2) -> {
            int sizeComparison = set1.size() - set2.size();
            return sizeComparison != 0 ? sizeComparison : set1.hashCode() - set2.hashCode();
        };
    }

    /**
     * <h4>@Initialization</h4>
     * Calls in the data from the external interface.
     */
    private void computeBaseCosts() {
        for (TaskRequest<T, W> taskRequest : unassignedTasks) {
            taskRequest.tenderGroupingCosts(unassignedWorkerGroupings);
            this.mapViableAssignments(taskRequest);
        }

        if (viableAssignmentMap.values().stream().anyMatch(Set::isEmpty)) {
            this.viability = Viability.BASE_COSTS_CONTAINED_INFINITY_COLUMN;
        }  else {
            this.viability = Viability.LIVE;
        }

    }



    /**
     * <h4>@Initialization</h4>
     * Computes an initial matrix from which solutions are derived.
     */
    private CostMatrixInterface<T, W> computeCostMatrix() {


        // Simplify the problem by removing any tasks with only a single viable worker assignment.
        removeSingleWorkerGroupingTasks();
        if (unassignedTasks.isEmpty()) {
            this.viability = Viability.REAL_TASKS_ALLOCATED;
            return new CostMatrix<>(new ArrayList<>(), new ArrayList<>());
        }

        // Normalise the matrix to a square
        addDoNothingTasks();
        List<WorkerGrouping<T, W>> workerGroupings = new ArrayList<>(this.unassignedWorkerGroupings);
        List<TaskRequest<T, W>> taskRequestList = new ArrayList<>(this.unassignedTasks);

        // Failsafe check for size-related bugs.
        if (workerGroupings.size() != taskRequestList.size()) {
            this.viability = Viability.UNKNOWN_ERROR;
            return null;
        }

        // Initialise the Matrix
        CostMatrixInterface<T, W> costMatrix = new CostMatrix<>(workerGroupings, taskRequestList);
        costMatrix.computeMarginalTaskCosts();

        // Check for NaNs:
        if (costMatrix.anyNaN()) {
            this.viability = Viability.UNKNOWN_ERROR;
            return null;
        }


        return costMatrix;
    }

    /**
     * <h4>@Initialization</h4>
     * Builds a record of the tasks that have at least one finite worker cost.
     */
    private void mapViableAssignments(TaskRequest<T, W> taskRequest) {
        Map<WorkerGrouping<T, W>, TaskCost> workerGroupingMap = taskRequest.getWorkerGroupingMap();
        Set<WorkerGrouping<T, W>> workerGroupings = viableAssignmentMap.computeIfAbsent(taskRequest, k -> new HashSet<>());
        workerGroupingMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue().getFinalValue() != Double.POSITIVE_INFINITY)
                .forEach(entry -> workerGroupings.add(entry.getKey()));
    }

    /**
     * <h4>@Initialization</h4>
     * Simplifies the problem by eliminating tasks with only a unique viable assignment.
     */
    private void removeSingleWorkerGroupingTasks() {
        boolean checkForSingleWorkerGroupings = true;
        while (checkForSingleWorkerGroupings) {
            checkForSingleWorkerGroupings = false;
            ArrayList<TaskRequest<T, W>> taskCache = new ArrayList<>(unassignedTasks);
            for (TaskRequest<T, W> task : taskCache) {
                if (task.countAvailableWorkerGroupings() == 1) {
                    assignUniqueWorkerGroupingBid(task);
                    checkForSingleWorkerGroupings = true;
                }
            }
        }
    }

    /**
     * Normalises the shape of the CostMatrix to ensure it is a square.
     */
    private void addDoNothingTasks() {
        int doNothingTaskCount = unassignedWorkerGroupings.size() - unassignedTasks.size();
        List<DoNothingTask<T, W>> extraTasks = new ArrayList<>();

        for (int i = 0; i < doNothingTaskCount; i++) {
            extraTasks.add(new DoNothingTask<>(taskSize));
        }
        extraTasks.forEach(t -> t.tenderGroupingCosts(unassignedWorkerGroupings));
        unassignedTasks.addAll(extraTasks);

    }

    /**
     * <h4>@Initialization</h4>
     * Caches the uniquely viable assignments found in the previous method.
     */
    private void assignUniqueWorkerGroupingBid(TaskRequest<T, W> task) {
        Map<WorkerGrouping<T, W>, TaskCost> workerMap = task.getWorkerGroupingMap();
        WorkerGrouping<T, W> uniquelyViableWorkerGrouping = workerMap.keySet().stream().findFirst().orElseThrow();

        unassignedTasks.remove(task);
        unassignedWorkerGroupings.remove(uniquelyViableWorkerGrouping);

        for (TaskRequest<T, W> taskRequest : unassignedTasks) {
            taskRequest.removeWorkerGroupingOffer(uniquelyViableWorkerGrouping);
        }
        uniqueViableAssignments.put(task, uniquelyViableWorkerGrouping);
    }

    /**
     * <h3>Main Public Interface</h3>
     * <h4>@Solving</h4>
     * <p style="padding: 8px">
     * This method triggers the matrix solving sequence.<br>
     * It returns a boolean for the <strong>success</strong> or <strong>failure</strong> of the overall assignment.
     * </p>
     */
    public boolean applyAlgorithm() {
        boolean outcome = false;

        // MAIN BRANCH 1, OPTION A: There is at least one real task with multiple assignment options.
        if (viability == Viability.LIVE) {
            // SUB BRANCH: First time applying the algorithm.
            if (solvedMatrices == 0 && failedAssignmentBranches.isEmpty()) {
                outcome = solveCostMatrix();
            }
            // SUB BRANCH: The algorithm has previously been applied.
            else {
                outcome = iterateSolution();
            }
            // MAIN BRANCH 1, OPTION B: All the real tasks have only a single viable assignment
        } else if (viability == Viability.REAL_TASKS_ALLOCATED) {
            outcome = true;
        }

        // MAIN BRANCH 2: When we've assigned all the real tasks.
        if (outcome) {
            // Confirm the invariant assignments first.
            if (!uniqueViableAssignments.isEmpty()) {
                uniqueViableAssignments.entrySet().stream()
                        .map(entry -> new Assignment<>(entry.getKey(), entry.getValue()))
                        .forEach(confirmedAssignmentSet::add); // UNIQUE ASSIGNMENT ADDED
            }
            // Confirm the variable assignments next.
            if (activeCostMatrix != null)
                confirmAssignments();
        }
//        Just in case there was something amiss with the proposed solution...
        if (confirmedAssignmentSet.isEmpty()) outcome = false;
        else solvedMatrices++;

        return outcome;
    }

    /**
     * <h4>@Solving</h4>
     */
    private boolean solveCostMatrix() {

        if (activeCostMatrix == null) {
            if (this.viability == Viability.REAL_TASKS_ALLOCATED)
                return true;
            else
                throw new IllegalStateException("No Cost Matrix to solve!" + this);
        }

        boolean possibleSolution = true;
        while (possibleSolution) {
            //        1. Find minimum crossings needed to cross all zeros and compare with size.
//            Do we already have a solved matrix?
            possibleSolution = activeCostMatrix.applyMinimumCrossings();

//            If we seem to have a solved matrix...
            if (possibleSolution) {
                // 4. Check all the assignments are valid as a set.
                boolean foundUnviableAssignments = checkForUnviableAssignments();

                // If there are some, we will need to break this loop and try the iteration of matrix generation.
                // Otherwise, return true, so we can only need to  confirm these assignments.
                return !foundUnviableAssignments;
            }

//        2. Assuming solution not reached, attempt to modify the matrix. If no modification was made, no progress can be made with this matrix.
                possibleSolution = activeCostMatrix.modifyCostsByLowestUncrossedValue();

        }

        return false;
    }

    /**
     * <h4>@Iterating</h4>
     */
    private boolean iterateSolution() {

        // CONDITION: The original set of tasks each had only a single viable assignment, so no iteration is possible
        if (viability == Viability.REAL_TASKS_ALLOCATED) {
            return false;
        }

        while (true) {

            boolean getNextUnviableAssignmentCombination = true;

            // 3. Fetch a combinatorial from the generator, which has to pass several tests.
            while (getNextUnviableAssignmentCombination) {

                // Check the Combinatorial generator can update.
                if (!checkCombinatorialState()) {
                    this.viability = Viability.COMBINATORIAL_SEARCH_EXHAUSTED;
                    return false;
                }

                // Then reset the active matrix.
                resetActiveCostMatrix();

                Set<Assignment<T, W>> nextCombination = this.activeCombinatorial.next();
                if (nextCombination.isEmpty()) {
                    continue;
                }
                Set<Assignment<T, W>> nextAssignmentsToOverride = new HashSet<>(nextCombination);
                nextAssignmentsToOverride.forEach(assignment -> activeCostMatrix.overrideAssignmentCost(assignment, Double.POSITIVE_INFINITY));
                activeCostMatrix.computeMarginalTaskCosts();
                // It also has to still be a viable matrix.
                if (activeCostMatrix.checkRowsAndColumnsAreViable()) {
                    // Then we can solve this matrix.
                    getNextUnviableAssignmentCombination = false;
                } else {
                    // Since the current override combination on its own made the matrix invalid, we can add it to the avoid list.
                    this.activeCombinatorial.addAvoidSet(nextCombination);
                }

            }

            if (solveCostMatrix()) {
                return true;
            }
        }

    }

    /**
     * <h4>@Solving</h4>
     * The final step of solving a matrix.
     */
    private void confirmAssignments() {

        Set<Assignment<T, W>> assignments = activeCostMatrix.getAssignments();
        for (Assignment<T, W> assignment : assignments) {
            TaskRequest<T, W> task = assignment.task();
            WorkerGrouping<T, W> workerGrouping = assignment.workerGrouping();
            // Adding assignments to the confirmed set and checking as we go.
            if (confirmedAssignmentSet.stream().noneMatch(assignmentAlreadyConfirmed -> assignmentAlreadyConfirmed.task().equals(task) || assignmentAlreadyConfirmed.workerGrouping().equals(workerGrouping))) {
                confirmedAssignmentSet.add(assignment); // VARIABLE ASSIGNMENT ADDED
            } else {
                // If we find a duplicate, clear the all assignments so far and abort.
                confirmedAssignmentSet.clear();
                break;
            }
        }
    }

    private boolean checkForUnviableAssignments() {
        Set<Assignment<T, W>> assignments = new HashSet<>(activeCostMatrix.getAssignments());
        return failedAssignmentBranches.contains(assignments);
    }

    /**
     * <h4>@Iterating</h4>
     * <b>Side Effect:</b> the previous assignment set is cached with the failed branches.
     */
    private boolean checkCombinatorialState() {
        if (this.activeCombinatorial == null || !activeCombinatorial.hasNext()) {
            return createCombinatorial();
        } else if (activeCombinatorial.hasNext()) {
            cacheAssignmentsFromActiveMatrix();
            return true;
        } else throw new IllegalStateException("What else could have happened? Combinatorial error.");
    }

    /**
     * <h4>Iterating</h4>
     */
    private void resetActiveCostMatrix() {
        confirmedAssignmentSet.clear();

        unassignedWorkerGroupings.clear();
        unassignedWorkerGroupings.addAll(workers);
        unassignedWorkerGroupings.removeAll(uniqueViableAssignments.values());

        unassignedTasks.clear();
        unassignedTasks.addAll(taskRequests);
        unassignedTasks.removeAll(uniqueViableAssignments.keySet());

        addDoNothingTasks();
        this.activeCostMatrix = this.templateCostMatrix.cloneMatrix();
    }

    /**
     * <h4>@Iterating</h4>
     * Returns false if it was unable to create a new combinatorial - this means we have exhausted this search space.
     */
    private boolean createCombinatorial() {
        // ASSUMPTION: we have solved at least one matrix, and this is still the "active matrix".
        if (activeCostMatrix == null) {
            return false;
        }

        // Only true if we're here after exhausting the active combinatorial.
        if (activeCombinatorial != null) {
            if (!cleanUpLastCombinatorial()) return false;
        }

        // Get the assignments, then set the matrix to null as we know it didn't lead to a viable branch.
        Set<Assignment<T, W>> mostRecentlySucceededAssignments = cacheAssignmentsFromActiveMatrix();
        if (mostRecentlySucceededAssignments == null) return false;
        activeCostMatrix = null;


        // Prepare a ranking for the remaining viable assignments, which creates a progressively less optimal outcome by excluding them in larger combinations.
        Comparator<Assignment<T, W>> viableSetSizeAndCrossSumComparator = new VoidOrderComparatorBuilder<T, W>()
                .setTaskRequests(this.taskRequests)
                .setWorkerGroupings(this.workers)
                .setViableAssignmentMap(this.viableAssignmentMap)
                .build();

        if (viableSetSizeAndCrossSumComparator == null) return false;

        // Create a comparator on the marginal cost, and tie-breaker on the complex ranking order generated above.
        Comparator<Assignment<T, W>> marginalCostComparator = new MarginalCostComparatorBuilder<>(templateCostMatrix).build();

        // Create the tree.
        TreeSet<Assignment<T, W>> assignmentTreeSet = new TreeSet<>(marginalCostComparator.thenComparing(viableSetSizeAndCrossSumComparator));

        // Add the assignments from the most recently successful assignment set.
        assignmentTreeSet.addAll(mostRecentlySucceededAssignments);

        // Seed a new combinatorial with this TreeSet
        this.activeCombinatorial = new BinSearchCombAdvanced<>(assignmentTreeSet);

        return true;
    }

    /**
     * <h4>Iterating</h4>
     * */
    private boolean cleanUpLastCombinatorial() {
        assert !activeCombinatorial.hasNext();

        Set<Assignment<T, W>> elementSet = activeCombinatorial.getElementSet();

        // We weren't able to make a viable branch using any of these,
        // so eliminate them permanently as they are otherwise the global optimum;
        elementSet.forEach(this::permanentlyEliminateAssignment);

        // Remove them from here as we don't need to check them anymore.
        failedAssignmentBranches.remove(elementSet);
        List<Set<Assignment<T, W>>> branchSets = new ArrayList<>(failedAssignmentBranches);
        for (Set<Assignment<T, W>> failedAssignmentBranch : branchSets) {
            failedAssignmentBranches.remove(failedAssignmentBranch);
            Set<Assignment<T, W>> livePartsOfFailedBranch = new HashSet<>(failedAssignmentBranch);
            livePartsOfFailedBranch.removeAll(elementSet);
            if (!livePartsOfFailedBranch.isEmpty()) failedAssignmentBranches.add(livePartsOfFailedBranch);
        }


        // Check we haven't now made the base matrix unviable. Doing so means we need to backtrack further.
        return templateCostMatrix.checkRowsAndColumnsAreViable();
    }

    /**
     * <h4>Iterating</h4>
     * */
    @Nullable
    private Set<Assignment<T, W>> cacheAssignmentsFromActiveMatrix() {
        Set<Assignment<T, W>> mostRecentlySucceededAssignments = activeCostMatrix.getAssignments();
        if (mostRecentlySucceededAssignments.isEmpty()) return null;
        this.failedAssignmentBranches.add(mostRecentlySucceededAssignments);
        return mostRecentlySucceededAssignments;
    }

    /**
     * <h4>Iterating</h4>
     * */
    private void permanentlyEliminateAssignment(Assignment<T, W> assignment) {
        templateCostMatrix.overrideAssignmentCost(assignment, Double.POSITIVE_INFINITY);
        removeAssignmentFromViableMap(assignment);
    }

    /**
     * <h4>Iterating</h4>
     * Is called after assignment is overridden to infinity in the initialCostMatrix. <br>
     * Avoids considering the assignment in future matrices.
     * */
    private void removeAssignmentFromViableMap(Assignment<T, W> assignment) {
        TaskRequest<T, W> task = assignment.task();
        WorkerGrouping<T, W> workerGrouping = assignment.workerGrouping();
        Set<WorkerGrouping<T, W>> workerGroupings = viableAssignmentMap.get(task);
        workerGroupings.remove(workerGrouping);
    }

    public static <T extends TaskSource<T, W>, W extends WorkerGrouping<T, W>> boolean doNothingTaskFilter(Assignment<T, W> assignment) {
        boolean doNothing = assignment.task().getClass().equals(DoNothingTask.class);
        if (doNothing) {
            System.out.println("Removing do nothing!");
        }
        return !doNothing;
    }



    public Set<Assignment<T, W>> getAssignedTasks() {
        return confirmedAssignmentSet.stream().filter(assignment -> !assignment.task().getClass().equals(DoNothingTask.class)).collect(Collectors.toUnmodifiableSet());
    }

    public double getSumOfAssignmentCosts() {
        return activeCostMatrix.getSumOfAssignments();
    }

    /**
     * Protected method for use in testing only.
     */
    protected void addUnviableAssignmentSet(Set<Assignment<T, W>> unviableSet) {
        if (unviableSet.isEmpty())
            return;
        failedAssignmentBranches.add(unviableSet);
    }

    public Viability getViability() {
        return viability;
    }

    public enum Viability {
        BASE_COSTS_CONTAINED_INFINITY_COLUMN,
        COMBINATORIAL_SEARCH_EXHAUSTED,
        REAL_TASKS_ALLOCATED,
        UNKNOWN_ERROR,
        LIVE
    }


}
