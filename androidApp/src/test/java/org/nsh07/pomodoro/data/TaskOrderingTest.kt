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
