package HungarianAuction.Auction;

import HungarianAuction.Method.Assignment;
import HungarianAuction.TaskElements.TaskBatch;
import HungarianAuction.TaskElements.TaskRequest;
import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.WorkerElements.DomainProxy;
import HungarianAuction.WorkerElements.WorkerDomain;
import HungarianAuction.WorkerElements.WorkerGrouping;
import HungarianAuction.WorkerElements.WorkerPool;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Responsible for determining who is able to enter an auction series, on the basis of who has tokens of the stated size.
 * All auctions in the stack all called in LIFO order, as soon as the callRunningAuctions method is called.
 * */

public class FixedQueueAuctionHouse<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> implements AuctionHouse<T,W> {


    // Initialise backwards queue (successful auctions) and cache of recently failed auctions..
    private final Deque<Auction<T, W>> successfulAuctions = new ArrayDeque<>();
    private final Deque<Auction<T, W>> failedAuctions = new ArrayDeque<>();

    public FixedQueueAuctionHouse() {
    }

    @Override
    public Auction.AuctionState branchFromLastSuccessfulAuction() {
        if (successfulAuctions.isEmpty())
            return Auction.AuctionState.TREE_FAILURE;

        Auction<T, W> lastSuccessfulAuction = successfulAuctions.poll();
        FixedQueueAuctionHouse.undoAuction(lastSuccessfulAuction);
        Auction.AuctionState stateAfterAlternativeSearch = lastSuccessfulAuction.findAlternativeAllocation();
        if (stateAfterAlternativeSearch == Auction.AuctionState.SUCCESS) {
            processSuccessfulAuction(lastSuccessfulAuction);
        } else {
            pushOntoFailedStack(lastSuccessfulAuction);
        }
        return stateAfterAlternativeSearch;
    }



    @Override
    public Auction.AuctionState createNextAuction(WorkerPool<T,W> workerPool, TaskBatch<T,W> preMadeTaskBatch, boolean useDomainProxies) {

        int currentTokenSize = preMadeTaskBatch.getTaskSize();
        Set<WorkerDomain<T,W>> unusedDomainsInThisFactoryBatch = new HashSet<>();
        for (TaskRequest<T,W> tTaskRequest : preMadeTaskBatch.getTasks()) {
            TaskSource<T,W> taskSource = tTaskRequest.getTaskSource();
            Set<WorkerDomain<T,W>> unusedDomains = taskSource.getUnusedDomains();
            // TODO add an option to ignore domains entirely?
            if (unusedDomains != null) unusedDomainsInThisFactoryBatch.addAll(unusedDomains);
        }
        WorkerPool<T,W> poolForThisAuction = null;

        Map<WorkerDomain<T,W>, Set<WorkerGrouping<T,W>>> availableWorkerGroupings = workerPool.getAvailableWorkerGroupings(currentTokenSize, preMadeTaskBatch.getBatchSize(), unusedDomainsInThisFactoryBatch);
        if (useDomainProxies && availableWorkerGroupings != null && preMadeTaskBatch.getBatchSize() != 1) {
            poolForThisAuction = createProxyPool(currentTokenSize, availableWorkerGroupings);
        } else {
            poolForThisAuction = workerPool;
        }

        return processNewAuction(poolForThisAuction, preMadeTaskBatch);
    }

    @NotNull
    private static <T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> WorkerPool<T, W> createProxyPool(int currentTokenSize, Map<WorkerDomain<T, W>, Set<WorkerGrouping<T, W>>> availableWorkerGroupings) {
        WorkerPool<T,W> poolForThisAuction;
        Set<DomainProxy<T,W>> domainProxies = availableWorkerGroupings.values().stream().map(workerGroupings -> new DomainProxy<>(workerGroupings, currentTokenSize)).collect(Collectors.toSet());
        Set<WorkerGrouping<T,W>> workerGroupingSet = domainProxies.stream().map(domainProxy -> (WorkerGrouping<T,W>) domainProxy).collect(Collectors.toSet());
        WorkerPool<T,W> proxyPool = new WorkerPool<>(domainProxies.stream().map(DomainProxy::getWorkers).flatMap(Set::stream).collect(Collectors.toSet()));
        proxyPool.addValidWorkerGroupings(workerGroupingSet);
        poolForThisAuction = proxyPool;
        return poolForThisAuction;
    }

    private Auction.AuctionState processNewAuction(WorkerPool<T, W> workerPool, TaskBatch<T, W> tTaskBatch) {
        Auction<T, W> auction = new Auction<>(workerPool, tTaskBatch);
        Auction.AuctionState auctionState = auction.validateState();

        if (auctionState == Auction.AuctionState.READY_TO_CALL) {
            auctionState = auction.callAuction();
        }

        if (auctionState != Auction.AuctionState.SUCCESS) {
            pushOntoFailedStack(auction);
            return auctionState;
        }

        processSuccessfulAuction(auction);
        return auctionState;
    }

    private void pushOntoFailedStack(Auction<T, W> failedAuction) {
        failedAuctions.push(failedAuction);
        if (failedAuctions.size() > 5) failedAuctions.pollLast();
    }

    private void processSuccessfulAuction(Auction<T, W> auction) {
        Result<T, W> result = auction.getResult();
        Set<Assignment<T, W>> winningAssignmentSet = result.getAssignmentSet();
        WorkerPool<T, W> workerPool = auction.getWorkerPool();

//        Assign the winning workerGroupings.
        winningAssignmentSet.forEach(assignment -> {
                    FixedQueueAuctionHouse.confirmAssignment(workerPool, assignment);
                }
        );

        successfulAuctions.push(auction);
    }

    private static <T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> void confirmAssignment(WorkerPool<T, W> workerPool, Assignment<T, W> assignment) {
        WorkerGrouping<T,W> workerGrouping = assignment.workerGrouping();
        TaskSource<T,W> taskSource = assignment.task().getTaskSource();
        if (workerGrouping instanceof DomainProxy<T,W> domainProxy) {
            workerGrouping = domainProxy.getOptimalWorkerGrouping(assignment.task());
        }
        taskSource.receiveWorkerGrouping(workerGrouping, assignment.task());
        workerPool.assignAll(List.of(workerGrouping));
    }

    private static <T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> void undoAuction(Auction<T, W> successfulAuction) {
        Set<Assignment<T,W>> assignmentSet = successfulAuction.getResult().getAssignmentSet();
        WorkerPool<T,W> workerPool = successfulAuction.getWorkerPool();
        assignmentSet.forEach(FixedQueueAuctionHouse::revokeAssignment);
        workerPool.resetWorkerAvailability();
    }

    private static <T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> void revokeAssignment(Assignment<T, W> assignment) {
        WorkerGrouping<T,W> workerGrouping = assignment.workerGrouping();
        TaskSource<T,W> taskSource = assignment.task().getTaskSource();
        W unboxedWorkerGrouping = workerGrouping.unboxWorkerGrouping(assignment.task());
        taskSource.recallWorkerGrouping(unboxedWorkerGrouping, assignment.task());
    }

    @Override
    public void undoSomeAuctions(int howManyToUndo) {
        for (int i = 0; i < howManyToUndo; i++) {
            Auction<T, W> auction = successfulAuctions.poll();
            if (auction != null) {
                undoAuction(auction);
                pushOntoFailedStack(auction);
            } else return;
        }
    }
}
