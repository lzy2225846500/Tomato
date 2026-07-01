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

import org.nsh07.pomodoro.data.TaskItem
import java.time.LocalDate

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
