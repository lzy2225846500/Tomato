/*
 * Copyright (c) 2026 Nishant Mishra
 *
 * This file is part of Tomato - a minimalist pomodoro timer for Android.
 *
 * Tomato is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tomato is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tomato.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package org.nsh07.pomodoro.ui.tasksScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.nsh07.pomodoro.data.TaskItem
import org.nsh07.pomodoro.ui.mergePaddingValues
import org.nsh07.pomodoro.ui.tasksScreen.viewModel.TasksAction
import org.nsh07.pomodoro.ui.tasksScreen.viewModel.TasksState
import org.nsh07.pomodoro.ui.tasksScreen.viewModel.TasksViewModel
import org.nsh07.pomodoro.ui.theme.CustomColors.listItemColors
import org.nsh07.pomodoro.ui.theme.CustomColors.topBarColors
import org.nsh07.pomodoro.ui.theme.LocalAppFonts
import org.nsh07.pomodoro.ui.theme.TomatoShapeDefaults.segmentedListItemShapes
import org.nsh07.pomodoro.utils.millisecondsToHoursMinutes
import tomato.shared.generated.resources.Res
import tomato.shared.generated.resources.add_task
import tomato.shared.generated.resources.cancel
import tomato.shared.generated.resources.change_due_date
import tomato.shared.generated.resources.check
import tomato.shared.generated.resources.check_circle_40dp
import tomato.shared.generated.resources.clear_due_date
import tomato.shared.generated.resources.completed_tasks
import tomato.shared.generated.resources.current_task
import tomato.shared.generated.resources.due_date
import tomato.shared.generated.resources.due_today
import tomato.shared.generated.resources.due_tomorrow
import tomato.shared.generated.resources.due_weekday
import tomato.shared.generated.resources.folder
import tomato.shared.generated.resources.hours_and_minutes_format
import tomato.shared.generated.resources.later
import tomato.shared.generated.resources.less
import tomato.shared.generated.resources.move_to_later
import tomato.shared.generated.resources.move_to_today
import tomato.shared.generated.resources.no_current_task
import tomato.shared.generated.resources.no_current_task_desc
import tomato.shared.generated.resources.no_tasks_today
import tomato.shared.generated.resources.overdue
import tomato.shared.generated.resources.play
import tomato.shared.generated.resources.set_due_date
import tomato.shared.generated.resources.start_focus
import tomato.shared.generated.resources.task_title
import tomato.shared.generated.resources.today
import tomato.shared.generated.resources.today_summary_focus
import tomato.shared.generated.resources.today_summary_pomodoros
import tomato.shared.generated.resources.today_summary_tasks
import tomato.shared.generated.resources.today_tasks
import tomato.shared.generated.resources.view_day
import java.time.LocalDate

@Composable
fun TasksScreenRoot(
    contentPadding: PaddingValues,
    onStartFocus: (TaskItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TasksScreen(
        state = state,
        contentPadding = contentPadding,
        onAction = viewModel::onAction,
        onStartFocus = onStartFocus,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TasksScreen(
    state: TasksState,
    contentPadding: PaddingValues,
    onAction: (TasksAction) -> Unit,
    onStartFocus: (TaskItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.today),
                        style = LocalTextStyle.current.copy(
                            fontFamily = LocalAppFonts.current.topBarTitle,
                            fontSize = 32.sp,
                            lineHeight = 32.sp
                        )
                    )
                },
                subtitle = {},
                colors = topBarColors,
                titleHorizontalAlignment = Alignment.CenterHorizontally,
                scrollBehavior = scrollBehavior
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        val insets = mergePaddingValues(innerPadding, contentPadding)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = insets,
            modifier = Modifier
                .background(topBarColors.containerColor)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(Modifier.height(14.dp)) }

            item {
                TodaySummaryRow(
                    completedTasks = state.todayCompletedTaskCount,
                    totalTasks = state.todayTotalTaskCount,
                    pomodoros = state.todayCompletedPomodoroCount,
                    focusTotal = state.todayFocusTotal
                )
            }

            item { Spacer(Modifier.height(10.dp)) }

            item {
                CurrentTaskPanel(
                    task = state.currentTask,
                    focusTotal = state.todayFocusTotal,
                    onStartFocus = onStartFocus,
                    onSetDone = { task, isDone -> onAction(TasksAction.SetDone(task.id, isDone)) },
                    onMoveToLater = { task -> onAction(TasksAction.MoveToLater(task.id)) }
                )
            }

            item { Spacer(Modifier.height(10.dp)) }

            item {
                AddTaskRow(
                    title = state.newTaskTitle,
                    onTitleChange = { onAction(TasksAction.SetNewTaskTitle(it)) },
                    onAdd = { onAction(TasksAction.AddTask(isToday = true)) }
                )
            }

            item { Spacer(Modifier.height(12.dp)) }

            if (state.activeTodayTasks.isEmpty() && state.completedTodayTasks.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.no_tasks_today),
                        style = typography.bodyLarge,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                }
            } else {
                item {
                    TaskSectionHeader(
                        title = stringResource(Res.string.today_tasks),
                        count = state.activeTodayTasks.size
                    )
                }
            }

            itemsIndexed(state.activeTodayTasks, key = { _, task -> task.id }) { index, task ->
                TaskRow(
                    task = task,
                    index = index,
                    count = state.activeTodayTasks.size,
                    onSetDone = { onAction(TasksAction.SetDone(task.id, it)) },
                    onStartFocus = { onStartFocus(task) },
                    onMove = { onAction(TasksAction.MoveToLater(task.id)) },
                    onEditDueDate = { onAction(TasksAction.OpenDueDateEditor(task)) },
                    moveLabel = stringResource(Res.string.move_to_later)
                )
            }

            if (state.completedTodayTasks.isNotEmpty()) {
                item { Spacer(Modifier.height(12.dp)) }
                item {
                    TaskSectionHeader(
                        title = stringResource(Res.string.completed_tasks),
                        count = state.completedTodayTasks.size
                    )
                }
                itemsIndexed(state.completedTodayTasks, key = { _, task -> task.id }) { index, task ->
                    TaskRow(
                        task = task,
                        index = index,
                        count = state.completedTodayTasks.size,
                        onSetDone = { onAction(TasksAction.SetDone(task.id, it)) },
                        onStartFocus = { onStartFocus(task) },
                        onMove = { onAction(TasksAction.MoveToLater(task.id)) },
                        onEditDueDate = { onAction(TasksAction.OpenDueDateEditor(task)) },
                        moveLabel = stringResource(Res.string.move_to_later)
                    )
                }
            }

            item { Spacer(Modifier.height(12.dp)) }

            item {
                LaterHeader(
                    showLater = state.showLater,
                    count = state.laterTasks.size,
                    onToggle = { onAction(TasksAction.SetShowLater(!state.showLater)) }
                )
            }

            if (state.showLater) {
                itemsIndexed(state.laterTasks, key = { _, task -> task.id }) { index, task ->
                    TaskRow(
                        task = task,
                        index = index,
                        count = state.laterTasks.size,
                        onSetDone = { onAction(TasksAction.SetDone(task.id, it)) },
                        onStartFocus = { onStartFocus(task) },
                        onMove = { onAction(TasksAction.MoveToToday(task.id)) },
                        onEditDueDate = { onAction(TasksAction.OpenDueDateEditor(task)) },
                        moveLabel = stringResource(Res.string.move_to_today)
                    )
                }
            }
        }

        state.dueDateEditorTask?.let { task ->
            DueDateEditorDialog(
                task = task,
                onDismiss = { onAction(TasksAction.DismissDueDateEditor) },
                onSetDueDate = { onAction(TasksAction.SetDueDate(task.id, it)) },
                onClearDueDate = { onAction(TasksAction.ClearDueDate(task.id)) }
            )
        }
    }
}

@Composable
private fun TodaySummaryRow(completedTasks: Int, totalTasks: Int, pomodoros: Int, focusTotal: Long) {
    val hoursMinutesFormat = stringResource(Res.string.hours_and_minutes_format)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        SummaryTile(
            text = stringResource(Res.string.today_summary_tasks, completedTasks, totalTasks),
            modifier = Modifier.weight(1f)
        )
        SummaryTile(
            text = stringResource(Res.string.today_summary_pomodoros, pomodoros),
            modifier = Modifier.weight(1f)
        )
        SummaryTile(
            text = stringResource(
                Res.string.today_summary_focus,
                remember(focusTotal, hoursMinutesFormat) {
                    millisecondsToHoursMinutes(focusTotal, hoursMinutesFormat)
                }
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryTile(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = colorScheme.surfaceContainerHigh,
        contentColor = colorScheme.onSurface,
        shape = shapes.large,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun CurrentTaskPanel(
    task: TaskItem?,
    focusTotal: Long,
    onStartFocus: (TaskItem) -> Unit,
    onSetDone: (TaskItem, Boolean) -> Unit,
    onMoveToLater: (TaskItem) -> Unit
) {
    val hoursMinutesFormat = stringResource(Res.string.hours_and_minutes_format)
    Surface(
        color = colorScheme.primaryContainer,
        contentColor = colorScheme.onPrimaryContainer,
        shape = shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                stringResource(Res.string.current_task),
                style = typography.labelLarge,
                color = colorScheme.onPrimaryContainer
            )
            if (task == null) {
                Text(
                    stringResource(Res.string.no_current_task),
                    style = typography.titleMedium,
                    color = colorScheme.onPrimaryContainer
                )
                Text(
                    stringResource(Res.string.no_current_task_desc),
                    style = typography.bodyMedium,
                    color = colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    task.title,
                    style = typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = colorScheme.onPrimaryContainer
                )
                DueDateChip(task)
                Text(
                    remember(focusTotal, hoursMinutesFormat) {
                        millisecondsToHoursMinutes(focusTotal, hoursMinutesFormat)
                    },
                    style = typography.bodySmall,
                    color = colorScheme.onPrimaryContainer
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onStartFocus(task) }) {
                        Icon(
                            painterResource(Res.drawable.play),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.start_focus))
                    }
                    OutlinedButton(onClick = { onSetDone(task, true) }) {
                        Icon(
                            painterResource(Res.drawable.check),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    OutlinedButton(onClick = { onMoveToLater(task) }) {
                        Icon(
                            painterResource(Res.drawable.folder),
                            contentDescription = stringResource(Res.string.move_to_later),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTaskRow(
    title: String,
    onTitleChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            singleLine = true,
            placeholder = { Text(stringResource(Res.string.task_title)) },
            modifier = Modifier.weight(1f)
        )
        FilledTonalIconButton(
            onClick = onAdd,
            enabled = title.isNotBlank()
        ) {
            Icon(painterResource(Res.drawable.check_circle_40dp), stringResource(Res.string.add_task))
        }
    }
}

@Composable
private fun TaskSectionHeader(title: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            title,
            style = typography.titleMedium,
            color = colorScheme.onSurfaceVariant
        )
        Text(
            count.toString(),
            style = typography.labelLarge,
            color = colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TaskRow(
    task: TaskItem,
    index: Int,
    count: Int,
    onSetDone: (Boolean) -> Unit,
    onStartFocus: () -> Unit,
    onMove: () -> Unit,
    onEditDueDate: () -> Unit,
    moveLabel: String
) {
    val dueDateActionLabel = if (task.dueDate == null) {
        stringResource(Res.string.set_due_date)
    } else {
        stringResource(Res.string.change_due_date)
    }
    SegmentedListItem(
        leadingContent = {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = onSetDone
            )
        },
        supportingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (task.isToday) stringResource(Res.string.today)
                    else stringResource(Res.string.later),
                    maxLines = 1
                )
                DueDateChip(task)
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = onEditDueDate) {
                    Icon(painterResource(Res.drawable.view_day), dueDateActionLabel)
                }
                IconButton(onClick = onMove) {
                    Icon(painterResource(Res.drawable.folder), moveLabel)
                }
                IconButton(onClick = onStartFocus) {
                    Icon(painterResource(Res.drawable.play), stringResource(Res.string.start_focus))
                }
            }
        },
        shapes = segmentedListItemShapes(index, count),
        colors = listItemColors,
        selected = false,
        onClick = { onSetDone(!task.isDone) }
    ) {
        Text(
            task.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (task.isDone) TextDecoration.LineThrough else null
        )
    }
}

@Composable
private fun DueDateChip(task: TaskItem) {
    val label = dueDateLabel(task.dueDate) ?: return
    val text = when (label.key) {
        DueDateLabelKey.OVERDUE -> stringResource(Res.string.overdue)
        DueDateLabelKey.TODAY -> stringResource(Res.string.due_today)
        DueDateLabelKey.TOMORROW -> stringResource(Res.string.due_tomorrow)
        DueDateLabelKey.WEEKDAY -> stringResource(Res.string.due_weekday, label.argument.orEmpty())
        DueDateLabelKey.DATE -> stringResource(Res.string.due_date, label.argument.orEmpty())
    }
    Text(
        text = text,
        style = typography.labelSmall,
        color = if (label.key == DueDateLabelKey.OVERDUE) {
            colorScheme.onErrorContainer
        } else {
            colorScheme.onSecondaryContainer
        },
        maxLines = 1,
        modifier = Modifier
            .clip(shapes.small)
            .background(
                if (label.key == DueDateLabelKey.OVERDUE) {
                    colorScheme.errorContainer
                } else {
                    colorScheme.secondaryContainer
                }
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LaterHeader(
    showLater: Boolean,
    count: Int,
    onToggle: () -> Unit
) {
    SegmentedListItem(
        leadingContent = {
            Icon(painterResource(Res.drawable.folder), null)
        },
        trailingContent = {
            TextButton(onClick = onToggle) {
                Text(if (showLater) stringResource(Res.string.less) else count.toString())
            }
        },
        shapes = segmentedListItemShapes(0, 1),
        colors = listItemColors,
        selected = showLater,
        onClick = onToggle
    ) {
        Text(stringResource(Res.string.later))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DueDateEditorDialog(
    task: TaskItem,
    onDismiss: () -> Unit,
    onSetDueDate: (LocalDate) -> Unit,
    onClearDueDate: () -> Unit
) {
    val today = remember { LocalDate.now() }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = shapes.extraLarge,
            color = colorScheme.surfaceContainerHigh,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Text(task.title, style = typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                DueDateDialogButton(
                    text = stringResource(Res.string.due_today),
                    onClick = { onSetDueDate(today) }
                )
                DueDateDialogButton(
                    text = stringResource(Res.string.due_tomorrow),
                    onClick = { onSetDueDate(today.plusDays(1)) }
                )
                DueDateDialogButton(
                    text = dueDateLabelText(today.plusDays(7)),
                    onClick = { onSetDueDate(today.plusDays(7)) }
                )
                DueDateDialogButton(
                    text = stringResource(Res.string.clear_due_date),
                    onClick = onClearDueDate
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun DueDateDialogButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}

@Composable
private fun dueDateLabelText(date: LocalDate): String {
    val label = dueDateLabel(date) ?: return ""
    return when (label.key) {
        DueDateLabelKey.OVERDUE -> stringResource(Res.string.overdue)
        DueDateLabelKey.TODAY -> stringResource(Res.string.due_today)
        DueDateLabelKey.TOMORROW -> stringResource(Res.string.due_tomorrow)
        DueDateLabelKey.WEEKDAY -> stringResource(Res.string.due_weekday, label.argument.orEmpty())
        DueDateLabelKey.DATE -> stringResource(Res.string.due_date, label.argument.orEmpty())
    }
}
