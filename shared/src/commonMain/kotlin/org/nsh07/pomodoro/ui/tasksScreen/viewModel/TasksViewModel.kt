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

package org.nsh07.pomodoro.ui.tasksScreen.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import org.nsh07.pomodoro.data.TaskRepository

class TasksViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {
    private val localState = MutableStateFlow(TasksState())

    val state: StateFlow<TasksState> = combine(
        localState,
        taskRepository.getActiveTodayTasks(),
        taskRepository.getCompletedTodayTasks(),
        taskRepository.getLaterTasks()
    ) { state, activeTodayTasks, completedTodayTasks, laterTasks ->
        state.copy(
            activeTodayTasks = activeTodayTasks,
            completedTodayTasks = completedTodayTasks,
            laterTasks = laterTasks
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasksState())

    fun onAction(action: TasksAction) {
        when (action) {
            is TasksAction.SetNewTaskTitle -> localState.update {
                it.copy(newTaskTitle = action.title)
            }

            is TasksAction.SetShowLater -> localState.update {
                it.copy(showLater = action.value)
            }

            is TasksAction.AddTask -> viewModelScope.launch {
                val title = localState.value.newTaskTitle
                val id = taskRepository.createTask(title, action.isToday)
                if (id != null) localState.update { it.copy(newTaskTitle = "") }
            }

            is TasksAction.SetDone -> viewModelScope.launch {
                taskRepository.setDone(action.id, action.isDone)
            }

            is TasksAction.MoveToToday -> viewModelScope.launch {
                taskRepository.moveToToday(action.id)
            }

            is TasksAction.MoveToLater -> viewModelScope.launch {
                taskRepository.moveToLater(action.id)
            }

            is TasksAction.DeleteTask -> viewModelScope.launch {
                taskRepository.deleteTask(action.id)
            }
        }
    }
}
