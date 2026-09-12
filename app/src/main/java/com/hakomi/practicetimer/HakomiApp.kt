package com.hakomi.practicetimer

import android.app.Application
import android.content.Context
import com.hakomi.practicetimer.data.SessionRepository
import com.hakomi.practicetimer.domain.TimerState
import com.hakomi.practicetimer.timer.GongPlayer
import com.hakomi.practicetimer.timer.TimerController
import com.hakomi.practicetimer.timer.TimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class HakomiApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var repository: SessionRepository
        private set
    lateinit var gongPlayer: GongPlayer
        private set
    lateinit var timerController: TimerController
        private set

    override fun onCreate() {
        super.onCreate()
        TimerService.ensureChannel(this)
        repository = SessionRepository(this)
        gongPlayer = GongPlayer(this).also { it.warmUp() }
        timerController = TimerController(
            repository = repository,
            scope = appScope,
            onPhaseCompleted = { gongPlayer.play() },
        )

        timerController.state
            .map { it.needsService() }
            .distinctUntilChanged()
            .onEach { syncTimerService() }
            .launchIn(appScope)
    }

    /**
     * The service (wake lock + notification) should exist exactly while a phase is running or paused.
     * Called on every state change and again when the activity comes to the foreground, because
     * Android refuses foreground-service starts from the background after a process restart.
     */
    fun syncTimerService() {
        if (timerController.state.value.needsService()) {
            try {
                TimerService.start(this)
            } catch (e: IllegalStateException) {
                // Not allowed right now; the next foreground sync will retry.
            } catch (e: SecurityException) {
                // Missing permission on an unusual device configuration; the in-app timer still works.
            }
        } else {
            TimerService.stop(this)
        }
    }

    private fun TimerState?.needsService(): Boolean = this != null && (isRunning || isPaused)

    companion object {
        fun from(context: Context): HakomiApp = context.applicationContext as HakomiApp
    }
}
