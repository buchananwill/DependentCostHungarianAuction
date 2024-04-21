package HungarianAuction.Method;

import HungarianAuction.TaskElements.TaskRequest;
import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.WorkerElements.WorkerGrouping;

public record Assignment<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>>(TaskRequest<T, W> task, WorkerGrouping<T, W> workerGrouping) {
}
