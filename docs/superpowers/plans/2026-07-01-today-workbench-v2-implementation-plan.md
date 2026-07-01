# Today Workbench V2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the Today page into a current-task workbench with summary metrics, persistent ordering, Today/Later drag movement, and lightweight due dates.

**Architecture:** Add ordering and due-date persistence to `TaskItem`, keep ordering rules in repository/helper code, expose derived workbench state through `TasksViewModel`, and keep Today UI components focused and locally scoped. Drag sorting should commit one repository operation at drop time, not continuously during movement.

**Tech Stack:** Kotlin Multiplatform, Android Compose Material 3, Room, Koin, Kotlin coroutines/Flow, JUnit, Gradle.

---

## File Structure

- Modify `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskItem.kt`
  - Add `sortOrder: Long` and `dueDate: LocalDate?`.
- Modify `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskDao.kt`
  - Add ordered queries, batch section/order updates, sort-order max queries, and due-date updates.
- Modify `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskRepository.kt`
  - Add ordering, cross-section moves, due-date operations, and completion placement rules.
- Modify `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/AppDatabase.kt`
  - Move to database version 4 and register migration.
- Modify `shared/src/androidMain/kotlin/org/nsh07/pomodoro/di/modules.kt`
  - Register the 3-to-4 Room migration in `Room.databaseBuilder`.
- Create `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskOrdering.kt`
  - Pure ordering helpers for list moves and compact `sortOrder` assignment.
- Modify `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/viewModel/TasksState.kt`
  - Add summary fields, current task, drag/date editor state.
- Modify `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/viewModel/TasksAction.kt`
  - Add reorder, cross-section move, and due-date actions.
- Modify `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/viewModel/TasksViewModel.kt`
  - Combine task flows with focus-session summary flows and handle new actions.
- Modify `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/TasksScreen.kt`
  - Replace the sparse list with summary, current-task panel, sortable Today/Later sections, completed section, and due-date editor.
- Modify `gradle/libs.versions.toml`
  - Add the Compose reorderable dependency.
- Modify `shared/build.gradle.kts`
  - Add the reorderable library to `commonMain`.
- Create `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/DueDateLabels.kt`
  - Convert `LocalDate?` into compact labels.
- Create `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/ReorderableTaskList.kt`
  - Local wrapper around the reorderable LazyColumn API for Today/Later unfinished task rows.
- Modify `shared/src/commonMain/composeResources/values/strings.xml`
  - Add new English strings.
- Modify `shared/src/commonMain/composeResources/values-zh-rCN/strings.xml`
  - Add simplified Chinese strings.
- Modify `shared/src/commonMain/composeResources/values-zh-rTW/strings.xml`
  - Add traditional Chinese strings.
- Add or modify `shared/schemas/org.nsh07.pomodoro.data.AppDatabase/4.json`
  - Room-generated schema for v4.
- Create `androidApp/src/test/java/org/nsh07/pomodoro/data/TaskOrderingTest.kt`
  - JVM tests for pure ordering helper behavior.

## Task 1: Add Pure Task Ordering Helpers

**Files:**
- Create: `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskOrdering.kt`
- Test: `androidApp/src/test/java/org/nsh07/pomodoro/data/TaskOrderingTest.kt`

- [ ] **Step 1: Create failing tests for ordering behavior**

Create `androidApp/src/test/java/org/nsh07/pomodoro/data/TaskOrderingTest.kt`:

```kotlin
package org.nsh07.pomodoro.data

import junit.framework.TestCase.assertEquals
import org.junit.Test

class TaskOrderingTest {
    @Test
    fun `move within same section compacts order`() {
        val tasks = listOf(
            TaskOrderUpdate(id = 1, isToday = true, sortOrder = 0),
            TaskOrderUpdate(id = 2, isToday = true, sortOrder = 1),
            TaskOrderUpdate(id = 3, isToday = true, sortOrder = 2)
        )

        val result = tasks.moveWithinSection(fromIndex = 2, toIndex = 0)

        assertEquals(
            listOf(
                TaskOrderUpdate(id = 3, isToday = true, sortOrder = 0),
                TaskOrderUpdate(id = 1, isToday = true, sortOrder = 1),
                TaskOrderUpdate(id = 2, isToday = true, sortOrder = 2)
            ),
            result
        )
    }

    @Test
    fun `move across sections inserts into target and compacts both sections`() {
        val today = listOf(
            TaskOrderUpdate(id = 1, isToday = true, sortOrder = 0),
            TaskOrderUpdate(id = 2, isToday = true, sortOrder = 1)
        )
        val later = listOf(
            TaskOrderUpdate(id = 3, isToday = false, sortOrder = 0),
            TaskOrderUpdate(id = 4, isToday = false, sortOrder = 1)
        )

        val result = moveAcrossSections(
            source = today,
            target = later,
            movedTaskId = 2,
            targetIndex = 1,
            targetIsToday = false
        )

        assertEquals(
            TaskSectionMoveResult(
                source = listOf(TaskOrderUpdate(id = 1, isToday = true, sortOrder = 0)),
                target = listOf(
                    TaskOrderUpdate(id = 3, isToday = false, sortOrder = 0),
                    TaskOrderUpdate(id = 2, isToday = false, sortOrder = 1),
                    TaskOrderUpdate(id = 4, isToday = false, sortOrder = 2)
                )
            ),
            result
        )
    }
}
```

