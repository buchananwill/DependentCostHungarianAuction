package HungarianAuction.TaskElements;

import HungarianAuction.WorkerElements.WorkerGrouping;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class DoNothingTask<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> extends TaskRequest<T, W> {

    private static final AtomicInteger doNothingTaskCounter = new AtomicInteger();

    private final int id;
    public DoNothingTask(int taskSize) {
        super(null, new EntryToken<>(taskSize));
        this.id = doNothingTaskCounter.incrementAndGet();
    }

    @Override
    public void tenderGroupingCosts(Set<WorkerGrouping<T,W>> unassignedWorkerGroupings) {
        for (WorkerGrouping<T,W> worker: unassignedWorkerGroupings
        ) {
            TaskCost cost = new TaskCost(0);
            modifyValueSum(worker, cost.getFinalValue());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoNothingTask<?, ?> that)) return false;
        return id == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }
}
