package com.ateszk0.ostromgep

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ateszk0.ostromgep.model.ExerciseSessionData
import kotlinx.coroutines.flow.MutableSharedFlow

object WorkoutEventBus {
    val events = MutableSharedFlow<WorkoutAction>(extraBufferCapacity = 10)
}

enum class WorkoutAction { SKIP_REST, ADD_15S, SUB_15S }

class WorkoutActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "ACTION_SKIP_REST" -> WorkoutEventBus.events.tryEmit(WorkoutAction.SKIP_REST)
            "ACTION_ADD_15S" -> WorkoutEventBus.events.tryEmit(WorkoutAction.ADD_15S)
            "ACTION_SUB_15S" -> WorkoutEventBus.events.tryEmit(WorkoutAction.SUB_15S)
        }
    }
}

class WorkoutService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        
        NotificationHelper.createChannel(this)
        startForeground(NotificationHelper.NOTIFICATION_ID, NotificationHelper.buildInitialNotification(this))
        return START_STICKY
    }
}

object NotificationHelper {
    const val CHANNEL_ID = "workout_channel"
    const val NOTIFICATION_ID = 1

    private var lastRestTime = 0
    private var currentMaxRest = 0

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Workout", NotificationManager.IMPORTANCE_LOW)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun getContentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_OPEN_WORKOUT"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, WorkoutActionReceiver::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(context, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun buildInitialNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(context.getString(R.string.notif_workout_active))
            .setContentText(context.getString(R.string.notif_tap_to_open))
            .setContentIntent(getContentIntent(context))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updateNotification(context: Context, targetExercise: ExerciseSessionData?, restTimerSeconds: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val title = targetExercise?.name ?: context.getString(R.string.notif_workout_active)
        
        val completedSets = targetExercise?.sets?.count { it.isCompleted } ?: 0
        val totalSets = targetExercise?.sets?.size ?: 0
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(title)
            .setContentIntent(getContentIntent(context))
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (restTimerSeconds > 0) {
            if (lastRestTime == 0) {
                currentMaxRest = maxOf(targetExercise?.restTimerDuration ?: 90, restTimerSeconds)
            } else if (restTimerSeconds > lastRestTime) {
                currentMaxRest += (restTimerSeconds - lastRestTime)
            }
            lastRestTime = restTimerSeconds

            val totalRest = currentMaxRest
            val minutes = restTimerSeconds / 60
            val seconds = restTimerSeconds % 60
            val timeString = String.format("%d:%02d", minutes, seconds)
            
            builder.setContentText(context.getString(R.string.notif_rest, timeString))
            builder.setProgress(totalRest, restTimerSeconds, false)
            
            builder.addAction(android.R.drawable.ic_media_next, context.getString(R.string.notif_skip), getPendingIntent(context, "ACTION_SKIP_REST"))
            builder.addAction(android.R.drawable.ic_media_rew, context.getString(R.string.notif_sub15), getPendingIntent(context, "ACTION_SUB_15S"))
            builder.addAction(android.R.drawable.ic_media_ff, context.getString(R.string.notif_add15), getPendingIntent(context, "ACTION_ADD_15S"))
        } else {
            lastRestTime = 0
            currentMaxRest = 0

            builder.setContentText(context.getString(R.string.notif_next_set, completedSets + 1, totalSets))
            builder.setProgress(0, 0, false)
        }

        manager.notify(NOTIFICATION_ID, builder.build())
    }
}
