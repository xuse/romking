# Design Document: Task Progress Push

## Architecture Overview

This feature introduces a structured progress model and push-based UI update mechanism, replacing the current polling approach. The architecture follows an observer pattern where `GlobalTaskService` acts as the event source, notifying registered `TaskProgressListener` instances whenever task progress changes or a task completes. The Vaadin UI layer uses server push (`@Push` + `UI.access()`) to safely propagate these notifications to the browser.

```
┌─────────────────────────────────────────────────────────────────┐
│  romking-core (simple-context IoC)                              │
│                                                                 │
│  ┌──────────────┐    notifies     ┌────────────────────────┐   │
│  │ Task impl    │───────────────▶│ GlobalTaskService       │   │
│  │ (updates     │  (progress      │  - listeners: List<>   │   │
│  │  TaskProgress│   callback)     │  - addListener()       │   │
│  │  internally) │                 │  - removeListener()    │   │
│  └──────────────┘                 └───────────┬────────────┘   │
│                                               │                 │
│  ┌──────────────────┐                         │ onProgressChanged│
│  │ TaskProgress     │                         │ onTaskCompleted  │
│  │  - currentStep   │                         ▼                 │
│  │  - processedItems│            ┌────────────────────────┐    │
│  │  - totalItems    │            │ TaskProgressListener   │    │
│  │  - percentage    │            │  (interface)           │    │
│  └──────────────────┘            └────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
                                               │
                    ┌──────────────────────────┼──────────────────┐
                    │  romaster (Spring Boot + Vaadin)            │
                    │                          │                  │
                    │         ┌────────────────┼───────────┐     │
                    │         ▼                ▼            ▼     │
                    │  ┌─────────────┐ ┌────────────┐ ┌────────┐│
                    │  │TaskOverview │ │DatImportView│ │Inline  ││
                    │  │View         │ │(migrated)   │ │Progress││
                    │  │             │ │             │ │Helper  ││
                    │  └─────────────┘ └────────────┘ └────────┘│
                    │         │                │            │     │
                    │         └────────────────┼────────────┘     │
                    │                          ▼                  │
                    │                   UI.access(() -> ...)      │
                    │                   (thread-safe push)        │
                    └────────────────────────────────────────────┘
```

## Components

### 1. TaskProgress (romking-core)

**Package:** `io.github.xuse.romking.tasks`

An immutable value object representing structured progress state.

```java
package io.github.xuse.romking.tasks;

import lombok.Getter;

@Getter
public class TaskProgress {
    private final String currentStep;
    private final int processedItems;
    private final int totalItems;
    private final int percentage;

    public TaskProgress(String currentStep, int processedItems, int totalItems) {
        this.currentStep = currentStep;
        this.processedItems = processedItems;
        this.totalItems = totalItems;
        this.percentage = computePercentage(processedItems, totalItems);
    }

    private static int computePercentage(int processed, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.min(100, Math.max(0, (processed * 100) / total));
    }

    @Override
    public String toString() {
        return String.format("[%d%%] %s (%d/%d)", percentage, currentStep, processedItems, totalItems);
    }
}
```

**Design decisions:**
- Immutable: each progress update creates a new instance, avoiding concurrency issues when listeners read progress from different threads.
- `percentage` is clamped to [0, 100] via `Math.min/Math.max` to handle edge cases where `processedItems > totalItems` (e.g., discovered additional items mid-task).
- Constructor computes percentage eagerly — no lazy computation needed for a simple int division.

### 2. TaskProgressListener (romking-core)

**Package:** `io.github.xuse.romking.tasks`

```java
package io.github.xuse.romking.tasks;

public interface TaskProgressListener {
    void onProgressChanged(Task task, TaskProgress progress);
    void onTaskCompleted(Task task, ProcessResult result);
}
```

**Design decisions:**
- Minimal interface with two callbacks covering the full task lifecycle.
- Resides in `romking-core` so it has no dependency on Vaadin or Spring — any module can implement it.

### 3. Task Interface Enhancement

**Package:** `io.github.xuse.romking.tasks`

```java
package io.github.xuse.romking.tasks;

public interface Task {
    TaskType getType();
    String getName();
    long getBegin();

    /**
     * Returns structured progress data.
     */
    TaskProgress getTaskProgress();

    /**
     * Backward-compatible string representation.
     * Default delegates to getTaskProgress().toString().
     */
    default String getProgress() {
        TaskProgress tp = getTaskProgress();
        return tp != null ? tp.toString() : "";
    }

    ProcessResult execute();
}
```

**Design decisions:**
- `getProgress()` becomes a default method delegating to `getTaskProgress().toString()`, preserving backward compatibility for `GlobalTask.fromActive()` and any other callers.
- `getTaskProgress()` is the new primary method; implementations maintain a `volatile TaskProgress` field updated during execution.

