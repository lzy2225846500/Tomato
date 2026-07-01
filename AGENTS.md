# AGENTS.md

## What This Project Is

This repository is a personal modified fork of `nsh07/Tomato`.

- Upstream project: https://github.com/nsh07/Tomato
- Personal fork: https://github.com/lzy2225846500/Tomato
- License: GNU GPLv3. Keep `LICENSE`, source headers, and fork attribution intact.
- Product direction: a lightweight Android TODO + Pomodoro app for personal use and a few friends.

The fork keeps the original timer app and adds a small task system: Today tasks, Later tasks, due dates, drag ordering, task-linked focus sessions, and a read-only Today TODO home-screen widget.

## Codebase Map

- `androidApp/`
  - Android application shell, manifest, app widgets, foreground service, DI module, and app entry points.
  - `androidApp/src/main/java/org/nsh07/pomodoro/MainActivity.kt`: Android activity hosting shared UI.
  - `androidApp/src/main/java/org/nsh07/pomodoro/TomatoApplication.kt`: Koin startup and app-level Android setup.
  - `androidApp/src/main/java/org/nsh07/pomodoro/di/androidModules.kt`: Android-side service/repository bindings.
  - `androidApp/src/main/java/org/nsh07/pomodoro/widget/`: Jetpack Glance widgets.
  - `androidApp/src/main/res/xml/*_widget_info.xml`: Android widget provider metadata.
- `shared/`
  - Kotlin Multiplatform shared app logic, Compose UI, Room data model, resources, and ViewModels.
  - `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/`: repositories, DAOs, entities, database, converters.
  - `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/`: Compose screens and screen-level components.
  - `shared/src/commonMain/composeResources/`: Compose Multiplatform strings, fonts, drawables, and generated resources.
  - `shared/schemas/org.nsh07.pomodoro.data.AppDatabase/`: Room schema snapshots.
- `docs/superpowers/specs/` and `docs/superpowers/plans/`
  - Design specs and implementation plans for user-facing feature work.
- `fastlane/`, `weblate/`, `.github/`
  - Upstream release, translation, and repository support files. Be conservative when editing these.

## Architecture And Data Flow

The app is split between Android-specific shell code and shared KMP code.

- Shared UI is built with Compose Multiplatform.
- Android-only widgets and services live in `androidApp`.
- Persistence uses Room in shared code.
- Koin wires repositories, DAOs, ViewModels, Android services, and widget-facing wrappers.

Task flow:

1. UI dispatches `TasksAction`.
2. `TasksViewModel` handles the action and calls `TaskRepository`.
3. `TaskRepository` updates Room through `TaskDao`.
4. Task flows update `TasksState`.
5. `TasksScreen` renders state.
6. Android `WidgetUpdatingTaskRepository` decorates `TaskRepository` and refreshes `TodayAppWidget` after task mutations.

Timer/focus flow:

1. Timer UI and service use timer state/repositories.
2. Focus sessions are persisted via `FocusSessionRepository`.
3. Task-bound focus data is read by Today and stats UI.

## Important Task Files

- `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskItem.kt`
  - Room entity for tasks.
  - Current task fields include `title`, `isDone`, `isToday`, `sortOrder`, `dueDate`, `createdAt`, and `completedAt`.
- `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskDao.kt`
  - Ordered task queries and update methods.
  - Keep Today/Later active queries ordered by `sortOrder ASC, id ASC`.
- `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskRepository.kt`
  - Task business rules: create, complete, Today/Later movement, due dates, reorder.
  - Keep ordering and section movement out of UI code.
- `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskOrdering.kt`
  - Pure helpers for compact order updates. Prefer testing this instead of embedding reorder logic in Compose.
- `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/viewModel/`
  - `TasksAction`, `TasksState`, and `TasksViewModel`.
  - Add new user intents to `TasksAction`, expose derived screen state through `TasksState`, and keep side effects in `TasksViewModel`.
- `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/TasksScreen.kt`
  - Today workbench UI.
  - Avoid growing this file with unrelated task logic; extract focused helpers when behavior gets non-trivial.
