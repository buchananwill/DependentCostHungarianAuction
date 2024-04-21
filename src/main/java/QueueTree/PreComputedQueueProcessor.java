package QueueTree;

import HungarianAuction.Auction.FixedQueueAuctionHouse;
import HungarianAuction.Auction.Auction;
import HungarianAuction.Auction.AuctionHouse;
import HungarianAuction.TaskElements.TaskBatch;
import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.WorkerElements.WorkerGrouping;
import HungarianAuction.WorkerElements.WorkerPool;


import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class PreComputedQueueProcessor<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>, B extends GenericBuildMetric<T,W>> {

    private static final Logger logger = LoggerFactory.getLogger(PreComputedQueueProcessor.class);
    private final AuctionHouse<T,W> auctionHouse = new FixedQueueAuctionHouse<>();
    private final Deque<TaskBatch<T,W>> forwardsQueue;
    private final Deque<TaskBatch<T,W>> backwardsQueue = new ArrayDeque<>();
    private final WorkerPoolManager<T, W> workerPoolManager;
    private final List<Integer> queueProgress = new ArrayList<>();
    private final BuildMetricExtractor<T,W, B> buildMetricExtractor;
    private boolean useDomainProxies = true;
    private int loopCounter = 0;
    public PreComputedQueueProcessor(TaskQueueBuilder<T,W> taskQueueBuilder, BuildMetricExtractor<T, W, B> buildMetricExtractor) {
        this.forwardsQueue = taskQueueBuilder.getTaskBatchForwardsDeque();
        this.workerPoolManager = taskQueueBuilder.getWorkerPoolManager();

        this.buildMetricExtractor = buildMetricExtractor;
    }

    public boolean isUseDomainProxies() {
        return useDomainProxies;
    }

    public void setUseDomainProxies(boolean useDomainProxies) {
        this.useDomainProxies = useDomainProxies;
    }

    public B getBuildMetric() {
        return buildMetricExtractor.getBuildMetric();
    }

    /**
     * RULES FOR ALLOCATION: <br/>
     * 1. It's faster to fail and backtrack than to try to forecast failure.<br/>
     * 2. Always include at least enough days to allocate a lesson time to every allocation entrant.<br/>
     * 3. Include days in tiers of the remaining availability of lesson times. Stop including tiers when days >= number of entrants.<br/>
     * 4. The default mode of allocation is to use proxies, limiting allocations to one per domain per auction.<br/>
     * 5. If auctions contain only a single entrant, or if the remaining periods are clustered on too few days, proxies are not used.
     */

    public Auction.AuctionState processTaskBatchQueue(int multiUndoIncrement, int timeOutInMs) {
        Auction.AuctionState taskQueueResult = getTaskQueueResult(multiUndoIncrement, timeOutInMs);
        buildMetricExtractor.extractBuildMetrics(taskQueueResult, forwardsQueue, backwardsQueue, queueProgress);
        return taskQueueResult;
    }





    @NotNull
    private Auction.AuctionState getTaskQueueResult(int multiUndoIncrement, int timeOutInMs) {
        LocalTime start = LocalTime.now();
        boolean processForwards = true;
        int startingQueueSize = forwardsQueue.size();
        int multiUndo = multiUndoIncrement;
        while (!forwardsQueue.isEmpty()) {
            if (loopCounter++ >= 20) {
                System.out.println("Next batch: " + backwardsQueue.size());
                loopCounter = 0;
            }
            queueProgress.add(backwardsQueue.size());
            assert forwardsQueue.size() + backwardsQueue.size() == startingQueueSize;
            buildMetricExtractor.incrementTotalAllocationLoops();
            if (!processForwards && backwardsQueue.isEmpty()) {
                if (!useDomainProxies) {
                    return Auction.AuctionState.TREE_FAILURE;
                } else {
                    useDomainProxies = false;
                    processForwards = true;
                    continue;
                }
            }

            processForwards = processForwards ? resultOfProcessForwards() : resultOfProcessBackwards();

            if (Duration.between(start, LocalTime.now()).toMillis() > timeOutInMs) {
                System.out.println("Time out!");
                int undoTarget = multiUndo;
                multiUndo += multiUndoIncrement;
                auctionHouse.undoSomeAuctions(undoTarget);
                int undoCounter = 0;
                while (!backwardsQueue.isEmpty() && undoCounter < undoTarget) {
                    TaskBatch<T,W> poll = backwardsQueue.poll();
                    workerPoolManager.notifyWorkerPoolSource(poll);
                    forwardsQueue.push(poll);
                    undoCounter++;
                }
                start = LocalTime.now();
            }
        }


        return Auction.AuctionState.SUCCESS;
    }

    private boolean resultOfProcessBackwards() {
        boolean processForwards = false;
        TaskBatch<T,W> previousBatch = backwardsQueue.poll();
        Auction.AuctionState auctionState = auctionHouse.branchFromLastSuccessfulAuction();
        if (auctionState == Auction.AuctionState.SUCCESS) {
            backwardsQueue.push(previousBatch);
            processForwards = true;
        } else {
            forwardsQueue.push(previousBatch);
        }
        workerPoolManager.notifyWorkerPoolSource(previousBatch);
        return processForwards;
    }

    private boolean resultOfProcessForwards() {
        boolean processForwards;
        TaskBatch<T,W> nextBatch = forwardsQueue.poll();
        processForwards = allocateNextBatch(nextBatch);
        if (processForwards) {
            backwardsQueue.push(nextBatch);
        } else {
            forwardsQueue.push(nextBatch);
        }
        workerPoolManager.notifyWorkerPoolSource(nextBatch);
        return processForwards;
    }


    private boolean allocateNextBatch(TaskBatch<T,W> nextBatch) {

        WorkerPool<T, W> cycleFactoryWorkerPool = workerPoolManager.getWorkerPool(nextBatch);

        if (buildMetricExtractor.getTotalAllocationLoops() == 1 && cycleFactoryWorkerPool.countAvailableWorkers() == 0)
            throw new IllegalStateException("Why no workers?");

        // Holding the auction.
        Auction.AuctionState auctionState = this.auctionHouse.createNextAuction(cycleFactoryWorkerPool, nextBatch, useDomainProxies);

        // Here is where we handle a auction that failed.
        return auctionState == Auction.AuctionState.SUCCESS;
    }


}

