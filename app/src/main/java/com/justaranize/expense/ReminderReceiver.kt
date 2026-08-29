package com.justaranize.expense

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val message =
            intent.getStringExtra(
                "message"
            ) ?: "Expense reminder"

        val id =
            intent.getIntExtra(
                "id",
                0
            )

        val openApp =
            Intent(
                context,
                MainActivity::class.java
            )

        val pending =
            PendingIntent.getActivity(
                context,
                id,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val notification =
            NotificationCompat.Builder(
                context,
                "expense_reminder"
            )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )
                .setContentTitle(
                    "Expense Reminder"
                )
                .setContentText(
                    message
                )
                .setAutoCancel(true)
                .setContentIntent(
                    pending
                )
                .build()

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.notify(
            id,
            notification
        )
    }
}
