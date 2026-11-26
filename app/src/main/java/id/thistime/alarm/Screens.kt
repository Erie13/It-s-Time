package id.thistime.alarm

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/** Layar alarm: langit makin terang tiap langkah selesai, sampai subuh penuh. */
@Composable
fun AlarmFlow(onSilence: () -> Unit, onSnooze: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val st = remember { Settings(ctx) }
    var stage by remember { mutableIntStateOf(0) }
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(1000) } }
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val p by animateFloatAsState(stage / 2f, tween(1200), label = "fajar")
    val a = 0.75f - 0.35f * p

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(colorStops = arrayOf(0f to Night, a to Night, (a + 0.2f) to Dusk, 1f to Dawn))
        )
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .statusBarsPadding().navigationBarsPadding().imePadding().padding(28.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Text(fmt.format(now), color = Ice, fontSize = 88.sp, fontWeight = FontWeight.Light, letterSpacing = (-3).sp)
            Text("Langkah ${stage + 1} dari 3", color = Dim, fontSize = 16.sp)
            Spacer(Modifier.height(36.dp))
            when (stage) {
                0 -> {
                    Text("Waktunya bangun.", color = Ice, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(48.dp))
                    CallSlider(onRight = { onSilence(); stage = 1 }, onLeft = onSnooze)
                }
                1 -> Verify(st.mode) { stage = 2 }
                else -> PushUp(st.reps, onDone)
            }
        }
    }
}

@Composable
fun CallSlider(onRight: () -> Unit, onLeft: () -> Unit) {
    var off by remember { mutableFloatStateOf(0f) }
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(38.dp)).background(Color.White.copy(alpha = 0.12f))
    ) {
        val limit = with(LocalDensity.current) { (maxWidth / 2 - 38.dp).toPx() }
        Row(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("← Tunda 5 menit", color = Ice.copy(alpha = 0.75f), fontSize = 14.sp)
            Text("Diamkan →", color = Ice.copy(alpha = 0.75f), fontSize = 14.sp)
        }
        Box(
            Modifier.align(Alignment.Center)
                .offset { IntOffset(off.roundToInt(), 0) }
                .size(64.dp).clip(CircleShape)
                .background(if (off < 0) Color(0xFFFFA94D) else Sun)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val o = off; off = 0f
                            if (o > limit * 0.85f) onRight() else if (o < -limit * 0.85f) onLeft()
                        },
                        onDragCancel = { off = 0f }
                    ) { _, d -> off = (off + d).coerceIn(-limit, limit) }
                },
            contentAlignment = Alignment.Center
        ) { Text("📞", fontSize = 26.sp) }
    }
}

@Composable
fun Verify(mode: String, onOk: () -> Unit) {
    val isText = mode == "text"
    var item by remember { mutableStateOf(if (isText) Quiz.phrases.random() to "" else Quiz.pick(mode)) }
    var input by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    Text(if (isText) "Ketik kalimat ini" else "Jawab pertanyaan ini", color = Ice, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))
    Text(item.first, color = Ice, fontSize = 19.sp, lineHeight = 28.sp)
    Spacer(Modifier.height(20.dp))
    OutlinedTextField(
        value = input,
        onValueChange = { v -> if (v.length - input.length <= 3) input = v },  // tolak paste
        label = { Text(if (isText) "Ketik persis seperti di atas" else "Ketik jawabanmu") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Ice, unfocusedTextColor = Ice, cursorColor = Sun,
            focusedBorderColor = Sun, unfocusedBorderColor = Ice.copy(alpha = 0.45f),
            focusedLabelColor = Sun, unfocusedLabelColor = Ice.copy(alpha = 0.7f)
        )
    )
    Text(err, color = Color(0xFFFFB4AB), modifier = Modifier.padding(top = 8.dp))
    Button(
        onClick = {
            val ok = if (isText) Quiz.okText(input, item.first) else Quiz.okAnswer(input, item.second)
            when {
                ok -> onOk()
                isText -> err = "Belum sama. Cek lagi hurufnya."
                else -> { err = "Jawaban salah. Ini soal baru."; item = Quiz.pick(mode); input = "" }
            }
        },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Sun, contentColor = OnSun)
    ) { Text("Kirim", fontWeight = FontWeight.SemiBold) }
}

@Suppress("DEPRECATION")
@Composable
fun PushUp(need: Int, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val owner = ctx as ComponentActivity
    var n by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf("Menyiapkan kamera…") }
    var showManual by remember { mutableStateOf(false) }
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }
    LaunchedEffect(Unit) { delay(60_000); showManual = true }
    val rep = { n++; if (n >= need) onDone() }
    val orient = LocalConfiguration.current.orientation

    Text("Push up", color = Ice, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    if (granted) {
        Text(msg, color = Ice.copy(alpha = 0.85f), fontSize = 14.sp)
        val exec = remember { Executors.newSingleThreadExecutor() }
        val analyzer = remember { PoseAnalyzer(onState = { msg = it }, onRep = { rep() }) }
        key(orient) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(260.dp).padding(top = 12.dp).clip(RoundedCornerShape(20.dp)),
                factory = { c ->
                    PreviewView(c).also { pv ->
                        val rot = owner.windowManager.defaultDisplay.rotation
                        val fut = ProcessCameraProvider.getInstance(c)
                        fut.addListener({
                            val prov = fut.get()
                            val prev = Preview.Builder().setTargetRotation(rot).build()
                                .also { it.setSurfaceProvider(pv.surfaceProvider) }
                            val an = ImageAnalysis.Builder().setTargetRotation(rot)
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                                .also { it.setAnalyzer(exec, analyzer) }
                            prov.unbindAll()
                            prov.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, prev, an)
                        }, ContextCompat.getMainExecutor(c))
                    }
                }
            )
        }
    } else {
        Text("Izinkan kamera supaya push up bisa dihitung.", color = Ice.copy(alpha = 0.85f), fontSize = 14.sp)
        Button(
            onClick = { launcher.launch(Manifest.permission.CAMERA) },
            modifier = Modifier.padding(top = 12.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Sun, contentColor = OnSun)
        ) { Text("Izinkan kamera") }
    }
    Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.Bottom) {
        Text("$n", color = Ice, fontSize = 96.sp, fontWeight = FontWeight.Light, letterSpacing = (-3).sp)
        Text("  dari $need", color = Ice.copy(alpha = 0.8f), fontSize = 18.sp, modifier = Modifier.padding(bottom = 22.dp))
    }
    if (showManual || !granted) {
        OutlinedButton(onClick = { rep() }, shape = RoundedCornerShape(16.dp)) {
            Text("Kamera tidak mendeteksi? Hitung manual", color = Ice)
        }
    }
}