### 4. Task Implementation Pattern

Each task implementation follows the same pattern. Example for `ImportDatTask`:

```java
public class ImportDatTask implements Task {
    private volatile TaskProgress taskProgress = new TaskProgress("等待开始", 0, 0);

    @Override
    public TaskProgress getTaskProgress() {
        return taskProgress;
    }

    @Override
    public ProcessResult execute() {
        this.begin = System.currentTimeMillis();
        // ... during processing:
        taskProgress = new TaskProgress("正在导入: " + fileName, processed, total);
        // The GlobalTaskService detects changes via polling or callback
        // ...
    }
}
```

**Progress reporting mechanism:** Task implementations update their internal `taskProgress` field. `GlobalTaskService` periodically reads this field from the execution wrapper (see below) and fires listener notifications when changes are detected.

### 5. GlobalTaskService Changes

**Package:** `io.github.xuse.romking.service`

```java
@Service
@Slf4j
public class GlobalTaskService implements ListDataProvider<GlobalTask, Void> {

    private final List<TaskProgressListener> listeners = new CopyOnWriteArrayList<>();
    // ... existing fields ...

    public void addListener(TaskProgressListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TaskProgressListener listener) {
        listeners.remove(listener);
    }

    public void submit(Task raw) {
        checkTasks(raw);
        activeTasks.add(raw);
        Runnable task = () -> {
            ProcessResult result;
            try {
                // Start progress monitoring
                ProgressMonitor monitor = new ProgressMonitor(raw);
                monitor.start();
                try {
                    result = raw.execute();
                } finally {
                    monitor.stop();
                }
            } catch (Exception ex) {
                log.error("global task {}.{} error.", raw.getType(), raw.getName(), ex);
                result = new ProcessResult(400, ex.getMessage());
            }
            try {
                saveTask(raw, result);
            } catch (Exception ex) {
                log.error("save task {}.{} error", raw.getType(), raw.getName(), ex);
            } finally {
                activeTasks.remove(raw);
                notifyTaskCompleted(raw, result);
            }
        };
        taskPool.submit(task);
    }

    private void notifyProgressChanged(Task task, TaskProgress progress) {
        for (TaskProgressListener listener : listeners) {
            try {
                listener.onProgressChanged(task, progress);
            } catch (Exception ex) {
                log.error("Listener error on progress change", ex);
            }
        }
    }

    private void notifyTaskCompleted(Task task, ProcessResult result) {
        for (TaskProgressListener listener : listeners) {
            try {
                listener.onTaskCompleted(task, result);
            } catch (Exception ex) {
                log.error("Listener error on task completed", ex);
            }
        }
    }

    // ... existing methods unchanged ...
}
```

**ProgressMonitor inner class:**

```java
private class ProgressMonitor {
    private final Task task;
    private volatile boolean running = true;
    private TaskProgress lastProgress;

    ProgressMonitor(Task task) {
        this.task = task;
    }

    void start() {
        Thread monitorThread = new Thread(() -> {
            while (running) {
                TaskProgress current = task.getTaskProgress();
                if (current != null && !current.equals(lastProgress)) {
                    lastProgress = current;
                    notifyProgressChanged(task, current);
                }
                try {
                    Thread.sleep(500); // Poll every 500ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "ProgressMonitor-" + task.getName());
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    void stop() {
        running = false;
        // Fire one final progress notification
        TaskProgress finalProgress = task.getTaskProgress();
        if (finalProgress != null && !finalProgress.equals(lastProgress)) {
            notifyProgressChanged(task, finalProgress);
        }
    }
}
```

**Design decisions:**
- `CopyOnWriteArrayList` for listeners: safe for concurrent iteration during notification while add/remove happen infrequently.
- Progress monitoring uses a daemon thread polling at 500ms intervals. This decouples task implementations from the notification mechanism — tasks just update their `TaskProgress` field, they don't need to know about listeners.
- Exception isolation: each listener callback is wrapped in try-catch so one failing listener doesn't prevent others from receiving notifications.
- The existing `checkTasks()` logic and 2-thread pool remain unchanged.

### 6. Vaadin Push Configuration

**File:** `io.github.xuse.Application`

```java
@SpringBootApplication
@Theme("default")
@Push  // Enable WebSocket-based server push
@Slf4j
public class Application implements AppShellConfigurator {
    // ... existing code unchanged ...
}
```

**Design decisions:**
- `@Push` with default transport (WebSocket) on the `AppShellConfigurator` class enables push for all views.
- No per-view push configuration needed — it's application-wide.
- All background-to-UI updates must use `UI.access(() -> { ... })`.

