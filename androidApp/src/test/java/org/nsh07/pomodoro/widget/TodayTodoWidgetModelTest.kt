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

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.nsh07.pomodoro.data.TaskItem
import java.time.Instant
import java.time.LocalDate

class TodayTodoWidgetModelTest {
    @Test
    fun `count includes all tasks while visible tasks are capped`() {
        val tasks = listOf(
            task(id = 1, title = "One"),
            task(id = 2, title = "Two"),
            task(id = 3, title = "Three")
        )

        val state = tasks.toTodayTodoWidgetState(maxItems = 2)

        assertEquals(3, state.unfinishedCount)
        assertEquals(listOf(1L, 2L), state.visibleTasks.map { it.id })
    }

    @Test
    fun `first visible task is current`() {
        val state = listOf(
            task(id = 1, title = "One"),
            task(id = 2, title = "Two")
        ).toTodayTodoWidgetState(maxItems = 2)

        assertTrue(state.visibleTasks[0].isCurrent)
        assertFalse(state.visibleTasks[1].isCurrent)
    }

    @Test
    fun `empty list creates empty widget state`() {
        val state = emptyList<TaskItem>().toTodayTodoWidgetState(maxItems = 4)

        assertEquals(0, state.unfinishedCount)
        assertEquals(emptyList<TodayTodoWidgetTask>(), state.visibleTasks)
    }

    @Test
    fun `negative visible count shows no rows`() {
        val state = listOf(task(id = 1, title = "One")).toTodayTodoWidgetState(maxItems = -1)

        assertEquals(1, state.unfinishedCount)
        assertEquals(emptyList<TodayTodoWidgetTask>(), state.visibleTasks)
    }

    private fun task(
        id: Long,
        title: String,
        dueDate: LocalDate? = null
    ): TaskItem = TaskItem(
        id = id,
        title = title,
        dueDate = dueDate,
        createdAt = Instant.EPOCH
    )
}
