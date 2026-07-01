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

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.Flow
import org.nsh07.pomodoro.data.TaskItem
import org.nsh07.pomodoro.data.TaskRepository
import java.time.LocalDate

class WidgetUpdatingTaskRepository(
    private val delegate: TaskRepository,
    private val context: Context
) : TaskRepository {
    override suspend fun createTask(title: String, isToday: Boolean): Long? =
        delegate.createTask(title, isToday).also { if (it != null) updateTodayWidget() }

    override suspend fun updateTitle(id: Long, title: String) {
        delegate.updateTitle(id, title)
        updateTodayWidget()
    }

    override suspend fun setDone(id: Long, isDone: Boolean) {
        delegate.setDone(id, isDone)
        updateTodayWidget()
    }

    override suspend fun moveToToday(id: Long) {
        delegate.moveToToday(id)
        updateTodayWidget()
    }

    override suspend fun moveToLater(id: Long) {
        delegate.moveToLater(id)
        updateTodayWidget()
    }

    override suspend fun moveWithinToday(fromIndex: Int, toIndex: Int) {
        delegate.moveWithinToday(fromIndex, toIndex)
        updateTodayWidget()
    }

    override suspend fun moveWithinLater(fromIndex: Int, toIndex: Int) {
        delegate.moveWithinLater(fromIndex, toIndex)
        updateTodayWidget()
    }

    override suspend fun moveToSection(taskId: Long, targetIsToday: Boolean, targetIndex: Int) {
        delegate.moveToSection(taskId, targetIsToday, targetIndex)
        updateTodayWidget()
    }

    override suspend fun setDueDate(id: Long, dueDate: LocalDate?) {
        delegate.setDueDate(id, dueDate)
        updateTodayWidget()
    }

    override suspend fun clearDueDate(id: Long) {
        delegate.clearDueDate(id)
        updateTodayWidget()
    }

    override suspend fun deleteTask(id: Long) {
        delegate.deleteTask(id)
        updateTodayWidget()
    }

    override suspend fun getTask(id: Long): TaskItem? = delegate.getTask(id)

    override fun getActiveTodayTasks(): Flow<List<TaskItem>> = delegate.getActiveTodayTasks()

    override fun getCompletedTodayTasks(): Flow<List<TaskItem>> =
        delegate.getCompletedTodayTasks()

    override fun getLaterTasks(): Flow<List<TaskItem>> = delegate.getLaterTasks()

    override fun getTodayCompletedCount(): Flow<Int> = delegate.getTodayCompletedCount()

    override suspend fun deleteAllTasks() {
        delegate.deleteAllTasks()
        updateTodayWidget()
    }

    private suspend fun updateTodayWidget() {
        try {
            TodayAppWidget().updateAll(context)
        } catch (exception: Exception) {
            Log.w("TodayWidget", "Unable to update Today widget", exception)
        }
    }
}
