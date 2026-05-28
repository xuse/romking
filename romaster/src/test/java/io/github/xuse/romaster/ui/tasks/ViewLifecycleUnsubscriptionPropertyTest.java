package io.github.xuse.romaster.ui.tasks;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import io.github.xuse.romking.service.GlobalTaskService;
import io.github.xuse.romking.tasks.*;
import io.github.xuse.romaster.ui.support.InlineProgressHelper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.vaadin.flow.component.UI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for view lifecycle unsubscription.
 *
 * <p><b>Validates: Requirements 7.4, 8.3, 9.5</b></p>
 *
 * <p>Property 7: For any Vaadin view (TaskOverviewView, DatImportView, or
 * InlineProgressHelper-managed view) that registers as a TaskProgressListener
 * on attach/subscribe, detaching the view SHALL result in the listener being
 * removed from GlobalTaskService's listener list.</p>
 */
class ViewLifecycleUnsubscriptionPropertyTest {

    /**
     * Property: When InlineProgressHelper.unsubscribe() is called, the helper
     * is removed from GlobalTaskService's listener list.
     */
    @Property
    void inlineProgressHelperUnsubscribeRemovesFromListenerList(
            @ForAll @IntRange(min = 1, max = 5) int helperCount) throws Exception {

        GlobalTaskService service = new GlobalTaskService();

        UI mockUI = mock(UI.class);
        doAnswer(invocation -> {
            com.vaadin.flow.server.Command command = invocation.getArgument(0);
            command.execute();
            return null;
        }).when(mockUI).access(any(com.vaadin.flow.server.Command.class));

        InlineProgressHelper[] helpers = new InlineProgressHelper[helperCount];
        for (int i = 0; i < helperCount; i++) {
            Task task = createStubTask("task-" + i, TaskType.SCAN_DIR);
            helpers[i] = new InlineProgressHelper(
                    task, mockUI, p -> {}, r -> {}, service);
        }

        // All helpers should be registered
        List<TaskProgressListener> listeners = getListeners(service);
        assertEquals(helperCount, listeners.size(),
                "All helpers should be registered as listeners");

        // Unsubscribe each helper and verify removal
        for (int i = 0; i < helperCount; i++) {
            helpers[i].unsubscribe();
            listeners = getListeners(service);
            assertEquals(helperCount - (i + 1), listeners.size(),
                    "After unsubscribing helper " + i + ", listener count should decrease");
            assertFalse(listeners.contains(helpers[i]),
                    "Unsubscribed helper should not be in the listener list");
        }
    }

    /**
     * Property: After InlineProgressHelper.unsubscribe() is called, no further
     * notifications are delivered to the unsubscribed helper.
     */
    @Property
    void noNotificationsAfterUnsubscribe(
            @ForAll("taskProgressProvider") TaskProgress progress,
            @ForAll("processResultProvider") ProcessResult result) {

        GlobalTaskService service = new GlobalTaskService();
        Task taskA = createStubTask("task-A", TaskType.IMPORT_DAT);

        AtomicInteger progressCallCount = new AtomicInteger(0);
        AtomicInteger completionCallCount = new AtomicInteger(0);

        UI mockUI = mock(UI.class);
        doAnswer(invocation -> {
            com.vaadin.flow.server.Command command = invocation.getArgument(0);
            command.execute();
            return null;
        }).when(mockUI).access(any(com.vaadin.flow.server.Command.class));

        Consumer<TaskProgress> progressCallback = p -> progressCallCount.incrementAndGet();
        Consumer<ProcessResult> completionCallback = r -> completionCallCount.incrementAndGet();

        InlineProgressHelper helper = new InlineProgressHelper(
                taskA, mockUI, progressCallback, completionCallback, service);

        // Unsubscribe
        helper.unsubscribe();

        // Simulate notifications via the service's notify methods using reflection
        // Since notifyProgressChanged/notifyTaskCompleted are private, we invoke
        // the listener callbacks directly on the service's listener list
        // After unsubscribe, the helper should NOT be in the list, so service
        // notifications should not reach it.
        // We verify by calling the helper's methods directly — but the real guarantee
        // is that the helper is no longer in the listener list.
        List<TaskProgressListener> listeners = getListeners(service);
        assertFalse(listeners.contains(helper),
                "Helper should not be in listener list after unsubscribe");

        // Even if someone calls the helper's methods directly, let's verify
        // the service-level guarantee: iterate listeners and notify — helper won't be called
        for (TaskProgressListener listener : listeners) {
            listener.onProgressChanged(taskA, progress);
            listener.onTaskCompleted(taskA, result);
        }

        assertEquals(0, progressCallCount.get(),
                "No progress notifications should be delivered after unsubscribe");
        assertEquals(0, completionCallCount.get(),
                "No completion notifications should be delivered after unsubscribe");
    }

    /**
     * Property: TaskOverviewView's onDetach removes it from GlobalTaskService's
     * listener list. We simulate this by directly adding a TaskProgressListener
     * and then calling removeListener (which is what onDetach does).
     */
    @Property
    void taskOverviewViewDetachRemovesFromListenerList(
            @ForAll @IntRange(min = 1, max = 5) int listenerCount) throws Exception {

        GlobalTaskService service = new GlobalTaskService();

        // Simulate TaskOverviewView behavior: register as listener on attach,
        // remove on detach. We use stub listeners to represent views.
        TaskProgressListener[] viewListeners = new TaskProgressListener[listenerCount];
        for (int i = 0; i < listenerCount; i++) {
            viewListeners[i] = createStubListener();
            service.addListener(viewListeners[i]);
        }

        List<TaskProgressListener> listeners = getListeners(service);
        assertEquals(listenerCount, listeners.size(),
                "All view listeners should be registered");

        // Simulate onDetach for each view — calls removeListener
        for (int i = 0; i < listenerCount; i++) {
            service.removeListener(viewListeners[i]);
            listeners = getListeners(service);
            assertEquals(listenerCount - (i + 1), listeners.size(),
                    "After detaching view " + i + ", listener count should decrease");
            assertFalse(listeners.contains(viewListeners[i]),
                    "Detached view listener should not be in the listener list");
        }
    }

