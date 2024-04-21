package HungarianAuction.Auction;

import HungarianAuction.TaskElements.TaskBatch;
import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.WorkerElements.WorkerGrouping;
import HungarianAuction.WorkerElements.WorkerPool;

public interface AuctionHouse<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> {

    Auction.AuctionState branchFromLastSuccessfulAuction();

    Auction.AuctionState createNextAuction(WorkerPool<T,W> workerPool, TaskBatch<T,W> preMadeTaskBatch, boolean useDomainProxies);


    void undoSomeAuctions(int howManyToUndo);

}
