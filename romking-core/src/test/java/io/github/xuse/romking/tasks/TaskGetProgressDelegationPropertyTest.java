package io.github.xuse.romking.tasks;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for getProgress() delegation round-trip.
 *
 * <p><b>Validates: Requirements 2.2</b></p>
 *
 * <p>Property 2: For any Task implementation with a non-null TaskProgress, calling
 * getProgress() SHALL return a string identical to getTaskProgress().toString().
 * When getTaskProgress() returns null, getProgress() SHALL return "".</p>
 */
class TaskGetProgressDelegationPropertyTest {

    /**
     * Property: For any Task with a non-null TaskProgress, getProgress() returns
     * the same string as getTaskProgress().toString().
     */
    @Property
    void getProgressDelegatesToTaskProgressToString(
            @ForAll("currentSteps") String currentStep,
            @ForAll @IntRange(min = 0, max = 10000) int processedItems,
            @ForAll @IntRange(min = 0, max = 10000) int totalItems) {

        TaskProgress taskProgress = new TaskProgress(currentStep, processedItems, totalItems);

        Task task = new Task() {
            @Override
            public TaskType getType() {
                return TaskType.SCAN_DIR;
            }

            @Override
            public String getName() {
                return "test-task";
            }

            @Override
            public long getBegin() {
                return System.currentTimeMillis();
            }

            @Override
            public TaskProgress getTaskProgress() {
                return taskProgress;
            }

            @Override
            public ProcessResult execute() {
                return new ProcessResult(200, "ok");
            }
        };

        assertEquals(taskProgress.toString(), task.getProgress(),
                String.format("getProgress() should equal getTaskProgress().toString() for step='%s', processed=%d, total=%d",
                        currentStep, processedItems, totalItems));
    }

    /**
     * Property: When getTaskProgress() returns null, getProgress() returns "".
     */
    @Property
    void getProgressReturnsEmptyStringWhenTaskProgressIsNull() {

        Task task = new Task() {
            @Override
            public TaskType getType() {
                return TaskType.SCAN_DIR;
            }

            @Override
            public String getName() {
                return "null-progress-task";
            }

            @Override
            public long getBegin() {
                return System.currentTimeMillis();
            }

            @Override
            public TaskProgress getTaskProgress() {
                return null;
            }

            @Override
            public ProcessResult execute() {
                return new ProcessResult(200, "ok");
            }
        };

        assertEquals("", task.getProgress(),
                "getProgress() should return empty string when getTaskProgress() returns null");
    }

    @Provide
    Arbitrary<String> currentSteps() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(50)
                .alpha()
                .map(s -> s.isEmpty() ? "step" : s);
    }
}
