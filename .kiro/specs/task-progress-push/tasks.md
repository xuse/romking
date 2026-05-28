# Implementation Plan: Task Progress Push

## Overview

Replace the polling-based task progress mechanism with a structured `TaskProgress` model and push-based UI updates via Vaadin `@Push`. The implementation proceeds bottom-up: core model → Task interface → GlobalTaskService observer infrastructure → task implementations → Vaadin UI layer (push config, TaskOverviewView, InlineProgressHelper, DatImportView migration).

## Tasks

- [x] 1. Create TaskProgress model and TaskProgressListener interface
  - [x] 1.1 Create `TaskProgress` class in `romking-core`
    - Create `io.github.xuse.romking.tasks.TaskProgress` as an immutable value object
    - Fields: `currentStep` (String), `processedItems` (int), `totalItems` (int), `percentage` (int)
    - Constructor computes percentage clamped to [0, 100]; `totalItems <= 0` yields 0
    - Override `toString()` to produce `[%d%%] %s (%d/%d)` format
    - Use `@Getter` from Lombok
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

  - [x] 1.2 Create `TaskProgressListener` interface in `romking-core`
    - Create `io.github.xuse.romking.tasks.TaskProgressListener`
    - Declare `onProgressChanged(Task task, TaskProgress progress)`
    - Declare `onTaskCompleted(Task task, ProcessResult result)`
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 1.3 Write property test for TaskProgress percentage computation
    - **Property 1: Percentage computation and clamping**
    - **Validates: Requirements 1.4, 1.5, 1.6**

  - [x] 1.4 Write property test for TaskProgress toString format
    - **Property 10: TaskProgress toString format**
    - **Validates: Requirements 3.2**

- [x] 2. Enhance Task interface and update implementations
  - [x] 2.1 Add `getTaskProgress()` to `Task` interface and make `getProgress()` a default method
    - Add `TaskProgress getTaskProgress()` method declaration
    - Convert `getProgress()` to a default method: delegates to `getTaskProgress().toString()` (returns `""` if null)
    - _Requirements: 2.1, 2.2_

  - [x] 2.2 Write property test for getProgress() delegation
    - **Property 2: getProgress() delegation round-trip**
    - **Validates: Requirements 2.2**

  - [x] 2.3 Update `ImportDatTask` to use `TaskProgress`
    - Add `private volatile TaskProgress taskProgress = new TaskProgress("等待开始", 0, 0)`
    - Implement `getTaskProgress()` returning the field
    - Replace `progress = "..."` assignments with `taskProgress = new TaskProgress(step, processed, total)`
    - Update `updateProgress` callback to create new TaskProgress instances
    - Remove the old `getProgress()` override (default method handles it)
    - _Requirements: 2.4_

  - [x] 2.4 Update `ScanRomTask` to use `TaskProgress`
    - Add `private volatile TaskProgress taskProgress` field
    - Implement `getTaskProgress()`
    - Replace `progress = "..."` with `taskProgress = new TaskProgress(step, processed, total)` using `dirCount`/total dirs as progress indicators
    - Remove the old `getProgress()` override
    - _Requirements: 2.3_

  - [x] 2.5 Update `ExportRomTask` to use `TaskProgress`
    - Add `private volatile TaskProgress taskProgress` field
    - Implement `getTaskProgress()`
    - Replace `progress = "..."` with `taskProgress = new TaskProgress(step, copiedFiles + skippedFiles + md5MatchSkipped, totalFiles)`
    - Remove the old `getProgress()` override
    - _Requirements: 2.5_

  - [x] 2.6 Update `ArchiveRomTask` to use `TaskProgress`
    - Add `private volatile TaskProgress taskProgress` field
    - Implement `getTaskProgress()`
    - Replace `progress = "..."` with `taskProgress = new TaskProgress(step, archivedRoms + duplicateSkipped, totalRoms)`
    - Remove the old `getProgress()` override
    - _Requirements: 2.6_

  - [x] 2.7 Update `VerifyRepoTask` to use `TaskProgress`
    - Add `private volatile TaskProgress taskProgress` field
    - Implement `getTaskProgress()`
    - Replace `progress = "..."` with `taskProgress = new TaskProgress(step, okCount + missingCount + corruptedCount, totalRoms)`
    - Remove the old `getProgress()` override
    - _Requirements: 2.7_

