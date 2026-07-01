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
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface TaskDao {
    @Insert(onConflict = REPLACE)
    suspend fun insertTask(task: TaskItem): Long

    @Query("UPDATE task_item SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)

    @Query("UPDATE task_item SET isDone = :isDone, completedAt = :completedAt WHERE id = :id")
    suspend fun setDone(id: Long, isDone: Boolean, completedAt: Instant?)

    @Query("UPDATE task_item SET isDone = :isDone, completedAt = :completedAt, sortOrder = :sortOrder WHERE id = :id")
    suspend fun setDone(id: Long, isDone: Boolean, completedAt: Instant?, sortOrder: Long)

    @Query("UPDATE task_item SET isToday = :isToday WHERE id = :id")
    suspend fun setToday(id: Long, isToday: Boolean)

    @Query("UPDATE task_item SET isToday = :isToday, sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSectionAndOrder(id: Long, isToday: Boolean, sortOrder: Long)

    @Query("UPDATE task_item SET dueDate = :dueDate WHERE id = :id")
    suspend fun setDueDate(id: Long, dueDate: LocalDate?)

    @Query("DELETE FROM task_item WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("SELECT * FROM task_item WHERE id = :id")
    suspend fun getTask(id: Long): TaskItem?

    @Query(
        "SELECT * FROM task_item " +
                "WHERE isToday = 1 AND isDone = 0 " +
                "ORDER BY sortOrder ASC, id ASC"
    )
    fun getActiveTodayTasks(): Flow<List<TaskItem>>

    @Query(
        "SELECT * FROM task_item " +
                "WHERE isToday = 1 AND isDone = 1 " +
                "ORDER BY completedAt DESC, id DESC"
    )
    fun getCompletedTodayTasks(): Flow<List<TaskItem>>

    @Query(
        "SELECT * FROM task_item " +
                "WHERE isToday = 0 AND isDone = 0 " +
                "ORDER BY sortOrder ASC, id ASC"
    )
    fun getLaterTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM task_item WHERE isToday = 1 AND isDone = 0 ORDER BY sortOrder ASC, id ASC")
    suspend fun getActiveTodayTasksSnapshot(): List<TaskItem>

    @Query("SELECT * FROM task_item WHERE isToday = 0 AND isDone = 0 ORDER BY sortOrder ASC, id ASC")
    suspend fun getLaterTasksSnapshot(): List<TaskItem>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM task_item WHERE isToday = :isToday AND isDone = 0")
    suspend fun getMaxSortOrder(isToday: Boolean): Long

    @Query(
        "SELECT COUNT(*) FROM task_item " +
                "WHERE isDone = 1 AND completedAt >= :startInclusive AND completedAt < :endExclusive"
    )
    fun getCompletedCount(startInclusive: Instant, endExclusive: Instant): Flow<Int>

    @Query("DELETE FROM task_item")
    suspend fun clearAll()
}
