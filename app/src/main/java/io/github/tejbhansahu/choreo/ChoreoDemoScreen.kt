package io.github.tejbhansahu.choreo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

private const val TOTAL_DOTS = 15_000

private data class Dot(val x: Float, val y: Float, val color: Color)

private val palette = listOf(
    Color(0xFF4C8DFF), Color(0xFF34C759), Color(0xFFFF9F0A),
    Color(0xFFFF375F), Color(0xFFBF5AF2), Color(0xFF32ADE6),
)

/** Simulates a non-trivial per-item cost (like building a marker bitmap) so pacing is meaningful. */
private fun simulateWork(seed: Float): Double {
    var acc = 0.0
    var i = 0
    while (i < 120) {
        acc += sqrt((seed + i) * 1.3 + 1.0)
        i++
    }
    return acc
}

@Composable
fun ChoreoDemoScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val caps = remember { AndroidDeviceProfiler(context).capabilities() }
    val dots = remember {
        List(TOTAL_DOTS) {
            Dot(Random.nextFloat(), Random.nextFloat(), palette[it % palette.size])
        }
    }
    val pacer = remember { FramePacer(ChoreographerFrameClock()) }
    val workSink = remember { DoubleArray(1) }

    var renderedCount by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("Idle — tap a button to render $TOTAL_DOTS dots.") }
    var mode by remember { mutableStateOf("") }

    // Live display FPS: dips when the main thread is blocked (unpaced), stays high when paced.
    var fps by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        var frames = 0
        var windowStart = 0L
        while (true) {
            withFrameNanos { now ->
                if (windowStart == 0L) windowStart = now
                frames++
                val elapsed = now - windowStart
                if (elapsed >= 500_000_000L) {
                    fps = (frames * 1_000_000_000.0 / elapsed).roundToInt()
                    frames = 0
                    windowStart = now
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { pacer.cancel() }
    }

    fun renderPaced() {
        mode = "paced"
        renderedCount = 0
        status = "Pacing ${dots.size} dots across frames…"
        pacer.process(
            items = dots,
            config = PacingConfig.forDevice(caps),
            onItem = { workSink[0] += simulateWork(it.x) },
            onProgress = { processed, _ -> renderedCount = processed },
            onComplete = { s ->
                status = "Paced ✓  ${s.frames} frames · ${"%.0f".format(s.effectiveFps)} fps · " +
                    "${s.totalTimeMs} ms · ${"%.2f".format(s.averageFrameTimeMs)} ms/frame"
            },
        )
    }

    fun renderUnpaced() {
        mode = "unpaced"
        renderedCount = 0
        pacer.cancel()
        val start = System.nanoTime()
        for (d in dots) workSink[0] += simulateWork(d.x) // blocks the main thread on purpose
        renderedCount = dots.size
        val ms = (System.nanoTime() - start) / 1_000_000
        status = "Unpaced ✗  blocked the UI thread for $ms ms (watch the FPS drop)"
    }

    fun clear() {
        pacer.cancel()
        renderedCount = 0
        mode = ""
        status = "Cleared."
    }

    val fraction = if (dots.isNotEmpty()) renderedCount / dots.size.toFloat() else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Choreo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Adaptive frame-paced rendering",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(12.dp)) {
                StatRow("Display", "${caps.refreshRateHz.roundToInt()} Hz  ·  budget ${"%.1f".format(caps.frameBudgetMs)} ms  ·  target ${"%.1f".format(caps.recommendedTargetFrameTimeMs)} ms")
                StatRow("CPU", "${caps.cpuCores} cores")
                StatRow("Tier", "${caps.tier} (score ${caps.performanceScore}/100)")
                StatRow("Start batch", "${caps.recommendedChunkSize} dots/frame")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { renderPaced() }, modifier = Modifier.weight(1f)) { Text("Render paced") }
            OutlinedButton(onClick = { renderUnpaced() }, modifier = Modifier.weight(1f)) { Text("Render all at once") }
        }
        OutlinedButton(onClick = { clear() }) { Text("Clear") }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Live FPS: $fps", fontWeight = FontWeight.SemiBold)
            Text("$renderedCount / ${dots.size}", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())

        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(320.dp)) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val w = size.width
                val h = size.height
                val count = renderedCount
                for (i in 0 until count) {
                    val d = dots[i]
                    drawCircle(color = d.color, radius = 2.2f, center = Offset(d.x * w, d.y * h))
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            if (mode == "paced") "Paced: dots appear smoothly, FPS holds."
            else if (mode == "unpaced") "Unpaced: the screen freezes, then all dots pop in at once."
            else "Compare: 'paced' stays responsive; 'all at once' hitches.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}
