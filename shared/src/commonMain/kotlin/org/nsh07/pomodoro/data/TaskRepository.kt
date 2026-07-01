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

interface TaskRepository {
    suspend fun createTask(title: String, isToday: Boolean = true): Long?
    suspend fun updateTitle(id: Long, title: String)
    suspend fun setDone(id: Long, isDone: Boolean)
    suspend fun moveToToday(id: Long)
    suspend fun moveToLater(id: Long)
    suspend fun deleteTask(id: Long)
    suspend fun getTask(id: Long): TaskItem?
    fun getActiveTodayTasks(): Flow<List<TaskItem>>
    fun getCompletedTodayTasks(): Flow<List<TaskItem>>
    fun getLaterTasks(): Flow<List<TaskItem>>
    fun getTodayCompletedCount(): Flow<Int>
    suspend fun deleteAllTasks()
}

class AppTaskRepository(
    private val taskDao: TaskDao,
    private val ioDispatcher: CoroutineDispatcher
) : TaskRepository {
    override suspend fun createTask(title: String, isToday: Boolean): Long? =
        withContext(ioDispatcher) {
            val trimmed = title.trim()
            if (trimmed.isEmpty()) return@withContext null
            taskDao.insertTask(
                TaskItem(
                    title = trimmed,
                    isToday = isToday,
                    createdAt = Instant.now()
                )
            )
        }

    override suspend fun updateTitle(id: Long, title: String) = withContext(ioDispatcher) {
        val trimmed = title.trim()
        if (trimmed.isNotEmpty()) taskDao.updateTitle(id, trimmed)
    }

    override suspend fun setDone(id: Long, isDone: Boolean) = withContext(ioDispatcher) {
        taskDao.setDone(id, isDone, if (isDone) Instant.now() else null)
    }

    override suspend fun moveToToday(id: Long) = withContext(ioDispatcher) {
        taskDao.setToday(id, true)
    }

    override suspend fun moveToLater(id: Long) = withContext(ioDispatcher) {
        taskDao.setToday(id, false)
    }

    override suspend fun deleteTask(id: Long) = withContext(ioDispatcher) {
        taskDao.deleteTask(id)
    }

    override suspend fun getTask(id: Long): TaskItem? = withContext(ioDispatcher) {
        taskDao.getTask(id)
    }

    override fun getActiveTodayTasks(): Flow<List<TaskItem>> = taskDao.getActiveTodayTasks()

    override fun getCompletedTodayTasks(): Flow<List<TaskItem>> = taskDao.getCompletedTodayTasks()

    override fun getLaterTasks(): Flow<List<TaskItem>> = taskDao.getLaterTasks()

    override fun getTodayCompletedCount(): Flow<Int> {
        val today = LocalDate.now()
        return taskDao.getCompletedCount(today.startInstant(), today.plusDays(1).startInstant())
    }

    override suspend fun deleteAllTasks() = withContext(ioDispatcher) {
        taskDao.clearAll()
    }
}
