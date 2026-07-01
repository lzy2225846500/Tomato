# Today TODO Widget Design

## Context

The app now has a Today workbench with ordered tasks, due dates, and a current-task concept derived from the first unfinished Today task. The desktop widget should reuse this model instead of creating a separate task workflow.

This feature is for personal use and a few friends, so the widget should stay lightweight, native, and easy to maintain.

## Product Direction

The widget is a glanceable Today TODO list. Its job is to answer one question from the Android home screen: what do I still need to do today?

The widget should not become a miniature task manager. The app remains the place for adding tasks, completing tasks, changing due dates, moving tasks between Today and Later, and reordering.

## In Scope

- One Android home-screen widget for Today unfinished tasks.
- A compact title showing the unfinished Today task count, such as `Today 3` or `今天 3项`.
- A list of unfinished Today tasks ordered exactly like the Today page.
- A subtle current-task treatment for the first task in the list.
- Optional short due-date labels for tasks with due dates.
- Empty state when there are no unfinished Today tasks.
- Tapping the widget opens the app's Today page.
- Widget refresh after task changes that affect Today tasks.
- Light and dark appearance that follows the system where possible.
- Chinese string coverage for visible widget text.

## Out of Scope

- Completing tasks directly from the widget.
- Adding tasks from the widget.
- Editing task titles or due dates from the widget.
- Drag sorting from the widget.
- Timer controls from the widget.
- Showing Later tasks.
- Showing completed tasks.
- Showing detailed Today statistics, pomodoro counts, or focus time.
- Multiple widget types in the first iteration.

## Information Architecture

The widget layout is:

1. Header:
   - Shows `Today N` or localized equivalent, such as `今天 N项`.
   - `N` is the count of unfinished Today tasks.
2. Task list:
   - Shows as many unfinished Today tasks as fit in the widget size.
   - Uses the same sort order as the app's Today unfinished section.
   - The first visible task is visually emphasized as the current task.
3. Due-date label:
   - Only shown when a task has a due date.
   - Uses compact labels such as overdue, today, tomorrow, or a short date.
4. Empty state:
   - Shows a short localized message such as `No tasks today`.
   - Tapping still opens the Today page.

The widget should be useful in small and medium launcher sizes. It should adapt by showing fewer or more rows rather than introducing separate modes.

## Data Rules

- The widget reads unfinished Today tasks only.
- Ordering matches the Today page: `sortOrder`, then stable fallback.
- The current task is the first unfinished Today task.
- Due dates do not move tasks into or out of the widget.
- Later and completed tasks are excluded.

## Interaction

- Tapping anywhere on the widget opens the app's Today page.
- The first iteration uses a single broad tap target instead of per-row actions.
- No destructive or state-changing action is available from the widget.

This keeps the widget reliable and avoids accidental completion from the launcher.

## Refresh Behavior

The widget should update when task state changes in ways that affect its content:

- Task created in Today.
- Task completed or uncompleted.
- Task moved between Today and Later.
- Task reordered in Today.
- Task title changed if title editing exists later.
- Task due date changed or cleared.

If the platform limits immediate updates, a lightweight periodic refresh is acceptable as a fallback.

## Visual Direction

The widget should feel like a native Android home-screen widget:

- Quiet Material-style surface.
- Rounded rectangle background.
- System light and dark theme support where possible.
- Dense but readable typography.
- The first task can use slightly stronger weight or a small accent indicator.
- Due-date labels should be compact and secondary.

The widget should not look like a scaled-down app screen. It should feel more like a clean home-screen note.

## Implementation Direction

Prefer Jetpack Glance App Widget for the first implementation. It fits the Kotlin/Compose stack and keeps widget UI isolated from the app's Compose screens.

The widget should consume task data through a small widget-facing data path rather than duplicating business rules in UI code. Shared due-date label logic should be reused if possible.

## Acceptance Criteria

- The launcher can add a Today TODO widget.
- The widget title shows the unfinished Today task count.
- The widget lists unfinished Today tasks in the same order as the Today page.
- The first unfinished Today task is subtly emphasized.
- Tasks with due dates show localized compact due-date labels.
- Later tasks and completed tasks do not appear.
- Tapping the widget opens the app to the Today page.
- The widget shows a localized empty state when there are no unfinished Today tasks.
- Widget text is localized in Simplified Chinese and existing supported languages where practical.
- The debug APK builds successfully.
