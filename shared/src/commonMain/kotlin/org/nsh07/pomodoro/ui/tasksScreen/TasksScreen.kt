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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import tomato.shared.generated.resources.Res
import tomato.shared.generated.resources.add_task
import tomato.shared.generated.resources.completed_tasks
import tomato.shared.generated.resources.check_circle_40dp
import tomato.shared.generated.resources.focus
import tomato.shared.generated.resources.folder
import tomato.shared.generated.resources.later
import tomato.shared.generated.resources.less
import tomato.shared.generated.resources.move_to_later
import tomato.shared.generated.resources.move_to_today
import tomato.shared.generated.resources.no_tasks_today
import tomato.shared.generated.resources.play
import tomato.shared.generated.resources.task_title
import tomato.shared.generated.resources.today

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
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets()
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
            }

            itemsIndexed(state.activeTodayTasks, key = { _, task -> task.id }) { index, task ->
                TaskRow(
                    task = task,
                    index = index,
                    count = state.activeTodayTasks.size,
                    onSetDone = { onAction(TasksAction.SetDone(task.id, it)) },
                    onStartFocus = { onStartFocus(task) },
                    onMove = { onAction(TasksAction.MoveToLater(task.id)) },
                    moveLabel = stringResource(Res.string.move_to_later)
                )
            }

            if (state.completedTodayTasks.isNotEmpty()) {
                item { Spacer(Modifier.height(12.dp)) }
                item {
                    Text(
                        stringResource(Res.string.completed_tasks),
                        style = typography.titleMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
                        moveLabel = stringResource(Res.string.move_to_today)
                    )
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TaskRow(
    task: TaskItem,
    index: Int,
    count: Int,
    onSetDone: (Boolean) -> Unit,
    onStartFocus: () -> Unit,
    onMove: () -> Unit,
    moveLabel: String
) {
    SegmentedListItem(
        leadingContent = {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = onSetDone
            )
        },
        supportingContent = {
            Text(
                if (task.isToday) stringResource(Res.string.today)
                else stringResource(Res.string.later),
                maxLines = 1
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = onMove) {
                    Icon(painterResource(Res.drawable.folder), moveLabel)
                }
                IconButton(onClick = onStartFocus) {
                    Icon(painterResource(Res.drawable.play), stringResource(Res.string.focus))
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
