package id.thistime.alarm

import android.content.Context
import java.text.Normalizer

data class Alarm(val h: Int, val m: Int, val on: Boolean)

class Settings(c: Context) {
    private val sp = c.getSharedPreferences("alarm", Context.MODE_PRIVATE)
    var hour: Int get() = sp.getInt("h", 5); set(v) { sp.edit().putInt("h", v).apply() }
    var minute: Int get() = sp.getInt("m", 0); set(v) { sp.edit().putInt("m", v).apply() }
    var mode: String get() = sp.getString("mode", "text")!!; set(v) { sp.edit().putString("mode", v).apply() }
    var reps: Int get() = sp.getInt("reps", 10); set(v) { sp.edit().putInt("reps", v).apply() }
    var sound: String get() = sp.getString("sound", "alarm")!!; set(v) { sp.edit().putString("sound", v).apply() }
    var customUri: String? get() = sp.getString("curi", null); set(v) { sp.edit().putString("curi", v).apply() }
    var customName: String? get() = sp.getString("cname", null); set(v) { sp.edit().putString("cname", v).apply() }
    var alarms: List<Alarm>
        get() = sp.getString("alarms", "")!!.split(";").filter { it.isNotBlank() }.map {
            val p = it.split(":"); Alarm(p[0].toInt(), p[1].toInt(), p[2] == "1")
        }
        set(v) { sp.edit().putString("alarms", v.joinToString(";") { "${it.h}:${it.m}:${if (it.on) 1 else 0}" }).apply() }
    var enabled: Boolean get() = sp.getBoolean("on", false); set(v) { sp.edit().putBoolean("on", v).apply() }
}

val MODES = listOf(
    "text" to "Ketik teks", "math" to "Matematika", "hist" to "Sejarah",
    "tokoh" to "Tokoh Indonesia", "mito" to "Mitologi", "kimia" to "Tabel periodik"
)

object Quiz {
    val phrases = listOf(
        "Bangun woy, udah pagi! Jangan jadi kebo, dunia nggak nungguin kamu molor.",
        "Heh, buka mata! Kasur itu bukan masa depanmu, ayo bangun dan kerjain targetmu.",
        "Bangun, males! Matahari udah naik dan kamu masih ngorok, malu sama ayam.",
        "Ayo bangun sekarang juga. Satu menit lagi rebahan, seribu penyesalan nanti."
    )
    private val bank: Map<String, List<Pair<String, String>>> = mapOf(
        "hist" to listOf(
            "Tahun Proklamasi Kemerdekaan Indonesia?" to "1945",
            "Kerajaan maritim besar di Sumatra Selatan?" to "Sriwijaya",
            "Sumpah Pemuda diikrarkan tahun?" to "1928",
            "Candi Borobudur dibangun oleh dinasti?" to "Syailendra",
            "Mahapatih Majapahit dengan Sumpah Palapa?" to "Gajah Mada"
        ),
        "tokoh" to listOf(
            "Presiden pertama Indonesia?" to "Soekarno",
            "Penulis Tetralogi Buru?" to "Pramoedya Ananta Toer",
            "Pelopor emansipasi perempuan dari Jepara?" to "R.A. Kartini",
            "Pencipta lagu Indonesia Raya?" to "W.R. Supratman",
            "Bapak Pendidikan Nasional?" to "Ki Hajar Dewantara"
        ),
        "mito" to listOf(
            "Raja para dewa Yunani?" to "Zeus",
            "Nama palu Thor dalam mitologi Nordik?" to "Mjolnir",
            "Dewi padi dan kesuburan di Jawa?" to "Dewi Sri",
            "Dewa Mesir berkepala anjing/serigala?" to "Anubis",
            "Dalam Ramayana, siapa yang menculik Sinta?" to "Rahwana"
        ),
        "kimia" to listOf(
            "Nama unsur dengan simbol Fe?" to "Besi",
            "Nama unsur dengan simbol Na?" to "Natrium",
            "Nomor atom oksigen?" to "8",
            "Nama unsur dengan simbol Au?" to "Emas",
            "Nama unsur dengan simbol K?" to "Kalium"
        )
    )

    fun pick(mode: String): Pair<String, String> =
        if (mode == "math") {
            val a = (12..49).random(); val b = (3..9).random(); val c = (10..90).random()
            "$a × $b + $c = ?" to (a * b + c).toString()
        } else bank.getValue(mode).random()

    private fun norm(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "").replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ").trim()

    private val alias = mapOf(
        "sukarno" to "soekarno", "pramudya" to "pramoedya", "srivijaya" to "sriwijaya",
        "sailendra" to "syailendra", "mjollnir" to "mjolnir"
    )

    fun okText(typed: String, phrase: String) = norm(typed) == norm(phrase)

    fun okAnswer(typed: String, answer: String): Boolean {
        val key = norm(answer).split(" ").maxByOrNull { it.length }!!
        return norm(typed).split(" ").map { alias[it] ?: it }.contains(key)
    }
}
