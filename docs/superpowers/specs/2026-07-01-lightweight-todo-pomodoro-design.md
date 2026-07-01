# Lightweight TODO + Pomodoro Design

## Context

We will build a lightweight Android productivity app by forking Tomato. The app is for personal use and sharing with a few friends, so Tomato's GPL-3.0 license is acceptable as long as any distributed derivative remains GPL-compatible.

Tomato already provides the parts that are hardest to get right on Android: a native Kotlin/Compose UI, foreground timer service, notifications, break/focus cycles, settings, Room persistence, and existing focus statistics. The new work should add a minimal task layer without turning the app into a heavy project-management system.

## Product Goal

The app should help the user move through a simple daily loop:

1. Write down today's tasks.
2. Start focusing, with or without choosing a task.
3. Complete tasks and accumulate focus history.
4. Review simple daily and weekly progress.

The first version should feel Android-native, fast, and quiet. It should avoid complex GTD concepts, accounts, cloud sync, projects, subtasks, labels, or collaboration.

## Scope

### In Scope

- A minimal Today task list.
- Tasks with title, completion state, and whether they belong to Today or Later.
- Starting a focus session from a task.
- Starting an unassigned focus session directly from the timer.
- Recording focus time against an optional task.
- Simple stats:
  - Today's focus minutes.
  - Today's pomodoro count.
  - Today's completed task count.
  - Last 7 days focus trend.
  - Task focus ranking.

### Out of Scope

- Cloud sync.
- User accounts.
- Projects, tags, subtasks, recurring tasks, and due dates.
- Calendar integration.
- Social or shared task lists.
- Commercial monetization.

## Recommended Foundation

Use Tomato as the base project.

Reasons:

- It is a modern native Android app using Kotlin and Compose.
- It already has a clean `shared` module, Room database, DI setup, timer service, settings, and stats screens.
- Its timer implementation already handles foreground service behavior, notifications, Android timer state, and focus/break transitions.
- It is lighter and more directly aligned with this app than Goodtime.

Goodtime remains a useful reference for mature focus-session concepts, but should not be the first fork target.

## Information Architecture

The app should expose three primary areas:

- `Today`: The default landing screen. Shows today's active tasks, completed tasks, and a small entry point for Later.
- `Timer`: The existing Tomato timer screen, extended with optional task context.
- `Stats`: Tomato's stats area, extended with task-aware summaries.

The current Tomato navigation should be extended rather than replaced. Add a `Screen.Tasks` or `Screen.Today` route and a corresponding bottom navigation item if the current app shell supports it. If the current Tomato home structure is timer-first, make Today the default initial route after the first task implementation is stable.

## Data Model

Add a task table:

```kotlin
TaskItem(
    id: Long,
    title: String,
    isDone: Boolean,
    isToday: Boolean,
    createdAt: Instant,
    completedAt: Instant?
)
```

Add a focus session table:

```kotlin
FocusSession(
    id: Long,
    taskId: Long?,
    startedAt: Instant,
    durationMs: Long,
    completed: Boolean
)
```

`taskId` is nullable because the user explicitly wants to allow focus sessions without choosing a task. These sessions should appear in stats as `Unassigned`.

Keep Tomato's existing `Stat` table for daily aggregate focus and break statistics. Do not replace it in the first version.

## Repositories

Add a `TaskRepository` responsible for:

- Creating tasks.
- Updating task title.
- Marking tasks done or undone.
- Moving tasks between Today and Later.
- Listing Today tasks.
- Listing Later tasks.
- Counting completed tasks for a date.

Add a `FocusSessionRepository` responsible for:

- Recording focus session slices with optional task assignment.
- Returning task focus totals.
- Returning daily pomodoro counts.
- Returning unassigned focus totals.

Keep `StatRepository` responsible for aggregate focus and break time. The timer service can write both aggregate stats and task-aware focus sessions.

## Timer Integration

Extend timer state with optional task context:

```kotlin
currentTaskId: Long?
currentTaskTitle: String?
```

Add timer actions for task binding:

```kotlin
SetCurrentTask(taskId: Long?, title: String?)
ClearCurrentTask
```

Starting from a task should:

1. Set the current task context.
2. Navigate to the timer screen.
3. Start or leave the timer ready to start, depending on the final UX choice during implementation.

Starting from the Timer screen without a task should keep `currentTaskId = null`.

When `TimerService.saveTimeToDb()` records focus time, it should continue calling `StatRepository.addFocusTime(...)` and additionally record a focus session slice with the current task ID. Break time should not be assigned to tasks.

If the timer is reset or skipped partway through a focus interval, record the elapsed focus duration as an incomplete or partial session. Completed full focus intervals should be marked completed.

## Today Screen

The Today screen should be practical and sparse:

- Inline add field for a task title.
- Active Today tasks.
- Completed Today tasks collapsed or visually quieter.
- A Later section or simple screen for tasks not in Today.
- Each active task has:
  - Completion toggle.
  - Title.
  - Small focus count or focus minutes.
  - Start focus action.

No priority, notes, due dates, tags, or subtasks in the first version.

## Stats

Extend existing stats rather than creating a separate analytics area.

The first version should include:

- Today's focus minutes from existing aggregate stats.
- Today's pomodoro count from focus sessions.
- Today's completed task count from tasks.
- Last 7 days focus trend from existing stats.
- Task focus ranking from focus sessions.
- An `Unassigned` row for sessions without a task.

Stats should stay readable and lightweight. Avoid complex filters in the first version.

## Error Handling

- Empty task titles should not create tasks.
- If a task is deleted or moved while it is bound to the timer, the current focus session should continue as unassigned or keep a historical title snapshot. Prefer historical snapshot only if it does not complicate the first version.
- If focus-session recording fails, the existing aggregate `Stat` write should still happen.
- Database migrations should preserve all existing Tomato data.

## Testing

Focus testing on repository behavior and timer integration:

- Task creation, completion, Today/Later movement.
- Focus session recording with a task.
- Focus session recording without a task.
- Stats aggregation for task ranking and unassigned sessions.
- Migration from Tomato's current database version to the new version.
- Timer save behavior does not double-count focus duration.

Manual Android verification will be needed later for foreground service, notifications, and UI flow. The current machine does not have Android SDK configured, so local compilation may require installing Android Studio or using CI.

## Open Decisions

- Whether tapping Start on a task immediately starts the timer or only opens the timer ready to start.
- Whether completed tasks remain in Today forever, disappear next day, or move into a daily history view.
- Whether partial sessions count as pomodoros or only as focus minutes.

Recommended defaults:

- Task Start opens the timer and starts immediately.
- Completed tasks remain visible today and become history after the day changes.
- Full focus intervals count as pomodoros; partial sessions count only as minutes.
