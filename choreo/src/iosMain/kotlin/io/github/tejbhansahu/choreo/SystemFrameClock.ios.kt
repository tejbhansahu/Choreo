package io.github.tejbhansahu.choreo

/** iOS: a [DisplayLinkFrameClock] on the main run loop's `CADisplayLink`. */
actual fun systemFrameClock(): FrameClock = DisplayLinkFrameClock()
