package com.hakomi.practicetimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hakomi.practicetimer.ui.AppNav
import com.hakomi.practicetimer.ui.theme.HakomiTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HakomiTheme {
                AppNav()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val app = HakomiApp.from(this)
        app.timerController.refreshNow()
        app.syncTimerService()
    }
}
