package io.github.tejbhansahu.choreo

/**
 * A source of per-frame callbacks (`Choreographer` on Android, `CADisplayLink` on iOS).
 *
 * [FramePacer] depends on this abstraction so its adaptive loop can be unit-tested with a fake
 * clock and reused unchanged across platforms.
 */
interface FrameClock {
    /**
     * Requests [onFrame] to be invoked once on the next display frame, where `frameTimeNanos` is
     * the frame timestamp in nanoseconds. Implementations invoke the callback on the main thread.
     */
    fun postFrame(onFrame: (frameTimeNanos: Long) -> Unit)

    /** Cancels any pending frame callback. */
    fun removeFrame()
}
