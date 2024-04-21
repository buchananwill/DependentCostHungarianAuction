package HungarianAuction.Auction;

import HungarianAuction.Method.CostMatrixSolver;
import HungarianAuction.TaskElements.TaskBatch;
import HungarianAuction.TaskElements.TaskRequest;
import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.WorkerElements.WorkerGrouping;
import HungarianAuction.WorkerElements.WorkerPool;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Within the allocation framework, AllocationAuction acts as:
 * <ol>
 * <li>
 * A node with the decision tree. Alternative allocation outcomes can be explored by adding invalid assignment sets and re-running the auction.
 * </li>
 * <li>
 * Ranking of the allocations in some algorithms.
 * </li>
 * </ol>
 * The actual allocation decision is deferred to the Hungarian Algorithm.
 */

public class Auction<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> {

    private static final AtomicInteger auctionCounter = new AtomicInteger(0);
    private final int id;
    private final int taskSize;
    private final TaskBatch<T,W> taskBatch;
    private final WorkerPool<T,W> workerPool;
    private CostMatrixSolver<T,W> costMatrixSolver = null;
    private Result<T,W> currentResult;
    private AuctionState currentState;

    public Auction(@NotNull WorkerPool<T, W> workerPool, @NotNull TaskBatch<T, W> taskBatch) {
        // This auction has no prior memory, but the batch may have been processed before on a different branch.
        taskBatch.getTasks().forEach(TaskRequest::resetAllCosts);

        // Initialize fields.
        this.taskBatch = taskBatch;
        this.taskSize = taskBatch.getTaskSize();
        this.workerPool = workerPool;
        this.currentState = AuctionState.INITIALISED;
        this.id = auctionCounter.incrementAndGet();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Auction<?, ?> that)) return false;
        return id == that.getId();
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "AllocationAuction{" + "auctionId=" + id + ", taskBatch=" + taskBatch + '}';
    }

    public AuctionState validateState() {
        if (taskBatch.getBatchSize() > workerPool.countAvailableWorkerGroupings(taskSize)) {
            setStateFailed();
        }

//        If no buyers, end the auction.
        if (taskBatch.getBatchSize() == 0) {
            currentState = AuctionState.COMPLETE_NULL;
        } else currentState = AuctionState.READY_TO_CALL;
        return this.currentState;
    }

    private void setStateFailed() {
        currentState = AuctionState.FAILURE;
    }

    /**
     * <h4>Calling the Auction can occur:</h4>
     * <ol>
     *     <li>From outside the class, after it is first instantiated.</li>
     *     <li>From within the class, when an alternative branch is requested.</li>
     * </ol>
     * */
    public AuctionState callAuction() {
        if (getCurrentState() == AuctionState.FAILURE || getCurrentState() == AuctionState.COMPLETE_NULL)
            return getCurrentState();

        setEntryTokensLive(true);


        if (costMatrixSolver == null) {
            initializeHungarianGroupingAllocator();
            if (this.currentState == AuctionState.FAILURE) {
                setEntryTokensLive(false);
                return this.currentState;
            }
        }

        boolean attempt = this.costMatrixSolver.applyAlgorithm();
        if (attempt) {
            this.currentResult = new Result<>(this.costMatrixSolver.getAssignedTasks());
            if (this.currentResult.getAssignmentSet().size() == taskBatch.getBatchSize()) {
                this.currentState = AuctionState.SUCCESS;
                taskBatch.setOutcome(currentResult.getAssignmentSet());
            }
            else setStateFailed();
        } else setStateFailed();

        setEntryTokensLive(false);

        return this.currentState;
    }

    public AuctionState findAlternativeAllocation() {
        currentResult = null;
        currentState = AuctionState.READY_TO_CALL;
        if (costMatrixSolver != null && costMatrixSolver.getViability() == CostMatrixSolver.Viability.LIVE) {
            this.callAuction();
        } else if (costMatrixSolver != null && costMatrixSolver.getViability() == CostMatrixSolver.Viability.REAL_TASKS_ALLOCATED) {
            this.currentState = AuctionState.FAILURE;
        }
        return this.currentState;
    }

    private void setEntryTokensLive(boolean state) {
        taskBatch.getTasks().stream().map(TaskRequest::getEntryToken).forEach(entryToken -> entryToken.setAuctionIsLive(state));
    }

    public AuctionState getCurrentState() {
        return currentState;
    }

    private void initializeHungarianGroupingAllocator() {
        List<WorkerGrouping<T,W>> workerList = new ArrayList<>(this.workerPool.getAvailableWorkerGroupings(taskSize));

        //  Use Hungarian Algorithm to Allocate tasks.
        List<TaskRequest<T,W>> taskRequestList = new ArrayList<>(taskBatch.getTasks());

        this.costMatrixSolver = new CostMatrixSolver<>(workerList, taskRequestList, taskSize);

        CostMatrixSolver.Viability viability = this.costMatrixSolver.getViability();

        if (viability == CostMatrixSolver.Viability.BASE_COSTS_CONTAINED_INFINITY_COLUMN
                || viability == CostMatrixSolver.Viability.UNKNOWN_ERROR) {
            setStateFailed();
        }
    }

    public Result<T,W> getResult() {
        return this.currentResult;
    }

    public WorkerPool<T,W> getWorkerPool() {
        return this.workerPool;
    }

    public enum AuctionState {
        INITIALISED,
        READY_TO_CALL,
        SUCCESS, // Normal state sequence.
        FAILURE, // E.g. not enough workerGroupings, one or more tasks have no valid workerGroupings.
        TREE_FAILURE, // When we reach the root of an Allocation Tree without finding a viable path to completion.
        COMPLETE_NULL, // No tasks in the batch provided.
    }
}