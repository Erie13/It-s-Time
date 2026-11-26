package id.thistime.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        AlarmScheduler.scheduleNext(c)   // langsung jadwalkan alarm berikutnya (05:30, 06:00, atau besok)
        ContextCompat.startForegroundService(
            c, Intent(c, AlarmService::class.java).setAction(AlarmService.ACTION_RING)
        )
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        AlarmScheduler.scheduleNext(c)
    }
}
