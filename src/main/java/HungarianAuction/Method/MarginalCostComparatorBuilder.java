package HungarianAuction.Method;

import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.WorkerElements.WorkerGrouping;

import java.util.Comparator;

public class MarginalCostComparatorBuilder<T extends TaskSource<T, W>, W extends WorkerGrouping<T, W>> {
    private final CostMatrixInterface<T, W> templateCostMatrix;

    public MarginalCostComparatorBuilder(CostMatrixInterface<T,W> templateCostMatrix) {
        this.templateCostMatrix = templateCostMatrix;
    }

    public Comparator<Assignment<T, W>> build() {
        return getMarginalCostLevelComparator();
    }

    private Comparator<Assignment<T, W>> getMarginalCostLevelComparator() {
        return (assignment1, assignment2) -> {
            double assignmentCost1 = templateCostMatrix.getAssignmentCost(assignment1);
            double assignmentCost2 = templateCostMatrix.getAssignmentCost(assignment2);
            if (assignmentCost1 < 0)
                assignmentCost1 = assignment1.task().getCost(assignment1.workerGrouping());
            if (assignmentCost2 < 0)
                assignmentCost2 = assignment1.task().getCost(assignment2.workerGrouping());
            return Double.compare(assignmentCost1, assignmentCost2);
        };
    }

}
