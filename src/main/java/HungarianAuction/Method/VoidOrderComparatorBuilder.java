package HungarianAuction.Method;

import HungarianAuction.TaskElements.TaskSource;
import HungarianAuction.TaskElements.TaskRequest;
import HungarianAuction.WorkerElements.WorkerGrouping;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class VoidOrderComparatorBuilder<T extends TaskSource<T, W>, W extends WorkerGrouping<T, W>> {

    private List<WorkerGrouping<T,W>> workerGroupings;
    private List<TaskRequest<T, W>> taskRequests;

    private Map<TaskRequest<T,W>, Set<WorkerGrouping<T,W>>> viableAssignmentMap;

    public VoidOrderComparatorBuilder<T,W> setWorkerGroupings(List<WorkerGrouping<T, W>> workerGroupings) {
        this.workerGroupings = workerGroupings;
        return this;
    }

    public VoidOrderComparatorBuilder<T,W> setTaskRequests(List<TaskRequest<T, W>> taskRequests) {
        this.taskRequests = taskRequests;
        return this;
    }

    public VoidOrderComparatorBuilder<T,W> setViableAssignmentMap(Map<TaskRequest<T, W>, Set<WorkerGrouping<T, W>>> viableAssignmentMap) {
        this.viableAssignmentMap = viableAssignmentMap;
        return this;
    }

    public Comparator<Assignment<T, W>> build() {
        if (workerGroupings == null || taskRequests == null || viableAssignmentMap == null) throw new IllegalStateException("Missing fields needed for comparator.");

        List<Assignment<T, W>> assignmentListVoidOrder = getAssignmentListVoidOrder();

        if (assignmentListVoidOrder.isEmpty()) return null;
        return Comparator.comparingInt(assignmentListVoidOrder::indexOf);
    }

    public VoidOrderComparatorBuilder() {
    }

    /**
     * <h4>Iterating</h4>
     * Prepares the ranking needed for seeding the efficient ordering of the combinatorial
     * */
    @NotNull
    private List<Assignment<T, W>> getAssignmentListVoidOrder() {
        List<Assignment<T, W>> assignmentListVoidOrder = new ArrayList<>();

        Map<TaskRequest<T, W>, List<Assignment<T, W>>> viableAssignmentListMap = new TreeMap<>(this.getViableAssignmentCountComparator(this.viableAssignmentMap));
        viableAssignmentMap.forEach((task, set) -> {
            Set<Assignment<T, W>> assignmentSet = this.convertToHypotheticalAssignments(task, set);
            TreeSet<Assignment<T, W>> assignmentTreeSet = putAssignmentsInRowColumnSumOrder(assignmentSet);
            viableAssignmentListMap.put(task, new LinkedList<>(assignmentTreeSet));
        });

        int numberOfTasks = viableAssignmentListMap.keySet().size();
        Set<TaskRequest<T, W>> tasksWithNoMoreAssignments = new HashSet<>();
        while (tasksWithNoMoreAssignments.size() < numberOfTasks) {
            for (Map.Entry<TaskRequest<T, W>, List<Assignment<T, W>>> entry : viableAssignmentListMap.entrySet()) {
                TaskRequest<T, W> task = entry.getKey();
                List<Assignment<T, W>> assignmentList = entry.getValue();
                if (!assignmentList.isEmpty()) {
                    Assignment<T, W> nextAssignment = assignmentList.remove(0);
                    assignmentListVoidOrder.add(nextAssignment);
                } else {
                    tasksWithNoMoreAssignments.add(task);
                }
            }
        }
        return assignmentListVoidOrder;
    }

    /**
     * <h4>Iterating</h4>
     * */
    private Set<Assignment<T, W>> convertToHypotheticalAssignments(TaskRequest<T, W> task, Set<WorkerGrouping<T, W>> set) {
        return set.stream().map(tWorkerGrouping -> new Assignment<>(task, tWorkerGrouping)).collect(Collectors.toSet());
    }

    private TreeSet<Assignment<T, W>> putAssignmentsInRowColumnSumOrder(Set<Assignment<T, W>> assignmentSet) {
        Comparator<Assignment<T, W>> assignmentComparator = new AssignmentComparatorBuilder<>(this.workerGroupings, this.taskRequests).build();
        TreeSet<Assignment<T, W>> assignmentTreeSet = new TreeSet<>(assignmentComparator);
        Set<Assignment<T, W>> actualTaskAssignments = assignmentSet.stream().filter(CostMatrixSolver::doNothingTaskFilter).collect(Collectors.toSet());
        assignmentTreeSet.addAll(actualTaskAssignments);
        return assignmentTreeSet;
    }

    /**
     * <h4>Iterating</h4>
     * @param viableAssignmentMap: The map of tasks to viable assignments that backs the comparator.
     * @return Comparator, ordering by ascending number of viable assignments, then hashcode as tiebreaker.
     */
    @NotNull
    private Comparator<TaskRequest<T, W>> getViableAssignmentCountComparator(Map<TaskRequest<T, W>, Set<WorkerGrouping<T, W>>> viableAssignmentMap) {
        return (task1, task2) -> {
            int count1 = viableAssignmentMap.get(task1).size();
            int count2 = viableAssignmentMap.get(task2).size();
            if (count1 != count2) return count1 - count2;
            else return task1.hashCode() - task2.hashCode();
        };
    }


}
