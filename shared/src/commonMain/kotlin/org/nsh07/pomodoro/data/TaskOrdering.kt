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
