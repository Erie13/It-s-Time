package id.thistime.alarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class AlarmService : Service() {
    companion object {
        const val ACTION_RING = "ring"
        const val ACTION_SILENCE = "silence"
        const val ACTION_STOP = "stop"
        private const val CH = "alarm"
        fun send(c: Context, action: String) =
            c.startService(Intent(c, AlarmService::class.java).setAction(action))
    }

    private var mp: MediaPlayer? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        when (i?.action) {
            ACTION_STOP -> { stopSound(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
            ACTION_SILENCE -> stopSound()
            else -> { goForeground(); startSound() }
        }
        return START_NOT_STICKY
    }

    private fun goForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH, "Alarm", NotificationManager.IMPORTANCE_HIGH).apply { setSound(null, null) }
        )
        val pi = PendingIntent.getActivity(
            this, 2,
            Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(this, CH)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ Waktunya bangun!")
            .setContentText("Ketuk untuk mematikan alarm")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .build()
        ServiceCompat.startForeground(
            this, 1, n,
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        )
    }

    private fun startSound() {
        stopSound()
        val s = Settings(this)
        val am = getSystemService(AudioManager::class.java)
        am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        val def = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val uri: Uri = when (s.sound) {
            "custom" -> s.customUri?.let(Uri::parse) ?: def
            "ringtone" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            "notif" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            else -> def
        }
        fun play(u: Uri): Boolean = try {
            mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                )
                setDataSource(this@AlarmService, u)
                isLooping = true
                prepare(); start()
            }
            true
        } catch (e: Exception) { mp?.release(); mp = null; false }
        if (!play(uri)) play(def)
        getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0))
    }

    private fun stopSound() {
        try { mp?.stop() } catch (_: Exception) {}
        mp?.release(); mp = null
        getSystemService(Vibrator::class.java)?.cancel()
    }

    override fun onDestroy() { stopSound(); super.onDestroy() }
}
