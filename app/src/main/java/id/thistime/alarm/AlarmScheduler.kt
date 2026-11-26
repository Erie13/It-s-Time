package id.thistime.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {
    private const val MAIN = 1    // alarm terdekat dari daftar
    private const val EXTRA = 3   // tunda (snooze) & tes, terpisah supaya tidak menimpa alarm utama

    private fun trigger(c: Context, code: Int) = PendingIntent.getBroadcast(
        c, code, Intent(c, AlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun show(c: Context) = PendingIntent.getActivity(
        c, 0, Intent(c, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun set(c: Context, at: Long, code: Int) {
        c.getSystemService(AlarmManager::class.java)
            .setAlarmClock(AlarmManager.AlarmClockInfo(at, show(c)), trigger(c, code))
    }

    /** Jadwalkan alarm aktif yang paling dekat. Dipanggil lagi tiap kali ada alarm bunyi. */
    fun scheduleNext(c: Context) {
        val on = Settings(c).alarms.filter { it.on }
        if (on.isEmpty()) c.getSystemService(AlarmManager::class.java).cancel(trigger(c, MAIN))
        else set(c, on.minOf { next(it.h, it.m) }, MAIN)
    }

    fun scheduleExtra(c: Context, at: Long) = set(c, at, EXTRA)

    fun next(hour: Int, minute: Int): Long {
        val t = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (t.timeInMillis <= System.currentTimeMillis()) t.add(Calendar.DAY_OF_YEAR, 1)
        return t.timeInMillis
    }
}
