package com.hakomi.practicetimer.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.hakomi.practicetimer.HakomiApp
import com.hakomi.practicetimer.MainActivity
import com.hakomi.practicetimer.R
import com.hakomi.practicetimer.domain.Phase
import com.hakomi.practicetimer.domain.TimeFormat
import com.hakomi.practicetimer.domain.TimerEngine
import com.hakomi.practicetimer.domain.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Keeps the countdown alive while a phase is running: holds a partial wake lock so the CPU
 * keeps ticking with the screen off, and shows the remaining time as an ongoing notification
 * with pause and finish actions. Started and stopped by [HakomiApp] as the timer state changes.
 */
class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var controller: TimerController

    override fun onCreate() {
        super.onCreate()
        controller = HakomiApp.from(this).timerController
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HakomiTimer:round").also {
            it.acquire(MAX_WAKE_LOCK_MILLIS)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_PAUSE -> controller.togglePause()
            ACTION_SKIP -> controller.skip()
        }
        val state = controller.state.value
        if (state == null || !state.needsService()) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground(buildNotification(state, controller.now.value))
        observe()
        return START_NOT_STICKY
    }

    private var observing = false

    private fun observe() {
        if (observing) return
        observing = true
        combine(controller.state, controller.now) { state, now -> state to now }
            .distinctUntilChanged { old, new -> notificationKey(old.first, old.second) == notificationKey(new.first, new.second) }
            .onEach { (state, now) ->
                if (state != null && state.needsService()) {
                    notificationManager().notify(NOTIFICATION_ID, buildNotification(state, now))
                } else {
                    stopSelf()
                }
            }
            .launchIn(scope)
    }

    private fun TimerState?.needsService(): Boolean = this != null && (isRunning || isPaused)

    /** Collapses the ticking clock to whole seconds so the notification only re-renders when its text changes. */
    private fun notificationKey(state: TimerState?, now: Long): Any? =
        state?.let { Triple(it.phase, it.runState, TimerEngine.remainingSeconds(it, now)) }

    private fun startInForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun buildNotification(state: TimerState, now: Long): Notification {
        val remaining = TimeFormat.countdown(TimerEngine.remainingSeconds(state, now))
        val title = when (state.phase) {
            Phase.PRACTICE -> getString(R.string.notification_title_practice, remaining)
            Phase.FEEDBACK -> getString(R.string.notification_title_feedback, remaining)
            Phase.BREAK -> getString(R.string.notification_title_break, remaining)
            Phase.FINISHED -> getString(R.string.notification_phase_done)
        }
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val pauseLabel = if (state.isPaused) "Resume" else "Pause"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(if (state.isPaused) getString(R.string.notification_paused) else getString(R.string.app_name))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(getColor(R.color.hakomi_green))
            .addAction(0, pauseLabel, servicePendingIntent(ACTION_TOGGLE_PAUSE, 1))
            .addAction(0, "Finish", servicePendingIntent(ACTION_SKIP, 2))
            .build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, TimerService::class.java).setAction(action)
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun notificationManager() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "practice_timer"
        private const val NOTIFICATION_ID = 41
        private const val ACTION_TOGGLE_PAUSE = "com.hakomi.practicetimer.TOGGLE_PAUSE"
        private const val ACTION_SKIP = "com.hakomi.practicetimer.SKIP"
        /** Safety net: no single phase is ever longer than this. */
        private const val MAX_WAKE_LOCK_MILLIS = 4 * 60 * 60 * 1000L

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }

        fun start(context: Context) {
            context.startForegroundService(Intent(context, TimerService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TimerService::class.java))
        }
    }
}
