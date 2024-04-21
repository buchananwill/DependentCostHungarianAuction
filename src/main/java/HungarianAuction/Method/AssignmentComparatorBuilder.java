package HungarianAuction.Method;

import HungarianAuction.TaskElements.TaskRequest;
import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.WorkerElements.WorkerGrouping;

import java.util.Comparator;
import java.util.List;

public class AssignmentComparatorBuilder<T extends TaskSource<T, W>, W extends WorkerGrouping<T, W>> {


    private final List<WorkerGrouping<T, W>> workers;
    private final List<TaskRequest<T, W>> taskRequests;

    public AssignmentComparatorBuilder(List<WorkerGrouping<T,W>> workers, List<TaskRequest<T,W>> taskRequests) {
        this.workers = workers;
        this.taskRequests = taskRequests;
    }

    public Comparator<Assignment<T, W>> build() {
        return (assignment1, assignment2) -> {
            double crossedSum1 = getSumOfRowAndColumnForAssignment(assignment1);
            double crossedSum2 = getSumOfRowAndColumnForAssignment(assignment2);
            if (crossedSum1 == crossedSum2) return assignment1.hashCode() - assignment2.hashCode();
            else return Double.compare(crossedSum1, crossedSum2);
        };
    }


    private double getSumOfRowAndColumnForAssignment(Assignment<T, W> assignment) {
        double sum = 0D;
        TaskRequest<T, W> task = assignment.task();
        WorkerGrouping<T, W> tWorkerGrouping = assignment.workerGrouping();
        for (WorkerGrouping<T, W> worker : this.workers) {
            double cost = task.getCost(worker);
            if (cost < Double.POSITIVE_INFINITY)
                sum += cost;
        }
        for (TaskRequest<T, W> taskRequest : this.taskRequests) {
            if (taskRequest == task) continue;
            double cost = taskRequest.getCost(tWorkerGrouping);
            if (cost < Double.POSITIVE_INFINITY)
                sum += cost;
        }
        return sum;
    }
}
