package io.github.tejbhansahu.choreo

/** Handle to an in-flight [FramePacer] run. */
interface PacerHandle {
    /** True while frames are still scheduled. */
    val isActive: Boolean

    /** Stops the run immediately; no further items are processed and the completion callback does not fire. */
    fun cancel()
}