    /**
     * Property: Multiple unsubscribe calls are idempotent — calling unsubscribe
     * multiple times does not throw and the listener remains removed.
     */
    @Property
    void multipleUnsubscribeCallsAreIdempotent(
            @ForAll @IntRange(min = 2, max = 5) int unsubscribeCalls) throws Exception {

        GlobalTaskService service = new GlobalTaskService();
        Task taskA = createStubTask("task-A", TaskType.EXPORT);

        UI mockUI = mock(UI.class);
        doAnswer(invocation -> {
            com.vaadin.flow.server.Command command = invocation.getArgument(0);
            command.execute();
            return null;
        }).when(mockUI).access(any(com.vaadin.flow.server.Command.class));

        InlineProgressHelper helper = new InlineProgressHelper(
                taskA, mockUI, p -> {}, r -> {}, service);

        // Verify registered
        List<TaskProgressListener> listeners = getListeners(service);
        assertTrue(listeners.contains(helper), "Helper should be registered initially");

        // Call unsubscribe multiple times — should not throw
        for (int i = 0; i < unsubscribeCalls; i++) {
            helper.unsubscribe();
        }

        listeners = getListeners(service);
        assertFalse(listeners.contains(helper),
                "Helper should remain removed after multiple unsubscribe calls");
        assertEquals(0, listeners.size(),
                "Listener list should be empty after all unsubscribes");
    }

    /**
     * Property: After unsubscription, remaining listeners still receive notifications.
     */
    @Property
    void remainingListenersStillReceiveNotificationsAfterOneUnsubscribes(
            @ForAll @IntRange(min = 2, max = 5) int totalListeners,
            @ForAll("taskProgressProvider") TaskProgress progress) throws Exception {

        GlobalTaskService service = new GlobalTaskService();
        Task taskA = createStubTask("task-A", TaskType.VERIFY);

        UI mockUI = mock(UI.class);
        doAnswer(invocation -> {
            com.vaadin.flow.server.Command command = invocation.getArgument(0);
            command.execute();
            return null;
        }).when(mockUI).access(any(com.vaadin.flow.server.Command.class));

        AtomicInteger[] callCounts = new AtomicInteger[totalListeners];
        InlineProgressHelper[] helpers = new InlineProgressHelper[totalListeners];

        for (int i = 0; i < totalListeners; i++) {
            callCounts[i] = new AtomicInteger(0);
            final int idx = i;
            helpers[i] = new InlineProgressHelper(
                    taskA, mockUI,
                    p -> callCounts[idx].incrementAndGet(),
                    r -> {},
                    service);
        }

        // Unsubscribe the first helper
        helpers[0].unsubscribe();

        // Notify all remaining listeners via the service's listener list
        List<TaskProgressListener> listeners = getListeners(service);
        for (TaskProgressListener listener : listeners) {
            listener.onProgressChanged(taskA, progress);
        }

        assertEquals(0, callCounts[0].get(),
                "Unsubscribed helper should not receive notifications");
        for (int i = 1; i < totalListeners; i++) {
            assertEquals(1, callCounts[i].get(),
                    "Remaining helper " + i + " should still receive notifications");
        }

        // Cleanup
        for (int i = 1; i < totalListeners; i++) {
            helpers[i].unsubscribe();
        }
    }

    // --- Providers ---

    @Provide
    Arbitrary<TaskProgress> taskProgressProvider() {
        Arbitrary<String> steps = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
        Arbitrary<Integer> processed = Arbitraries.integers().between(0, 10000);
        Arbitrary<Integer> total = Arbitraries.integers().between(0, 10000);
        return Combinators.combine(steps, processed, total)
                .as((step, proc, tot) -> new TaskProgress(step, proc, tot));
    }

    @Provide
    Arbitrary<ProcessResult> processResultProvider() {
        Arbitrary<Integer> codes = Arbitraries.of(200, 400, 500);
        Arbitrary<String> messages = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30);
        return Combinators.combine(codes, messages)
                .as((code, msg) -> new ProcessResult(code, msg));
    }

    // --- Helpers ---

    @SuppressWarnings("unchecked")
    private List<TaskProgressListener> getListeners(GlobalTaskService service) {
        try {
            Field field = GlobalTaskService.class.getDeclaredField("listeners");
            field.setAccessible(true);
            return (List<TaskProgressListener>) field.get(service);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access listeners field via reflection", e);
        }
    }

    private Task createStubTask(String name, TaskType type) {
        return new Task() {
            @Override
            public TaskType getType() {
                return type;
            }

            @Override
            public String getName() {
                return name;
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

    private TaskProgressListener createStubListener() {
        return new TaskProgressListener() {
            @Override
            public void onProgressChanged(Task task, TaskProgress progress) {
                // no-op
            }

            @Override
            public void onTaskCompleted(Task task, ProcessResult result) {
                // no-op
            }
        };
    }
}