- [x] 3. Checkpoint - Ensure all core module compiles
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Add observer infrastructure to GlobalTaskService
  - [x] 4.1 Add listener management and ProgressMonitor to `GlobalTaskService`
    - Add `private final List<TaskProgressListener> listeners = new CopyOnWriteArrayList<>()`
    - Add `addListener(TaskProgressListener)` and `removeListener(TaskProgressListener)` methods
    - Add `notifyProgressChanged(Task, TaskProgress)` — iterates listeners with try-catch per listener
    - Add `notifyTaskCompleted(Task, ProcessResult)` — iterates listeners with try-catch per listener
    - Add inner class `ProgressMonitor` with daemon thread polling `task.getTaskProgress()` every 500ms, firing `notifyProgressChanged` on change
    - Modify `submit()` to wrap execution with `ProgressMonitor.start()`/`stop()` and call `notifyTaskCompleted` in the finally block
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 10.1, 10.2, 10.3_

  - [x] 4.2 Write property test for listener notification delivery
    - **Property 4: All registered listeners receive progress notifications**
    - **Validates: Requirements 5.3**

  - [x] 4.3 Write property test for listener fault isolation
    - **Property 6: Listener fault isolation**
    - **Validates: Requirements 5.5, 5.6**

  - [x] 4.4 Write property test for task queue concurrency limit
    - **Property 9: Task queue concurrency limit**
    - **Validates: Requirements 10.1, 10.2**

- [x] 5. Modify `DatManageService.submitImport()` to return Task
  - [x] 5.1 Change `submitImport` return type from `void` to `Task`
    - Modify `submitImport(String, Platform, boolean, Set<Platform>)` to return the `ImportDatTask` instance
    - Update overloaded `submitImport` methods to propagate the return value
    - _Requirements: 9.1_

- [x] 6. Checkpoint - Ensure romking-core compiles and existing behavior preserved
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Configure Vaadin Push and create UI components
  - [x] 7.1 Add `@Push` annotation to Application class
    - Add `@Push` to `io.github.xuse.Application` (AppShellConfigurator)
    - Add import for `com.vaadin.flow.server.communication.PushConnection` / `com.vaadin.flow.shared.communication.PushMode` if needed
    - _Requirements: 6.1, 6.2_

  - [x] 7.2 Create `InlineProgressHelper` in romaster module
    - Create `io.github.xuse.romaster.ui.support.InlineProgressHelper`
    - Implements `TaskProgressListener`
    - Constructor accepts: `Task subscribedTask`, `UI ui`, `Consumer<TaskProgress> progressCallback`, `Consumer<ProcessResult> completionCallback`, `GlobalTaskService taskService`
    - `onProgressChanged`: filter by `task == subscribedTask`, then `ui.access(() -> progressCallback.accept(progress))`
    - `onTaskCompleted`: filter by `task == subscribedTask`, then `ui.access(() -> completionCallback.accept(result))`, then `unsubscribe()`
    - `unsubscribe()`: calls `taskService.removeListener(this)`
    - Auto-registers via `taskService.addListener(this)` in constructor
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

  - [x] 7.3 Write property test for InlineProgressHelper task filtering
    - **Property 8: InlineProgressHelper task filtering**
    - **Validates: Requirements 9.3, 9.4**

  - [x] 7.4 Create `TaskOverviewView` in romaster module
    - Create `io.github.xuse.romaster.ui.tasks.TaskOverviewView`
    - Route: `/tasks`, `@PageTitle("任务总览")`, `@Menu(order = 6, icon = "vaadin:tasks", title = "任务总览")`, `@PermitAll`
    - Extends `Main`, implements `TaskProgressListener`
    - `onAttach`: store UI reference, call `taskService.addListener(this)`, refresh active section
    - `onDetach`: call `taskService.removeListener(this)`
    - `onProgressChanged`: `ui.access(() -> refreshActiveSection())`
    - `onTaskCompleted`: `ui.access(() -> { refreshActiveSection(); historyGrid.getDataProvider().refreshAll(); })`
    - Active section: VerticalLayout with task cards (type icon, name, currentStep, ProgressBar, elapsed time)
    - History section: paginated Grid<GlobalTask> from database
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8_

  - [x] 7.5 Write property test for view lifecycle unsubscription
    - **Property 7: View lifecycle unsubscription**
    - **Validates: Requirements 7.4, 8.3, 9.5**

- [x] 8. Migrate DatImportView from polling to push
  - [x] 8.1 Remove polling pattern and add push-based updates in `DatImportView`
    - Remove `startAutoRefresh()` method and all `UI.setPollInterval()` calls
    - Remove `PollListener` registration if present
    - After `datService.submitImport(...)`, capture the returned `Task` instance
    - Create `InlineProgressHelper` with progress callback refreshing `taskGrid`
    - Add `addDetachListener(e -> helper.unsubscribe())` for cleanup
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 9. Final checkpoint - Full build verification
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- The implementation language is Java (matching the existing codebase)
- `GlobalTask.fromActive()` already calls `getProgress()` which becomes a default method delegating to `getTaskProgress().toString()` — no changes needed there
- The `ProgressMonitor` uses a daemon thread polling at 500ms to decouple task implementations from the notification mechanism

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3", "1.4", "2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.4", "2.5", "2.6", "2.7"] },
    { "id": 3, "tasks": ["4.1", "5.1"] },
    { "id": 4, "tasks": ["4.2", "4.3", "4.4", "7.1", "7.2"] },
    { "id": 5, "tasks": ["7.3", "7.4", "8.1"] },
    { "id": 6, "tasks": ["7.5"] }
  ]
}
```
