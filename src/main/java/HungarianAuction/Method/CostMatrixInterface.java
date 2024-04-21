package HungarianAuction.Method;

import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.WorkerElements.WorkerGrouping;

import java.util.Set;

public interface CostMatrixInterface<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> {
    boolean checkRowsAndColumnsAreViable();

    int getId();

    double getAssignmentCost(Assignment<T, W> assignment);

    CostMatrixInterface<T, W> cloneMatrix();

    void computeMarginalTaskCosts();

    boolean applyMinimumCrossings();

    boolean modifyCostsByLowestUncrossedValue();

    Set<Assignment<T, W>> getAssignments();

    void overrideAssignmentCost(Assignment<T, W> invalidAssignment, double cost);

    double getSumOfAssignments();

    boolean anyNaN();
}
