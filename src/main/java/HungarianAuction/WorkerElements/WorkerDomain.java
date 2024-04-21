package HungarianAuction.WorkerElements;



import HungarianAuction.TaskElements.TaskSource;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Use Domains in the WorkerPool to organise and limit the depth of allocation into any particular dimension, e.g. day.
 * */
public class WorkerDomain<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> {
    private static final AtomicInteger domainIDCounter = new AtomicInteger();

    private final int id;



    Map<Worker<T,W>, Set<WorkerGrouping<T,W>>> unitToWorkerGroupingMap = new HashMap<>();


    private final Set<WorkerGrouping<T,W>> setOfWorkerGroupings = new HashSet<>();

    private final Set<Worker<T,W>> unitsInThisDomain;


    public WorkerDomain(Set<Worker<T,W>> unitsInThisDomain) {
        this.unitsInThisDomain = Set.copyOf(unitsInThisDomain);
        id = domainIDCounter.getAndIncrement();
    }



    public boolean addSubDomain(WorkerGrouping<T,W> tWorkerGrouping) {
        Set<Worker<T,W>> subDomainUnits = tWorkerGrouping.getWorkers();
        if (unitsInThisDomain.containsAll(subDomainUnits)) {
            setOfWorkerGroupings.add(tWorkerGrouping);
            subDomainUnits.forEach(
                    unit -> unitToWorkerGroupingMap.computeIfAbsent(unit, k -> new HashSet<>())
                            .add(tWorkerGrouping)
            );
            return true;
        } else return false;
    }

    public Set<Worker<T,W>> workersInThisDomain() {
        return unitsInThisDomain;
    }

    public Set<WorkerGrouping<T,W>> getAllSubDomains() {
        return new HashSet<>(setOfWorkerGroupings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkerDomain<?, ?> domain)) return false;
        return id == domain.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Domain{" +
                "domainID=" + id +
                ", setOfSubDomains=" + setOfWorkerGroupings +
                '}';
    }



}
