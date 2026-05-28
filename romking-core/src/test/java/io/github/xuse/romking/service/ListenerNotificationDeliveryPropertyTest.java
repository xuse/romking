package io.github.xuse.romking.service;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import io.github.xuse.romking.tasks.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for listener notification delivery in GlobalTaskService.
 *
 * <p><b>Validates: Requirements 5.3</b></p>
 *
 * <p>Property 4: For any set of N registered TaskProgressListeners and any progress
 * change event, GlobalTaskService SHALL invoke onProgressChanged on all N listeners
 * exactly once per event.</p>
 */
class ListenerNotificationDeliveryPropertyTest {

    /**
     * Property: All N registered listeners receive exactly one onProgressChanged
     * notification per notifyProgressChanged invocation.
     */
    @Property
    void allRegisteredListenersReceiveProgressNotification(
            @ForAll @IntRange(min = 1, max = 50) int listenerCount,
            @ForAll("taskProgressProvider") TaskProgress progress) throws Exception {

        GlobalTaskService service = new GlobalTaskService();

        // Track invocation counts per listener
        List<AtomicInteger> invocationCounts = new ArrayList<>();
        List<TaskProgressListener> listeners = new ArrayList<>();

        for (int i = 0; i < listenerCount; i++) {
            AtomicInteger count = new AtomicInteger(0);
            invocationCounts.add(count);
            TaskProgressListener listener = new TaskProgressListener() {
                @Override
                public void onProgressChanged(Task task, TaskProgress p) {
                    count.incrementAndGet();
                }

                @Override
                public void onTaskCompleted(Task task, ProcessResult result) {
                    // not relevant for this property
                }
            };
            listeners.add(listener);
            service.addListener(listener);
        }

        // Create a simple task stub
        Task stubTask = createStubTask();

        // Invoke notifyProgressChanged via reflection (it's private)
        Method notifyMethod = GlobalTaskService.class.getDeclaredMethod(
                "notifyProgressChanged", Task.class, TaskProgress.class);
        notifyMethod.setAccessible(true);
        notifyMethod.invoke(service, stubTask, progress);

        // Verify all listeners received exactly one notification
        for (int i = 0; i < listenerCount; i++) {
            assertEquals(1, invocationCounts.get(i).get(),
                    String.format("Listener %d should have been notified exactly once, but was notified %d times",
                            i, invocationCounts.get(i).get()));
        }
    }

    /**
     * Property: Listeners that are removed before notification do NOT receive the event,
     * while remaining listeners still receive exactly one notification each.
     */
    @Property
    void removedListenersDoNotReceiveNotification(
            @ForAll @IntRange(min = 2, max = 30) int totalListeners,
            @ForAll @IntRange(min = 1, max = 29) int removeCount,
            @ForAll("taskProgressProvider") TaskProgress progress) throws Exception {

        // Ensure removeCount < totalListeners
        int actualRemoveCount = Math.min(removeCount, totalListeners - 1);

        GlobalTaskService service = new GlobalTaskService();

        List<AtomicInteger> invocationCounts = new ArrayList<>();
        List<TaskProgressListener> listeners = new ArrayList<>();

        for (int i = 0; i < totalListeners; i++) {
            AtomicInteger count = new AtomicInteger(0);
            invocationCounts.add(count);
            TaskProgressListener listener = new TaskProgressListener() {
                @Override
                public void onProgressChanged(Task task, TaskProgress p) {
                    count.incrementAndGet();
                }

                @Override
                public void onTaskCompleted(Task task, ProcessResult result) {
                }
            };
            listeners.add(listener);
            service.addListener(listener);
        }

        // Remove some listeners
        for (int i = 0; i < actualRemoveCount; i++) {
            service.removeListener(listeners.get(i));
        }

        Task stubTask = createStubTask();

        Method notifyMethod = GlobalTaskService.class.getDeclaredMethod(
                "notifyProgressChanged", Task.class, TaskProgress.class);
        notifyMethod.setAccessible(true);
        notifyMethod.invoke(service, stubTask, progress);

        // Removed listeners should NOT have been notified
        for (int i = 0; i < actualRemoveCount; i++) {
            assertEquals(0, invocationCounts.get(i).get(),
                    String.format("Removed listener %d should not have been notified, but was notified %d times",
                            i, invocationCounts.get(i).get()));
        }

        // Remaining listeners should have been notified exactly once
        for (int i = actualRemoveCount; i < totalListeners; i++) {
            assertEquals(1, invocationCounts.get(i).get(),
                    String.format("Remaining listener %d should have been notified exactly once, but was notified %d times",
                            i, invocationCounts.get(i).get()));
        }
    }

    /**
     * Property: Multiple notification events result in exactly one invocation per event per listener.
     */
    @Property
    void multipleEventsDeliverCorrectCountToAllListeners(
            @ForAll @IntRange(min = 1, max = 20) int listenerCount,
            @ForAll @IntRange(min = 1, max = 10) int eventCount,
            @ForAll("taskProgressProvider") TaskProgress progress) throws Exception {

        GlobalTaskService service = new GlobalTaskService();

        List<AtomicInteger> invocationCounts = new ArrayList<>();

        for (int i = 0; i < listenerCount; i++) {
            AtomicInteger count = new AtomicInteger(0);
            invocationCounts.add(count);
            TaskProgressListener listener = new TaskProgressListener() {
                @Override
                public void onProgressChanged(Task task, TaskProgress p) {
                    count.incrementAndGet();
                }

                @Override
                public void onTaskCompleted(Task task, ProcessResult result) {
                }
            };
            service.addListener(listener);
        }

        Task stubTask = createStubTask();

        Method notifyMethod = GlobalTaskService.class.getDeclaredMethod(
                "notifyProgressChanged", Task.class, TaskProgress.class);
        notifyMethod.setAccessible(true);

        // Fire multiple events
        for (int e = 0; e < eventCount; e++) {
            notifyMethod.invoke(service, stubTask, progress);
        }

        // Each listener should have been notified exactly eventCount times
        for (int i = 0; i < listenerCount; i++) {
            assertEquals(eventCount, invocationCounts.get(i).get(),
                    String.format("Listener %d should have been notified %d times, but was notified %d times",
                            i, eventCount, invocationCounts.get(i).get()));
        }
    }

    @Provide
    Arbitrary<TaskProgress> taskProgressProvider() {
        Arbitrary<String> steps = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
        Arbitrary<Integer> processed = Arbitraries.integers().between(0, 10000);
        Arbitrary<Integer> total = Arbitraries.integers().between(0, 10000);
        return Combinators.combine(steps, processed, total)
                .as((step, proc, tot) -> new TaskProgress(step, proc, tot));
    }

    private Task createStubTask() {
        return new Task() {
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
                return new TaskProgress("testing", 0, 0);
            }

            @Override
            public ProcessResult execute() {
                return new ProcessResult(200, "OK");
            }
        };
    }
}
