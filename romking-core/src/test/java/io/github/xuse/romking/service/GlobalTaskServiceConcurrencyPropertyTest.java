package io.github.xuse.romking.service;

import io.github.xuse.romking.tasks.ProcessResult;
import io.github.xuse.romking.tasks.Task;
import io.github.xuse.romking.tasks.TaskProgress;
import io.github.xuse.romking.tasks.TaskType;
import net.jqwik.api.*;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for GlobalTaskService task queue concurrency limit.
 *
 * <p><b>Validates: Requirements 10.1, 10.2</b></p>
 *
 * <p>Property 9: For any sequence of task submissions to GlobalTaskService,
 * the number of concurrently active tasks SHALL never exceed 2.</p>
 */
class GlobalTaskServiceConcurrencyPropertyTest {

    /**
     * Property: When activeTasks already contains 2 tasks, submitting any additional task
     * SHALL throw IllegalStateException with message "活动任务数已经达到2".
     */
    @Property
    void concurrencyLimitRejectsThirdTask(
            @ForAll("taskTypes") TaskType existingType1,
            @ForAll("taskTypes") TaskType existingType2,
            @ForAll("taskTypes") TaskType newType,
            @ForAll("taskNames") String name1,
            @ForAll("taskNames") String name2,
            @ForAll("taskNames") String newName) {

        GlobalTaskService service = createServiceWithActiveTasks(
                createStubTask(existingType1, name1),
                createStubTask(existingType2, name2)
        );

        Task thirdTask = createStubTask(newType, newName);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.submit(thirdTask));
        assertEquals("活动任务数已经达到2", ex.getMessage());
    }

    /**
     * Property: When activeTasks contains fewer than 2 tasks and no constraint violations exist,
     * the submission SHALL NOT throw IllegalStateException for concurrency limit.
     * (It may still throw for singleton/duplicate checks, which are tested separately.)
     */
    @Property
    void submissionAllowedWhenBelowConcurrencyLimit(
            @ForAll("taskTypes") TaskType taskType,
            @ForAll("taskNames") String taskName) {

        // Service with 0 active tasks - submission should pass checkTasks
        GlobalTaskService service = createServiceWithActiveTasks();

        Task newTask = createBlockingStubTask(taskType, taskName);

        // Should not throw for concurrency limit (task will be added to activeTasks)
        assertDoesNotThrow(() -> service.submit(newTask));
    }

    /**
     * Property: When a singleton-type task is already active, submitting another task
     * of the same singleton type SHALL throw IllegalStateException with message "同类任务已经在运行".
     *
     * <p>Note: Currently all TaskType values have singleton=false. This test validates the
     * checkTasks logic by using reflection to temporarily set the singleton field to true.</p>
     */
    @Property
    void singletonTypeCheckRejectsDuplicateType(
            @ForAll("taskTypes") TaskType type,
            @ForAll("taskNames") String existingName,
            @ForAll("taskNames") String newName) throws Exception {

        // Temporarily set singleton=true via reflection to test the code path
        Field singletonField = TaskType.class.getDeclaredField("singleton");
        singletonField.setAccessible(true);
        boolean originalValue = type.singleton;
        try {
            singletonField.setBoolean(type, true);

            GlobalTaskService service = createServiceWithActiveTasks(
                    createStubTask(type, existingName)
            );

            Task duplicateTypeTask = createStubTask(type, newName);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> service.submit(duplicateTypeTask));
            assertEquals("同类任务已经在运行", ex.getMessage());
        } finally {
            singletonField.setBoolean(type, originalValue);
        }
    }

    /**
     * Property: When a non-singleton-type task with the same name is already active,
     * submitting another task of the same type and name SHALL throw IllegalStateException
     * with message "相同的任务已经在运行".
     */
    @Property
    void duplicateNameCheckRejectsSameNameSameType(
            @ForAll("taskTypes") TaskType taskType,
            @ForAll("taskNames") String sameName) {

        // All current TaskType values are non-singleton
        GlobalTaskService service = createServiceWithActiveTasks(
                createStubTask(taskType, sameName)
        );

        Task duplicateNameTask = createBlockingStubTask(taskType, sameName);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.submit(duplicateNameTask));
        assertEquals("相同的任务已经在运行", ex.getMessage());
    }

    /**
     * Property: For non-singleton types with different names, submission is allowed
     * when below the concurrency limit.
     */
    @Property
    void differentNameAllowedForNonSingletonType(
            @ForAll("taskTypes") TaskType taskType,
            @ForAll("distinctNamePairs") String[] namePair) {

        String existingName = namePair[0];
        String newName = namePair[1];

        GlobalTaskService service = createServiceWithActiveTasks(
                createStubTask(taskType, existingName)
        );

        Task newTask = createBlockingStubTask(taskType, newName);

        // Should not throw - different name, non-singleton, below limit
        assertDoesNotThrow(() -> service.submit(newTask));
    }

    // --- Providers ---

    @Provide
    Arbitrary<TaskType> taskTypes() {
        return Arbitraries.of(TaskType.values());
    }

    @Provide
    Arbitrary<String> taskNames() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
    }

    @Provide
    Arbitrary<String[]> distinctNamePairs() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                .tuple2()
                .filter(t -> !t.get1().equals(t.get2()))
                .map(t -> new String[]{t.get1(), t.get2()});
    }

    // --- Helpers ---

    /**
     * Creates a GlobalTaskService and injects stub tasks into its activeTasks list
     * via reflection, simulating tasks that are already running.
     */
    private GlobalTaskService createServiceWithActiveTasks(Task... tasks) {
        GlobalTaskService service = new GlobalTaskService();
        try {
            Field activeTasksField = GlobalTaskService.class.getDeclaredField("activeTasks");
            activeTasksField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Task> activeTasks = (List<Task>) activeTasksField.get(service);
            for (Task task : tasks) {
                activeTasks.add(task);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up activeTasks via reflection", e);
        }
        return service;
    }

    /**
     * Creates a simple stub Task that does nothing on execute.
     * Used for pre-populating the activeTasks list.
     */
    private Task createStubTask(TaskType type, String name) {
        return new Task() {
            @Override
            public TaskType getType() { return type; }

            @Override
            public String getName() { return name; }

            @Override
            public long getBegin() { return System.currentTimeMillis(); }

            @Override
            public TaskProgress getTaskProgress() { return null; }

            @Override
            public ProcessResult execute() {
                return new ProcessResult(200, "done");
            }
        };
    }

    /**
     * Creates a stub Task that blocks on execute (sleeps for a long time).
     * Used when we need submit() to succeed and the task to remain in activeTasks
     * without completing immediately.
     */
    private Task createBlockingStubTask(TaskType type, String name) {
        return new Task() {
            @Override
            public TaskType getType() { return type; }

            @Override
            public String getName() { return name; }

            @Override
            public long getBegin() { return System.currentTimeMillis(); }

            @Override
            public TaskProgress getTaskProgress() { return null; }

            @Override
            public ProcessResult execute() {
                try {
                    Thread.sleep(60_000); // Block for a long time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new ProcessResult(200, "done");
            }
        };
    }
}
