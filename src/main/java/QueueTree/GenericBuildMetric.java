package QueueTree;

import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.WorkerElements.WorkerGrouping;

public interface GenericBuildMetric<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> {
}
