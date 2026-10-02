package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.SmartNoteEntity
import kotlin.math.abs

/**
 * Real-time notification & scheduled reminder manager for Smart Notes, Checklists, and Financial Alerts.
 * Supports:
 * 1. Instant real-time heads-up notifications
 * 2. Live Pinned Status-Bar Ongoing Notifications (with live checklist progress bar!)
 * 3. Scheduled AlarmManager reminders via ReminderAlarmReceiver
 */
object NoteNotificationHelper {

    const val CHANNEL_REALTIME_ALERTS = "hisab_realtime_alerts"
    const val CHANNEL_LIVE_PINNED_NOTES = "hisab_live_pinned_notes"

    const val EXTRA_NOTE_ID = "extra_note_id"
    const val EXTRA_NOTE_TITLE = "extra_note_title"
    const val EXTRA_NOTE_BODY = "extra_note_body"
    const val EXTRA_NOTE_BUDGET = "extra_note_budget"

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val alertChannel = NotificationChannel(
                CHANNEL_REALTIME_ALERTS,
                "Hisab Real-Time Reminders & Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Instant notifications for task reminders, due alerts, and financial notes"
                enableVibration(true)
            }

            val pinnedChannel = NotificationChannel(
                CHANNEL_LIVE_PINNED_NOTES,
                "Pinned Live Notes & Checklists",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Ongoing status-bar notes and live shopping/todo checklists"
            }

