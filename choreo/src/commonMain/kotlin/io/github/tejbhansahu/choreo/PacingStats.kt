package io.github.tejbhansahu.choreo

/** Summary of a completed (or cancelled) [FramePacer] run. */
data class PacingStats(
    /** Number of items the run was asked to process. */
    val totalItems: Int,
    /** Items attempted so far (`succeededItems + failedItems`). */
    val processedItems: Int,
    /** Items whose `onItem` returned normally. */
    val succeededItems: Int,
    /** Items whose `onItem` threw and were skipped (reported via `onError`, if supplied). */
    val failedItems: Int,
    /** Number of display frames used. */
    val frames: Int,
    /** Wall-clock duration of the run in milliseconds. */
    val totalTimeMs: Long,
    /** Mean per-frame work time in milliseconds. */
    val averageFrameTimeMs: Double,
    /** Frames per second achieved across the run. */
    val effectiveFps: Double,
) {
    /** True when every item was attempted. */
    val completed: Boolean get() = processedItems >= totalItems
}
