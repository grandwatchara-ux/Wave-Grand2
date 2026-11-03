package com.waveecuracing.app

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF080B0A)
private val Panel = Color(0xFF141A17)
private val Green = Color(0xFF21D07A)
private val Muted = Color(0xFF8A9890)
private val White = Color(0xFFF3F7F4)
private val Amber = Color(0xFFFFB547)

data class Telemetry(
    val rpm: Int = 1400,
    val tps: Double = 0.0,
    val afr: Double = 14.2,
    val speed: Double = 8.0,
    val ect: Double = 85.0,
    val iat: Double = 33.0,
    val map: Double = 35.0,
    val battery: Double = 13.7,
    val gear: Int = 1,
    val injectorMs: Double = 2.0,
    val ignition: Double = 19.0
)

data class LogPoint(val time: String, val rpm: Int, val afr: Double, val tps: Double)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WaveEcuApp(contentResolver) }
    }
}

@Composable
fun WaveEcuApp(resolver: ContentResolver) {
    var page by remember { mutableStateOf(0) }
    var data by remember { mutableStateOf(Telemetry()) }
    var source by remember { mutableStateOf("DEMO DATA") }
    var csvName by remember { mutableStateOf("ยังไม่ได้เลือกไฟล์") }
    var status by remember { mutableStateOf("โหมดทดลอง — ยังไม่ได้เชื่อมต่อ ECU") }
    var logPoints by remember { mutableStateOf(listOf<LogPoint>()) }
    var mapProfiles by remember { mutableStateOf(listOf("Map 2: Street Performance (จากไฟล์ตัวอย่าง)")) }
    var message by remember { mutableStateOf("") }

    val csvPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val parsed = readLatestTelemetry(resolver, uri)
                data = parsed.first
                logPoints = parsed.second
                csvName = uri.lastPathSegment ?: "CSV"
                source = "CSV IMPORT"
                status = "นำเข้าข้อมูลสำเร็จ — เป็นข้อมูลบันทึก ไม่ใช่ Live ECU"
                message = "อ่านข้อมูลจาก CSV แล้ว ${parsed.second.size} แถว"
            } catch (e: Exception) {
                message = "อ่าน CSV ไม่สำเร็จ: ${e.message ?: "รูปแบบไฟล์ไม่ถูกต้อง"}"
            }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Green, background = Bg, surface = Panel,
            onPrimary = Bg, onBackground = White, onSurface = White
        )
    ) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                NavigationBar(containerColor = Panel, contentColor = Green) {
                    val items = listOf("หน้าหลัก", "กราฟ", "บันทึก", "MAP")
                    val icons = listOf(Icons.Default.Speed, Icons.Default.ShowChart, Icons.Default.Storage, Icons.Default.Tune)
                    items.forEachIndexed { i, label ->
                        NavigationBarItem(
                            selected = page == i,
                            onClick = { page = i },
                            icon = { Icon(icons[i], contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Green,
                                selectedTextColor = Green,
                                indicatorColor = Green.copy(alpha = .13f),
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Header(source)
                when (page) {
                    0 -> Dashboard(data, status, csvName, onImport = { csvPicker.launch(arrayOf("text/*", "text/csv", "application/vnd.ms-excel")) },
                        onSave = {
                            val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                            logPoints = logPoints + LogPoint(now, data.rpm, data.afr, data.tps)
                            message = "เพิ่มค่าปัจจุบันลง Data Log แล้ว"
                        })
                    1 -> GraphPage(logPoints, data)
                    2 -> LogPage(logPoints, csvName, onClear = { logPoints = emptyList(); message = "ล้างรายการบันทึกแล้ว" })
                    else -> MapPage(mapProfiles, onAdd = {
                        mapProfiles = mapProfiles + "โปรไฟล์ใหม่ ${mapProfiles.size + 1}"
                        message = "สร้างโปรไฟล์ว่างแล้ว — ยังไม่เขียนค่าเข้า ECU"
                    })
                }
                if (message.isNotBlank()) {
                    Text(message, color = Green, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
                }
            }
        }
    }
}

@Composable
fun Header(source: String) {
    Row(
        Modifier.fillMaxWidth().background(Bg).padding(horizontal = 18.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("WAVE ECU", color = Green, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
            Text("RACING TELEMETRY  /  HONDA WAVE 110i", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(if (source == "LIVE") Green else Amber, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(source, color = if (source == "LIVE") Green else Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Text("READ ONLY", color = Muted, fontSize = 9.sp)
        }
    }
}

@Composable
fun Dashboard(data: Telemetry, status: String, csvName: String, onImport: () -> Unit, onSave: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CardBlock {
            Text("ENGINE RPM", color = Muted, fontSize = 11.sp, letterSpacing = 1.4.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%,d".format(data.rpm), color = White, fontSize = 48.sp, fontWeight = FontWeight.Black)
                Text(" RPM", color = Green, fontSize = 13.sp, modifier = Modifier.padding(bottom = 9.dp))
            }
            RpmBar(data.rpm)
            Text("แสดงค่าจากแหล่งข้อมูลที่เลือกเท่านั้น", color = Muted, fontSize = 10.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("AFR", "%.1f".format(data.afr), "", Modifier.weight(1f))
            MetricCard("TPS", "%.1f".format(data.tps), "%", Modifier.weight(1f))
            MetricCard("SPEED", "%.0f".format(data.speed), "km/h", Modifier.weight(1f))
        }
        Text("ENGINE SENSORS", color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("COOLANT", "%.0f".format(data.ect), "°C", Modifier.weight(1f))
            MetricCard("INTAKE", "%.0f".format(data.iat), "°C", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("MAP PRESSURE", "%.0f".format(data.map), "kPa", Modifier.weight(1f))
            MetricCard("BATTERY", "%.1f".format(data.battery), "V", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("INJECTOR", "%.2f".format(data.injectorMs), "ms", Modifier.weight(1f))
            MetricCard("IGNITION", "%.0f".format(data.ignition), "° BTDC", Modifier.weight(1f))
        }
        CardBlock {
            Text("DATA SOURCE", color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(status, color = White, fontSize = 12.sp)
            Text(csvName, color = Muted, fontSize = 10.sp)
            Text("การเชื่อมต่อ ECU จริงยังไม่เปิดใช้งานจนกว่าจะทราบรุ่นกล่องและโปรโตคอล", color = Amber, fontSize = 10.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onImport, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                Icon(Icons.Default.FileOpen, null); Spacer(Modifier.width(6.dp)); Text("นำเข้า CSV", color = Bg)
            }
            OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("บันทึกค่า")
            }
        }
    }
}

@Composable
fun RpmBar(rpm: Int) {
    val progress = (rpm / 12000f).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.fillMaxWidth().height(10.dp).background(Color(0xFF29332D), RoundedCornerShape(8.dp))) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Green, RoundedCornerShape(8.dp)))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0", color = Muted, fontSize = 9.sp); Text("6,000", color = Muted, fontSize = 9.sp); Text("12,000 RPM", color = Muted, fontSize = 9.sp)
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (unit.isNotBlank()) Text(" $unit", color = Green, fontSize = 9.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp), content = content)
    }
}

@Composable
fun GraphPage(points: List<LogPoint>, current: Telemetry) {
    var metric by remember { mutableStateOf("RPM") }
    val values = when (metric) {
        "AFR" -> points.map { it.afr.toFloat() }
        "TPS" -> points.map { it.tps.toFloat() }
        else -> points.map { it.rpm.toFloat() }
    }.ifEmpty { listOf(current.rpm.toFloat()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("TELEMETRY GRAPH", color = Green, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Row(horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("RPM", "AFR", "TPS").forEach { item ->
                FilterChip(selected = metric == item, onClick = { metric = item }, label = { Text(item) })
            }
        }
        CardBlock {
            Text(metric, color = Muted, fontSize = 11.sp)
            TelemetryChart(values)
            Text("จุดข้อมูล: ${points.size}", color = Muted, fontSize = 10.sp)
        }
        Text("กราฟจะละเอียดขึ้นเมื่อมีการนำเข้า CSV หรือบันทึกข้อมูลเพิ่ม", color = Muted, fontSize = 11.sp)
    }
}

@Composable
fun TelemetryChart(values: List<Float>) {
    val min = values.minOrNull() ?: 0f
    val max = values.maxOrNull() ?: 1f
    Canvas(Modifier.fillMaxWidth().height(190.dp)) {
        val left = 8f; val right = size.width - 8f
        val top = 10f; val bottom = size.height - 12f
        for (i in 0..4) {
            val y = top + (bottom - top) * i / 4f
            drawLine(Color(0xFF344139), Offset(left, y), Offset(right, y), 1f)
        }
        if (values.size > 1) {
            val range = (max - min).takeIf { it > 0f } ?: 1f
            val pts = values.mapIndexed { index, v ->
                Offset(left + (right-left) * index / (values.size-1).toFloat(),
                    bottom - (v-min)/range*(bottom-top))
            }
            for (i in 0 until pts.lastIndex) drawLine(Green, pts[i], pts[i+1], 3f, cap = StrokeCap.Round)
            pts.forEach { drawCircle(Green, 3f, it) }
        } else {
            drawCircle(Green, 5f, Offset((left+right)/2, (top+bottom)/2))
        }
    }
}

@Composable
fun LogPage(points: List<LogPoint>, fileName: String, onClear: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("DATA LOG", color = Green, fontSize = 18.sp, fontWeight = FontWeight.Black)
            TextButton(onClick = onClear) { Text("ล้างรายการ", color = Amber) }
        }
        CardBlock {
            Text("ไฟล์ล่าสุด", color = Muted, fontSize = 10.sp)
            Text(fileName, color = White, fontSize = 12.sp)
            Text("บันทึกในหน่วยความจำของหน้าจอระหว่างเปิดแอป", color = Muted, fontSize = 10.sp)
        }
        if (points.isEmpty()) {
            Text("ยังไม่มีข้อมูลบันทึก — นำเข้า CSV หรือกดบันทึกค่าจากหน้าหลัก", color = Muted, fontSize = 12.sp)
        } else {
            points.takeLast(100).reversed().forEachIndexed { i, p ->
                CardBlock {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("#${points.size-i}  ${p.time}", color = Green, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("${p.rpm} RPM", color = White, fontWeight = FontWeight.Bold)
                    }
                    Text("AFR ${"%.1f".format(p.afr)}   •   TPS ${"%.1f".format(p.tps)}%", color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun MapPage(profiles: List<String>, onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("MAP MANAGER", color = Green, fontSize = 18.sp, fontWeight = FontWeight.Black)
        CardBlock {
            Text("READ-ONLY PROFILE STORAGE", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("จัดเก็บชื่อโปรไฟล์และข้อมูลที่นำเข้าเพื่อเปรียบเทียบเท่านั้น ยังไม่รองรับการเขียนแผนที่เข้า ECU", color = Muted, fontSize = 11.sp)
        }
        profiles.forEachIndexed { i, name ->
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, null, tint = Green)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(if (i == 0) "Imported / Sample" else "Local draft", color = Muted, fontSize = 10.sp)
                    }
                    Text("READ", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("สร้างโปรไฟล์ว่าง", color = Bg)
        }
    }
}

private fun readLatestTelemetry(resolver: ContentResolver, uri: Uri): Pair<Telemetry, List<LogPoint>> {
    val input = resolver.openInputStream(uri) ?: error("เปิดไฟล์ไม่ได้")
    val lines = BufferedReader(InputStreamReader(input)).use { it.readLines() }.filter { it.isNotBlank() }
    require(lines.size >= 2) { "ไฟล์ไม่มีข้อมูล" }
    val header = parseCsvLine(lines.first()).map { it.trim() }
    val rows = lines.drop(1).map { parseCsvLine(it) }
    fun idx(name: String) = header.indexOf(name)
    fun value(row: List<String>, name: String, fallback: Double = 0.0): Double {
        val i = idx(name)
        return if (i >= 0 && i < row.size) row[i].trim().toDoubleOrNull() ?: fallback else fallback
    }
    val points = rows.mapIndexed { i, row ->
        LogPoint(
            time = row.getOrNull(idx("Time_ISO"))?.takeLast(8) ?: "#${i+1}",
            rpm = value(row, "RPM").toInt(),
            afr = value(row, "AFR_Wideband", 14.7),
            tps = value(row, "TPS_Percent")
        )
    }
    val last = rows.last()
    val telemetry = Telemetry(
        rpm = value(last, "RPM").toInt(),
        tps = value(last, "TPS_Percent"),
        afr = value(last, "AFR_Wideband", 14.7),
        speed = value(last, "Speed_kmh"),
        ect = value(last, "ECT_Coolant_degC"),
        iat = value(last, "IAT_Intake_degC"),
        map = value(last, "MAP_Pressure_kPa"),
        battery = value(last, "Battery_Voltage_V"),
        gear = value(last, "Gear").toInt(),
        injectorMs = value(last, "Injector_Pulse_ms"),
        ignition = value(last, "Ignition_BTDC_deg")
    )
    return telemetry to points
}

private fun parseCsvLine(line: String): List<String> {
    val out = mutableListOf<String>()
    val cell = StringBuilder()
    var quoted = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            c == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> { cell.append('"'); i++ }
            c == '"' -> quoted = !quoted
            c == ',' && !quoted -> { out.add(cell.toString()); cell.clear() }
            else -> cell.append(c)
        }
        i++
    }
    out.add(cell.toString())
    return out
}
