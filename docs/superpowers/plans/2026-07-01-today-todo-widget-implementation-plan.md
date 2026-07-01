# Today TODO Widget Implementation Plan

> **For agentic workers:** Implement task by task, committing after each completed task. Keep the widget read-only; do not add task completion, editing, timer controls, or sorting from the launcher.

**Goal:** Convert the existing Today home-screen widget from focus statistics into a glanceable Today TODO list that shows unfinished Today tasks, the task count, optional due-date labels, and opens the Today page when tapped.

**Architecture:** Reuse the existing Glance widget infrastructure in `androidApp`. Add a small Android-side widget data adapter that reads ordered Today tasks from `TaskRepository`, reuse the existing due-date label helper where practical, and keep widget layout logic inside `TodayAppWidget`. Trigger `TodayAppWidget.updateAll(context)` from task mutations so the launcher stays current.

**Tech Stack:** Android App Widget, Jetpack Glance, Kotlin, Koin, Room-backed `TaskRepository`, Compose resource strings, Gradle.

---

## File Structure

- Modify `androidApp/src/main/java/org/nsh07/pomodoro/widget/TodayAppWidget.kt`
  - Replace focus-stat UI with read-only TODO-list UI.
  - Inject `TaskRepository` instead of `StatRepository`.
  - Render title count, task rows, due-date labels, current-task emphasis, and empty state.
- Modify `androidApp/src/main/res/xml/today_widget_info.xml`
  - Keep the same receiver and provider file.
  - Update min/target sizing if needed for a readable TODO list.
- Modify `shared/src/commonMain/composeResources/values/strings.xml`
  - Add or update widget strings.
- Modify `shared/src/commonMain/composeResources/values-zh-rCN/strings.xml`
  - Add Simplified Chinese widget strings.
- Modify `shared/src/commonMain/composeResources/values-zh-rTW/strings.xml`
  - Add Traditional Chinese widget strings.
- Modify `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskRepository.kt`
  - Add widget update hooks after task mutations that affect Today tasks, or expose a narrow update trigger from Android if a common repository hook is not appropriate.
- Optional create `androidApp/src/main/java/org/nsh07/pomodoro/widget/TodayTodoWidgetModel.kt`
  - Small Android-side DTO and mapping helpers for widget display.
- Optional create `androidApp/src/test/java/org/nsh07/pomodoro/widget/TodayTodoWidgetModelTest.kt`
  - JVM tests for row limiting, count formatting inputs, and due-date mapping if the mapping has non-trivial logic.

## Task 1: Add Widget Display Strings

**Files:**
- Modify `shared/src/commonMain/composeResources/values/strings.xml`
- Modify `shared/src/commonMain/composeResources/values-zh-rCN/strings.xml`
- Modify `shared/src/commonMain/composeResources/values-zh-rTW/strings.xml`

- [ ] **Step 1: Add title and empty-state strings**

Add strings for:

```xml
<string name="today_widget_title_count">Today %1$d</string>
<string name="today_widget_title_count_zh">今天 %1$d项</string>
<string name="today_widget_empty">No tasks today</string>
<string name="today_widget_empty_desc">Tap to plan your day</string>
<string name="today_widget_current_task">Current</string>
```

Use locale-specific resource names only if the existing Compose resource generator cannot handle the same formatted string cleanly across English and Chinese. Prefer one shared key per concept:

```xml
<string name="today_widget_title_count">Today %1$d</string>
```

```xml
<string name="today_widget_title_count">今天 %1$d项</string>
```

```xml
<string name="today_widget_title_count">今天 %1$d項</string>
```

Also update `today_widget_desc` to describe TODOs rather than focus stats:

- English: `Today's tasks`
- Simplified Chinese: `今日待办`
- Traditional Chinese: `今日待辦`

- [ ] **Step 2: Reuse existing due-date strings**

Prefer existing strings from the Today page for due-date labels:

- `due_today`
- `due_tomorrow`
- `due_weekday`
- `due_date`
- `overdue`

Do not add duplicate due-date strings unless Glance-side formatting needs shorter wording.

- [ ] **Step 3: Compile resources**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :shared:compileAndroidMain --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit strings**

