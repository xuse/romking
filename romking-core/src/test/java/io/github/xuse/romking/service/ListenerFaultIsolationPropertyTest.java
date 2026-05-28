package io.github.xuse.romking.service;

import io.github.xuse.romking.tasks.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for listener fault isolation in GlobalTaskService.
 *
 * <p><b>Validates: Requirements 5.5, 5.6</b></p>
 *
 * <p>Property 6: For any ordered list of registered listeners where listener at index K
 * throws an exception during callback invocation, all listeners at indices K+1 through N-1
 * SHALL still receive the notification, and the task execution SHALL not be blocked.</p>
 */
class ListenerFaultIsolationPropertyTest {

    /**
     * Property: When a listener at index K throws during onProgressChanged,
     * all listeners at indices K+1 through N-1 still receive the notification.
     */
    @Property(tries = 50)
    void allListenersAfterThrowingOneStillReceiveProgressNotification(
            @ForAll @IntRange(min = 2, max = 10) int listenerCount,
            @ForAll @IntRange(min = 0, max = 9) int throwingIndex) {

        // Ensure throwingIndex is within bounds
        int effectiveThrowingIndex = throwingIndex % listenerCount;

        GlobalTaskService service = new GlobalTaskService();
        AtomicInteger[] receivedCounts = new AtomicInteger[listenerCount];
        for (int i = 0; i < listenerCount; i++) {
            receivedCounts[i] = new AtomicInteger(0);
        }

        // Register listeners, one of which throws
        for (int i = 0; i < listenerCount; i++) {
            final int index = i;
            service.addListener(new TaskProgressListener() {
                @Override
                public void onProgressChanged(Task task, TaskProgress progress) {
                    if (index == effectiveThrowingIndex) {
                        throw new RuntimeException("Simulated failure at listener " + index);
                    }
                    receivedCounts[index].incrementAndGet();
                }

                @Override
                public void onTaskCompleted(Task task, ProcessResult result) {
                    // not used in this test
                }
            });
        }

        // Invoke notifyProgressChanged via reflection
        Task testTask = createTestTask();
        TaskProgress progress = new TaskProgress("test step", 5, 10);
        invokeNotifyProgressChanged(service, testTask, progress);

        // All listeners except the throwing one should have received the notification
        for (int i = 0; i < listenerCount; i++) {
            if (i == effectiveThrowingIndex) {
                // The throwing listener won't increment its counter
                assertEquals(0, receivedCounts[i].get(),
                        "Throwing listener at index " + i + " should not have incremented counter");
            } else {
                assertEquals(1, receivedCounts[i].get(),
                        "Listener at index " + i + " should have received exactly 1 notification");
            }
        }
    }

    /**
     * Property: When a listener at index K throws during onTaskCompleted,
     * all listeners at indices K+1 through N-1 still receive the completion notification.
     */
    @Property(tries = 50)
    void allListenersAfterThrowingOneStillReceiveCompletionNotification(
            @ForAll @IntRange(min = 2, max = 10) int listenerCount,
            @ForAll @IntRange(min = 0, max = 9) int throwingIndex) {

        int effectiveThrowingIndex = throwingIndex % listenerCount;

        GlobalTaskService service = new GlobalTaskService();
        AtomicInteger[] receivedCounts = new AtomicInteger[listenerCount];
        for (int i = 0; i < listenerCount; i++) {
            receivedCounts[i] = new AtomicInteger(0);
        }

        for (int i = 0; i < listenerCount; i++) {
            final int index = i;
            service.addListener(new TaskProgressListener() {
                @Override
                public void onProgressChanged(Task task, TaskProgress progress) {
                    // not used in this test
                }

                @Override
                public void onTaskCompleted(Task task, ProcessResult result) {
                    if (index == effectiveThrowingIndex) {
                        throw new RuntimeException("Simulated failure at listener " + index);
                    }
                    receivedCounts[index].incrementAndGet();
                }
            });
        }

        Task testTask = createTestTask();
        ProcessResult result = new ProcessResult(200, "done");
        invokeNotifyTaskCompleted(service, testTask, result);

        for (int i = 0; i < listenerCount; i++) {
            if (i == effectiveThrowingIndex) {
                assertEquals(0, receivedCounts[i].get(),
                        "Throwing listener at index " + i + " should not have incremented counter");
            } else {
                assertEquals(1, receivedCounts[i].get(),
                        "Listener at index " + i + " should have received exactly 1 completion notification");
            }
        }
    }

