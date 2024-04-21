package HungarianAuction.TaskElements;

import HungarianAuction.WorkerElements.WorkerGrouping;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskRequest<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> {
    private final TaskSource<T, W> taskSource;

    private static final AtomicInteger taskOfferingCounter = new AtomicInteger();

    private final EntryToken<T,W> entryToken;

    private final int id;

    private final Map<WorkerGrouping<T,W>, TaskCost> mapWorkerGroupingTaskCost = new HashMap<>();

    public TaskRequest(TaskSource<T,W> taskSource, EntryToken<T,W> token) {
        this.taskSource = taskSource;
        this.entryToken = token;
        id = taskOfferingCounter.incrementAndGet();
    }

    public TaskSource<T,W> getTaskSource() {
        return taskSource;
    }

    public double getCost(WorkerGrouping<T,W> workerGrouping) {
        return mapWorkerGroupingTaskCost.getOrDefault(workerGrouping, new TaskCost(Double.POSITIVE_INFINITY)).getFinalValue();
    }

    public int getEntryTokenSize() {
        return entryToken.size();
    }


    public int countAvailableWorkerGroupings() {
        return mapWorkerGroupingTaskCost.size();
    }


    public void modifyValueSum(WorkerGrouping<T,W> worker, double increase) {
        mapWorkerGroupingTaskCost.computeIfAbsent(worker, k -> new TaskCost()).modifySum(increase);
    }

    @Override
    public String toString() {
        return "TaskOffering{" +
                "element=" + taskSource +
                ", bids=" + mapWorkerGroupingTaskCost +
                '}';
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskRequest<?, ?> that)) return false;
        return id == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


    public void tenderGroupingCosts(Set<WorkerGrouping<T,W>> unassignedWorkerGroupings) {

        for (WorkerGrouping<T,W> workerGrouping : unassignedWorkerGroupings
             ) {
            tenderWorkerGroupingOffer(workerGrouping);
        }
    }

    private void tenderWorkerGroupingOffer(WorkerGrouping<T,W> workerGrouping) {
        if (mapWorkerGroupingTaskCost.containsKey(workerGrouping))
            return;
        TaskCost cost = workerGrouping.calculateTotalCost(this);
        if (cost.getFinalValue()==Double.POSITIVE_INFINITY)
            return;
        modifyValueSum(workerGrouping, cost.getFinalValue());
    }


    public Map<WorkerGrouping<T,W>, TaskCost> getWorkerGroupingMap() {
        return Collections.unmodifiableMap(new HashMap<>(mapWorkerGroupingTaskCost));
    }

    public void removeWorkerGroupingOffer(WorkerGrouping<T,W> tWorker) {
        mapWorkerGroupingTaskCost.remove(tWorker);
    }

    public EntryToken<T,W> getEntryToken() {
        return entryToken;
    }

    public void resetAllCosts() {
        mapWorkerGroupingTaskCost.clear();
    }

    protected int getId() {
        return id;
    }

}
