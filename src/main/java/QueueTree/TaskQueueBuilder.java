package QueueTree;

import HungarianAuction.TaskElements.TaskBatch;
import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.WorkerElements.WorkerGrouping;

import java.util.Deque;

public interface TaskQueueBuilder<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> {
    Deque<TaskBatch<T, W>> getTaskBatchForwardsDeque();

    WorkerPoolManager<T, W> getWorkerPoolManager();
}