- [ ] **Step 2: Run the tests and verify they fail**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :androidApp:testFossDebugUnitTest --tests org.nsh07.pomodoro.data.TaskOrderingTest --console=plain
```

Expected: compilation fails because `TaskOrderUpdate`, `moveWithinSection`, `moveAcrossSections`, and `TaskSectionMoveResult` do not exist.

- [ ] **Step 3: Add the ordering helper**

Create `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskOrdering.kt`:

```kotlin
package org.nsh07.pomodoro.data

data class TaskOrderUpdate(
    val id: Long,
    val isToday: Boolean,
    val sortOrder: Long
)

data class TaskSectionMoveResult(
    val source: List<TaskOrderUpdate>,
    val target: List<TaskOrderUpdate>
)

fun List<TaskOrderUpdate>.moveWithinSection(fromIndex: Int, toIndex: Int): List<TaskOrderUpdate> {
    if (isEmpty()) return emptyList()
    val safeFrom = fromIndex.coerceIn(indices)
    val safeTo = toIndex.coerceIn(indices)
    val mutable = toMutableList()
    val moved = mutable.removeAt(safeFrom)
    mutable.add(safeTo, moved)
    return mutable.compactOrders(moved.isToday)
}

fun moveAcrossSections(
    source: List<TaskOrderUpdate>,
    target: List<TaskOrderUpdate>,
    movedTaskId: Long,
    targetIndex: Int,
    targetIsToday: Boolean
): TaskSectionMoveResult {
    val moved = source.first { it.id == movedTaskId }
    val sourceIsToday = moved.isToday
    val nextSource = source.filterNot { it.id == movedTaskId }.compactOrders(sourceIsToday)
    val nextTarget = target.toMutableList()
    nextTarget.add(
        targetIndex.coerceIn(0, nextTarget.size),
        moved.copy(isToday = targetIsToday)
    )
    return TaskSectionMoveResult(
        source = nextSource,
        target = nextTarget.compactOrders(targetIsToday)
    )
}

fun List<TaskOrderUpdate>.compactOrders(isToday: Boolean): List<TaskOrderUpdate> =
    mapIndexed { index, task ->
        task.copy(isToday = isToday, sortOrder = index.toLong())
    }
```

- [ ] **Step 4: Run ordering tests and verify they pass**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :androidApp:testFossDebugUnitTest --tests org.nsh07.pomodoro.data.TaskOrderingTest --console=plain
```

Expected: `TaskOrderingTest` passes.

- [ ] **Step 5: Commit ordering helper**

```powershell
git add shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskOrdering.kt androidApp/src/test/java/org/nsh07/pomodoro/data/TaskOrderingTest.kt
git commit -m "Add task ordering helpers"
```

## Task 2: Migrate Task Storage To Sort Order And Due Date

**Files:**
- Modify: `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskItem.kt`
- Modify: `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskDao.kt`
- Modify: `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/AppDatabase.kt`
- Modify: `shared/src/androidMain/kotlin/org/nsh07/pomodoro/di/modules.kt`
- Generate: `shared/schemas/org.nsh07.pomodoro.data.AppDatabase/4.json`

- [ ] **Step 1: Extend `TaskItem`**

Update `TaskItem` to:

```kotlin
package org.nsh07.pomodoro.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Immutable
@Entity(tableName = "task_item")
data class TaskItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val isDone: Boolean = false,
    val isToday: Boolean = true,
    val sortOrder: Long = 0,
    val dueDate: LocalDate? = null,
    val createdAt: Instant,
    val completedAt: Instant? = null
)
```

- [ ] **Step 2: Add DAO methods for order and due date**

Update `TaskDao` with these query changes and methods:

```kotlin
@Query(
    "SELECT * FROM task_item " +
            "WHERE isToday = 1 AND isDone = 0 " +
            "ORDER BY sortOrder ASC, id ASC"
)
fun getActiveTodayTasks(): Flow<List<TaskItem>>

@Query(
    "SELECT * FROM task_item " +
            "WHERE isToday = 0 AND isDone = 0 " +
            "ORDER BY sortOrder ASC, id ASC"
)
fun getLaterTasks(): Flow<List<TaskItem>>

@Query(
    "SELECT * FROM task_item " +
            "WHERE isToday = 1 AND isDone = 1 " +
            "ORDER BY completedAt DESC, id DESC"
)
fun getCompletedTodayTasks(): Flow<List<TaskItem>>

@Query("SELECT COALESCE(MAX(sortOrder), -1) FROM task_item WHERE isToday = :isToday AND isDone = 0")
suspend fun getMaxSortOrder(isToday: Boolean): Long

@Query("UPDATE task_item SET isDone = :isDone, completedAt = :completedAt, sortOrder = :sortOrder WHERE id = :id")
suspend fun setDone(id: Long, isDone: Boolean, completedAt: Instant?, sortOrder: Long)

@Query("UPDATE task_item SET isToday = :isToday, sortOrder = :sortOrder WHERE id = :id")
suspend fun setSectionAndOrder(id: Long, isToday: Boolean, sortOrder: Long)

@Query("UPDATE task_item SET dueDate = :dueDate WHERE id = :id")
suspend fun setDueDate(id: Long, dueDate: LocalDate?)
```

Keep existing `insertTask`, `updateTitle`, `deleteTask`, `getTask`, `getCompletedCount`, and `clearAll`.

- [ ] **Step 3: Add manual Room migration from 3 to 4**

Update `AppDatabase.kt`:

```kotlin
@Database(
    entities = [
        IntPreference::class,
        BooleanPreference::class,
        StringPreference::class,
        Stat::class,
        TaskItem::class,
        FocusSession::class
    ],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3)
    ]
)
```

Do not add `AutoMigration(from = 3, to = 4)` because `sortOrder` is non-null and needs deterministic initialization.