- `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/DueDateLabels.kt`
  - Shared due-date label mapping.
- `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/ReorderableTaskList.kt`
  - Visible row mapping for Today/Later drag reordering.

## Widget Files

- `androidApp/src/main/java/org/nsh07/pomodoro/widget/TodayAppWidget.kt`
  - Read-only Today TODO widget.
  - Reads ordered unfinished Today tasks and renders a Glance list.
  - Do not add completion/edit/sort actions unless the feature is explicitly redesigned.
- `androidApp/src/main/java/org/nsh07/pomodoro/widget/TodayTodoWidgetModel.kt`
  - Small display model and list cap logic for the widget.
- `androidApp/src/main/java/org/nsh07/pomodoro/widget/WidgetUpdatingTaskRepository.kt`
  - Android-side `TaskRepository` decorator that calls `TodayAppWidget().updateAll(context)` after mutations.
  - Keep Glance dependencies out of `shared/commonMain`.
- `androidApp/src/main/java/org/nsh07/pomodoro/widget/TimerAppWidget.kt`
  - Timer widget. Do not mix task widget behavior into it.
- `androidApp/src/main/java/org/nsh07/pomodoro/widget/HistoryAppWidget.kt`
  - Focus history widget.

## Room And Migration Guidance

- `AppDatabase` version and `shared/schemas/...` must be updated together.
- Existing user data matters. Do not use destructive migrations.
- Non-null columns need deterministic migration defaults.
- After changing entities, run `:shared:compileAndroidMain` and check the generated schema.
- Keep task ordering stable:
  - active Today: `sortOrder ASC, id ASC`
  - active Later: `sortOrder ASC, id ASC`
  - completed Today: `completedAt DESC, id DESC`

## UI Guidance

- Follow existing Compose Material 3 patterns.
- Keep this app quiet, native, and utilitarian; avoid landing-page-like UI.
- Today page current task is derived, not separately persisted: first unfinished Today task.
- Due dates do not automatically move tasks between Today and Later.
- Quick capture should stay fast. Avoid adding heavy controls into the add row.
- Completed tasks are separate from active ordering and should not be draggable in the current design.
- Use existing string resources and add Chinese coverage for visible new UI.

## Localization

Default strings are in:

- `shared/src/commonMain/composeResources/values/strings.xml`

For this fork, at minimum keep Simplified and Traditional Chinese updated:

- `shared/src/commonMain/composeResources/values-zh-rCN/strings.xml`
- `shared/src/commonMain/composeResources/values-zh-rTW/strings.xml`

There are many upstream translations. Do not machine-translate all locales casually. If adding a feature quickly, add default English plus Chinese and let other locales fall back unless the user asks for broader translation work.

## Testing And Verification

Use the local Android SDK bundled in this workspace when needed:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
```

Run unit tests:

```powershell
.\gradlew.bat :androidApp:testFossDebugUnitTest --console=plain
```

Compile shared Android code:

```powershell
.\gradlew.bat :shared:compileAndroidMain --console=plain
```

Build the FOSS debug APK:

```powershell
.\gradlew.bat :androidApp:assembleFossDebug --console=plain
```

Check whitespace before committing:

```powershell
git diff --check
```

For task/data/widget changes, prefer running all four commands before claiming completion. If no emulator or Android device is available, say so and provide the debug APK path after a successful build.

## Git And Remote Rules

- `origin` is the user's fork. Push user work there.
- `upstream` is the original `nsh07/Tomato` repository. Do not push there.
- Keep commits coherent and readable.
- Do not rewrite public history unless the user explicitly requests it.
- Do not revert unrelated user changes.
- Before push, check:

```powershell
git status --short --branch
git remote -v
```

## GPL And Attribution

- This is GPLv3 code. Keep the GPL license file.
- Preserve existing copyright headers.
- Make it clear in docs that this is a modified fork, not the official upstream app.
- If distributing APKs, provide corresponding source code for the distributed version.
