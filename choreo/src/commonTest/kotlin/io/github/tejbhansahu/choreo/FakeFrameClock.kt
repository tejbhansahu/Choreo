package io.github.tejbhansahu.choreo

/**
 * Test [FrameClock] that records the pending callback and lets the test drive frames manually via
 * [tick] / [runToCompletion] instead of a real display.
 */
class FakeFrameClock : FrameClock {
    private var pending: ((Long) -> Unit)? = null
    private var frameTimeNanos: Long = 0L

    val hasPending: Boolean get() = pending != null

    override fun postFrame(onFrame: (Long) -> Unit) {
        pending = onFrame
    }

    override fun removeFrame() {
        pending = null
    }

    /** Fires the single pending frame callback, if any. */
    fun tick() {
        val cb = pending ?: return
        pending = null
        frameTimeNanos += 16_000_000L
        cb(frameTimeNanos)
    }

    /** Fires frames until none remain (or [maxFrames] is reached, to guard against runaway loops). */
    fun runToCompletion(maxFrames: Int = 1_000_000) {
        var n = 0
        while (pending != null && n < maxFrames) {
            tick()
            n++
        }
    }
}
