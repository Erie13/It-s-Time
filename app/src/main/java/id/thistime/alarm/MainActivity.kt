package id.thistime.alarm

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent { AppTheme { Home() } }
    }
}

private val SOUNDS = listOf("alarm" to "Alarm HP", "ringtone" to "Dering HP", "notif" to "Notifikasi HP")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home() {
    val c = LocalContext.current
    val st = remember { Settings(c) }
    val ts = rememberTimePickerState(5, 0, true)
    val alarms = remember { mutableStateListOf<Alarm>().apply { addAll(st.alarms) } }
    var mode by remember { mutableStateOf(st.mode) }
    var reps by remember { mutableIntStateOf(st.reps) }
    var snd by remember { mutableStateOf(st.sound) }
    var file by remember { mutableStateOf(st.customName) }
    var showPicker by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(20_000); now = System.currentTimeMillis() } }

    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u: Uri? ->
        if (u != null) {
            c.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            st.customUri = u.toString()
            file = u.lastPathSegment ?: "audio"
            st.customName = file
            snd = "custom"
        }
    }
    val askPerm = { perm.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS)) }

    fun save() { st.mode = mode; st.reps = reps; st.sound = snd }
    fun commit() {
        save()
        val sorted = alarms.sortedWith(compareBy({ it.h }, { it.m }))
        alarms.clear(); alarms.addAll(sorted)
        st.alarms = sorted
        st.enabled = sorted.any { it.on }
        AlarmScheduler.scheduleNext(c)
    }

    val nextAt = alarms.filter { it.on }.minOfOrNull { AlarmScheduler.next(it.h, it.m) }
    val mins = if (nextAt != null) ((nextAt - now) / 60000).toInt().coerceAtLeast(0) else 0

    Column(Modifier.fillMaxSize().background(Night).statusBarsPadding()) {
        // Jam berikutnya langsung di atas latar malam, tanpa kartu.
        Column(Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 24.dp)) {
            if (nextAt != null) {
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nextAt)),
                    color = Sun, fontSize = 96.sp, fontWeight = FontWeight.Light, letterSpacing = (-3).sp
                )
                Text("Berbunyi dalam ${mins / 60} jam ${mins % 60} menit", color = Ice, fontSize = 17.sp)
            } else {
                Text("Tidak ada alarm", color = Ice, fontSize = 40.sp, fontWeight = FontWeight.Light)
                Text("Tambah satu untuk mulai.", color = Dim, fontSize = 17.sp)
            }
        }

        // Satu lembar untuk semua pengaturan.
        Column(
            Modifier.weight(1f).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).background(Raised)
                .verticalScroll(rememberScrollState()).navigationBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Alarm", color = Ice, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            if (alarms.isEmpty()) Text("Belum ada alarm.", color = Dim)
            alarms.forEachIndexed { idx, a ->
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(
                        "%02d:%02d".format(a.h, a.m), color = Ice, fontSize = 34.sp, fontWeight = FontWeight.Light,
                        modifier = Modifier.alpha(if (a.on) 1f else 0.4f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            a.on, { alarms[idx] = a.copy(on = it); commit() },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Sun, checkedThumbColor = Night,
                                uncheckedTrackColor = RaisedHi, uncheckedThumbColor = Dim,
                                uncheckedBorderColor = Dim.copy(alpha = 0.5f)
                            )
                        )
                        TextButton({ alarms.removeAt(idx); commit() }) { Text("Hapus", color = Dim) }
                    }
                }
                if (idx < alarms.lastIndex) HorizontalDivider(color = RaisedHi)
            }
            Button(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Sun, contentColor = OnSun)
            ) { Text("Tambah alarm", fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(20.dp))
            Text("Verifikasi pagi", color = Ice, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Dikerjakan setelah geser layar.", color = Dim, fontSize = 13.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp)) {
                MODES.forEach { (k, n) ->
                    FilterChip(mode == k, { mode = k }, { Text(n) }, Modifier.padding(end = 8.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Push up", color = Ice, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Dihitung otomatis lewat kamera depan.", color = Dim, fontSize = 13.sp)
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton({ reps = (reps - 1).coerceAtLeast(1) }) { Text("−", color = Ice) }
                Text("$reps", color = Ice, fontSize = 34.sp, fontWeight = FontWeight.Light)
                OutlinedButton({ reps = (reps + 1).coerceAtMost(100) }) { Text("+", color = Ice) }
                Text("kali", color = Dim)
            }

            Spacer(Modifier.height(20.dp))
            Text("Suara alarm", color = Ice, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp)) {
                SOUNDS.forEach { (k, n) ->
                    FilterChip(snd == k, { snd = k }, { Text(n) }, Modifier.padding(end = 8.dp))
                }
                FilterChip(snd == "custom", { pick.launch(arrayOf("audio/*")) },
                    { Text(file?.let { "Suara saya: $it" } ?: "+ Suara sendiri") })
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { commit() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ice, contentColor = Night)
            ) { Text("Simpan pengaturan", fontWeight = FontWeight.SemiBold) }
            OutlinedButton(
                onClick = { save(); askPerm(); AlarmScheduler.scheduleExtra(c, System.currentTimeMillis() + 15000) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
            ) { Text("Tes alarm (15 detik)", color = Ice) }
        }
    }

    if (showPicker) AlertDialog(
        onDismissRequest = { showPicker = false },
        title = { Text("Pilih jam alarm") },
        text = { TimePicker(ts) },
        confirmButton = {
            TextButton({
                if (alarms.none { it.h == ts.hour && it.m == ts.minute }) alarms.add(Alarm(ts.hour, ts.minute, true))
                showPicker = false; askPerm(); commit()
            }) { Text("Tambah", color = Sun) }
        },
        dismissButton = { TextButton({ showPicker = false }) { Text("Batal", color = Dim) } }
    )
}