- [ ] **Step 4: Register migration in Android DI**

Modify `shared/src/androidMain/kotlin/org/nsh07/pomodoro/di/modules.kt` imports:

```kotlin
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
```

Add below `androidModule`:

```kotlin
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE task_item ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE task_item ADD COLUMN dueDate TEXT")
        connection.execSQL(
            """
            UPDATE task_item
            SET sortOrder = (
                SELECT COUNT(*)
                FROM task_item AS earlier
                WHERE earlier.isDone = task_item.isDone
                    AND earlier.isToday = task_item.isToday
                    AND (
                        earlier.createdAt < task_item.createdAt
                        OR (earlier.createdAt = task_item.createdAt AND earlier.id <= task_item.id)
                    )
            ) - 1
            """.trimIndent()
        )
    }
}
```

Update `createDatabase`:

```kotlin
return Room.databaseBuilder(
    context,
    AppDatabase::class.java,
    "app_database"
).addMigrations(MIGRATION_3_4).build()
```

- [ ] **Step 5: Compile shared Android code to generate schema**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :shared:compileAndroidMain --console=plain
```

Expected: build succeeds and `shared/schemas/org.nsh07.pomodoro.data.AppDatabase/4.json` exists.

- [ ] **Step 6: Verify schema contains new columns**

Run:

```powershell
Select-String -Path .\shared\schemas\org.nsh07.pomodoro.data.AppDatabase\4.json -Pattern '"version": 4','"columnName": "sortOrder"','"columnName": "dueDate"'
```

Expected: all three patterns are found.

- [ ] **Step 7: Commit database migration**

```powershell
git add shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskItem.kt shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskDao.kt shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/AppDatabase.kt shared/src/androidMain/kotlin/org/nsh07/pomodoro/di/modules.kt shared/schemas/org.nsh07.pomodoro.data.AppDatabase/4.json
git commit -m "Add task ordering and due dates to database"
```

## Task 3: Add Repository Operations For Ordering And Due Dates

**Files:**
- Modify: `shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskRepository.kt`

- [ ] **Step 1: Extend the repository interface**

Update `TaskRepository`:

```kotlin
interface TaskRepository {
    suspend fun createTask(title: String, isToday: Boolean = true): Long?
    suspend fun updateTitle(id: Long, title: String)
    suspend fun setDone(id: Long, isDone: Boolean)
    suspend fun moveToToday(id: Long)
    suspend fun moveToLater(id: Long)
    suspend fun moveWithinToday(fromIndex: Int, toIndex: Int)
    suspend fun moveWithinLater(fromIndex: Int, toIndex: Int)
    suspend fun moveToSection(taskId: Long, targetIsToday: Boolean, targetIndex: Int)
    suspend fun setDueDate(id: Long, dueDate: LocalDate?)
    suspend fun clearDueDate(id: Long)
    suspend fun deleteTask(id: Long)
    suspend fun getTask(id: Long): TaskItem?
    fun getActiveTodayTasks(): Flow<List<TaskItem>>
    fun getCompletedTodayTasks(): Flow<List<TaskItem>>
    fun getLaterTasks(): Flow<List<TaskItem>>
    fun getTodayCompletedCount(): Flow<Int>
    suspend fun deleteAllTasks()
}
```

- [ ] **Step 2: Update task creation and completion placement**

In `AppTaskRepository.createTask`, compute trailing order:

```kotlin
val nextOrder = taskDao.getMaxSortOrder(isToday) + 1
taskDao.insertTask(
    TaskItem(
        title = trimmed,
        isToday = isToday,
        sortOrder = nextOrder,
        createdAt = Instant.now()
    )
)
```

In `setDone`, assign trailing sort order when uncompleting:

```kotlin
override suspend fun setDone(id: Long, isDone: Boolean) = withContext(ioDispatcher) {
    val task = taskDao.getTask(id) ?: return@withContext
    val sortOrder = if (isDone) task.sortOrder else taskDao.getMaxSortOrder(task.isToday) + 1
    taskDao.setDone(id, isDone, if (isDone) Instant.now() else null, sortOrder)
}
```

- [ ] **Step 3: Update move helpers to place at section bottom**

Update existing moves:

```kotlin
override suspend fun moveToToday(id: Long) = withContext(ioDispatcher) {
    taskDao.setSectionAndOrder(id, true, taskDao.getMaxSortOrder(true) + 1)
}

override suspend fun moveToLater(id: Long) = withContext(ioDispatcher) {
    taskDao.setSectionAndOrder(id, false, taskDao.getMaxSortOrder(false) + 1)
}
```

- [ ] **Step 4: Add reorder implementations**

Add:

```kotlin
override suspend fun moveWithinToday(fromIndex: Int, toIndex: Int) = withContext(ioDispatcher) {
    val tasks = taskDao.getActiveTodayTasksSnapshot()
    tasks.map { TaskOrderUpdate(it.id, true, it.sortOrder) }
        .moveWithinSection(fromIndex, toIndex)
        .forEach { taskDao.setSectionAndOrder(it.id, it.isToday, it.sortOrder) }
}

override suspend fun moveWithinLater(fromIndex: Int, toIndex: Int) = withContext(ioDispatcher) {
    val tasks = taskDao.getLaterTasksSnapshot()
    tasks.map { TaskOrderUpdate(it.id, false, it.sortOrder) }
        .moveWithinSection(fromIndex, toIndex)
        .forEach { taskDao.setSectionAndOrder(it.id, it.isToday, it.sortOrder) }
}

