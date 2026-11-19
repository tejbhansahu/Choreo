package io.github.tejbhansahu.choreo

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.opengl.GLES20
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File

/**
 * Android [DeviceProfiler]. Scores the device across CPU, RAM, GPU/display, OS version and storage
 * into a [PerformanceTier], and reports the real display refresh rate and CPU core count.
 *
 * Pass any [Context]; the application context is retained. All probes are defensive and fall back
 * to conservative values on error, so [capabilities] never throws.
 */
class AndroidDeviceProfiler(context: Context) : DeviceProfiler {

    private val appContext: Context = context.applicationContext

    override fun capabilities(): DeviceCapabilities {
        val cores = Runtime.getRuntime().availableProcessors()
        val refreshRate = readRefreshRateHz()

        val details = mutableListOf<String>()
        var score = 0
        cpuScore(cores).also { score += it; details += "CPU $it/40" }
        ramScore().also { score += it; details += "RAM $it/25" }
        gpuScore(refreshRate).also { score += it; details += "GPU $it/20" }
        osScore().also { score += it; details += "OS $it/10" }
        storageScore().also { score += it; details += "Storage $it/5" }

        return DeviceCapabilities(
            refreshRateHz = refreshRate,
            cpuCores = cores,
            performanceScore = score,
            tier = PerformanceTier.fromScore(score),
            details = details.joinToString(", "),
        )
    }

    private fun cpuScore(cores: Int): Int {
        var score = when {
            cores >= 8 -> 15
            cores >= 6 -> 12
            cores >= 4 -> 8
            cores >= 2 -> 4
            else -> 0
        }
        val maxFreq = maxCpuFrequencyKhz()
        score += when {
            maxFreq >= 2_800_000 -> 15
            maxFreq >= 2_400_000 -> 12
            maxFreq >= 2_000_000 -> 8
            maxFreq >= 1_800_000 -> 5
            else -> 2
        }
        val arch = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
        score += when {
            arch.contains("arm64-v8a") -> 10
            arch.contains("armeabi-v7a") -> 5
            else -> 0
        }
        return score.coerceIn(0, 40)
    }

    private fun ramScore(): Int {
        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val totalGb = mem.totalMem / (1024.0 * 1024.0 * 1024.0)
        var score = when {
            totalGb >= 12 -> 25
            totalGb >= 8 -> 20
            totalGb >= 6 -> 15
            totalGb >= 4 -> 10
            totalGb >= 3 -> 5
            else -> 2
        }
        if (am.memoryClass < 192) score = (score * 0.7).toInt()
        return score.coerceIn(0, 25)
    }

    @SuppressLint("ServiceCast")
    @Suppress("DEPRECATION")
    private fun gpuScore(refreshRate: Float): Int {
        var score = 0
        try {
            val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(metrics)
            val pixels = metrics.widthPixels.toLong() * metrics.heightPixels.toLong()
            score += when {
                pixels >= 3840L * 2160 -> 10
                pixels >= 2400L * 1080 -> 8
                pixels >= 1920L * 1080 -> 6
                pixels >= 1280L * 720 -> 3
                else -> 1
            }
        } catch (_: Throwable) {
            score += 3
        }
        score += when {
            refreshRate >= 120f -> 5
            refreshRate >= 90f -> 3
            refreshRate >= 60f -> 2
            else -> 0
        }
        val renderer = glRenderer()
        score += when {
            renderer.contains("Adreno 7", ignoreCase = true) -> 5
            renderer.contains("Adreno 6", ignoreCase = true) -> 4
            renderer.contains("Mali-G7", ignoreCase = true) -> 4
            renderer.contains("Mali-G5", ignoreCase = true) -> 2
            renderer.contains("PowerVR", ignoreCase = true) -> 2
            else -> 1
        }
        return score.coerceIn(0, 20)
    }

    private fun osScore(): Int = when {
        Build.VERSION.SDK_INT >= 34 -> 10
        Build.VERSION.SDK_INT >= 33 -> 9
        Build.VERSION.SDK_INT >= 31 -> 7
        Build.VERSION.SDK_INT >= 30 -> 5
        Build.VERSION.SDK_INT >= 29 -> 3
        else -> 1
    }

    private fun storageScore(): Int {
        val mounts = runCatching { File("/proc/mounts").readText() }.getOrDefault("")
        return when {
            mounts.contains("ufs", ignoreCase = true) -> 5
            mounts.contains("nvme", ignoreCase = true) -> 5
            mounts.contains("emmc 5.1", ignoreCase = true) -> 2
            mounts.contains("emmc", ignoreCase = true) -> 1
            else -> 0
        }
    }

    @Suppress("DEPRECATION")
    private fun readRefreshRateHz(): Float = try {
        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val rate = display.refreshRate
        if (rate > 0f) rate else 60f
    } catch (_: Throwable) {
        60f
    }

    private fun maxCpuFrequencyKhz(): Long = try {
        var maxFreq = 0L
        for (i in 0 until Runtime.getRuntime().availableProcessors()) {
            val file = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
            if (file.exists()) {
                val freq = file.readText().trim().toLongOrNull() ?: 0L
                if (freq > maxFreq) maxFreq = freq
            }
        }
        maxFreq
    } catch (_: Throwable) {
        0L
    }

    private fun glRenderer(): String = try {
        GLES20.glGetString(GLES20.GL_RENDERER).orEmpty()
    } catch (_: Throwable) {
        ""
    }
}
