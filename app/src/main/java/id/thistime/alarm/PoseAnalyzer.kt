package id.thistime.alarm

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlin.math.abs
import kotlin.math.atan2

/** Deteksi kerangka tubuh (ML Kit Pose) lalu hitung push up dari sudut siku. */
class PoseAnalyzer(
    private val onState: (String) -> Unit,
    private val onRep: () -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build()
    )
    private var armed = false
    private var okFrames = 0
    private var down = false
    private var smooth = -1.0
    private var lastRep = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(p: ImageProxy) {
        val img = p.image
        if (img == null) { p.close(); return }
        detector.process(InputImage.fromMediaImage(img, p.imageInfo.rotationDegrees))
            .addOnSuccessListener { handle(it) }
            .addOnCompleteListener { p.close() }
    }

    private fun angle(a: PoseLandmark, b: PoseLandmark, c: PoseLandmark): Double {
        val r = atan2(c.position.y - b.position.y, c.position.x - b.position.x) -
            atan2(a.position.y - b.position.y, a.position.x - b.position.x)
        var d = abs(Math.toDegrees(r.toDouble()))
        if (d > 180) d = 360 - d
        return d
    }

    private fun handle(pose: Pose) {
        val sides = listOf(
            intArrayOf(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST),
            intArrayOf(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)
        )
        var best: List<PoseLandmark>? = null
        var bestScore = 0f
        for (s in sides) {
            val l = s.map { pose.getPoseLandmark(it) }
            if (l.any { it == null }) continue
            val q = l.minOf { it!!.inFrameLikelihood }
            if (q > bestScore) { bestScore = q; best = l.map { it!! } }
        }
        val b = best
        if (b == null || bestScore < 0.5f) {
            okFrames = maxOf(0, okFrames - 1)
            onState("Lengan belum terlihat (skor ${(bestScore * 100).toInt()}%). Mundurkan HP sampai bahu, siku, dan tangan terlihat dari samping.")
            return
        }
        val a = angle(b[0], b[1], b[2])
        if (!armed) {
            okFrames++
            if (okFrames >= 8) { armed = true; onState("Terlacak! Mulai push up.") }
            else onState("Terdeteksi (siku ${a.toInt()}°). Tahan sebentar…")
            return
        }
        smooth = if (smooth < 0) a else smooth * 0.6 + a * 0.4
        onState("Siku ${smooth.toInt()}° | skor ${(bestScore * 100).toInt()}%")
        if (smooth < 115 && !down) down = true
        else if (smooth > 145 && down) {
            down = false
            val now = System.currentTimeMillis()
            if (now - lastRep > 600) { lastRep = now; onRep() }
        }
    }
}