override suspend fun moveToSection(taskId: Long, targetIsToday: Boolean, targetIndex: Int) =
    withContext(ioDispatcher) {
        val sourceIsToday = taskDao.getTask(taskId)?.isToday ?: return@withContext
        val source = if (sourceIsToday) taskDao.getActiveTodayTasksSnapshot() else taskDao.getLaterTasksSnapshot()
        val target = if (targetIsToday) taskDao.getActiveTodayTasksSnapshot() else taskDao.getLaterTasksSnapshot()
        val result = moveAcrossSections(
            source = source.map { TaskOrderUpdate(it.id, sourceIsToday, it.sortOrder) },
            target = target.map { TaskOrderUpdate(it.id, targetIsToday, it.sortOrder) },
            movedTaskId = taskId,
            targetIndex = targetIndex,
            targetIsToday = targetIsToday
        )
        (result.source + result.target).forEach {
            taskDao.setSectionAndOrder(it.id, it.isToday, it.sortOrder)
        }
    }
```

Add snapshot DAO methods required by this step:

```kotlin
@Query("SELECT * FROM task_item WHERE isToday = 1 AND isDone = 0 ORDER BY sortOrder ASC, id ASC")
suspend fun getActiveTodayTasksSnapshot(): List<TaskItem>

@Query("SELECT * FROM task_item WHERE isToday = 0 AND isDone = 0 ORDER BY sortOrder ASC, id ASC")
suspend fun getLaterTasksSnapshot(): List<TaskItem>
```

- [ ] **Step 5: Add due-date repository methods**

```kotlin
override suspend fun setDueDate(id: Long, dueDate: LocalDate?) = withContext(ioDispatcher) {
    taskDao.setDueDate(id, dueDate)
}

override suspend fun clearDueDate(id: Long) = withContext(ioDispatcher) {
    taskDao.setDueDate(id, null)
}
```

- [ ] **Step 6: Compile**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :shared:compileAndroidMain --console=plain
```

Expected: build succeeds.

- [ ] **Step 7: Commit repository operations**

```powershell
git add shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskDao.kt shared/src/commonMain/kotlin/org/nsh07/pomodoro/data/TaskRepository.kt
git commit -m "Add task reorder repository operations"
```

## Task 4: Expand Tasks ViewModel State

**Files:**
- Modify: `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/viewModel/TasksState.kt`
- Modify: `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/viewModel/TasksAction.kt`
- Modify: `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/viewModel/TasksViewModel.kt`
- No DI file edit is needed for `TasksViewModel`; `viewModel<TasksViewModel>()` uses constructor injection.

- [ ] **Step 1: Add state fields**

Update `TasksState`:

```kotlin
@Immutable
data class TasksState(
    val activeTodayTasks: List<TaskItem> = emptyList(),
    val completedTodayTasks: List<TaskItem> = emptyList(),
    val laterTasks: List<TaskItem> = emptyList(),
    val currentTask: TaskItem? = null,
    val todayCompletedTaskCount: Int = 0,
    val todayTotalTaskCount: Int = 0,
    val todayCompletedPomodoroCount: Int = 0,
    val todayFocusTotal: Long = 0,
    val newTaskTitle: String = "",
    val showLater: Boolean = false,
    val dueDateEditorTask: TaskItem? = null
)
```

- [ ] **Step 2: Add actions**

Update `TasksAction`:

```kotlin
sealed interface TasksAction {
    data class SetNewTaskTitle(val title: String) : TasksAction
    data class SetShowLater(val value: Boolean) : TasksAction
    data class AddTask(val isToday: Boolean = true) : TasksAction
    data class SetDone(val id: Long, val isDone: Boolean) : TasksAction
    data class MoveToToday(val id: Long) : TasksAction
    data class MoveToLater(val id: Long) : TasksAction
    data class MoveWithinToday(val fromIndex: Int, val toIndex: Int) : TasksAction
    data class MoveWithinLater(val fromIndex: Int, val toIndex: Int) : TasksAction
    data class MoveToSection(val taskId: Long, val targetIsToday: Boolean, val targetIndex: Int) : TasksAction
    data class OpenDueDateEditor(val task: TaskItem) : TasksAction
    data class SetDueDate(val id: Long, val dueDate: LocalDate) : TasksAction
    data class ClearDueDate(val id: Long) : TasksAction
    data class DeleteTask(val id: Long) : TasksAction
    data object DismissDueDateEditor : TasksAction
}
```

Add `import java.time.LocalDate`.

- [ ] **Step 3: Inject focus-session summary into ViewModel**

Change constructor:

```kotlin
class TasksViewModel(
    private val taskRepository: TaskRepository,
    private val focusSessionRepository: FocusSessionRepository
) : ViewModel() {
```

Add import for `FocusSessionRepository`.

- [ ] **Step 4: Combine summary flows**

Add private holder types near the top of `TasksViewModel.kt`:

```kotlin
private data class TaskLists(
    val activeTodayTasks: List<TaskItem>,
    val completedTodayTasks: List<TaskItem>,
    val laterTasks: List<TaskItem>
)

private data class TodaySummary(
    val completedTaskCount: Int,
    val completedPomodoroCount: Int,
    val focusTotal: Long
)
```

Update `state` with nested `combine` calls:

```kotlin
private val taskLists = combine(
    taskRepository.getActiveTodayTasks(),
    taskRepository.getCompletedTodayTasks(),
    taskRepository.getLaterTasks()
) { activeTodayTasks, completedTodayTasks, laterTasks ->
    TaskLists(activeTodayTasks, completedTodayTasks, laterTasks)
}

private val todaySummary = combine(
    taskRepository.getTodayCompletedCount(),
    focusSessionRepository.getTodayCompletedPomodoroCount(),
    focusSessionRepository.getTodayFocusTotal()
) { completedTaskCount, completedPomodoroCount, focusTotal ->
    TodaySummary(completedTaskCount, completedPomodoroCount, focusTotal)
}

val state: StateFlow<TasksState> = combine(
    localState,
    taskLists,
    todaySummary
) { local, lists, summary ->
    local.copy(
        activeTodayTasks = lists.activeTodayTasks,
        completedTodayTasks = lists.completedTodayTasks,
        laterTasks = lists.laterTasks,
        currentTask = lists.activeTodayTasks.firstOrNull(),
        todayCompletedTaskCount = summary.completedTaskCount,
        todayTotalTaskCount = lists.activeTodayTasks.size + lists.completedTodayTasks.size,
        todayCompletedPomodoroCount = summary.completedPomodoroCount,
        todayFocusTotal = summary.focusTotal
    )
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasksState())
```

- [ ] **Step 5: Handle new actions**

Add cases:

```kotlin
is TasksAction.MoveWithinToday -> viewModelScope.launch {
    taskRepository.moveWithinToday(action.fromIndex, action.toIndex)
}

is TasksAction.MoveWithinLater -> viewModelScope.launch {
    taskRepository.moveWithinLater(action.fromIndex, action.toIndex)
}

is TasksAction.MoveToSection -> viewModelScope.launch {
    taskRepository.moveToSection(action.taskId, action.targetIsToday, action.targetIndex)
}

is TasksAction.OpenDueDateEditor -> localState.update {
    it.copy(dueDateEditorTask = action.task)
}

TasksAction.DismissDueDateEditor -> localState.update {
    it.copy(dueDateEditorTask = null)
}

is TasksAction.SetDueDate -> viewModelScope.launch {
    taskRepository.setDueDate(action.id, action.dueDate)
    localState.update { it.copy(dueDateEditorTask = null) }
}

is TasksAction.ClearDueDate -> viewModelScope.launch {
    taskRepository.clearDueDate(action.id)
    localState.update { it.copy(dueDateEditorTask = null) }
}
```

- [ ] **Step 6: Compile**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :shared:compileAndroidMain --console=plain
```

Expected: build succeeds.

- [ ] **Step 7: Commit ViewModel state**

```powershell
git add shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/viewModel/TasksState.kt shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/viewModel/TasksAction.kt shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/viewModel/TasksViewModel.kt
git commit -m "Expand Today workbench state"
```

## Task 5: Add Due-Date Labels And Strings

**Files:**
- Create: `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/DueDateLabels.kt`
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`
- Modify: `shared/src/commonMain/composeResources/values-zh-rCN/strings.xml`
- Modify: `shared/src/commonMain/composeResources/values-zh-rTW/strings.xml`

- [ ] **Step 1: Add due-date label helper**

Create `DueDateLabels.kt`:

```kotlin
package org.nsh07.pomodoro.ui.tasksScreen

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class DueDateLabel(
    val key: DueDateLabelKey,
    val argument: String? = null
)

enum class DueDateLabelKey {
    OVERDUE,
    TODAY,
    TOMORROW,
    WEEKDAY,
    DATE
}

fun dueDateLabel(dueDate: LocalDate?, today: LocalDate = LocalDate.now()): DueDateLabel? {
    if (dueDate == null) return null
    return when {
        dueDate.isBefore(today) -> DueDateLabel(DueDateLabelKey.OVERDUE)
        dueDate == today -> DueDateLabel(DueDateLabelKey.TODAY)
        dueDate == today.plusDays(1) -> DueDateLabel(DueDateLabelKey.TOMORROW)
        dueDate.isBefore(today.plusDays(7)) -> DueDateLabel(
            DueDateLabelKey.WEEKDAY,
            dueDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        )
        else -> DueDateLabel(
            DueDateLabelKey.DATE,
            dueDate.format(DateTimeFormatter.ofPattern("MMM d"))
        )
    }
}
```

- [ ] **Step 2: Add English strings**

Add to `values/strings.xml`:

```xml
<string name="current_task">Current task</string>
<string name="start_focus">Start focus</string>
<string name="today_summary_tasks">%1$d/%2$d Tasks</string>
<string name="today_summary_pomodoros">%1$d Pomodoros</string>
<string name="today_summary_focus">%1$s Focus</string>
<string name="no_current_task">No current task</string>
<string name="no_current_task_desc">Add a task or move one from Later to choose what to focus on next.</string>
<string name="today_tasks">Today</string>
<string name="set_due_date">Set due date</string>
<string name="change_due_date">Change due date</string>
<string name="clear_due_date">Clear due date</string>
<string name="due_today">Due today</string>
<string name="due_tomorrow">Due tomorrow</string>
<string name="due_weekday">Due %1$s</string>
<string name="due_date">Due %1$s</string>
<string name="overdue">Overdue</string>
<string name="drag_task">Drag task</string>
```

- [ ] **Step 3: Add simplified Chinese strings**

Add to `values-zh-rCN/strings.xml`:

```xml
<string name="current_task">当前任务</string>
<string name="start_focus">开始专注</string>
<string name="today_summary_tasks">%1$d/%2$d 任务</string>
<string name="today_summary_pomodoros">%1$d 个番茄钟</string>
<string name="today_summary_focus">%1$s 专注</string>
<string name="no_current_task">暂无当前任务</string>
<string name="no_current_task_desc">添加一个任务，或从稍后移入今天，来决定下一步专注什么。</string>
<string name="today_tasks">今日任务</string>
<string name="set_due_date">设置截止日期</string>
<string name="change_due_date">修改截止日期</string>
<string name="clear_due_date">清除截止日期</string>
<string name="due_today">今天截止</string>
<string name="due_tomorrow">明天截止</string>
<string name="due_weekday">%1$s 截止</string>
<string name="due_date">%1$s 截止</string>
<string name="overdue">已逾期</string>
<string name="drag_task">拖动任务</string>
```

- [ ] **Step 4: Add traditional Chinese strings**

Add to `values-zh-rTW/strings.xml`:

```xml
<string name="current_task">目前任務</string>
<string name="start_focus">開始專注</string>
<string name="today_summary_tasks">%1$d/%2$d 任務</string>
<string name="today_summary_pomodoros">%1$d 個番茄鐘</string>
<string name="today_summary_focus">%1$s 專注</string>
<string name="no_current_task">尚無目前任務</string>
<string name="no_current_task_desc">新增任務，或從稍後移入今天，來決定下一步專注什麼。</string>
<string name="today_tasks">今日任務</string>
<string name="set_due_date">設定截止日期</string>
<string name="change_due_date">修改截止日期</string>
<string name="clear_due_date">清除截止日期</string>
<string name="due_today">今天截止</string>
<string name="due_tomorrow">明天截止</string>
<string name="due_weekday">%1$s 截止</string>
<string name="due_date">%1$s 截止</string>
<string name="overdue">已逾期</string>
<string name="drag_task">拖曳任務</string>
```

- [ ] **Step 5: Compile resources**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :shared:compileAndroidMain --console=plain
```

Expected: build succeeds.

- [ ] **Step 6: Commit labels and strings**

```powershell
git add shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/DueDateLabels.kt shared/src/commonMain/composeResources/values/strings.xml shared/src/commonMain/composeResources/values-zh-rCN/strings.xml shared/src/commonMain/composeResources/values-zh-rTW/strings.xml
git commit -m "Add Today due date labels"
```

## Task 6: Build Today Workbench UI Without Drag

**Files:**
- Modify: `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/TasksScreen.kt`

- [ ] **Step 1: Split UI into focused composables**

In `TasksScreen.kt`, keep `TasksScreenRoot` and `TasksScreen`. Add private composables:

```kotlin
@Composable
private fun TodaySummaryRow(completedTasks: Int, totalTasks: Int, pomodoros: Int, focusTotal: Long)

@Composable
private fun CurrentTaskPanel(
    task: TaskItem?,
    focusTotal: Long,
    onStartFocus: (TaskItem) -> Unit,
    onSetDone: (TaskItem, Boolean) -> Unit,
    onMoveToLater: (TaskItem) -> Unit
)

@Composable
private fun TaskSectionHeader(title: String, count: Int)

@Composable
private fun DueDateChip(task: TaskItem)
```

Move existing row code behind these helpers before changing behavior.

- [ ] **Step 2: Add summary row**

Use `millisecondsToHoursMinutes` with localized `hours_and_minutes_format` for focus time. Display three compact surfaces using Material colors and no nested cards:

```kotlin
TodaySummaryRow(
    completedTasks = state.todayCompletedTaskCount,
    totalTasks = state.todayTotalTaskCount,
    pomodoros = state.todayCompletedPomodoroCount,
    focusTotal = state.todayFocusTotal
)
```

- [ ] **Step 3: Add current-task panel**

Place below summary and above quick add. Behavior:

- If `state.currentTask != null`, show title, due-date chip, start button, complete button, and move-to-later button.
- If null, show `no_current_task` and `no_current_task_desc`.
- `Start focus` calls existing `onStartFocus(task)`.

- [ ] **Step 4: Update task rows with due-date entry point**

Add a calendar icon/menu action to `TaskRow`:

```kotlin
onEditDueDate: () -> Unit
```

Call `onAction(TasksAction.OpenDueDateEditor(task))` from the icon/menu.

- [ ] **Step 5: Add a simple due-date editor**

Use a `BasicAlertDialog` with these actions for this iteration:

- Today
- Tomorrow
- Pick +7 days
- Clear

Do not add a full calendar picker in this task. The future calendar/planning view is explicitly out of scope for V2, and the preset dialog satisfies set/change/clear behavior without making quick capture heavier.

- [ ] **Step 6: Compile**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :shared:compileAndroidMain --console=plain
```

Expected: build succeeds.

- [ ] **Step 7: Commit non-drag workbench UI**

```powershell
git add shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/TasksScreen.kt
git commit -m "Add Today workbench UI"
```

## Task 7: Add Drag Reordering And Cross-Section Moves

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`
- Create: `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/ReorderableTaskList.kt`
- Modify: `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/TasksScreen.kt`
- Create: `shared/src/commonMain/composeResources/drawable/drag_handle.xml`

- [ ] **Step 1: Add the reorderable dependency**

Add this line to the existing `[versions]` block in `gradle/libs.versions.toml`:

```toml
reorderable = "3.1.0"
```

Add this line to the existing `[libraries]` block in `gradle/libs.versions.toml`:

```toml
reorderable = { module = "sh.calvin.reorderable:reorderable", version.ref = "reorderable" }
```

Add to `shared/build.gradle.kts` inside `commonMain.dependencies`:

```kotlin
implementation(libs.reorderable)
```

- [ ] **Step 2: Compile to verify dependency resolution**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :shared:compileAndroidMain --console=plain
```

Expected: Gradle resolves `sh.calvin.reorderable:reorderable:3.1.0` and the build succeeds.

