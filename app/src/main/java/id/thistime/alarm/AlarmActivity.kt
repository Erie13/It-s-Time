package id.thistime.alarm

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

class AlarmActivity : ComponentActivity() {
    private var closed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            AppTheme {
                BackHandler(true) {}   // tombol back diblokir
                AlarmFlow(
                    onSilence = { AlarmService.send(this, AlarmService.ACTION_SILENCE) },
                    onSnooze = {
                        AlarmService.send(this, AlarmService.ACTION_STOP)
                        AlarmScheduler.scheduleExtra(this, System.currentTimeMillis() + 5 * 60_000L)
                        closed = true; finish()
                    },
                    onDone = {
                        AlarmService.send(this, AlarmService.ACTION_STOP)
                        closed = true; finish()
                    }
                )
            }
        }
    }

    // Kabur dari layar alarm sebelum selesai -> alarm bunyi lagi.
    override fun onStop() {
        super.onStop()
        if (!closed && !isFinishing) AlarmService.send(this, AlarmService.ACTION_RING)
    }
}
