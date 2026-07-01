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
    suspend fun moveWithinToday(fromIndex: Int, toIndex: Int)
    suspend fun moveWithinLater(fromIndex: Int, toIndex: Int)
    suspend fun moveToSection(taskId: Long, targetIsToday: Boolean, targetIndex: Int)
    suspend fun setDueDate(id: Long, dueDate: LocalDate?)
    suspend fun clearDueDate(id: Long)
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
            val nextOrder = taskDao.getMaxSortOrder(isToday) + 1
            taskDao.insertTask(
                TaskItem(
                    title = trimmed,
                    isToday = isToday,
                    sortOrder = nextOrder,
                    createdAt = Instant.now()
                )
            )
        }

    override suspend fun updateTitle(id: Long, title: String) = withContext(ioDispatcher) {
        val trimmed = title.trim()
        if (trimmed.isNotEmpty()) taskDao.updateTitle(id, trimmed)
    }

    override suspend fun setDone(id: Long, isDone: Boolean) = withContext(ioDispatcher) {
        val task = taskDao.getTask(id) ?: return@withContext
        val sortOrder = if (isDone) task.sortOrder else taskDao.getMaxSortOrder(task.isToday) + 1
        taskDao.setDone(id, isDone, if (isDone) Instant.now() else null, sortOrder)
    }

    override suspend fun moveToToday(id: Long) = withContext(ioDispatcher) {
        taskDao.setSectionAndOrder(id, true, taskDao.getMaxSortOrder(true) + 1)
    }

    override suspend fun moveToLater(id: Long) = withContext(ioDispatcher) {
        taskDao.setSectionAndOrder(id, false, taskDao.getMaxSortOrder(false) + 1)
    }

    override suspend fun moveWithinToday(fromIndex: Int, toIndex: Int) = withContext(ioDispatcher) {
        val tasks = taskDao.getActiveTodayTasksSnapshot()
        tasks.map { TaskOrderUpdate(it.id, true, it.sortOrder) }
            .moveWithinSection(fromIndex, toIndex)
            .forEach { taskDao.setSectionAndOrder(it.id, it.isToday, it.sortOrder) }
    }

    override suspend fun moveWithinLater(fromIndex: Int, toIndex: Int) = withContext(ioDispatcher) {
        val tasks = taskDao.getLaterTasksSnapshot()
        tasks.map { TaskOrderUpdate(it.id, false, it.sortOrder) }
            .moveWithinSection(fromIndex, toIndex)
            .forEach { taskDao.setSectionAndOrder(it.id, it.isToday, it.sortOrder) }
    }

    override suspend fun moveToSection(taskId: Long, targetIsToday: Boolean, targetIndex: Int) =
        withContext(ioDispatcher) {
            val sourceIsToday = taskDao.getTask(taskId)?.isToday ?: return@withContext
            if (sourceIsToday == targetIsToday) {
                val source = if (sourceIsToday) {
                    taskDao.getActiveTodayTasksSnapshot()
                } else {
                    taskDao.getLaterTasksSnapshot()
                }
                val fromIndex = source.indexOfFirst { it.id == taskId }
                if (fromIndex < 0) return@withContext
                source.map { TaskOrderUpdate(it.id, sourceIsToday, it.sortOrder) }
                    .moveWithinSection(fromIndex, targetIndex)
                    .forEach { taskDao.setSectionAndOrder(it.id, it.isToday, it.sortOrder) }
                return@withContext
            }
            val source = if (sourceIsToday) {
                taskDao.getActiveTodayTasksSnapshot()
            } else {
                taskDao.getLaterTasksSnapshot()
            }
            val target = if (targetIsToday) {
                taskDao.getActiveTodayTasksSnapshot()
            } else {
                taskDao.getLaterTasksSnapshot()
            }
            val result = moveAcrossSections(
                source = source.map { TaskOrderUpdate(it.id, sourceIsToday, it.sortOrder) },
                target = target.map { TaskOrderUpdate(it.id, targetIsToday, it.sortOrder) },
                movedTaskId = taskId,
                targetIndex = targetIndex,
                targetIsToday = targetIsToday
            )
            (result.source + result.target).forEach {
                taskDao.setSectionAndOrder(it.id, it.isToday, it.sortOrder)
            }
        }

    override suspend fun setDueDate(id: Long, dueDate: LocalDate?) = withContext(ioDispatcher) {
        taskDao.setDueDate(id, dueDate)
    }

    override suspend fun clearDueDate(id: Long) = withContext(ioDispatcher) {
        taskDao.setDueDate(id, null)
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