- [ ] **Step 3: Create unified visible-row model**

Create `shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/ReorderableTaskList.kt` with these row models:

```kotlin
package org.nsh07.pomodoro.ui.tasksScreen

import androidx.compose.runtime.Immutable
import org.nsh07.pomodoro.data.TaskItem

@Immutable
sealed interface TodayVisibleRow {
    val key: String

    data object TodayHeader : TodayVisibleRow {
        override val key: String = "today-header"
    }

    data class TodayTask(val task: TaskItem) : TodayVisibleRow {
        override val key: String = "today-${task.id}"
    }

    data object LaterHeader : TodayVisibleRow {
        override val key: String = "later-header"
    }

    data class LaterTask(val task: TaskItem) : TodayVisibleRow {
        override val key: String = "later-${task.id}"
    }
}

fun buildTodayVisibleRows(
    todayTasks: List<TaskItem>,
    laterTasks: List<TaskItem>,
    showLater: Boolean
): List<TodayVisibleRow> {
    val rows = mutableListOf<TodayVisibleRow>()
    rows += TodayVisibleRow.TodayHeader
    rows += todayTasks.map { TodayVisibleRow.TodayTask(it) }
    rows += TodayVisibleRow.LaterHeader
    if (showLater) rows += laterTasks.map { TodayVisibleRow.LaterTask(it) }
    return rows
}

fun TodayVisibleRow.taskOrNull(): TaskItem? =
    when (this) {
        is TodayVisibleRow.TodayTask -> task
        is TodayVisibleRow.LaterTask -> task
        TodayVisibleRow.TodayHeader,
        TodayVisibleRow.LaterHeader -> null
    }
```

- [ ] **Step 4: Add drop mapping helper**

Add to `ReorderableTaskList.kt`:

```kotlin
data class TodayDropResult(
    val taskId: Long,
    val targetIsToday: Boolean,
    val targetIndex: Int
)

fun mapVisibleDrop(
    rows: List<TodayVisibleRow>,
    fromVisibleIndex: Int,
    toVisibleIndex: Int
): TodayDropResult? {
    val movedTask = rows.getOrNull(fromVisibleIndex)?.taskOrNull() ?: return null
    val targetIndex = toVisibleIndex.coerceIn(0, rows.size - 1)
    val targetIsToday = when (rows.getOrNull(targetIndex)) {
        TodayVisibleRow.TodayHeader -> true
        is TodayVisibleRow.TodayTask -> true
        TodayVisibleRow.LaterHeader -> false
        is TodayVisibleRow.LaterTask -> false
        null -> movedTask.isToday
    }
    val beforeTargetRows = rows.take(targetIndex)
    val sectionIndex = beforeTargetRows.count {
        when {
            targetIsToday && it is TodayVisibleRow.TodayTask -> true
            !targetIsToday && it is TodayVisibleRow.LaterTask -> true
            else -> false
        }
    }
    return TodayDropResult(
        taskId = movedTask.id,
        targetIsToday = targetIsToday,
        targetIndex = sectionIndex
    )
}
```

- [ ] **Step 5: Add local drag state in `TasksScreen`**

In `TasksScreen.kt`, add these imports:

```kotlin
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.draggableHandle
import sh.calvin.reorderable.rememberReorderableLazyListState
```

Inside `TasksScreen`, before the main `LazyColumn`, add:

```kotlin
val lazyListState = rememberLazyListState()
val sourceRows = remember(state.activeTodayTasks, state.laterTasks, state.showLater) {
    buildTodayVisibleRows(
        todayTasks = state.activeTodayTasks,
        laterTasks = state.laterTasks,
        showLater = state.showLater
    )
}
var visibleRows by remember { mutableStateOf(sourceRows) }
var pendingDrop by remember { mutableStateOf<TodayDropResult?>(null) }
LaunchedEffect(sourceRows) {
    visibleRows = sourceRows
    pendingDrop = null
}
val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
    val currentRows = visibleRows
    val drop = mapVisibleDrop(currentRows, from.index, to.index)
    if (drop != null) {
        pendingDrop = drop
        visibleRows = currentRows.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }
}
```

- [ ] **Step 6: Replace direct Today/Later `itemsIndexed` usage**

In the main `LazyColumn`, set `state = lazyListState`.

Replace the direct unfinished Today and Later task list rendering with one `itemsIndexed` block:

```kotlin
itemsIndexed(visibleRows, key = { _, row -> row.key }) { _, row ->
    when (row) {
        TodayVisibleRow.TodayHeader -> TaskSectionHeader(
            title = stringResource(Res.string.today_tasks),
            count = state.activeTodayTasks.size
        )
        TodayVisibleRow.LaterHeader -> LaterHeader(
            count = state.laterTasks.size,
            showLater = state.showLater,
            onToggle = { onAction(TasksAction.SetShowLater(!state.showLater)) }
        )
        is TodayVisibleRow.TodayTask,
        is TodayVisibleRow.LaterTask -> {
            val task = row.taskOrNull() ?: return@itemsIndexed
            ReorderableItem(reorderableLazyListState, key = row.key) { isDragging ->
                TaskRow(
                    task = task,
                    isDragging = isDragging,
                    dragScope = this,
                    onDragStopped = {
                        pendingDrop?.let { drop ->
                            onAction(
                                TasksAction.MoveToSection(
                                    taskId = drop.taskId,
                                    targetIsToday = drop.targetIsToday,
                                    targetIndex = drop.targetIndex
                                )
                            )
                        }
                        pendingDrop = null
                    },
                    onSetDone = { onAction(TasksAction.SetDone(task.id, it)) },
                    onMove = {
                        onAction(
                            if (task.isToday) {
                                TasksAction.MoveToLater(task.id)
                            } else {
                                TasksAction.MoveToToday(task.id)
                            }
                        )
                    },
                    onEditDueDate = { onAction(TasksAction.OpenDueDateEditor(task)) },
                    onDelete = { onAction(TasksAction.DeleteTask(task.id)) }
                )
            }
        }
    }
}
```

