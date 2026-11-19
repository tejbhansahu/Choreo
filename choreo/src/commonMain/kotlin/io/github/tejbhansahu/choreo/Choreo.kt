package io.github.tejbhansahu.choreo

/**
 * Convenience factory: a [FramePacer] on the platform-default [FrameClock] ([systemFrameClock]).
 *
 * Especially handy from Swift/iOS, where Kotlin default arguments (like `FramePacer`'s optional
 * `timeSource`) are not available, so `FramePacer(systemFrameClock())` cannot be expressed directly.
 *
 * Swift: `let pacer = ChoreoKt.defaultFramePacer()`
 */
fun defaultFramePacer(): FramePacer = FramePacer(systemFrameClock())
