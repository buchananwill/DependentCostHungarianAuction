package HungarianAuction.WorkerElements;

import HungarianAuction.TaskElements.TaskSource;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class WorkerPool<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> {
    private final static AtomicInteger workerPoolCounter = new AtomicInteger(0);

    private final int id;
    private final Set<Worker<T,W>> availableWorkers;

    private final Set<WorkerGrouping<T,W>> validWorkerGroupings = new HashSet<>();

    private final Set<Worker<T,W>> assignedWorkers;
    private Set<WorkerDomain<T,W>> domains;

    public WorkerPool(Set<? extends Worker<T,W>> availableWorkers) {
        this.availableWorkers = new HashSet<>(availableWorkers);
        assignedWorkers = new HashSet<>();
        id = workerPoolCounter.incrementAndGet();
    }

    public boolean assignWorker(Worker<T,W> worker) {
        if (availableWorkers.remove(worker)) {
            assignedWorkers.add(worker);
            return true;
        } else {
            throw new IllegalStateException("Cannot assign " + worker + " - wasn't available.");
        }
    }

    public int countAvailableWorkerGroupings(int groupingSize) {

        return availableWorkers.size();
    }


    public Set<WorkerGrouping<T,W>> getAvailableWorkerGroupings(int groupingSize) {

        HashSet<WorkerGrouping<T,W>> validWorkerGroupings = new HashSet<>(this.validWorkerGroupings.stream()
                .filter(tWorkerGrouping -> tWorkerGrouping.getSize() == groupingSize)
                .toList()
        );

        HashSet<WorkerGrouping<T,W>> availableWorkerGroupings = new HashSet<>();

        for (WorkerGrouping<T,W> grouping: validWorkerGroupings) {
            boolean addThisGrouping = true;
            for (Worker<T,W> worker:grouping.getWorkers()
                 ) {
                if (!availableWorkers.contains(worker)) {
                    addThisGrouping = false;
                    break;
                }
            }
                if (addThisGrouping) availableWorkerGroupings.add(grouping);
        }
        return availableWorkerGroupings;

    }

    public Map<WorkerDomain<T,W>, Set<WorkerGrouping<T,W>>> getAvailableWorkerGroupings(int groupingSize, int minimumDomainNumber, Set<WorkerDomain<T,W>> feasibleDomains) {
        Set<WorkerGrouping<T,W>> availableWorkerGroupings = getAvailableWorkerGroupings(groupingSize);

        // Map feasible domains to their available workerGrouping.
        Map<WorkerDomain<T,W>, Set<WorkerGrouping<T,W>>> availableWorkersByDomain = domains.stream().filter(feasibleDomains::contains)
                .collect(Collectors.toMap(
                domain -> domain,
                domain -> {
                    Set<WorkerGrouping<T,W>> workerSet = new HashSet<>(domain.getAllSubDomains());
                    workerSet.retainAll(availableWorkerGroupings);
                    return workerSet;
                }
        ));

        Map<WorkerDomain<T,W>, Set<WorkerGrouping<T,W>>> workerDomainSetMap = new HashMap<>();

        while(workerDomainSetMap.size() < minimumDomainNumber) {
            int mostDomainAvailability = availableWorkersByDomain.values().stream().map(Set::size).max(Comparator.naturalOrder()).orElse(0);
            if (mostDomainAvailability==0) {
                return null;
            }

            Set<WorkerDomain<T,W>> workerDomains = new HashSet<>(availableWorkersByDomain.keySet());
            for (WorkerDomain<T,W> domain: workerDomains
                 ) {
                Set<WorkerGrouping<T,W>> workerGroupings = availableWorkersByDomain.get(domain);
                if (workerGroupings.size()==mostDomainAvailability) {
                    workerDomainSetMap.put(domain, workerGroupings);
                    availableWorkersByDomain.remove(domain);
                }
            }
        }

        return workerDomainSetMap;

    }

    public void assignAll(List<WorkerGrouping<T,W>> assignedWorkers) {
        assignedWorkers.stream()
                .flatMap(workerGrouping -> workerGrouping.getWorkers().stream())
                .forEach(this::assignWorker);
    }

    public void unassignWorker(Worker<T,W> worker) {
        assignedWorkers.remove(worker);
        availableWorkers.add(worker);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkerPool<?, ?> that = (WorkerPool<?, ?>) o;
        return id == that.getId();
    }

    private int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean addValidWorkerGroupings(Set<WorkerGrouping<T,W>> workerGroupingSet) {
        return validWorkerGroupings.addAll(workerGroupingSet);
    }

    public void setDomains(Set<WorkerDomain<T,W>> tessellationBoxDomains) {
        this.domains = tessellationBoxDomains;
        addValidWorkerGroupings(tessellationBoxDomains.stream()
                .map(WorkerDomain::getAllSubDomains)
                .flatMap(Set::stream)
                .collect(Collectors.toSet()));
    }

    public void resetWorkerAvailability() {
        availableWorkers.addAll(assignedWorkers);
        assignedWorkers.clear();
    }


    public int countAvailableWorkers() {
        return availableWorkers.size();
    }
}