### 7. TaskOverviewView

**Package:** `io.github.xuse.romaster.ui.tasks`  
**Route:** `/tasks`

```java
@Route("tasks")
@PageTitle("任务总览")
@Menu(order = 6, icon = "vaadin:tasks", title = "任务总览")
@PermitAll
public class TaskOverviewView extends Main implements TaskProgressListener {

    private final GlobalTaskService taskService;
    private final VerticalLayout activeSection;
    private final Grid<GlobalTask> historyGrid;
    private UI ui;

    public TaskOverviewView(RomConsole console) {
        this.taskService = console.getBean(GlobalTaskService.class);
        // Build active tasks section
        activeSection = new VerticalLayout();
        // Build history grid (paginated, from DB)
        historyGrid = buildHistoryGrid();
        add(activeSection, historyGrid);
    }

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        this.ui = event.getUI();
        taskService.addListener(this);
        refreshActiveSection();
    }

    @Override
    protected void onDetach(DetachEvent event) {
        taskService.removeListener(this);
        super.onDetach(event);
    }

    @Override
    public void onProgressChanged(Task task, TaskProgress progress) {
        ui.access(() -> refreshActiveSection());
    }

    @Override
    public void onTaskCompleted(Task task, ProcessResult result) {
        ui.access(() -> {
            refreshActiveSection();
            historyGrid.getDataProvider().refreshAll();
        });
    }

    private void refreshActiveSection() {
        // Rebuild active task cards with progress bars
    }

    private Grid<GlobalTask> buildHistoryGrid() {
        // Paginated grid from database
    }
}
```

**Active task card structure:**
- Task type icon + task name
- Current step text (from `TaskProgress.currentStep`)
- Vaadin `ProgressBar` component bound to `TaskProgress.percentage / 100.0`
- Elapsed time display

### 8. InlineProgressHelper

**Package:** `io.github.xuse.romaster.ui.support`

```java
public class InlineProgressHelper implements TaskProgressListener {

    private final Task subscribedTask;
    private final UI ui;
    private final Consumer<TaskProgress> progressCallback;
    private final Consumer<ProcessResult> completionCallback;
    private final GlobalTaskService taskService;

    public InlineProgressHelper(Task task, UI ui,
            Consumer<TaskProgress> progressCallback,
            Consumer<ProcessResult> completionCallback,
            GlobalTaskService taskService) {
        this.subscribedTask = task;
        this.ui = ui;
        this.progressCallback = progressCallback;
        this.completionCallback = completionCallback;
        this.taskService = taskService;
        taskService.addListener(this);
    }

    @Override
    public void onProgressChanged(Task task, TaskProgress progress) {
        if (task != subscribedTask) return;
        ui.access(() -> progressCallback.accept(progress));
    }

    @Override
    public void onTaskCompleted(Task task, ProcessResult result) {
        if (task != subscribedTask) return;
        ui.access(() -> completionCallback.accept(result));
        unsubscribe();
    }

    public void unsubscribe() {
        taskService.removeListener(this);
    }
}
```

**Usage in a business view:**

```java
// After submitting a task:
Task importTask = datService.submitImport(path, null, false, platforms);
InlineProgressHelper helper = new InlineProgressHelper(
    importTask, getUI().orElseThrow(),
    progress -> updateProgressBar(progress),
    result -> showCompletionNotification(result),
    taskService
);
// On view detach:
addDetachListener(e -> helper.unsubscribe());
```

**Design decisions:**
- Filters events by task identity (`==` reference comparison) since each Task instance is unique.
- Auto-unsubscribes on task completion to prevent memory leaks.
- Caller is responsible for calling `unsubscribe()` on view detach (or using `addDetachListener`).

### 9. DatImportView Migration

The migration replaces the polling pattern with push-based updates:

**Removed:**
- `startAutoRefresh()` method
- `UI.setPollInterval()` calls
- `PollListener` registration

**Added:**
- `InlineProgressHelper` instantiation after task submission
- `addDetachListener` to unsubscribe on view detach
- Progress callback updates the task grid via `taskGrid.getDataProvider().refreshAll()`

```java
private void doImport(String path, Set<Platform> platforms) {
    // ... validation unchanged ...
    try {
        DatManageService datService = console.getBean(DatManageService.class);
        Task importTask = datService.submitImport(path, null, false, platforms);
        Notification.show("导入任务已提交", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        GlobalTaskService taskService = console.getBean(GlobalTaskService.class);
        InlineProgressHelper helper = new InlineProgressHelper(
            importTask, getUI().orElseThrow(),
            progress -> taskGrid.getDataProvider().refreshAll(),
            result -> taskGrid.getDataProvider().refreshAll(),
            taskService
        );
        addDetachListener(e -> helper.unsubscribe());
    } catch (Exception ex) {
        // ... error handling unchanged ...
    }
}
```

