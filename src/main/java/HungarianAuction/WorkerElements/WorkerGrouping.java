package HungarianAuction.WorkerElements;

import HungarianAuction.TaskElements.TaskCost;
import HungarianAuction.TaskElements.TaskRequest;
import HungarianAuction.TaskElements.TaskSource;

import java.util.Set;

public interface WorkerGrouping<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> {
    Set<Worker<T, W>> getWorkers();
    int getSize();

    void setScarcityFactor(double scarcityFactor);

    double getScarcityFactor();

    TaskCost calculateMaxWorkerCost(TaskRequest<T,W> taskRequest);
    TaskCost calculateMinWorkerCost(TaskRequest<T,W> taskRequest);
    TaskCost calculateTotalCost(TaskRequest<T,W> taskRequest);

    W unboxWorkerGrouping(TaskRequest<T,W> taskRequest);

}
