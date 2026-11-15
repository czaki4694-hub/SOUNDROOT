package com.soundroot.app

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Bg = Color(0xFF090812)
private val Panel = Color(0xFF171426)
private val Purple = Color(0xFF9B6DFF)
private val Cyan = Color(0xFF55E6E0)
private val Muted = Color(0xFFAAA4BC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SoundRootScreen(this) }
    }
}

@Composable
private fun SoundRootScreen(context: Context) {
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var maxVolume by remember { mutableIntStateOf(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) }
    var volume by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    var rootStatus by remember { mutableStateOf("جارٍ الفحص…") }
    var tinyStatus by remember { mutableStateOf("لم يتم الفحص بعد") }
    var output by remember { mutableStateOf("") }

    suspend fun checkCommand(command: String): String = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val text = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val code = process.waitFor()
            when {
                code == 0 && text.isNotBlank() -> text.trim().take(1200)
                code == 0 -> "تم تنفيذ الفحص بنجاح"
                error.isNotBlank() -> error.trim().take(500)
                else -> "تعذر تنفيذ الأمر (رمز $code)"
            }
        } catch (e: Exception) { e.message ?: "تعذر تشغيل su" }
    }

    LaunchedEffect(Unit) {
        rootStatus = try {
            val p = withContext(Dispatchers.IO) { Runtime.getRuntime().exec(arrayOf("su", "-c", "id -u")) }
            val result = withContext(Dispatchers.IO) { p.inputStream.bufferedReader().readText().trim() }
            withContext(Dispatchers.IO) { p.waitFor() }
            if (result == "0") "Root متاح" else "Root غير مؤكد"
        } catch (_: Exception) { "Root غير متاح أو لم يُمنح الإذن" }
        tinyStatus = "اضغط فحص TinyMix للتحقق من وجود الأداة"
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Bg, surface = Panel)) {
        Column(
            modifier = Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, null, tint = Cyan, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("SOUNDROOT", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text("AUDIO CONTROL CENTER", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                }
            }
            StatusCard("حالة Root", rootStatus, rootStatus == "Root متاح")
            StatusCard("TinyMix", tinyStatus, tinyStatus.contains("متاح"))
            Button(
                onClick = {
                    tinyStatus = "جارٍ فحص TinyMix…"
                    output = ""
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) { Text("فحص TinyMix") }
            LaunchedEffect(tinyStatus) {
                if (tinyStatus == "جارٍ فحص TinyMix…") {
                    val result = checkCommand("command -v tinymix && tinymix")
                    output = result
                    tinyStatus = if (result.contains("tinymix", true) || result.contains("Usage", true)) "TinyMix متاح" else "لم يتم تأكيد TinyMix: $result"
                }
            }
            if (output.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("نتيجة الفحص", color = Cyan, fontWeight = FontWeight.Bold)
                        Text(output, color = Muted, fontSize = 11.sp)
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, null, tint = Purple)
                        Spacer(Modifier.width(8.dp))
                        Text("مستوى صوت الوسائط", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("${volume.toInt()} / $maxVolume", color = Cyan, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        onValueChangeFinished = { audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume.toInt(), 0) },
                        valueRange = 0f..maxVolume.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = Cyan, activeTrackColor = Purple)
                    )
                    Text("يستخدم حد الصوت الذي يسمح به Android؛ لا يتجاوز الحد الأقصى للنظام.", color = Muted, fontSize = 11.sp)
                }
            }
            FeatureCard(Icons.Default.VolumeUp, "Speaker Boost", "تحكم آمن عبر مستوى صوت النظام", "لا يتم تغيير مكاسب مضخم السماعة تلقائياً.")
            FeatureCard(Icons.Default.Mic, "Microphone", "إعدادات الميكروفون غير مفعّلة حالياً", "تحتاج عناصر Mixer مؤكدة خاصة بمسار التسجيل.")
            FeatureCard(Icons.Default.GraphicEq, "Headphones / Earpiece", "قراءة فقط في هذه النسخة", "قيم HPHL وHPHR التي زودتنا بها عند الحد الأعلى المسجل.")
            Text("SOUNDROOT  •  v1.0.0  •  Redmi Note 9 Pro", color = Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun StatusCard(title: String, value: String, good: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Muted, fontSize = 12.sp)
                Text(value, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text(if (good) "●" else "●", color = if (good) Cyan else Color(0xFFFFC66D), fontSize = 20.sp)
        }
    }
}

@Composable
private fun FeatureCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, detail: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Purple, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Cyan, fontSize = 12.sp)
                Text(detail, color = Muted, fontSize = 11.sp)
            }
        }
    }
}
