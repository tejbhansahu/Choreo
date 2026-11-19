package io.github.tejbhansahu.choreo

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.TimeSource

/**
 * Spreads the processing of a large collection across display frames, adapting the per-frame batch
 * size to stay within the frame budget so the UI thread never janks.
 *
 * Give it a [FrameClock] ([systemFrameClock] returns the platform default) and call [process]. The
 * adaptive loop lives entirely in common code, which keeps it identical across Android (XML & Compose),
 * iOS and KMP shared code, and makes it unit-testable with a fake clock.
 *
 * ```
 * // Shared / KMP code:
 * val pacer = FramePacer(systemFrameClock())
 * pacer.process(
 *     items = markers,
 *     config = PacingConfig.forDevice(capabilities),
 *     onItem = { addMarker(it) },
 *     onError = { i, item, e -> log("marker $i failed: $e") },
 *     onComplete = { stats -> log("${stats.frames} frames @ ${stats.effectiveFps} fps") },
 * )
 * ```
 *
 * ### Threading & lifecycle
 * A pacer is single-threaded: create and drive it on the main thread. It runs **one** job at a time —
 * calling [process] again cancels the previous run. Call [cancel] (or [PacerHandle.cancel]) when the
 * host is torn down (Android `onDestroy` / Compose `DisposableEffect`, iOS `viewWillDisappear`).
 *
 * @param timeSource clock used to measure each frame's work; defaults to the monotonic system clock
 * and is injectable for testing.
 */
class FramePacer(
    private val frameClock: FrameClock,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private var current: PacedRun<*>? = null

    /** True while a run is in progress on this pacer. */
    val isRunning: Boolean get() = current?.isActive == true

    /**
     * Processes [items], invoking [onItem] for each, spreading the work across display frames and
     * adapting the per-frame batch size to stay within [config]'s budget. Any run already in
     * progress on this pacer is cancelled first.
     *
     * @param onProgress optional per-frame callback with running processed/total counts.
     * @param onError optional callback for an item whose [onItem] threw; the run continues. When
     * omitted, failures are still counted in [PacingStats.failedItems] but not surfaced.
     * @param onComplete optional callback fired once all items are attempted (not on [cancel]).
     * @return a [PacerHandle] to cancel this run.
     */
    fun <T> process(
        items: List<T>,
        config: PacingConfig = PacingConfig(),
        onItem: (T) -> Unit,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null,
        onError: ((index: Int, item: T, error: Throwable) -> Unit)? = null,
        onComplete: ((PacingStats) -> Unit)? = null,
    ): PacerHandle {
        current?.cancel()
        val run = PacedRun(items, config, onItem, onProgress, onError, onComplete)
        current = run
        run.start()
        return run
    }

    /** Cancels the current run, if any. Safe to call when nothing is running. */
    fun cancel() {
        current?.cancel()
    }

    private inner class PacedRun<T>(
        private val items: List<T>,
        private val config: PacingConfig,
        private val onItem: (T) -> Unit,
        private val onProgress: ((Int, Int) -> Unit)?,
        private val onError: ((Int, T, Throwable) -> Unit)?,
        private val onComplete: ((PacingStats) -> Unit)?,
    ) : PacerHandle {
        private val total = items.size
        private var index = 0
        private var succeeded = 0
        private var failed = 0
        private var chunk = config.initialChunkSize.coerceIn(config.minChunkSize, config.maxChunkSize)
        private var frames = 0
        private var totalFrameTimeMs = 0.0
        private val recent = ArrayDeque<Double>()
        private val startMark = timeSource.markNow()
        private var active = true

        override val isActive: Boolean get() = active

        fun start() {
            if (total == 0) finish() else frameClock.postFrame(::doFrame)
        }

        private fun doFrame(frameTimeNanos: Long) {
            if (!active) return
            if (index >= total) {
                finish()
                return
            }

            val frameStart = timeSource.markNow()
            val end = min(index + chunk, total)
            var i = index
            while (i < end) {
                val item = items[i]
                try {
                    onItem(item)
                    succeeded++
                } catch (e: Exception) {
                    // A single bad item must never crash the frame loop; count it and carry on.
                    failed++
                    onError?.invoke(i, item, e)
                }
                i++
            }
            index = end

            val frameMs = frameStart.elapsedNow().inWholeMicroseconds / 1000.0
            frames++
            totalFrameTimeMs += frameMs
            recent.addLast(frameMs)
            if (recent.size > config.windowSize) recent.removeFirst()
            onProgress?.invoke(index, total)

            chunk = nextChunkSize(frameMs)

            if (index < total) frameClock.postFrame(::doFrame) else finish()
        }

        /**
         * Adaptive sizing: estimate time-per-item from a rolling average, compute the batch that
         * would fill the target, then nudge toward it with emergency/backoff/grow bands and smoothing.
         */
        private fun nextChunkSize(frameMs: Double): Int {
            val avgRecent = if (recent.isEmpty()) frameMs else recent.average()
            val perItem = avgRecent / max(1, chunk)
            val optimal = if (perItem > 0.0) (config.targetFrameTimeMs / perItem).roundToInt() else chunk

            val proposed = when {
                frameMs > config.maxFrameTimeMs * 1.5 -> max(config.minChunkSize, chunk / 3)        // emergency
                frameMs > config.maxFrameTimeMs -> max(config.minChunkSize, optimal * 8 / 10)        // over budget
                frameMs > config.targetFrameTimeMs -> max(config.minChunkSize, optimal)              // fine-tune down
                frameMs < config.targetFrameTimeMs * 0.7 -> min(config.maxChunkSize, optimal)        // speed up
                else -> min(config.maxChunkSize, max(chunk, optimal * 9 / 10))                        // steady
            }
            // Damp large swings to keep sizing stable.
            val smoothed = if (abs(proposed - chunk) > chunk / 2) (chunk + proposed) / 2 else proposed
            return smoothed.coerceIn(config.minChunkSize, config.maxChunkSize)
        }

        private fun finish() {
            if (!active) return
            active = false
            if (current === this) current = null
            frameClock.removeFrame()
            val totalMs = startMark.elapsedNow().inWholeMilliseconds
            val avg = if (frames > 0) totalFrameTimeMs / frames else 0.0
            val fps = if (totalMs > 0) frames * 1000.0 / totalMs else 0.0
            onComplete?.invoke(
                PacingStats(
                    totalItems = total,
                    processedItems = index,
                    succeededItems = succeeded,
                    failedItems = failed,
                    frames = frames,
                    totalTimeMs = totalMs,
                    averageFrameTimeMs = avg,
                    effectiveFps = fps,
                ),
            )
        }

        override fun cancel() {
            if (!active) return
            active = false
            if (current === this) current = null
            frameClock.removeFrame()
        }
    }
}