```powershell
git add shared/src/commonMain/composeResources/values/strings.xml shared/src/commonMain/composeResources/values-zh-rCN/strings.xml shared/src/commonMain/composeResources/values-zh-rTW/strings.xml
git commit -m "Add Today TODO widget strings"
```

## Task 2: Create Widget Data Mapping

**Files:**
- Optional create `androidApp/src/main/java/org/nsh07/pomodoro/widget/TodayTodoWidgetModel.kt`
- Optional create `androidApp/src/test/java/org/nsh07/pomodoro/widget/TodayTodoWidgetModelTest.kt`

- [ ] **Step 1: Create a small display model if needed**

If `TodayAppWidget` starts to mix data shaping with layout, create:

```kotlin
data class TodayTodoWidgetTask(
    val id: Long,
    val title: String,
    val dueDate: LocalDate?,
    val isCurrent: Boolean
)

data class TodayTodoWidgetState(
    val unfinishedCount: Int,
    val visibleTasks: List<TodayTodoWidgetTask>
)
```

Add a mapper:

```kotlin
fun List<TaskItem>.toTodayTodoWidgetState(maxItems: Int): TodayTodoWidgetState =
    TodayTodoWidgetState(
        unfinishedCount = size,
        visibleTasks = take(maxItems).mapIndexed { index, task ->
            TodayTodoWidgetTask(
                id = task.id,
                title = task.title,
                dueDate = task.dueDate,
                isCurrent = index == 0
            )
        }
    )
```

- [ ] **Step 2: Add focused tests if a mapper exists**

Test:

