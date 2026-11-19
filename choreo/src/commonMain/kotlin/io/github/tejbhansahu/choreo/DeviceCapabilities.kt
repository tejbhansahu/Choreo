package io.github.tejbhansahu.choreo

/**
 * A snapshot of the device characteristics that matter for frame-paced work.
 *
 * Obtain one from a [DeviceProfiler] (`AndroidDeviceProfiler` / `IosDeviceProfiler`) and feed it
 * to [PacingConfig.forDevice] to tune [FramePacer] to the actual display and hardware.
 */
data class DeviceCapabilities(
    /** Display refresh rate in Hz (e.g. 60, 90, 120). */
    val refreshRateHz: Float,
    /** Number of CPU cores available to the process. */
    val cpuCores: Int,
    /** Overall performance score in `0..100`. */
    val performanceScore: Int,
    /** Coarse performance tier derived from [performanceScore]. */
    val tier: PerformanceTier,
    /** Human-readable breakdown of how the score was computed. */
    val details: String = "",
) {
    /** Milliseconds available per frame at this refresh rate (e.g. ~16.67 at 60Hz, ~8.33 at 120Hz). */
    val frameBudgetMs: Double
        get() = if (refreshRateHz > 0f) 1000.0 / refreshRateHz else DEFAULT_FRAME_BUDGET_MS

    /** A safe per-frame work target that leaves headroom for the system to render (70% of budget). */
    val recommendedTargetFrameTimeMs: Double
        get() = frameBudgetMs * TARGET_BUDGET_FRACTION

    /** Suggested starting batch size for [FramePacer], from the device [tier]. */
    val recommendedChunkSize: Int
        get() = tier.recommendedChunkSize

    companion object {
        /** Fraction of the frame budget [FramePacer] aims to fill with work, leaving the rest for rendering. */
        const val TARGET_BUDGET_FRACTION: Double = 0.7

        /** Fallback budget (~60Hz) when the refresh rate is unknown. */
        const val DEFAULT_FRAME_BUDGET_MS: Double = 16.667
    }
}
