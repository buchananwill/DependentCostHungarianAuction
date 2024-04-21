package QueueTree;

import HungarianAuction.TaskElements.TaskBatch;
import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.WorkerElements.WorkerGrouping;
import HungarianAuction.WorkerElements.WorkerPool;

public interface WorkerPoolManager<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> {
    WorkerPool<T,W> getWorkerPool(TaskBatch<T,W> taskBatch);

    void notifyWorkerPoolSource(TaskBatch<T,W> taskBatch);
}
