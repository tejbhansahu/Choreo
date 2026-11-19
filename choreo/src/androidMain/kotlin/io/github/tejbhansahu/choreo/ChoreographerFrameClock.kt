package io.github.tejbhansahu.choreo

import android.view.Choreographer

/**
 * [FrameClock] backed by Android's [Choreographer]. Callbacks are delivered on the main thread, so
 * create and drive [FramePacer] from the main thread.
 */
class ChoreographerFrameClock(
    private val choreographer: Choreographer = Choreographer.getInstance(),
) : FrameClock {

    private var callback: Choreographer.FrameCallback? = null

    override fun postFrame(onFrame: (frameTimeNanos: Long) -> Unit) {
        removeFrame()
        val cb = Choreographer.FrameCallback { frameTimeNanos -> onFrame(frameTimeNanos) }
        callback = cb
        choreographer.postFrameCallback(cb)
    }

    override fun removeFrame() {
        callback?.let { choreographer.removeFrameCallback(it) }
        callback = null
    }
}
