# Lightweight TODO + Pomodoro Implementation Plan

## Constraints

- Base project: Tomato fork.
- Target use: personal use and sharing with a few friends.
- License posture: GPL-3.0 is acceptable.
- Local machine: Java and Git are available, Android SDK is not configured.
- Verification must therefore combine code review, repository-level tests where possible, and later Android build verification through Android Studio or CI.

## Phase 1: Data Layer

Goal: add task and task-aware focus-session persistence without touching UI.

Files likely involved:

- `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/AppDatabase.kt`
- New `TaskItem.kt`
- New `TaskDao.kt`
- New `TaskRepository.kt`
- New `FocusSession.kt`
- New `FocusSessionDao.kt`
- New `FocusSessionRepository.kt`
- `shared/src/androidMain/kotlin/org/nsh07/pomodoro/di/modules.kt`
- `shared/schemas/org.nsh07.pomodoro.data.AppDatabase/...`

Tasks:

1. Add Room entities for `TaskItem` and `FocusSession`.
2. Add DAOs with flows for Today tasks, Later tasks, completed counts, focus totals, pomodoro counts, and task rankings.
3. Add repositories that wrap DAOs and keep date/time logic out of UI.
4. Increase Room database version and add migration or auto-migration if possible.
5. Register DAOs and repositories in Koin.
6. Add repository tests where existing test infrastructure allows it.

Acceptance:

- Existing Tomato `Stat` table remains intact.
- New task/focus session tables can be queried independently.
- Null `taskId` sessions are supported.
- Data-layer code compiles once Android SDK is available.

## Phase 2: Today Screen

Goal: create a minimal task UI that can be used without timer integration.

Files likely involved:

- `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/Screen.kt`
- New `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/...`
- `androidApp/src/main/java/org/nsh07/pomodoro/ui/AppScreen.kt`
- `shared/src/commonMain/composeResources/values/strings.xml`
- Drawable resources for task tab icons, if no suitable existing icons are available.

Tasks:

1. Add `Screen.Today`.
2. Add `TasksViewModel` with state for Today, Later, inline title input, and loading/error states.
3. Add `TodayScreenRoot` and composables for:
   - Inline add field.
   - Active Today tasks.
   - Completed Today tasks.
   - Later entry/section.
4. Add bottom navigation item for Today.
5. Make Today the default root screen only after it is usable.

Acceptance:

- User can create a task.
- User can complete and uncomplete a task.
- User can move a task between Today and Later.
- UI remains sparse and does not introduce priority, due date, tags, or notes.

## Phase 3: Timer Task Binding

Goal: let a focus session optionally belong to a task.

Files likely involved:

- `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/timerScreen/viewModel/TimerState.kt`
- `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/timerScreen/viewModel/TimerAction.kt`
- `shared/src/androidMain/kotlin/org/nsh07/pomodoro/ui/timerScreen/viewModel/TimerViewModel.kt`
- `shared/src/androidMain/kotlin/org/nsh07/pomodoro/ui/timerScreen/TimerScreen.kt`
- `androidApp/src/main/java/org/nsh07/pomodoro/service/TimerService.kt`
- `androidApp/src/main/java/org/nsh07/pomodoro/ui/AppScreen.kt`

Tasks:

1. Add `currentTaskId` and `currentTaskTitle` to `TimerState`.
2. Add timer actions for setting and clearing current task context.
3. Add a Start Focus action on Today task rows.
4. Starting from a task should set task context, navigate to Timer, and start the timer immediately unless that causes service-state issues.
5. Timer screen should show current task context compactly when present.
6. Timer screen should still allow unassigned sessions.

Acceptance:

- Starting from a task binds that task to the timer.
- Starting directly from Timer records unassigned focus.
- Clearing/changing task context does not reset the timer unless deliberately chosen.

## Phase 4: Focus Session Recording

Goal: write task-aware focus history while preserving existing aggregate stats.

Files likely involved:

- `androidApp/src/main/java/org/nsh07/pomodoro/service/TimerService.kt`
- New or existing focus session repository files.

Tasks:

1. Inject `FocusSessionRepository` into `TimerService`.
2. Update `saveTimeToDb()` so focus time writes both:
   - `StatRepository.addFocusTime(...)`
   - `FocusSessionRepository.recordFocus(...)`
3. Do not record break time against tasks.
4. Mark full focus completions as completed pomodoros.
5. Treat partial reset/skip intervals as focus minutes but not completed pomodoros.
6. Guard against double-counting by respecting `lastSavedDuration`.

Acceptance:

- Existing daily focus totals remain correct.
- Task focus totals match focus-session rows.
- Unassigned sessions are visible in data.
- Partial sessions do not inflate pomodoro counts.

## Phase 5: Stats Extension

Goal: extend Tomato stats with simple task-aware summaries.

Files likely involved:

- `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/statsScreen/viewModel/StatsViewModel.kt`
- `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/statsScreen/screens/StatsMainScreen.kt`
- New small stats components if needed.

Tasks:

1. Inject `TaskRepository` and `FocusSessionRepository` into `StatsViewModel`.
2. Add flows for:
   - Today's completed task count.
   - Today's completed pomodoro count.
   - Task focus ranking.
   - Unassigned focus total.
3. Add a compact Today summary section.
4. Add a task focus ranking section.
5. Reuse existing chart components for 7-day trend instead of creating a new chart system.

Acceptance:

- Stats answers: time focused, pomodoros completed, tasks completed, and where focus went.
- Existing Tomato week/month/year stats still work.
- Task stats stay simple and scannable.

## Phase 6: Backup, Reset, and Polish

Goal: avoid leaving new data outside existing maintenance flows.

Files likely involved:

- `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/BackupRestoreManager.kt`
- `shared/src/androidMain/kotlin/org/nsh07/pomodoro/data/AndroidBackupRestoreManager.kt`
- Existing settings reset and backup/restore screens.
- Strings and launcher branding if renaming the app.

Tasks:

1. Include task and focus-session tables in backup/restore if Tomato backup exports DB-level data.
2. Ensure reset data clears tasks and focus sessions.
3. Decide whether to rename app/package for personal fork.
4. Remove or hide Tomato Plus/paywall surfaces if they are irrelevant for personal use.
5. Review icons and strings for new Today functionality.

Acceptance:

- Reset clears all user productivity data.
- Backup/restore includes new task data or explicitly documents that it does not.
- App does not present confusing commercial surfaces for the personal fork.

## Verification Plan

Immediate local checks:

- Git status before and after each phase.
- Static grep/review for duplicated routes, missing DI bindings, and missing strings.
- Repository tests if they can run without Android SDK.

Android-required checks:

- Gradle sync in Android Studio or CI.
- Room schema generation.
- Unit tests.
- App launch on emulator/device.
- Foreground timer service behavior.
- Notification actions.
- Timer survives backgrounding.
- Task-bound and unassigned focus sessions record correctly.

## Suggested Commit Sequence

1. Add task and focus-session data layer.
2. Add Today screen and navigation.
3. Bind tasks to timer state.
4. Record focus sessions from timer service.
5. Extend stats with task summaries.
6. Polish backup/reset/branding.

Each commit should be independently reviewable and avoid unrelated refactors.
