package QueueTree;

import HungarianAuction.Auction.Auction;
import HungarianAuction.TaskElements.TaskBatch;
import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.WorkerElements.WorkerGrouping;

import java.util.Deque;
import java.util.List;

public interface BuildMetricExtractor<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>, B extends GenericBuildMetric<T,W>> {
    B getBuildMetric();


    void incrementTotalAllocationLoops();

    void extractBuildMetrics(Auction.AuctionState taskQueueResult, Deque<TaskBatch<T, W>> forwardsQueue, Deque<TaskBatch<T, W>> backwardsQueue, List<Integer> queueProgress);

    int getTotalAllocationLoops();
}
