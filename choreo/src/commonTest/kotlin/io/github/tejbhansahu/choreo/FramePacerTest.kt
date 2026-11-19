package io.github.tejbhansahu.choreo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.TestTimeSource

class FramePacerTest {

    @Test
    fun processesEveryItemAcrossFrames() {
        val clock = FakeFrameClock()
        val time = TestTimeSource()
        val pacer = FramePacer(clock, time)

        val processed = mutableListOf<Int>()
        var stats: PacingStats? = null
        pacer.process(
            items = (1..1000).toList(),
            config = PacingConfig(initialChunkSize = 20),
            onItem = { processed += it; time += 300.microseconds },
            onComplete = { stats = it },
        )
        clock.runToCompletion()

        assertEquals((1..1000).toList(), processed)
        assertEquals(1000, stats?.succeededItems)
        assertEquals(0, stats?.failedItems)
        assertTrue(stats?.completed == true)
        assertFalse(pacer.isRunning)
    }

    @Test
    fun emptyListCompletesWithoutFrames() {
        val clock = FakeFrameClock()
        val pacer = FramePacer(clock, TestTimeSource())

        var stats: PacingStats? = null
        pacer.process(items = emptyList<Int>(), onItem = {}, onComplete = { stats = it })

        assertFalse(clock.hasPending)
        assertEquals(0, stats?.frames)
        assertTrue(stats?.completed == true)
    }

    @Test
    fun cancelStopsProcessing() {
        val clock = FakeFrameClock()
        val time = TestTimeSource()
        val pacer = FramePacer(clock, time)

        var count = 0
        val handle = pacer.process(
            items = (1..1000).toList(),
            config = PacingConfig(initialChunkSize = 10),
            onItem = { count++; time += 200.microseconds },
        )
        clock.tick()
        val afterOneFrame = count
        handle.cancel()
        clock.runToCompletion()

        assertEquals(afterOneFrame, count)
        assertFalse(handle.isActive)
        assertFalse(clock.hasPending)
    }

    @Test
    fun spreadsHeavyItemsOverManyFrames() {
        val clock = FakeFrameClock()
        val time = TestTimeSource()
        val pacer = FramePacer(clock, time)

        // Each item costs 5ms: even a small batch blows a 16ms budget, so the pacer must shrink
        // the batch and therefore take several frames to finish even a short list.
        var frames = 0
        pacer.process(
            items = (1..40).toList(),
            config = PacingConfig(initialChunkSize = 20, targetFrameTimeMs = 11.0, maxFrameTimeMs = 16.0),
            onItem = { time += 5_000.microseconds },
            onComplete = { frames = it.frames },
        )
        clock.runToCompletion()

        assertTrue(frames > 2, "expected heavy items to span several frames, got $frames")
    }

    @Test
    fun itemFailuresAreCountedAndReportedWithoutStoppingTheRun() {
        val clock = FakeFrameClock()
        val time = TestTimeSource()
        val pacer = FramePacer(clock, time)

        val reportedFailures = mutableListOf<Int>()
        var stats: PacingStats? = null
        pacer.process(
            items = (1..100).toList(),
            config = PacingConfig(initialChunkSize = 10),
            onItem = {
                time += 100.microseconds
                if (it % 10 == 0) throw IllegalStateException("boom $it")
            },
            onError = { _, item, _ -> reportedFailures += item },
            onComplete = { stats = it },
        )
        clock.runToCompletion()

        assertEquals(10, stats?.failedItems)
        assertEquals(90, stats?.succeededItems)
        assertEquals(100, stats?.processedItems)
        assertTrue(stats?.completed == true)
        assertEquals((1..100).filter { it % 10 == 0 }, reportedFailures)
    }

    @Test
    fun startingANewRunCancelsThePrevious() {
        val clock = FakeFrameClock()
        val time = TestTimeSource()
        val pacer = FramePacer(clock, time)

        var aCount = 0
        val a = pacer.process(
            items = (1..1000).toList(),
            config = PacingConfig(initialChunkSize = 5),
            onItem = { aCount++; time += 100.microseconds },
        )
        clock.tick()
        val aAfterReplace = aCount

        var bCount = 0
        val b = pacer.process(
            items = (1..50).toList(),
            config = PacingConfig(initialChunkSize = 5),
            onItem = { bCount++; time += 100.microseconds },
        )
        clock.runToCompletion()

        assertFalse(a.isActive)
        assertEquals(aAfterReplace, aCount, "the replaced run must not advance further")
        assertEquals(50, bCount)
        assertFalse(b.isActive)
    }
}
