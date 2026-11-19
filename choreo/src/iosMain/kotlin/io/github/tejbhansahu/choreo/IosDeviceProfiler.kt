package io.github.tejbhansahu.choreo

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIScreen

/**
 * iOS [DeviceProfiler].
 *
 * iOS exposes far less hardware detail than Android: refresh rate, CPU core count, RAM and OS are
 * available, but CPU frequency, GPU model and storage type are not — so the score is a coarser
 * estimate than on Android and is prefixed "iOS estimate" in [DeviceCapabilities.details].
 *
 * Experimental: the iOS targets are not yet enabled in the build; this source is provided so the
 * platform implementation is ready when they are.
 */
@OptIn(ExperimentalForeignApi::class)
class IosDeviceProfiler : DeviceProfiler {

    override fun capabilities(): DeviceCapabilities {
        val info = NSProcessInfo.processInfo
        val cores = info.activeProcessorCount.toInt()
        val refreshRate = UIScreen.mainScreen.maximumFramesPerSecond.toInt()
            .let { if (it > 0) it.toFloat() else 60f }
        val ramGb = info.physicalMemory.toDouble() / (1024.0 * 1024.0 * 1024.0)

        val details = mutableListOf<String>()
        var score = 0
        cpuScore(cores).also { score += it; details += "CPU $it/55" }
        ramScore(ramGb).also { score += it; details += "RAM $it/25" }
        refreshScore(refreshRate).also { score += it; details += "Display $it/20" }

        return DeviceCapabilities(
            refreshRateHz = refreshRate,
            cpuCores = cores,
            performanceScore = score,
            tier = PerformanceTier.fromScore(score),
            details = "iOS estimate: " + details.joinToString(", "),
        )
    }

    private fun cpuScore(cores: Int): Int = when {
        cores >= 6 -> 55
        cores >= 4 -> 40
        cores >= 2 -> 25
        else -> 10
    }

    private fun ramScore(gb: Double): Int = when {
        gb >= 8 -> 25
        gb >= 6 -> 20
        gb >= 4 -> 15
        gb >= 3 -> 10
        else -> 5
    }

    private fun refreshScore(hz: Float): Int = when {
        hz >= 120f -> 20
        hz >= 90f -> 14
        hz >= 60f -> 10
        else -> 5
    }
}
