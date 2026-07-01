/*
 * Copyright (c) 2025-2026 Nishant Mishra
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
import android.os.Build
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.GlanceTheme.colors
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.nsh07.pomodoro.MainActivity
import org.nsh07.pomodoro.R
import org.nsh07.pomodoro.data.TaskRepository
import org.nsh07.pomodoro.ui.tasksScreen.DueDateLabelKey
import org.nsh07.pomodoro.ui.tasksScreen.dueDateLabel
import org.nsh07.pomodoro.ui.theme.lightScheme
import org.nsh07.pomodoro.widget.TomatoWidgetSize.Height1
import org.nsh07.pomodoro.widget.TomatoWidgetSize.Height2
import org.nsh07.pomodoro.widget.TomatoWidgetSize.Width4
import java.time.LocalDate

class TodayAppWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val taskRepository: TaskRepository = get()
        val tasks = taskRepository.getActiveTodayTasks().first()

        provideContent {
            key(LocalSize.current) {
                GlanceTheme {
                    Content(tasks.toTodayTodoWidgetState(maxItems = visibleTaskCount()))
                }
            }
        }
    }

    @Composable
    private fun visibleTaskCount(): Int {
        val size = LocalSize.current
        return when {
            size.height <= Height1 -> 1
            size.height < Height2 -> 2
            size.width >= Width4 -> 5
            else -> 3
        }
    }

    @Composable
    private fun Content(state: TodayTodoWidgetState) {
        val context = LocalContext.current
        Box(
            modifier = GlanceModifier
                .then(
                    if (Build.VERSION.SDK_INT >= 31) GlanceModifier.background(colors.widgetBackground)
                    else GlanceModifier.background(
                        ImageProvider(R.drawable.rounded_24dp),
                        colorFilter = ColorFilter.tint(colors.widgetBackground)
                    )
                )
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Column(GlanceModifier.fillMaxSize()) {
                Text(
                    context.getString(R.string.today_widget_title_count, state.unfinishedCount),
                    style = TextStyle(
                        color = colors.onSurface,
                        fontSize = typography.titleMedium.fontSize,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(GlanceModifier.height(8.dp))

                if (state.visibleTasks.isEmpty()) {
                    EmptyState(context)
                } else {
                    TaskList(context, state.visibleTasks)
                }
            }
        }
    }

    @Composable
    private fun EmptyState(context: Context) {
        Column(GlanceModifier.fillMaxWidth()) {
            Text(
                context.getString(R.string.today_widget_empty),
                style = TextStyle(
                    color = colors.onSurface,
                    fontSize = typography.bodyLarge.fontSize,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                context.getString(R.string.today_widget_empty_desc),
                style = TextStyle(
                    color = colors.onSurfaceVariant,
                    fontSize = typography.bodyMedium.fontSize
                )
            )
        }
    }

    @Composable
    private fun TaskList(
        context: Context,
        tasks: List<TodayTodoWidgetTask>
    ) {
        Column(GlanceModifier.fillMaxWidth()) {
            tasks.forEachIndexed { index, task ->
                if (index > 0) Spacer(GlanceModifier.height(6.dp))
                TaskRow(context, task)
            }
        }
    }

    @Composable
    private fun TaskRow(
        context: Context,
        task: TodayTodoWidgetTask
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (task.isCurrent) {
                Box(
                    modifier = GlanceModifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .cornerRadius(4.dp)
                        .background(colors.primary)
                ) {}
                Spacer(GlanceModifier.width(8.dp))
            } else {
                Spacer(GlanceModifier.width(12.dp))
            }

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    task.title,
                    style = TextStyle(
                        color = colors.onSurface,
                        fontSize = typography.bodyMedium.fontSize,
                        fontWeight = if (task.isCurrent) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }

            task.dueDate?.let { dueDate ->
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    context.widgetDueDateText(dueDate),
                    style = TextStyle(
                        color = colors.onSurfaceVariant,
                        fontSize = typography.labelSmall.fontSize
                    )
                )
            }
        }
    }

    private fun Context.widgetDueDateText(dueDate: LocalDate): String {
        val label = dueDateLabel(dueDate) ?: return ""
        return when (label.key) {
            DueDateLabelKey.OVERDUE -> getString(R.string.overdue)
            DueDateLabelKey.TODAY -> getString(R.string.due_today)
            DueDateLabelKey.TOMORROW -> getString(R.string.due_tomorrow)
            DueDateLabelKey.WEEKDAY -> getString(R.string.due_weekday, label.argument)
            DueDateLabelKey.DATE -> getString(R.string.due_date, label.argument)
        }
    }

    @OptIn(ExperimentalGlancePreviewApi::class)
    @Preview(widthDp = 400, heightDp = 216)
    @Composable
    private fun ContentPreview() {
        GlanceTheme(colors = ColorProviders(lightScheme)) {
            Box(GlanceModifier.background(Color.White)) {
                Box(GlanceModifier.cornerRadius(32.dp)) {
                    Content(
                        TodayTodoWidgetState(
                            unfinishedCount = 4,
                            visibleTasks = listOf(
                                TodayTodoWidgetTask(
                                    id = 1,
                                    title = "Review Today page",
                                    dueDate = LocalDate.now(),
                                    isCurrent = true
                                ),
                                TodayTodoWidgetTask(
                                    id = 2,
                                    title = "Plan widget refresh",
                                    dueDate = LocalDate.now().plusDays(1),
                                    isCurrent = false
                                ),
                                TodayTodoWidgetTask(
                                    id = 3,
                                    title = "Build debug APK",
                                    dueDate = null,
                                    isCurrent = false
                                )
                            )
                        )
                    )
                }
            }
        }
    }
}
