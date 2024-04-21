package HungarianAuction.Auction;

import HungarianAuction.Method.Assignment;
import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.WorkerElements.WorkerGrouping;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.util.Set;

public class Result<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> {

    private final Set<Assignment<T,W>> assignmentSet;

    private final LocalTime timestamp;


    public Result(@NotNull Set<Assignment<T,W>> assignedWorkerGroupingList) {
        this.assignmentSet = Set.copyOf(assignedWorkerGroupingList);
        timestamp = LocalTime.now();
    }

    // getters
    public Set<Assignment<T,W>> getAssignmentSet() {
        return assignmentSet;
    }


    public LocalTime getTimestamp() {
        return timestamp;
    }
}
