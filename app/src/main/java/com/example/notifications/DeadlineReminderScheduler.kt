package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.DeadlineEntity

class DeadlineReminderScheduler(private val context: Context) {

    fun schedule(deadline: DeadlineEntity) {
        if (!deadline.reminderEnabled || deadline.isCompleted) {
            cancel(deadline.id)
            return
        }

        val triggerAtMillis = deadline.dueEpochMillis - (deadline.reminderMinutesBefore * 60_000L)
        if (triggerAtMillis <= System.currentTimeMillis()) {
            cancel(deadline.id)
            return
        }

        DeadlineReminderReceiver.ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(deadline)

        // Uses standard inexact-while-idle delivery so no restricted SCHEDULE_EXACT_ALARM permission is required
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancel(deadlineId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DeadlineReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            deadlineId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun buildPendingIntent(deadline: DeadlineEntity): PendingIntent {
        val intent = Intent(context, DeadlineReminderReceiver::class.java).apply {
            putExtra(DeadlineReminderReceiver.EXTRA_DEADLINE_ID, deadline.id)
            putExtra(DeadlineReminderReceiver.EXTRA_TITLE, deadline.title)
            putExtra(DeadlineReminderReceiver.EXTRA_SUBJECT, deadline.subjectName)
            putExtra(DeadlineReminderReceiver.EXTRA_TYPE, deadline.type)
        }
        return PendingIntent.getBroadcast(
            context,
            deadline.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