- Count includes all unfinished Today tasks, not just visible tasks.
- Visible tasks are capped by `maxItems`.
- First visible task is current.
- Empty input produces count `0` and no visible rows.

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :androidApp:testFossDebugUnitTest --tests org.nsh07.pomodoro.widget.TodayTodoWidgetModelTest --console=plain
```

Expected: tests pass if added.

- [ ] **Step 3: Commit mapping**

```powershell
git add androidApp/src/main/java/org/nsh07/pomodoro/widget/TodayTodoWidgetModel.kt androidApp/src/test/java/org/nsh07/pomodoro/widget/TodayTodoWidgetModelTest.kt
git commit -m "Add Today TODO widget model"
```

If the mapper is small enough to keep private inside `TodayAppWidget`, skip this commit.

## Task 3: Replace Today Widget UI With TODO List

**Files:**
- Modify `androidApp/src/main/java/org/nsh07/pomodoro/widget/TodayAppWidget.kt`
- Modify `androidApp/src/main/res/xml/today_widget_info.xml` if sizing needs adjustment

- [ ] **Step 1: Read tasks from the repository**

In `TodayAppWidget.provideGlance`, replace `StatRepository` with `TaskRepository`:

```kotlin
val taskRepository: TaskRepository = get()
val tasks = taskRepository.getActiveTodayTasks().first()
```

Build the widget state from those ordered tasks. Keep repository ordering as the source of truth.

- [ ] **Step 2: Replace focus-stat content**

Remove the focus time, quarter values, refresh icon, and stacked bar from `TodayAppWidget`.

Render:

- Header text: `today_widget_title_count`, using the unfinished Today count.
- Empty state when count is zero.
- Task rows when count is greater than zero.
- First row with stronger text weight or a small accent indicator.
- Due-date label when `task.dueDate != null`.

Use Glance components only; do not import regular Compose Material components into Glance UI.

- [ ] **Step 3: Choose row count by widget size**

Use `LocalSize.current` and `TomatoWidgetSize` to cap visible tasks:

- Height around `Height1`: 1-2 tasks.
- Height around `Height2`: 3-4 tasks.
- Taller widgets: up to 6 tasks.

The count in the header must remain the full unfinished Today count.

- [ ] **Step 4: Add due-date text helper**

Use existing `dueDateLabel` from the Today page and convert keys into Android string resources inside the widget:

```kotlin
private fun Context.widgetDueDateText(dueDate: LocalDate): String
```

The widget should display compact localized labels:

- Overdue
- Today
- Tomorrow
- Weekday or short date for future dates

- [ ] **Step 5: Keep click behavior simple**

Keep a broad click target:

```kotlin
.clickable(actionStartActivity<MainActivity>())
```

Do not add per-row click actions in V1.

- [ ] **Step 6: Adjust widget provider sizing only if needed**

Keep the existing `TodayWidgetReceiver` and `today_widget_info.xml`.

If the current provider max height `132dp` prevents the TODO list from being useful, update it to allow taller resizing while keeping the existing minimum size.

- [ ] **Step 7: Compile**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :androidApp:compileFossDebugKotlin --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit widget UI**

```powershell
git add androidApp/src/main/java/org/nsh07/pomodoro/widget/TodayAppWidget.kt androidApp/src/main/res/xml/today_widget_info.xml
git commit -m "Show Today tasks in home widget"
```

## Task 4: Refresh Widget After Task Changes

**Files:**
- Modify `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskRepository.kt` if a common callback can be injected cleanly
- Or modify Android app wiring to provide a widget updater dependency
- Possibly create `androidApp/src/main/java/org/nsh07/pomodoro/widget/TodayWidgetUpdater.kt`

- [ ] **Step 1: Decide the update boundary**

Prefer a tiny Android-side updater abstraction so common data code does not directly know about Glance:

```kotlin
fun interface TodayWidgetUpdater {
    suspend fun update()
}
```

On Android, implement it with:

```kotlin
TodayAppWidget().updateAll(context)
```

If injecting this into `AppTaskRepository` is awkward because the repository lives in shared common code, use an Android-specific wrapper or call updates from ViewModel actions after repository mutations. The priority is a clean boundary and no Glance dependency in `commonMain`.

- [ ] **Step 2: Trigger updates for relevant task mutations**

Refresh after:

- `createTask`
- `setDone`
- `moveToToday`
- `moveToLater`
- `moveWithinToday`
- `moveToSection`
- `setDueDate`
- `clearDueDate`
- `deleteTask`
- `deleteAllTasks`

Refreshing after extra task mutations is acceptable. Missing Today-affecting mutations is not.

- [ ] **Step 3: Keep update failures non-fatal**

Widget updates should not make task operations fail. If needed, catch and ignore/log widget update errors around `updateAll`.

- [ ] **Step 4: Compile**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :shared:compileAndroidMain :androidApp:compileFossDebugKotlin --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit refresh integration**

```powershell
git status --short
git add <exact files changed for widget refresh>
git commit -m "Refresh Today widget after task changes"
```

## Task 5: Verify End-To-End

**Files:**
- Modify only files needed to fix compile, resource, or packaging errors.

- [ ] **Step 1: Run unit tests**

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :androidApp:testFossDebugUnitTest --console=plain
```

Expected: tests pass.

- [ ] **Step 2: Compile shared Android code**

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :shared:compileAndroidMain --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Build FOSS debug APK**

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :androidApp:assembleFossDebug --console=plain
```

Expected: `BUILD SUCCESSFUL` and `androidApp/build/outputs/apk/foss/debug/androidApp-foss-debug.apk` is updated.

- [ ] **Step 4: Check resources and manifest packaging**

Verify:

- `today_widget_info.xml` still points to `TodayWidgetReceiver`.
- `today_widget_desc` resolves in English, Simplified Chinese, and Traditional Chinese.
- No duplicate resource names were introduced incorrectly.

- [ ] **Step 5: Check whitespace**

```powershell
git diff --check
```

Expected: exit code 0.

- [ ] **Step 6: Manual acceptance checklist**

Use a device or emulator when available:

- Launcher can add the Today widget.
- Widget title shows `今天 N项` in Simplified Chinese.
- Widget lists unfinished Today tasks only.
- Task order matches the Today page.
- First task is subtly emphasized.
- Due-date labels are shown when present.
- Later and completed tasks are excluded.
- Empty state appears when there are no unfinished Today tasks.
- Tapping the widget opens the app.
- Adding, completing, moving, reordering, and changing due dates refresh the widget.

- [ ] **Step 7: Commit verification fixes**

If verification required fixes:

```powershell
git status --short
git add <exact fixed files>
git commit -m "Polish Today TODO widget"
```

If there are no changes, do not create an empty commit.
