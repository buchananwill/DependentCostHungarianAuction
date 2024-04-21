package HungarianAuction.TaskElements;

import HungarianAuction.Method.Assignment;
import HungarianAuction.WorkerElements.WorkerGrouping;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskBatch<T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>>{

    private static final int NO_NESTING = 0;
    private static final int BASE_TASK_SIZE = 1;
    private static final AtomicInteger taskBatchCounter = new AtomicInteger();
    private final Set<TaskRequest<T, W>> tasks = new HashSet<>();
    private final int degreeOfNesting;
    private final int taskSize;

    private final int id;
    private Set<Assignment<T,W>> outcomes;

    public TaskBatch(Collection<TaskRequest<T,W>> taskRequests) {
        this(taskRequests, NO_NESTING, BASE_TASK_SIZE);
    }

    public TaskBatch(Collection<TaskRequest<T,W>> taskRequests, int degreeOfNesting, int taskSize) {
        id = taskBatchCounter.getAndIncrement();
        this.degreeOfNesting = degreeOfNesting;
        this.taskSize = taskSize;
        if (taskRequests.isEmpty()) throw new IllegalArgumentException(
                "No tasks provided: " +
                        this +
                        " : " +
                        taskRequests);
        validateTaskRequests(taskRequests);
    }

    private void validateTaskRequests(Collection<TaskRequest<T,W>> taskRequests) {
        for (TaskRequest<T,W> tTaskRequest : taskRequests) {
            if (tTaskRequest.getEntryTokenSize() != this.taskSize)
                throw new IllegalArgumentException("Tasks must all have matching size: " + tTaskRequest + " not of size " + this.taskSize);
            else tasks.add(tTaskRequest);
        }
    }

    public static <T extends TaskSource<T,W>, W extends WorkerGrouping<T,W>> Comparator<TaskBatch<T,W>> getComparator() {
        Comparator<TaskBatch<T,W>> nesting = Comparator.comparing(TaskBatch::getDegreeOfNesting);
        Comparator<TaskBatch<T,W>> taskLength = Comparator.comparing(TaskBatch::getTaskSize);
        Comparator<TaskBatch<T,W>> size = Comparator.comparing(TaskBatch::getBatchSize);
        Comparator<TaskBatch<T,W>> bandwidth = Comparator.comparing(TaskBatch::getTotalTaskBandwidth);
        Comparator<TaskBatch<T,W>> taskID = Comparator.comparing(TaskBatch::hashCode);

        return nesting // Least nesting first - i.e. the root of the allocation tree.
                .thenComparing(taskLength.reversed())  // Largest task lengths first.
                .thenComparing(size) // Batches with fewest tasks first, to keep the combinations suppressed.
                .thenComparing(bandwidth.reversed()) // Bigger bandwidth first.
                .thenComparing(taskID); // Tie-breaker using the ID.

    }

    public int getDegreeOfNesting() {
        return this.degreeOfNesting;
    }

    public int getTaskSize(){
        return this.taskSize;
    }

    public int getBatchSize() {
        return tasks.size();
    }

    public int getTotalTaskBandwidth() {
        return tasks.stream()
                .map(TaskRequest::getTaskSource)
                .mapToInt(TaskSource::getTotalTaskBandwidth)
                .sum();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, degreeOfNesting, taskSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskBatch<?, ?> taskBatch = (TaskBatch<?, ?>) o;

        return id == taskBatch.id;
    }

    @Override
    public String toString() {
        return "TaskBatch{" +
                "; Nesting: " + degreeOfNesting +
                "; TaskLength: " + getTaskSize() +
                "; Size: " + getBatchSize() +
                "; tasks=" + tasks +
                '}';
    }

    @NotNull
    public Set<Assignment<T,W>> getOutcomes() {
        if (outcomes == null) return new HashSet<>();
        return outcomes;
    }

    public void setOutcome(Set<Assignment<T,W>> outcomes) {
        this.outcomes = outcomes;
    }

    public int getMaxTaskBandwidth() {
        return tasks.stream()
                .map(TaskRequest::getTaskSource)
                .mapToInt(TaskSource::getMaxTaskBandwidth)
                .max()
                .orElse(0);
    }

    public Set<TaskRequest<T,W>> getTasks() {
        return new HashSet<>(tasks);
    }
}
