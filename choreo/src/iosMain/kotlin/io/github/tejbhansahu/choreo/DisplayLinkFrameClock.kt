package io.github.tejbhansahu.choreo

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CADisplayLink
import platform.darwin.NSObject

/**
 * [FrameClock] backed by iOS's `CADisplayLink`. Callbacks are delivered on the main run loop, so
 * create and drive [FramePacer] from the main thread.
 *
 * Experimental: the iOS targets are not yet enabled in the build; this source is provided so the
 * platform implementation is ready when they are.
 */
@OptIn(ExperimentalForeignApi::class)
class DisplayLinkFrameClock : FrameClock {

    private var displayLink: CADisplayLink? = null
    private var callback: ((Long) -> Unit)? = null

    private val proxy = DisplayLinkProxy { link ->
        val cb = callback
        removeFrame()
        val nanos = (link.timestamp * 1_000_000_000.0).toLong()
        cb?.invoke(nanos)
    }

    override fun postFrame(onFrame: (frameTimeNanos: Long) -> Unit) {
        callback = onFrame
        displayLink?.invalidate()
        val link = CADisplayLink.displayLinkWithTarget(proxy, NSSelectorFromString("onFrame:"))
        link.addToRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)
        displayLink = link
    }

    override fun removeFrame() {
        callback = null
        displayLink?.invalidate()
        displayLink = null
    }
}

@OptIn(ExperimentalForeignApi::class)
private class DisplayLinkProxy(
    private val onTick: (CADisplayLink) -> Unit,
) : NSObject() {
    @ObjCAction
    fun onFrame(link: CADisplayLink) {
        onTick(link)
    }
}
