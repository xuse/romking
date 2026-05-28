package io.github.xuse.romaster.ui.support;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import io.github.xuse.romking.service.GlobalTaskService;
import io.github.xuse.romking.tasks.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.vaadin.flow.component.UI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for InlineProgressHelper task filtering.
 *
 * <p><b>Validates: Requirements 9.3, 9.4</b></p>
 *
 * <p>Property 8: For any InlineProgressHelper subscribed to Task A, progress or
 * completion events for a different Task B SHALL NOT trigger the helper's callbacks.</p>
 */
class InlineProgressHelperTaskFilteringPropertyTest {

    /**
     * Property: Progress events for a different Task B do NOT trigger the
     * progress callback of a helper subscribed to Task A.
     */
    @Property
    void progressEventsForDifferentTaskDoNotTriggerCallback(
            @ForAll("taskProgressProvider") TaskProgress progressA,
            @ForAll("taskProgressProvider") TaskProgress progressB) {

        GlobalTaskService service = new GlobalTaskService();
        Task taskA = createStubTask("task-A", TaskType.SCAN_DIR);
        Task taskB = createStubTask("task-B", TaskType.EXPORT);

        AtomicInteger progressCallCount = new AtomicInteger(0);
        AtomicInteger completionCallCount = new AtomicInteger(0);

        // Mock UI to bypass Vaadin's UI.access() — execute the command immediately
        UI mockUI = mock(UI.class);
        doAnswer(invocation -> {
            com.vaadin.flow.server.Command command = invocation.getArgument(0);
            command.execute();
            return null;
        }).when(mockUI).access(any(com.vaadin.flow.server.Command.class));

        Consumer<TaskProgress> progressCallback = p -> progressCallCount.incrementAndGet();
        Consumer<ProcessResult> completionCallback = r -> completionCallCount.incrementAndGet();

        // Subscribe helper to Task A
        InlineProgressHelper helper = new InlineProgressHelper(
                taskA, mockUI, progressCallback, completionCallback, service);

        // Fire progress event for Task B — should NOT trigger callback
        helper.onProgressChanged(taskB, progressB);

        assertEquals(0, progressCallCount.get(),
                "Progress callback should NOT be invoked for a different task");
        assertEquals(0, completionCallCount.get(),
                "Completion callback should NOT be invoked for a different task");

        // Cleanup
        helper.unsubscribe();
    }

    /**
     * Property: Completion events for a different Task B do NOT trigger the
     * completion callback of a helper subscribed to Task A.
     */
    @Property
    void completionEventsForDifferentTaskDoNotTriggerCallback(
            @ForAll("processResultProvider") ProcessResult resultB) {

        GlobalTaskService service = new GlobalTaskService();
        Task taskA = createStubTask("task-A", TaskType.SCAN_DIR);
        Task taskB = createStubTask("task-B", TaskType.IMPORT_DAT);

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

        // Fire completion event for Task B — should NOT trigger callback
        helper.onTaskCompleted(taskB, resultB);

        assertEquals(0, progressCallCount.get(),
                "Progress callback should NOT be invoked for a different task's completion");
        assertEquals(0, completionCallCount.get(),
                "Completion callback should NOT be invoked for a different task");

        // Cleanup
        helper.unsubscribe();
    }

    /**
     * Property: Progress events for the subscribed Task A DO trigger the
     * progress callback exactly once per event.
     */
    @Property
    void progressEventsForSubscribedTaskDoTriggerCallback(
            @ForAll @IntRange(min = 1, max = 10) int eventCount,
            @ForAll("taskProgressProvider") TaskProgress progress) {

        GlobalTaskService service = new GlobalTaskService();
        Task taskA = createStubTask("task-A", TaskType.SCAN_DIR);

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

        // Fire progress events for Task A — should trigger callback
        for (int i = 0; i < eventCount; i++) {
            helper.onProgressChanged(taskA, progress);
        }

        assertEquals(eventCount, progressCallCount.get(),
                "Progress callback should be invoked exactly once per event for the subscribed task");
        assertEquals(0, completionCallCount.get(),
                "Completion callback should NOT be invoked for progress events");

        // Cleanup
        helper.unsubscribe();
    }

    /**
     * Property: Completion event for the subscribed Task A DOES trigger the
     * completion callback exactly once.
     */
    @Property
    void completionEventForSubscribedTaskDoesTriggerCallback(
            @ForAll("processResultProvider") ProcessResult result) {

        GlobalTaskService service = new GlobalTaskService();
        Task taskA = createStubTask("task-A", TaskType.ARCHIVE);

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

        // Fire completion event for Task A — should trigger callback
        helper.onTaskCompleted(taskA, result);

        assertEquals(0, progressCallCount.get(),
                "Progress callback should NOT be invoked for completion events");
        assertEquals(1, completionCallCount.get(),
                "Completion callback should be invoked exactly once for the subscribed task");
    }

    /**
     * Property: Mixed events — progress for Task A triggers callback,
     * progress for Task B does not, regardless of ordering.
     */
    @Property
    void mixedEventsOnlyTriggerForSubscribedTask(
            @ForAll @IntRange(min = 1, max = 5) int taskAEvents,
            @ForAll @IntRange(min = 1, max = 5) int taskBEvents,
            @ForAll("taskProgressProvider") TaskProgress progressA,
            @ForAll("taskProgressProvider") TaskProgress progressB) {

        GlobalTaskService service = new GlobalTaskService();
        Task taskA = createStubTask("task-A", TaskType.VERIFY);
        Task taskB = createStubTask("task-B", TaskType.SCAN_DIR);

        AtomicInteger progressCallCount = new AtomicInteger(0);

        UI mockUI = mock(UI.class);
        doAnswer(invocation -> {
            com.vaadin.flow.server.Command command = invocation.getArgument(0);
            command.execute();
            return null;
        }).when(mockUI).access(any(com.vaadin.flow.server.Command.class));

        Consumer<TaskProgress> progressCallback = p -> progressCallCount.incrementAndGet();
        Consumer<ProcessResult> completionCallback = r -> {};

        InlineProgressHelper helper = new InlineProgressHelper(
                taskA, mockUI, progressCallback, completionCallback, service);

        // Interleave events for Task A and Task B
        for (int i = 0; i < Math.max(taskAEvents, taskBEvents); i++) {
            if (i < taskBEvents) {
                helper.onProgressChanged(taskB, progressB);
            }
            if (i < taskAEvents) {
                helper.onProgressChanged(taskA, progressA);
            }
        }

        assertEquals(taskAEvents, progressCallCount.get(),
                "Only events for the subscribed task should trigger the callback");

        // Cleanup
        helper.unsubscribe();
    }

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
}
