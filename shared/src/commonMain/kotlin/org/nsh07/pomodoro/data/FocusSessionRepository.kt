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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate

interface FocusSessionRepository {
    suspend fun recordFocus(
        taskId: Long?,
        taskTitle: String?,
        durationMs: Long,
        completed: Boolean,
        startedAt: Instant = Instant.now()
    )

    fun getTodayFocusTotal(): Flow<Long>
    fun getTodayCompletedPomodoroCount(): Flow<Int>
    fun getTodayUnassignedFocusTotal(): Flow<Long>
    fun getTaskFocusRanking(days: Long = 7, limit: Int = 5): Flow<List<TaskFocusSummary>>
    suspend fun deleteAllSessions()
}

class AppFocusSessionRepository(
    private val focusSessionDao: FocusSessionDao,
    private val ioDispatcher: CoroutineDispatcher
) : FocusSessionRepository {
    override suspend fun recordFocus(
        taskId: Long?,
        taskTitle: String?,
        durationMs: Long,
        completed: Boolean,
        startedAt: Instant
    ) = withContext(ioDispatcher) {
        if (durationMs <= 0) return@withContext
        focusSessionDao.insertSession(
            FocusSession(
                taskId = taskId,
                taskTitle = taskTitle,
                startedAt = startedAt,
                durationMs = durationMs,
                completed = completed
            )
        )
    }

    override fun getTodayFocusTotal(): Flow<Long> {
        val today = LocalDate.now()
        return focusSessionDao.getFocusTotal(today.startInstant(), today.plusDays(1).startInstant())
    }

    override fun getTodayCompletedPomodoroCount(): Flow<Int> {
        val today = LocalDate.now()
        return focusSessionDao.getCompletedPomodoroCount(
            today.startInstant(),
            today.plusDays(1).startInstant()
        )
    }

    override fun getTodayUnassignedFocusTotal(): Flow<Long> {
        val today = LocalDate.now()
        return focusSessionDao.getUnassignedFocusTotal(
            today.startInstant(),
            today.plusDays(1).startInstant()
        )
    }

    override fun getTaskFocusRanking(days: Long, limit: Int): Flow<List<TaskFocusSummary>> {
        val end = Instant.now()
        val start = LocalDate.now().minusDays(days - 1).startInstant()
        return focusSessionDao.getTaskFocusRanking(start, end, limit)
    }

    override suspend fun deleteAllSessions() = withContext(ioDispatcher) {
        focusSessionDao.clearAll()
    }
}
