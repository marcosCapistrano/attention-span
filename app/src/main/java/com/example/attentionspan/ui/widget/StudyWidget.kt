package com.example.attentionspan.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.attentionspan.MainActivity
import com.example.attentionspan.data.local.database.AppDatabase
import com.example.attentionspan.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import java.util.*

class StudyWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        val database = AppDatabase.getDatabase(context)
        val prefs = UserPreferences(context)
        
        val periodDays = kotlinx.coroutines.runBlocking { prefs.periodDays.first() } 
        val sessions = kotlinx.coroutines.runBlocking { database.studySessionDao().getAllSessions().first() }

        val startTime = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -periodDays)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        val filteredSessions = sessions.filter { it.startTime >= startTime }
        val avgMinutes = if (filteredSessions.isEmpty()) 0.0 else {
            filteredSessions.map { it.durationMillis.toDouble() / 60000 }.average()
        }

        WidgetContent(avgMinutes, periodDays)
    }

    @Composable
    private fun WidgetContent(avgMinutes: Double, periodDays: Int) {
        GlanceTheme {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surface)
                    .padding(8.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "Avg Focus",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    )
                )
                Text(
                    text = "%.1f min".format(avgMinutes),
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.primary
                    )
                )
                Text(
                    text = "Last $periodDays days",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        }
    }
}

class StudyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StudyWidget()
}
