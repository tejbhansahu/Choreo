package io.github.tejbhansahu.choreo

/** Android: a [ChoreographerFrameClock] on the main-thread [android.view.Choreographer]. */
actual fun systemFrameClock(): FrameClock = ChoreographerFrameClock()
