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

package org.nsh07.pomodoro.widget

import org.nsh07.pomodoro.data.TaskItem
import java.time.LocalDate

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

fun List<TaskItem>.toTodayTodoWidgetState(maxItems: Int): TodayTodoWidgetState {
    val visibleCount = maxItems.coerceAtLeast(0)
    return TodayTodoWidgetState(
        unfinishedCount = size,
        visibleTasks = take(visibleCount).mapIndexed { index, task ->
            TodayTodoWidgetTask(
                id = task.id,
                title = task.title,
                dueDate = task.dueDate,
                isCurrent = index == 0
            )
        }
    )
}