**Note:** `DatManageService.submitImport()` must be modified to return the `Task` instance so the view can subscribe to it.

### 10. Data Flow Sequence

```
User clicks "开始导入"
  → DatImportView.doImport()
    → GlobalTaskService.submit(importTask)
      → activeTasks.add(importTask)
      → taskPool.submit(wrapper)
        → ProgressMonitor.start() (daemon thread, 500ms poll)
        → importTask.execute()
          → importTask updates taskProgress field
        → ProgressMonitor detects change
          → GlobalTaskService.notifyProgressChanged()
            → InlineProgressHelper.onProgressChanged()
              → UI.access(() -> taskGrid.refreshAll())
                → Vaadin Push → browser update
        → task completes
        → ProgressMonitor.stop()
        → GlobalTaskService.notifyTaskCompleted()
          → InlineProgressHelper.onTaskCompleted()
            → UI.access(() -> show completion)
            → helper.unsubscribe()
        → saveTask() → DB insert
        → activeTasks.remove(importTask)
```

## Error Handling

| Scenario | Handling |
|----------|----------|
| Listener throws in `onProgressChanged` | Caught, logged, remaining listeners still notified |
| Listener throws in `onTaskCompleted` | Caught, logged, remaining listeners still notified |
| UI session closed while task running | `UI.access()` silently fails (Vaadin handles gracefully) |
| Task throws exception | Caught in wrapper, `ProcessResult(400, msg)` created, `onTaskCompleted` still fires |
| ProgressMonitor thread interrupted | Loop exits, final progress notification attempted |
| View detaches without explicit unsubscribe | Listener remains registered but `UI.access()` becomes no-op; InlineProgressHelper pattern ensures cleanup |

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Percentage computation and clamping

*For any* `processedItems` (≥ 0) and `totalItems` (≥ 0) values used to construct a TaskProgress, the resulting `percentage` field SHALL equal `(processedItems * 100) / totalItems` when `totalItems > 0`, SHALL equal 0 when `totalItems == 0`, and SHALL always be within the range [0, 100] inclusive.

**Validates: Requirements 1.4, 1.5, 1.6**

### Property 2: getProgress() delegation round-trip

*For any* Task implementation with a non-null TaskProgress, calling `getProgress()` SHALL return a string identical to `getTaskProgress().toString()`.

**Validates: Requirements 2.2**

### Property 3: GlobalTask.fromActive string representation

*For any* Task with a TaskProgress containing a non-empty `currentStep` and a valid `percentage`, `GlobalTask.fromActive(task).getResult()` SHALL produce a string containing both the `currentStep` text and the `percentage` value.

**Validates: Requirements 3.1, 3.2**

### Property 4: All registered listeners receive progress notifications

*For any* set of N registered TaskProgressListeners and any progress change event, `GlobalTaskService` SHALL invoke `onProgressChanged` on all N listeners exactly once per event.

**Validates: Requirements 5.3**

### Property 5: All registered listeners receive completion notifications

*For any* set of N registered TaskProgressListeners and any task completion event, `GlobalTaskService` SHALL invoke `onTaskCompleted` on all N listeners exactly once per event.

**Validates: Requirements 5.4**

### Property 6: Listener fault isolation

*For any* ordered list of registered listeners where listener at index K throws an exception during callback invocation, all listeners at indices K+1 through N-1 SHALL still receive the notification, and the task execution SHALL not be blocked.

**Validates: Requirements 5.5, 5.6**

### Property 7: View lifecycle unsubscription

*For any* Vaadin view (TaskOverviewView, DatImportView, or InlineProgressHelper-managed view) that registers as a TaskProgressListener on attach/subscribe, detaching the view SHALL result in the listener being removed from GlobalTaskService's listener list.

**Validates: Requirements 7.4, 8.3, 9.5**

### Property 8: InlineProgressHelper task filtering

*For any* InlineProgressHelper subscribed to Task A, progress or completion events for a different Task B SHALL NOT trigger the helper's callbacks.

**Validates: Requirements 9.3, 9.4**

### Property 9: Task queue concurrency limit

*For any* sequence of task submissions to GlobalTaskService, the number of concurrently active tasks SHALL never exceed 2.

**Validates: Requirements 10.1, 10.2**

### Property 10: TaskProgress toString format

*For any* TaskProgress instance, `toString()` SHALL produce a string containing the percentage value, the currentStep text, the processedItems count, and the totalItems count.

**Validates: Requirements 3.2**
