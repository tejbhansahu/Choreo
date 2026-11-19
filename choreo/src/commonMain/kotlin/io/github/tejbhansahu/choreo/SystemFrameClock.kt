package io.github.tejbhansahu.choreo

/**
 * Returns the platform-default [FrameClock] — `Choreographer` on Android, `CADisplayLink` on iOS.
 *
 * Call this from shared/KMP code so you never have to drop into platform source just to obtain a
 * frame clock:
 *
 * ```
 * val pacer = FramePacer(systemFrameClock())
 * ```
 */
expect fun systemFrameClock(): FrameClock
