package com.hakomi.practicetimer.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * Synthesises the same soft gong the web app produced with WebAudio oscillators:
 * a 200 Hz fundamental with inharmonic partials, each fading exponentially over three seconds.
 * Generating it means no audio asset has to ship with the app.
 */
class GongPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val pcm: ShortArray by lazy { synthesize() }

    fun warmUp() {
        Thread { pcm }.start()
    }

    fun play() {
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HakomiTimer:gong")
        wakeLock.acquire(DURATION_MILLIS + 2_000L)
        Thread {
            try {
                vibrate()
                playTrack()
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
            }
        }.start()
    }

    private fun playTrack() {
        val samples = pcm
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        try {
            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep(DURATION_MILLIS + 200L)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            track.release()
        }
    }

    private fun vibrate() {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() != true) return
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 120, 180), -1))
    }

    private fun synthesize(): ShortArray {
        val total = SAMPLE_RATE * DURATION_SECONDS.toInt()
        val buffer = FloatArray(total)
        PARTIALS.forEachIndexed { index, ratio ->
            val frequency = BASE_FREQUENCY * ratio
            val startGain = 0.15f / (index + 1)
            val decayRatio = 0.001f / startGain
            for (i in 0 until total) {
                val t = i / SAMPLE_RATE.toFloat()
                val gain = startGain * decayRatio.pow(t / DURATION_SECONDS)
                buffer[i] += gain * sin(2.0 * PI * frequency * t).toFloat()
            }
        }
        // Short fade-in avoids a click on the first sample.
        val attack = (SAMPLE_RATE * 0.005f).toInt()
        for (i in 0 until attack) buffer[i] *= i / attack.toFloat()

        val peak = buffer.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1e-4f)
        val scale = 0.85f / peak * Short.MAX_VALUE
        return ShortArray(total) { (buffer[it] * scale).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort() }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val DURATION_SECONDS = 3f
        const val DURATION_MILLIS = 3_000L
        const val BASE_FREQUENCY = 200.0
        val PARTIALS = doubleArrayOf(1.0, 2.76, 5.4, 8.54, 13.3)
    }
}