    /**
     * Property: When multiple listeners throw exceptions, all non-throwing listeners
     * still receive the notification (fault isolation is per-listener, not just first failure).
     */
    @Property(tries = 50)
    void multipleThrowingListenersDoNotBlockOthers(
            @ForAll @IntRange(min = 3, max = 10) int listenerCount,
            @ForAll @IntRange(min = 1, max = 5) int throwingCount) {

        int effectiveThrowingCount = Math.min(throwingCount, listenerCount - 1); // at least one non-throwing

        GlobalTaskService service = new GlobalTaskService();
        List<Boolean> shouldThrow = new ArrayList<>();
        AtomicInteger[] receivedCounts = new AtomicInteger[listenerCount];

        // Distribute throwing listeners evenly
        for (int i = 0; i < listenerCount; i++) {
            receivedCounts[i] = new AtomicInteger(0);
            shouldThrow.add(i < effectiveThrowingCount);
        }

        for (int i = 0; i < listenerCount; i++) {
            final int index = i;
            service.addListener(new TaskProgressListener() {
                @Override
                public void onProgressChanged(Task task, TaskProgress progress) {
                    if (shouldThrow.get(index)) {
                        throw new RuntimeException("Simulated failure at listener " + index);
                    }
                    receivedCounts[index].incrementAndGet();
                }

                @Override
                public void onTaskCompleted(Task task, ProcessResult result) {
                    if (shouldThrow.get(index)) {
                        throw new RuntimeException("Simulated failure at listener " + index);
                    }
                    receivedCounts[index].incrementAndGet();
                }
            });
        }

        Task testTask = createTestTask();
        TaskProgress progress = new TaskProgress("step", 1, 10);
        invokeNotifyProgressChanged(service, testTask, progress);

        // Verify all non-throwing listeners received the notification
        for (int i = 0; i < listenerCount; i++) {
            if (shouldThrow.get(i)) {
                assertEquals(0, receivedCounts[i].get(),
                        "Throwing listener at index " + i + " should not have incremented counter");
            } else {
                assertEquals(1, receivedCounts[i].get(),
                        "Non-throwing listener at index " + i + " should have received notification");
            }
        }
    }

    /**
     * Property: Task execution is not blocked by listener exceptions.
     * Verifies that notifyProgressChanged and notifyTaskCompleted return normally
     * even when listeners throw.
     */
    @Property(tries = 50)
    void taskExecutionNotBlockedByListenerExceptions(
            @ForAll @IntRange(min = 1, max = 10) int listenerCount) {

        GlobalTaskService service = new GlobalTaskService();

        // All listeners throw
        for (int i = 0; i < listenerCount; i++) {
            service.addListener(new TaskProgressListener() {
                @Override
                public void onProgressChanged(Task task, TaskProgress progress) {
                    throw new RuntimeException("All listeners fail");
                }

                @Override
                public void onTaskCompleted(Task task, ProcessResult result) {
                    throw new RuntimeException("All listeners fail");
                }
            });
        }

        Task testTask = createTestTask();
        TaskProgress progress = new TaskProgress("step", 3, 10);
        ProcessResult result = new ProcessResult(200, "success");

        // These should NOT throw even though all listeners throw
        assertDoesNotThrow(() -> invokeNotifyProgressChanged(service, testTask, progress),
                "notifyProgressChanged should not propagate listener exceptions");
        assertDoesNotThrow(() -> invokeNotifyTaskCompleted(service, testTask, result),
                "notifyTaskCompleted should not propagate listener exceptions");
    }

    // --- Helper methods ---

    private Task createTestTask() {
        return new Task() {
            private final long begin = System.currentTimeMillis();

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
                return begin;
            }

            @Override
            public TaskProgress getTaskProgress() {
                return new TaskProgress("testing", 0, 0);
            }

            @Override
            public ProcessResult execute() {
                return new ProcessResult(200, "ok");
            }
        };
    }

    private void invokeNotifyProgressChanged(GlobalTaskService service, Task task, TaskProgress progress) {
        try {
            Method method = GlobalTaskService.class.getDeclaredMethod(
                    "notifyProgressChanged", Task.class, TaskProgress.class);
            method.setAccessible(true);
            method.invoke(service, task, progress);
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException("Failed to invoke notifyProgressChanged", e);
        }
    }

    private void invokeNotifyTaskCompleted(GlobalTaskService service, Task task, ProcessResult result) {
        try {
            Method method = GlobalTaskService.class.getDeclaredMethod(
                    "notifyTaskCompleted", Task.class, ProcessResult.class);
            method.setAccessible(true);
            method.invoke(service, task, result);
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException("Failed to invoke notifyTaskCompleted", e);
        }
    }
}