            nm.createNotificationChannel(alertChannel)
            nm.createNotificationChannel(pinnedChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun buildLaunchAppPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Fires an immediate real-time heads-up notification for a Smart Note or Task.
     */
    fun postRealtimeNoteNotification(
        context: Context,
        note: SmartNoteEntity,
        currencySymbol: String = "৳",
        customHeader: String? = null
    ): Boolean {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return false

        val items = note.checklistItems()
        val checkedCount = items.count { it.isChecked }
        val totalPaisa = note.effectiveTotalPaisa()

        val title = buildString {
            if (!customHeader.isNullOrBlank()) {
                append(customHeader).append(": ")
            }
            append(note.title.ifBlank { "Hisab Smart Note" })
        }

        val summaryLine = buildString {
            if (items.isNotEmpty()) {
                append("Checklist: $checkedCount/${items.size} completed")
            }
            if (totalPaisa > 0L) {
                if (isNotEmpty()) append(" • ")
                append("Total: ${MoneyUtils.formatPaisa(totalPaisa, currencySymbol)}")
            }
            if (note.content.isNotBlank()) {
                if (isNotEmpty()) append(" — ")
                append(note.content)
            }
        }.ifBlank { "Tap to view your note and checklist in Hisab" }

        val bigText = buildString {
            if (note.content.isNotBlank()) {
                appendLine(note.content)
            }
            if (items.isNotEmpty()) {
                appendLine("Tasks ($checkedCount/${items.size}):")
                for (item in items.take(6)) {
                    val mark = if (item.isChecked) "☑" else "☐"
                    val amtStr = if (item.amountPaisa > 0L) {
                        " (${MoneyUtils.formatPaisa(item.amountPaisa, currencySymbol)})"
                    } else ""
                    appendLine("$mark ${item.text}$amtStr")
                }
            }
            if (totalPaisa > 0L) {
                append("Target / Total: ${MoneyUtils.formatPaisa(totalPaisa, currencySymbol)}")
            }
        }.trim().ifEmpty { summaryLine }

        val notifId = abs(note.id.hashCode()) + 1000
        val builder = NotificationCompat.Builder(context, CHANNEL_REALTIME_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(summaryLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(buildLaunchAppPendingIntent(context, notifId))

        if (items.isNotEmpty()) {
            builder.setProgress(items.size, checkedCount, false)
        }

        return try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Synchronizes a live ongoing status-bar notification when a note has `isPinnedToNotification = true`.
     * Whenever the user checks/unchecks a todo item in the app, the status bar notification updates its progress bar in real time!
     */
    fun syncOngoingPinnedNotification(
        context: Context,
        note: SmartNoteEntity,
        currencySymbol: String = "৳"
    ) {
        ensureChannelsCreated(context)
        val notifId = abs(note.id.hashCode()) + 50000
        val nm = NotificationManagerCompat.from(context)

        if (!note.isPinnedToNotification || note.isArchived) {
            nm.cancel(notifId)
            return
        }

        if (!hasNotificationPermission(context)) return

        val items = note.checklistItems()
        val checkedCount = items.count { it.isChecked }
        val totalPaisa = note.effectiveTotalPaisa()

        val subtitle = buildString {
            if (items.isNotEmpty()) {
                append("$checkedCount/${items.size} done")
            }
            if (totalPaisa > 0L) {
                if (isNotEmpty()) append(" • ")
                append(MoneyUtils.formatPaisa(totalPaisa, currencySymbol))
            }
            if (note.content.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(note.content)
            }
        }.ifBlank { "Pinned live note from Hisab" }

        val bigBody = buildString {
            if (note.content.isNotBlank()) appendLine(note.content)
            for (item in items.take(8)) {
                val box = if (item.isChecked) "✓" else "○"
                val amt = if (item.amountPaisa > 0L) " — ${MoneyUtils.formatPaisa(item.amountPaisa, currencySymbol)}" else ""
                appendLine("$box ${item.text}$amt")
            }
        }.trim().ifEmpty { subtitle }

        val builder = NotificationCompat.Builder(context, CHANNEL_LIVE_PINNED_NOTES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📌 ${note.title}")
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigBody))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(buildLaunchAppPendingIntent(context, notifId))

        if (items.isNotEmpty()) {
            builder.setProgress(items.size, checkedCount, false)
        }

        try {
            nm.notify(notifId, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun cancelNoteNotifications(context: Context, noteId: String) {
        val nm = NotificationManagerCompat.from(context)
        nm.cancel(abs(noteId.hashCode()) + 1000)
        nm.cancel(abs(noteId.hashCode()) + 50000)
    }

    /**
     * Schedules a real-time AlarmManager broadcast for `note.reminderMillis`.
     */
    fun scheduleNoteReminder(
        context: Context,
        note: SmartNoteEntity,
        currencySymbol: String = "৳"
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reqCode = abs(note.id.hashCode()) + 90000

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_NOTE_ID, note.id)
            putExtra(EXTRA_NOTE_TITLE, note.title)
            putExtra(EXTRA_NOTE_BODY, note.content.ifBlank { "Scheduled reminder from Hisab Notes" })
            putExtra(
                EXTRA_NOTE_BUDGET,
                if (note.effectiveTotalPaisa() > 0L) {
                    MoneyUtils.formatPaisa(note.effectiveTotalPaisa(), currencySymbol)
                } else ""
            )
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerMillis = note.reminderMillis
        if (triggerMillis == null || triggerMillis <= System.currentTimeMillis()) {
            alarmManager.cancel(pendingIntent)
            return
        }

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } catch (_: Exception) {
        }
    }
}

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NoteNotificationHelper.ensureChannelsCreated(context)
        if (!NoteNotificationHelper.hasNotificationPermission(context)) return

        val noteId = intent.getStringExtra(NoteNotificationHelper.EXTRA_NOTE_ID) ?: "reminder"
        val title = intent.getStringExtra(NoteNotificationHelper.EXTRA_NOTE_TITLE) ?: "Hisab Reminder"
        val body = intent.getStringExtra(NoteNotificationHelper.EXTRA_NOTE_BODY) ?: "You have a scheduled note/task due now."
        val budget = intent.getStringExtra(NoteNotificationHelper.EXTRA_NOTE_BUDGET).orEmpty()

        val displayBody = if (budget.isNotBlank()) "$body • Amount: $budget" else body
        val notifId = abs(noteId.hashCode()) + 20000

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            notifId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NoteNotificationHelper.CHANNEL_REALTIME_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ Reminder: $title")
            .setContentText(displayBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {
        }
    }
}
