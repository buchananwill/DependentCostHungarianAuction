package HungarianAuction.TaskElements;

import HungarianAuction.WorkerElements.WorkerDomain;
import HungarianAuction.WorkerElements.WorkerGrouping;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public interface TaskSource<T extends TaskSource<T,W>, W extends WorkerGrouping<T, W>> {

    TaskRequest<T, W> getLargestUnallocatedTask();
    TaskRequest<T, W> getTaskOfSize(int size);

    void receiveWorkerGrouping(@NotNull WorkerGrouping<T, W> workerGrouping, @NotNull TaskRequest<T, W> taskRequest);
    void recallWorkerGrouping(@NotNull WorkerGrouping<T, W> workerGrouping, @NotNull TaskRequest<T, W> taskRequest);

    T unboxSource();

    boolean hasTokensOfSize(int tokenSize);


    int getTotalTaskBandwidth();

    int getMaxTaskBandwidth();

    int getMaxUnallocatedTokenSize();

    int countTokensOfSize(int tokenSize);

    Set<WorkerDomain<T, W>> getUnusedDomains();


    List<T> getNodeLinks();
}
