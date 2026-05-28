# Requirements Document

## Introduction

This feature replaces the current polling-based task progress mechanism with a structured progress model and push-based UI updates. The `Task` interface's `getProgress()` String return is replaced by a `TaskProgress` object. `GlobalTaskService` gains an observer pattern for progress notifications. The Vaadin UI layer adopts server push (`@Push`) to deliver real-time updates, eliminating `UI.setPollInterval()` polling. A dedicated task overview page (`/tasks`) and reusable inline progress helpers are introduced for business views.

## Glossary

- **Task**: Interface in `romking-core` representing a background operation (implementations: ScanRomTask, ImportDatTask, ExportRomTask, ArchiveRomTask, VerifyRepoTask)
- **TaskProgress**: Structured progress data object containing currentStep, processedItems, totalItems, and percentage
- **GlobalTaskService**: Service in `romking-core` managing background task submission, lifecycle, and observer notifications
- **TaskProgressListener**: Observer interface receiving progress change and task completion callbacks
- **ProcessResult**: Existing result object returned upon task completion (code + message + details)
- **Vaadin_Push**: Vaadin server push mechanism using `@Push` annotation and `UI.access()` for thread-safe UI updates
- **TaskOverviewView**: Vaadin view at `/tasks` displaying active tasks with real-time progress and historical tasks from the database
- **InlineProgressHelper**: Reusable helper class enabling business views to show inline progress after task submission
- **GlobalTask**: Database entity (`tasks` table) representing a completed task record; `fromActive()` maps a running Task to a display object

## Requirements

### Requirement 1: TaskProgress Data Model

**User Story:** As a developer, I want a structured progress object so that UI components can render progress bars and step descriptions without parsing free-text strings.

#### Acceptance Criteria

1. THE TaskProgress SHALL contain a `currentStep` field of type String describing the current operation step
2. THE TaskProgress SHALL contain a `processedItems` field of type int representing the number of items processed so far
3. THE TaskProgress SHALL contain a `totalItems` field of type int representing the total number of items to process
4. THE TaskProgress SHALL contain a `percentage` field of type int constrained to the range 0 through 100 inclusive
5. WHEN `totalItems` is greater than zero, THE TaskProgress SHALL compute `percentage` as `(processedItems * 100) / totalItems`
6. WHEN `totalItems` is zero, THE TaskProgress SHALL report `percentage` as 0

### Requirement 2: Task Interface Enhancement

**User Story:** As a developer, I want the Task interface to expose structured progress so that all task implementations provide consistent progress data.

#### Acceptance Criteria

1. THE Task interface SHALL declare a `getTaskProgress()` method returning a TaskProgress instance
2. THE Task interface SHALL retain the existing `getProgress()` method as a default method that delegates to `getTaskProgress().toString()`
3. WHEN `getTaskProgress()` is called on ScanRomTask, THE ScanRomTask SHALL return a TaskProgress reflecting the current scan step, processed directories or files, total directories or files, and computed percentage
4. WHEN `getTaskProgress()` is called on ImportDatTask, THE ImportDatTask SHALL return a TaskProgress reflecting the current import step, processed DAT entries, total DAT entries, and computed percentage
5. WHEN `getTaskProgress()` is called on ExportRomTask, THE ExportRomTask SHALL return a TaskProgress reflecting the current export step, processed ROM files, total ROM files, and computed percentage
6. WHEN `getTaskProgress()` is called on ArchiveRomTask, THE ArchiveRomTask SHALL return a TaskProgress reflecting the current archive step, processed ROM files, total ROM files, and computed percentage
7. WHEN `getTaskProgress()` is called on VerifyRepoTask, THE VerifyRepoTask SHALL return a TaskProgress reflecting the current verify step, processed ROM files, total ROM files, and computed percentage

### Requirement 3: GlobalTask Backward Compatibility

**User Story:** As a developer, I want `GlobalTask.fromActive()` to remain functional so that existing code consuming GlobalTask records continues to work without modification.

#### Acceptance Criteria

1. WHEN `GlobalTask.fromActive(Task)` is called, THE GlobalTask SHALL set the `result` field to the string representation of the Task's TaskProgress
2. THE GlobalTask `result` field mapping SHALL produce a human-readable string containing currentStep and percentage information

### Requirement 4: TaskProgressListener Observer Interface

**User Story:** As a developer, I want an observer interface so that any component can subscribe to task progress changes without coupling to specific UI frameworks.

#### Acceptance Criteria

1. THE TaskProgressListener interface SHALL declare an `onProgressChanged(Task, TaskProgress)` method
2. THE TaskProgressListener interface SHALL declare an `onTaskCompleted(Task, ProcessResult)` method
3. THE TaskProgressListener interface SHALL reside in the `romking-core` module package `io.github.xuse.romking.tasks`

