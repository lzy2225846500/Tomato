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

data class TodayDropResult(
    val taskId: Long,
    val targetIsToday: Boolean,
    val targetIndex: Int
)

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

fun mapVisibleDrop(
    rows: List<TodayVisibleRow>,
    fromVisibleIndex: Int,
    toVisibleIndex: Int
): TodayDropResult? {
    if (rows.isEmpty()) return null
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
