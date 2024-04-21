package HungarianAuction.WorkerElements;

import HungarianAuction.TaskElements.TaskCost;
import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.TaskElements.TaskRequest;

import java.util.*;
import java.util.stream.Collectors;

public class DomainProxy<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> implements WorkerGrouping<T, W> {
    @Override
    public void setScarcityFactor(double scarcityFactor) {

    }

    @Override
    public double getScarcityFactor() {
        return proxySet.stream().map(WorkerGrouping::getScarcityFactor).max(Comparator.naturalOrder()).orElse(Double.POSITIVE_INFINITY);
    }

    @Override
    public W unboxWorkerGrouping(TaskRequest<T, W> taskRequest) {
        return getOptimalWorkerGrouping(taskRequest).unboxWorkerGrouping(taskRequest);
    }

    private final int proxySize;
    private final Set<WorkerGrouping<T,W>> proxySet;
    Map<TaskRequest<T,W>, WorkerGrouping<T,W>> optimalWorkerMap = new HashMap<>();
    Map<TaskRequest<T,W>, TaskCost> bestCostMap = new HashMap<>();


    public DomainProxy(Set<WorkerGrouping<T,W>> proxySet, int proxySize) {
        this.proxySet = Set.copyOf(proxySet);
        this.proxySize = proxySize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainProxy<?, ?> that)) return false;
        return proxySize == that.getProxySize() && Objects.equals(proxySet, that.proxySet);
    }

    private int getProxySize() {
        return proxySize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(proxySize, proxySet);
    }

    @Override
    public Set<Worker<T,W>> getWorkers() {
        return proxySet.stream().map(WorkerGrouping::getWorkers).flatMap(Set::stream).collect(Collectors.toSet());
    }

    @Override
    public int getSize() {
        return proxySize;
    }

    @Override
    public TaskCost calculateMaxWorkerCost(TaskRequest<T,W> taskRequest) {
        return null;
    }

    @Override
    public TaskCost calculateMinWorkerCost(TaskRequest<T,W> taskRequest) {
        return null;
    }

    @Override
    public TaskCost calculateTotalCost(TaskRequest<T,W> taskRequest) {
        if (bestCostMap.containsKey(taskRequest)) return bestCostMap.get(taskRequest);
        for (WorkerGrouping<T,W> workerG : proxySet) {
            TaskCost taskCost = workerG.calculateTotalCost(taskRequest);
            if (!optimalWorkerMap.containsKey(taskRequest)) {
                bestCostMap.put(taskRequest, taskCost);
                optimalWorkerMap.put(taskRequest, workerG);
            } else if (taskCost.getFinalValue() < bestCostMap.get(taskRequest).getFinalValue()) {
                bestCostMap.put(taskRequest, taskCost);
                optimalWorkerMap.put(taskRequest, workerG);
            }
        }
        return bestCostMap.get(taskRequest);
    }

    public WorkerGrouping<T,W> getOptimalWorkerGrouping(TaskRequest<T,W> task) {
        return optimalWorkerMap.get(task);
    }


    @Override
    public String toString() {
        return "DomainProxy{" +
                "proxySet=" + proxySet +
                '}';
    }
}