### Requirement 5: GlobalTaskService Observer Management

**User Story:** As a developer, I want GlobalTaskService to manage listener registration so that observers receive notifications throughout a task's lifecycle.

#### Acceptance Criteria

1. THE GlobalTaskService SHALL provide an `addListener(TaskProgressListener)` method for registering observers
2. THE GlobalTaskService SHALL provide a `removeListener(TaskProgressListener)` method for unregistering observers
3. WHEN a running Task's TaskProgress changes, THE GlobalTaskService SHALL invoke `onProgressChanged(Task, TaskProgress)` on all registered listeners
4. WHEN a Task completes execution, THE GlobalTaskService SHALL invoke `onTaskCompleted(Task, ProcessResult)` on all registered listeners
5. THE GlobalTaskService SHALL invoke listener callbacks from the task execution thread without blocking task processing on listener exceptions
6. IF a listener throws an exception during callback invocation, THEN THE GlobalTaskService SHALL log the exception and continue notifying remaining listeners
7. THE GlobalTaskService SHALL use a thread-safe collection for storing listeners to support concurrent add, remove, and iteration

### Requirement 6: Vaadin Server Push Configuration

**User Story:** As a user, I want the UI to update in real time without manual page refresh so that I can monitor task progress as it happens.

#### Acceptance Criteria

1. THE Romaster application SHALL annotate the AppShellConfigurator implementation with `@Push`
2. THE Romaster application SHALL use WebSocket-based push transport as the default push mechanism
3. WHEN a background thread updates UI components, THE Romaster application SHALL wrap the update in `UI.access()` to ensure thread safety

### Requirement 7: Remove Polling Pattern

**User Story:** As a developer, I want to eliminate the polling pattern so that the application uses a single consistent mechanism for real-time updates.

#### Acceptance Criteria

1. THE DatImportView SHALL remove the `startAutoRefresh()` method and all `UI.setPollInterval()` calls
2. THE DatImportView SHALL subscribe to GlobalTaskService as a TaskProgressListener upon task submission
3. WHEN a progress change notification is received, THE DatImportView SHALL update the task grid using `UI.access()`
4. WHEN the subscribed task completes or the view detaches, THE DatImportView SHALL unsubscribe from GlobalTaskService

### Requirement 8: Task Overview Page

**User Story:** As a user, I want a dedicated task overview page so that I can see all active and historical tasks in one place.

#### Acceptance Criteria

1. THE TaskOverviewView SHALL be accessible at the route `/tasks`
2. WHEN the TaskOverviewView attaches to the UI, THE TaskOverviewView SHALL register as a TaskProgressListener with GlobalTaskService
3. WHEN the TaskOverviewView detaches from the UI, THE TaskOverviewView SHALL unregister from GlobalTaskService
4. THE TaskOverviewView SHALL display active tasks in a section showing a progress bar and descriptive text for each running task
5. WHEN a progress change notification is received, THE TaskOverviewView SHALL update the active task display using `UI.access()`
6. WHEN a task completion notification is received, THE TaskOverviewView SHALL remove the task from the active section and refresh the historical section
7. THE TaskOverviewView SHALL display historical tasks in a paginated Grid loaded from the database
8. THE TaskOverviewView active task section SHALL show the task type, task name, currentStep text, and a progress bar reflecting the percentage

### Requirement 9: Inline Progress Helper for Business Views

**User Story:** As a developer, I want a reusable helper class so that any business view can show inline task progress with minimal boilerplate.

#### Acceptance Criteria

1. THE InlineProgressHelper SHALL provide a method to subscribe to a specific Task's progress after submission
2. THE InlineProgressHelper SHALL accept a UI reference and a callback for rendering progress updates
3. WHEN a progress change notification is received for the subscribed Task, THE InlineProgressHelper SHALL invoke the rendering callback within `UI.access()`
4. WHEN the subscribed Task completes, THE InlineProgressHelper SHALL invoke a completion callback and automatically unsubscribe from GlobalTaskService
5. WHEN the owning view detaches, THE InlineProgressHelper SHALL automatically unsubscribe from GlobalTaskService
6. THE InlineProgressHelper SHALL reside in the `romaster` module for use by Vaadin business views

### Requirement 10: Task Queue Constraints Preservation

**User Story:** As a developer, I want the existing task queue constraints to remain unchanged so that the system's concurrency model is preserved.

#### Acceptance Criteria

1. THE GlobalTaskService SHALL maintain the maximum of 2 concurrent active tasks
2. THE GlobalTaskService SHALL maintain the existing `checkTasks()` validation logic (singleton type check and duplicate name check)
3. THE GlobalTaskService progress notification mechanism SHALL operate independently of the task submission and execution logic
