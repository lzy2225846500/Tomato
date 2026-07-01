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

package org.nsh07.pomodoro.ui.tasksScreen

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class DueDateLabel(
    val key: DueDateLabelKey,
    val argument: String? = null
)

enum class DueDateLabelKey {
    OVERDUE,
    TODAY,
    TOMORROW,
    WEEKDAY,
    DATE
}

fun dueDateLabel(dueDate: LocalDate?, today: LocalDate = LocalDate.now()): DueDateLabel? {
    if (dueDate == null) return null
    return when {
        dueDate.isBefore(today) -> DueDateLabel(DueDateLabelKey.OVERDUE)
        dueDate == today -> DueDateLabel(DueDateLabelKey.TODAY)
        dueDate == today.plusDays(1) -> DueDateLabel(DueDateLabelKey.TOMORROW)
        dueDate.isBefore(today.plusDays(7)) -> DueDateLabel(
            DueDateLabelKey.WEEKDAY,
            dueDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        )
        else -> DueDateLabel(
            DueDateLabelKey.DATE,
            dueDate.format(DateTimeFormatter.ofPattern("MMM d"))
        )
    }
}
