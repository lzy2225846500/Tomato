# Today Workbench V2 Design

## Context

The first TODO + Pomodoro iteration added a usable Today page, task binding, focus-session recording, and simple task-aware stats. The current Today page is intentionally sparse: an add field, active Today tasks, completed tasks, and a collapsible Later section.

V2 should make the page feel more like a daily work surface without turning it into a full task-management system. The app is still for personal use and sharing with a few friends, so the design should stay native, quiet, and maintainable.

## Product Direction

The Today page becomes a current-task workbench:

1. Show today's progress at a glance.
2. Promote the next task to a clear current-task area.
3. Let the user control what is current by dragging task order.
4. Support lightweight due dates without adding a calendar view in this iteration.

The current task is not a separate pinned concept. It is always the first unfinished task in the Today section. This keeps the model simple while making drag ordering meaningful.

## In Scope

- A lightweight Today summary row.
- A current-task panel.
- Sortable Today unfinished tasks.
- Sortable Later unfinished tasks.
- Cross-section drag between Today and Later.
- Completed tasks displayed separately and excluded from drag sorting.
- Optional due date on each task.
- A simple per-task due-date entry point to set, change, or clear the due date.
- Due-date labels in task rows and the current-task panel.
- Room schema update for task ordering and due dates.
- Chinese string coverage for the new UI.

## Out of Scope

- Calendar or planning screen.
- Future 7 days view.
- Notifications or reminders for due dates.
- Recurring tasks.
- Projects, tags, subtasks, priorities, notes, accounts, cloud sync, or collaboration.

These are future projects, not hidden requirements for this iteration.

## Information Architecture

The Today page keeps the same top-level route and navigation item. Its content changes to:

1. Top app bar: `Today`.
2. Summary row:
   - Completed tasks today, shown as `2/5 Tasks` or equivalent localized text.
   - Completed pomodoros today.
   - Focus time today.
3. Current-task panel:
   - Shows the first unfinished Today task.
   - Displays title, due-date label when present, and task focus information when available.
   - Primary action starts focus for that task.
   - Secondary actions stay minimal, such as complete or move to Later if the UI can expose them cleanly.
4. Quick add row:
   - Remains fast and single-line.
   - Does not include inline due-date controls.
5. Today unfinished section:
   - Drag-sortable.
   - Supports dropping Later tasks into this section.
6. Later unfinished section:
   - Drag-sortable.
   - Supports dropping Today tasks into this section.
7. Completed section:
   - Visually quieter.
   - Not drag-sortable.

If there is no unfinished Today task, the current-task panel shows an empty state that encourages adding a task or moving one from Later.

## Task Model

Extend `TaskItem` with ordering and due-date fields:

```kotlin
TaskItem(
    id: Long,
    title: String,
    isDone: Boolean,
    isToday: Boolean,
    sortOrder: Long,
    dueDate: LocalDate?,
    createdAt: Instant,
    completedAt: Instant?
)
```

`isToday` decides whether an unfinished task belongs to Today or Later. `sortOrder` decides its order within that section. `dueDate` is a constraint shown to the user; it does not automatically move the task between Today and Later.

Room can store `dueDate` through the existing converters if `LocalDate` is already supported. If not, store it as an ISO local-date string via a converter.

## Ordering Rules

- Today unfinished tasks are ordered by `sortOrder`, then `id` as a stable fallback.
- Later unfinished tasks are ordered by `sortOrder`, then `id` as a stable fallback.
- Completed tasks are ordered by `completedAt` descending.
- The current task is `activeTodayTasks.firstOrNull()`.
- Dragging within a section updates `sortOrder` for that section.
- Dragging across Today and Later updates `isToday` and reassigns `sortOrder` in both source and target sections.
- Sorting is saved when the drag gesture completes, not continuously during every drag movement.
- Sort orders should be compacted after reordering to avoid unbounded gaps or duplicates.

Completion behavior:

- Completing a task sets `isDone = true` and `completedAt = now`.
- Completing a task keeps `isToday` and `sortOrder` as historical state.
- Uncompleting a task sets `isDone = false` and `completedAt = null`.
- Uncompleted tasks return to the bottom of their original section by assigning a new trailing `sortOrder`.

