package HungarianAuction.TaskElements;

import HungarianAuction.WorkerElements.WorkerGrouping;

import java.util.*;
import java.util.stream.Collectors;

public class TaskPool<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> {

    /**
     * Initialise the TaskPool with all the tasks needed to complete an allocation auction series.
     * The task sources should consume their "lesson/auction tokens" as they generate TaskOfferings -
     * this architecture is superseding the old token entry system.
     * The TaskPool will then be ready to return the batches of tasks in priority order.
     * */
    private final Set<TaskRequest<T,W>> taskRequests;

    private final Queue<TaskBatch<T,W>> taskBatches = new PriorityQueue<>();

    private final Map<Integer, List<List<TaskRequest<T,W>>>> nestedTaskOfferingListsMap = new HashMap<>();

    public TaskPool(Collection<TaskRequest<T,W>> taskRequests) {
        this.taskRequests = new HashSet<>(taskRequests);
        sortTasksIntoNestedLists();
        nestedTaskOfferingListsMap.keySet().forEach(this::mapListOfListsToBatches);
    }

    private void mapListOfListsToBatches(Integer integer) {
        nestedTaskOfferingListsMap.get(integer)
                .stream()
                .map(TaskBatch::new)
                .forEach(taskBatches::add);
    }

    private void sortTasksIntoNestedLists() {
        for (TaskRequest<T,W> task : taskRequests) {
            int taskSize = task.getEntryTokenSize();
            TaskSource<T,W> source = task.getTaskSource();
            List<List<TaskRequest<T,W>>> listOfLists = nestedTaskOfferingListsMap.computeIfAbsent(
                    taskSize,
                    k -> new ArrayList<>()
            );
            if (listOfLists.isEmpty()) {
                listOfLists.add(new ArrayList<>(Collections.singleton(task)));
            } else addToFirstListWithoutThisSource(source, task, listOfLists);
        }
    }

    private void addToFirstListWithoutThisSource(TaskSource<T,W> source, TaskRequest<T,W> task, List<List<TaskRequest<T,W>>> listOfLists) {
        for (List<TaskRequest<T, W>> sublist : listOfLists) {
            if (sublistDoesNotContainSource(sublist, source)) {
                sublist.add(task);
                return;
            }
        }
        listOfLists.add(new ArrayList<>(List.of(task)));
    }

    private boolean sublistDoesNotContainSource(List<TaskRequest<T,W>> sublist, TaskSource<T,W> source) {
        return sublist.stream().noneMatch(task -> task.getTaskSource().equals(source));
    }

    public boolean hasNextTaskBatch() {
        return (!taskBatches.isEmpty());
    }

    public TaskBatch<T,W> getNextTaskBatch() {
        return taskBatches.poll();
    }

    public Set<Integer> getTaskSizesSet() {
        return nestedTaskOfferingListsMap.keySet();
    }

    public Set<TaskSource<T,W>> getSources() {
        return taskRequests.stream().map(TaskRequest::getTaskSource).collect(Collectors.toSet());
    }
}
