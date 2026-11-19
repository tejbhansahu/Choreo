package io.github.tejbhansahu.choreo

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Tuning parameters for [FramePacer].
 *
 * Defaults target a 60Hz display. Prefer [forDevice] to derive these from the device's real
 * refresh rate and performance tier.
 */
data class PacingConfig(
    /** Per-frame work target in ms. The pacer grows the batch toward this and backs off beyond it. */
    val targetFrameTimeMs: Double = 11.0,
    /** Hard per-frame ceiling in ms (the frame budget). Exceeding it shrinks the batch aggressively. */
    val maxFrameTimeMs: Double = 16.0,
    /** Batch size used for the first frame. */
    val initialChunkSize: Int = 20,
    /** Smallest batch the pacer will shrink to. */
    val minChunkSize: Int = 1,
    /** Largest batch the pacer will grow to. */
    val maxChunkSize: Int = 100,
    /** Number of recent frames averaged when deciding the next batch size. */
    val windowSize: Int = 3,
) {
    init {
        require(initialChunkSize >= 1) { "initialChunkSize must be >= 1" }
        require(minChunkSize >= 1) { "minChunkSize must be >= 1" }
        require(maxChunkSize >= minChunkSize) { "maxChunkSize must be >= minChunkSize" }
        require(windowSize >= 1) { "windowSize must be >= 1" }
        require(maxFrameTimeMs >= targetFrameTimeMs) { "maxFrameTimeMs must be >= targetFrameTimeMs" }
    }

    companion object {
        /** Builds a config tuned to a device's measured [DeviceCapabilities]. */
        fun forDevice(caps: DeviceCapabilities): PacingConfig {
            val recommended = caps.recommendedChunkSize
            return PacingConfig(
                targetFrameTimeMs = caps.recommendedTargetFrameTimeMs,
                maxFrameTimeMs = caps.frameBudgetMs,
                initialChunkSize = recommended,
                maxChunkSize = max(recommended, (recommended * 1.5).roundToInt()),
            )
        }
    }
}
