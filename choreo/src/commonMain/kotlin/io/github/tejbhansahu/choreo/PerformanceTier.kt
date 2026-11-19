package io.github.tejbhansahu.choreo

/**
 * Coarse performance class of a device, with a sensible starting batch size for [FramePacer].
 *
 * The [recommendedChunkSize] is only a starting point — [FramePacer] measures real frame times
 * and adapts the batch size up or down from here.
 */
enum class PerformanceTier(val recommendedChunkSize: Int) {
    /** Top-end hardware (e.g. 12GB+ RAM, 120Hz, flagship SoC). */
    FLAGSHIP(100),

    /** Upper mid-range. */
    HIGH_END(75),

    /** Mid-range. */
    MID_RANGE(50),

    /** Low mid-range. */
    LOW_MID(30),

    /** Budget / entry-level hardware. */
    LOW_END(15);

    companion object {
        /** Maps a 0..100 performance score to a tier. */
        fun fromScore(score: Int): PerformanceTier = when {
            score >= 85 -> FLAGSHIP
            score >= 70 -> HIGH_END
            score >= 50 -> MID_RANGE
            score >= 30 -> LOW_MID
            else -> LOW_END
        }
    }
}
