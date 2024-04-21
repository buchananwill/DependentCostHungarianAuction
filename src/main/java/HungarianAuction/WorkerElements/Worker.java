package HungarianAuction.WorkerElements;

import HungarianAuction.TaskElements.TaskCost;
import HungarianAuction.TaskElements.TaskRequest;
import HungarianAuction.TaskElements.TaskSource;

public interface Worker<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> {

    /**
     * Must return positive infinity if a worker is unable to carry out a task.
     */
    TaskCost calculateBaseCost(TaskRequest<T, W> taskRequest);



}
