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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insertSession(session: FocusSession): Long

    @Query(
        "SELECT COALESCE(SUM(durationMs), 0) FROM focus_session " +
                "WHERE startedAt >= :startInclusive AND startedAt < :endExclusive"
    )
    fun getFocusTotal(startInclusive: Instant, endExclusive: Instant): Flow<Long>

    @Query(
        "SELECT COUNT(*) FROM focus_session " +
                "WHERE completed = 1 AND startedAt >= :startInclusive AND startedAt < :endExclusive"
    )
    fun getCompletedPomodoroCount(startInclusive: Instant, endExclusive: Instant): Flow<Int>

    @Query(
        "SELECT COALESCE(SUM(durationMs), 0) FROM focus_session " +
                "WHERE taskId IS NULL AND startedAt >= :startInclusive AND startedAt < :endExclusive"
    )
    fun getUnassignedFocusTotal(
        startInclusive: Instant,
        endExclusive: Instant
    ): Flow<Long>

    @Query(
        "SELECT taskId, taskTitle AS title, COALESCE(SUM(durationMs), 0) AS totalFocusMs, " +
                "COALESCE(SUM(CASE WHEN completed = 1 THEN 1 ELSE 0 END), 0) AS completedPomodoros " +
                "FROM focus_session " +
                "WHERE startedAt >= :startInclusive AND startedAt < :endExclusive " +
                "GROUP BY taskId, taskTitle " +
                "ORDER BY totalFocusMs DESC " +
                "LIMIT :limit"
    )
    fun getTaskFocusRanking(
        startInclusive: Instant,
        endExclusive: Instant,
        limit: Int
    ): Flow<List<TaskFocusSummary>>

    @Query("DELETE FROM focus_session")
    suspend fun clearAll()
}
