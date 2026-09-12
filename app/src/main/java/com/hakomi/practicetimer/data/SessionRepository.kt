package com.hakomi.practicetimer.data

import android.content.Context
import android.content.SharedPreferences
import com.hakomi.practicetimer.domain.PracticeSession
import com.hakomi.practicetimer.domain.SessionPlan
import com.hakomi.practicetimer.domain.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists the active session and the last used plan so the app can be killed mid-session
 * and come back exactly where it was. Backed by SharedPreferences; the payloads are tiny.
 */
class SessionRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _session = MutableStateFlow(read<PracticeSession>(KEY_SESSION))
    val session: StateFlow<PracticeSession?> = _session

    /** The plan used last time, offered as the starting point on the planning screen. */
    fun lastPlan(): SessionPlan? = read(KEY_LAST_PLAN)

    fun startSession(plan: SessionPlan, startMillis: Long) {
        write(KEY_LAST_PLAN, plan)
        setSession(PracticeSession(plan = plan, startMillis = startMillis))
    }

    fun updateSession(transform: (PracticeSession) -> PracticeSession) {
        _session.update { current -> current?.let(transform) }
        write(KEY_SESSION, _session.value)
    }

    fun clearSession() = setSession(null)

    private fun setSession(session: PracticeSession?) {
        _session.value = session
        write(KEY_SESSION, session)
    }

    fun readTimer(): TimerState? = read(KEY_TIMER)

    fun writeTimer(state: TimerState?) = write(KEY_TIMER, state)

    private inline fun <reified T> read(key: String): T? {
        val raw = prefs.getString(key, null) ?: return null
        return try {
            json.decodeFromString<T>(raw)
        } catch (e: SerializationException) {
            prefs.edit().remove(key).apply()
            null
        } catch (e: IllegalArgumentException) {
            prefs.edit().remove(key).apply()
            null
        }
    }

    private inline fun <reified T> write(key: String, value: T?) {
        val editor = prefs.edit()
        if (value == null) editor.remove(key) else editor.putString(key, json.encodeToString<T>(value))
        editor.apply()
    }

    private companion object {
        const val PREFS_NAME = "hakomi_practice_timer"
        const val KEY_SESSION = "session"
        const val KEY_LAST_PLAN = "last_plan"
        const val KEY_TIMER = "timer"
    }
}