## Due-Date Behavior

Due dates are optional. They should make tasks easier to interpret without making the page feel like a calendar.

Task rows and the current-task panel show a compact due-date label:

- `Due today`
- `Due tomorrow`
- Weekday label for dates in the near future, such as `Due Friday`
- `Overdue` for dates before today
- A short date for farther dates, such as `Jul 18`

Localized Chinese labels should be provided for these states.

The task row exposes a small calendar action or menu entry. It supports:

- Set due date.
- Change due date.
- Clear due date.

The quick-add row does not include due-date controls in this iteration. This preserves fast capture.

## Drag Interaction

The drag interaction should feel native and predictable:

- Use a visible drag handle in task rows.
- Drag handles are shown only for unfinished tasks.
- Today and Later sections are both valid drop zones.
- While dragging, the item should visibly lift or separate from the list.
- Dropping into a collapsed Later section should either expand it before drag or require the user to expand it first. Prefer requiring explicit expansion for the first implementation.
- Completed tasks cannot be dragged into active sections in this iteration.

Implementation should prefer a proven Compose reorder solution if the project already has one or if adding a small, maintained dependency is clearly safer than custom gesture code. If no suitable dependency is available, implement a small local reorder helper scoped to the Today page.

## ViewModel State

`TasksState` should expose:

- `activeTodayTasks`
- `laterTasks`
- `completedTodayTasks`
- `currentTask`
- `todayCompletedTaskCount`
- `todayTotalTaskCount`
- `todayCompletedPomodoroCount`
- `todayFocusTotal`
- due-date picker state if the UI uses a bottom sheet or dialog

`currentTask` can be derived from `activeTodayTasks.firstOrNull()` and does not need persistence.

## Actions

Add task actions for:

- Reorder within Today.
- Reorder within Later.
- Move and reorder across Today and Later.
- Set due date.
- Clear due date.
- Open and dismiss due-date editor state for the date dialog or bottom sheet.

Existing actions for add, complete, uncomplete, move to Today, move to Later, and start focus remain.

## Repository And DAO Changes

`TaskRepository` should expose operations that keep ordering logic out of UI:

- Create a task at the bottom of Today or Later.
- Set done or undone, applying the completion rules above.
- Set or clear due date.
- Reorder tasks within a section.
- Move tasks across sections with a target index.
- Normalize sort orders for Today and Later.

DAO support should include:

- Ordered active Today flow.
- Ordered Later flow.
- Completed tasks flow.
- Batch update for section and sort order.
- Due-date update by task ID.

## Migration

Increase the Room database version from `3` to `4`.

Migration should:

- Add `sortOrder` to `task_item`.
- Add nullable `dueDate` to `task_item`.
- Initialize `sortOrder` for existing tasks using a deterministic order, preferably `createdAt` then `id`.
- Preserve existing task, focus-session, and stat data.

Auto migration is acceptable only if it can correctly initialize non-null `sortOrder`. Otherwise use a manual migration.

## Future Project Notes

The following should be documented as later work, not implemented in V2:

- Calendar or planning view.
- Future 7 days view.
- Due-date reminders and notifications.
- Recurring tasks.
- Better date-based task review and filtering.

The data model should not block these future additions, but V2 should not build their UI.

## Acceptance Criteria

- Today page shows a current-task panel when at least one unfinished Today task exists.
- The current task is always the first unfinished Today task.
- Starting focus from the current-task panel binds the task to the timer.
- Today summary shows task completion, pomodoros, and focus time.
- Today unfinished tasks can be reordered and the order persists after app restart.
- Later unfinished tasks can be reordered and the order persists after app restart.
- Tasks can be dragged between Today and Later and remain in the dropped position.
- Completed tasks are excluded from drag sorting.
- Uncompleting a task returns it to the bottom of its original section.
- Due dates can be set, changed, cleared, and displayed.
- Due dates do not automatically move tasks between Today and Later.
- Existing tasks survive migration.
- Chinese strings are present for new visible UI.
- FOSS debug APK builds successfully.
