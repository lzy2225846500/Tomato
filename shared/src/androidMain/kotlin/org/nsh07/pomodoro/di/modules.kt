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

package org.nsh07.pomodoro.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel
import org.nsh07.pomodoro.data.AndroidBackupRestoreManager
import org.nsh07.pomodoro.data.AppDatabase
import org.nsh07.pomodoro.data.BackupRestoreManager
import org.nsh07.pomodoro.ui.settingsScreen.screens.backupRestore.viewModel.BackupRestoreViewModel
import org.nsh07.pomodoro.ui.settingsScreen.viewModel.SettingsViewModel
import org.nsh07.pomodoro.ui.statsScreen.viewModel.StatsViewModel
import org.nsh07.pomodoro.ui.tasksScreen.viewModel.TasksViewModel
import org.nsh07.pomodoro.ui.timerScreen.viewModel.TimerViewModel

val dbModule = module {
    single<AppDatabase> { create(::createDatabase) }
    single { get<AppDatabase>().preferenceDao() }
    single { get<AppDatabase>().statDao() }
    single { get<AppDatabase>().taskDao() }
    single { get<AppDatabase>().focusSessionDao() }
    single { get<AppDatabase>().systemDao() }
}

val viewModels = module {
    viewModel<BackupRestoreViewModel>()
    viewModel<TimerViewModel>()
    viewModel<TasksViewModel>()
    viewModel<SettingsViewModel>()
    viewModel<StatsViewModel>()
}

val androidModule = module {
    single<AndroidBackupRestoreManager>() bind BackupRestoreManager::class
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE task_item ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE task_item ADD COLUMN dueDate TEXT")
        connection.execSQL(
            """
            UPDATE task_item
            SET sortOrder = (
                SELECT COUNT(*)
                FROM task_item AS earlier
                WHERE earlier.isDone = task_item.isDone
                    AND earlier.isToday = task_item.isToday
                    AND (
                        earlier.createdAt < task_item.createdAt
                        OR (earlier.createdAt = task_item.createdAt AND earlier.id <= task_item.id)
                    )
            ) - 1
            """.trimIndent()
        )
    }
}

private fun createDatabase(context: Context): AppDatabase {
    return Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "app_database"
    ).addMigrations(MIGRATION_3_4).build()
}