Render completed tasks after this block and do not wrap completed rows in `ReorderableItem`.

- [ ] **Step 7: Render drag handle in each unfinished task row**

Update the task row composable so unfinished rows receive:

```kotlin
dragScope: ReorderableCollectionItemScope?,
onDragStopped: () -> Unit
```

Inside trailing content, add:

```kotlin
IconButton(
    modifier = if (dragScope != null) {
        with(dragScope) {
            Modifier.draggableHandle(onDragStopped = onDragStopped)
        }
    } else {
        Modifier
    },
    onClick = {}
) {
    Icon(painterResource(Res.drawable.drag_handle), stringResource(Res.string.drag_task))
}
```

Create `shared/src/commonMain/composeResources/drawable/drag_handle.xml`:

```xml
<!--
  ~ Copyright (c) 2026 Nishant Mishra
  ~
  ~ This file is part of Tomato - a minimalist pomodoro timer for Android.
  ~
  ~ Tomato is free software: you can redistribute it and/or modify it under the terms of the GNU
  ~ General Public License as published by the Free Software Foundation, either version 3 of the
  ~ License, or (at your option) any later version.
  ~
  ~ Tomato is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
  ~ the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
  ~ Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License along with Tomato.
  ~ If not, see <https://www.gnu.org/licenses/>.
  -->

<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:tint="#000000"
    android:viewportWidth="960"
    android:viewportHeight="960">
    <path
        android:fillColor="#e3e3e3"
        android:pathData="M320,760q-33,0 -56.5,-23.5T240,680q0,-33 23.5,-56.5T320,600q33,0 56.5,23.5T400,680q0,33 -23.5,56.5T320,760ZM640,760q-33,0 -56.5,-23.5T560,680q0,-33 23.5,-56.5T640,600q33,0 56.5,23.5T720,680q0,33 -23.5,56.5T640,760ZM320,560q-33,0 -56.5,-23.5T240,480q0,-33 23.5,-56.5T320,400q33,0 56.5,23.5T400,480q0,33 -23.5,56.5T320,560ZM640,560q-33,0 -56.5,-23.5T560,480q0,-33 23.5,-56.5T640,400q33,0 56.5,23.5T720,480q0,33 -23.5,56.5T640,560ZM320,360q-33,0 -56.5,-23.5T240,280q0,-33 23.5,-56.5T320,200q33,0 56.5,23.5T400,280q0,33 -23.5,56.5T320,360ZM640,360q-33,0 -56.5,-23.5T560,280q0,-33 23.5,-56.5T640,200q33,0 56.5,23.5T720,280q0,33 -23.5,56.5T640,360Z" />
</vector>
```

- [ ] **Step 8: Compile**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :shared:compileAndroidMain --console=plain
```

Expected: build succeeds.

- [ ] **Step 9: Commit drag integration**

```powershell
git add gradle/libs.versions.toml shared/build.gradle.kts shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/ReorderableTaskList.kt shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/TasksScreen.kt shared/src/commonMain/composeResources/drawable/drag_handle.xml
git commit -m "Add Today task drag reordering"
```

## Task 8: Verify End-To-End Build And Polish

**Files:**
- Modify any files touched by compile/resource errors.
- No new feature scope.

- [ ] **Step 1: Run unit tests**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :androidApp:testFossDebugUnitTest --console=plain
```

Expected: build succeeds and unit tests pass.

- [ ] **Step 2: Run shared Android compile**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :shared:compileAndroidMain --console=plain
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run FOSS debug APK build**

Run:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :androidApp:assembleFossDebug --console=plain
```

Expected: `BUILD SUCCESSFUL` and `androidApp/build/outputs/apk/foss/debug/androidApp-foss-debug.apk` is updated.

- [ ] **Step 4: Verify Room schema**

Run:

```powershell
Select-String -Path .\shared\schemas\org.nsh07.pomodoro.data.AppDatabase\4.json -Pattern '"version": 4','"columnName": "sortOrder"','"columnName": "dueDate"'
```

Expected: all patterns are found.

- [ ] **Step 5: Check for whitespace errors**

Run:

```powershell
git diff --check
```

Expected: exit code 0. Windows line-ending warnings are acceptable if the command exits 0.

- [ ] **Step 6: Review acceptance criteria manually**

Use this checklist before final commit:

- Current-task panel exists.
- Current task uses first unfinished Today task.
- Start focus from current panel binds the task.
- Summary shows tasks, pomodoros, and focus time.
- Today order persists after app restart.
- Later order persists after app restart.
- Cross-section drag moves tasks between Today and Later.
- Completed tasks are excluded from drag sorting.
- Uncomplete returns task to section bottom.
- Due dates can be set, changed, cleared, and displayed.
- Due dates do not auto-move tasks.
- Chinese strings exist for new UI.

- [ ] **Step 7: Commit final polish**

If there are verification fixes, stage the exact files reported by `git status --short`. Example:

```powershell
git status --short
git add shared/src/commonMain/kotlin/org/nsh07/pomodoro/ui/tasksScreen/TasksScreen.kt
git commit -m "Polish Today workbench v2"
```

If there are no changes, do not create an empty commit.
