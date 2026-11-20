# Choreo

[![CI](https://github.com/tejbhansahu/Choreo/actions/workflows/ci.yml/badge.svg)](https://github.com/tejbhansahu/Choreo/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.tejbhansahu/choreo?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.tejbhansahu/choreo)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

**Adaptive, frame-budgeted work scheduling for Kotlin Multiplatform.**

Choreo spreads a large batch of work — adding map markers, inflating views, drawing points,
applying a diff — across display frames, measuring each frame and **auto-tuning the batch size to
the real refresh rate** so the UI thread never janks. One tiny, dependency-free engine in
`commonMain`; the only platform piece is the frame clock (`Choreographer` on Android,
`CADisplayLink` on iOS).

> Status: early (0.1.0). API may change before 1.0.

---

## Demo

**Frame-paced rendering** — paced vs. all-at-once, with a live FPS meter (the `:app` sample):

https://github.com/user-attachments/assets/3a9aa160-530b-468c-9201-d55003b968b3

**Real-world origin** — the map-marker rendering in the production app Choreo was extracted from:

https://github.com/user-attachments/assets/f1108f3b-78a9-4985-ad40-af2df54a5352

---

## Why

Doing thousands of UI operations in one pass freezes the main thread. The usual fix — "do N per
frame" — hardcodes a number that's wrong on every other device: too slow on a flagship, still
janky on a budget phone, and oblivious to whether the screen runs at 60, 90 or 120 Hz.

Choreo measures instead of guessing:

- Reads the **actual refresh rate** → frame budget (60 Hz → 16.7 ms, 120 Hz → 8.3 ms).
- Scores the device into a **performance tier** for a sensible starting batch size.
- **Adapts live**: grows the batch when frames are cheap, backs off when they're expensive, with
  smoothing and an emergency downshift.

---

## Install

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.tejbhansahu:choreo:0.1.0")
}
```

> The `0.1.0` release is pending the first Maven Central push (publishing is fully configured —
> see [docs/PUBLISHING.md](docs/PUBLISHING.md)). Until it lands, include the module directly:

```kotlin
// settings.gradle.kts
include(":choreo")
// consumer build.gradle.kts
dependencies { implementation(project(":choreo")) }
```

---

## Quick start (shared / KMP code)

```kotlin
val pacer = FramePacer(systemFrameClock())          // Choreographer or CADisplayLink
val caps = /* AndroidDeviceProfiler(context) or IosDeviceProfiler() */ .capabilities()

pacer.process(
    items = markers,
    config = PacingConfig.forDevice(caps),          // budget derived from the real refresh rate
    onItem = { marker -> addToMap(marker) },
    onProgress = { done, total -> updateProgress(done, total) },
    onError = { index, item, e -> log("item $index failed: $e") },
    onComplete = { stats -> log("${stats.frames} frames @ ${stats.effectiveFps} fps") },
)
```

A throwing `onItem` is counted and reported via `onError` without stopping the run; starting a new
`process(...)` cancels the previous one; call `pacer.cancel()` on teardown.

## Android — XML / Views

```kotlin
private val pacer = FramePacer(ChoreographerFrameClock())

fun render(markers: List<Marker>) {
    val caps = AndroidDeviceProfiler(requireContext()).capabilities()
    pacer.process(markers, PacingConfig.forDevice(caps), onItem = { map.addMarker(it) })
}

override fun onDestroyView() { pacer.cancel(); super.onDestroyView() }
```

## Android — Compose

The library has **no Compose dependency**; drive it from effects:

```kotlin
val pacer = remember { FramePacer(systemFrameClock()) }
DisposableEffect(data) {
    pacer.process(data, onItem = { /* ... */ })
    onDispose { pacer.cancel() }
}
```

## iOS — SwiftUI

```swift
import Choreo

let pacer = ChoreoKt.defaultFramePacer()
let caps = IosDeviceProfiler().capabilities()
pacer.process(
    items: items,
    config: PacingConfig.companion.forDevice(caps: caps),
    onItem: { _ in /* ... */ },
    onProgress: { done, _ in renderedCount = done.intValue },
    onError: nil,
    onComplete: { stats in print("\(stats.frames) frames") }
)
```

See [`iosApp/`](iosApp/) for the full SwiftUI demo and framework-linking steps.

---

## API

| Type | Role |
|---|---|
| `FramePacer` | The scheduler. `process(...)` spreads work across frames; `cancel()`, `isRunning`. |
| `FrameClock` | Per-frame callback source. `ChoreographerFrameClock` / `DisplayLinkFrameClock`, or `systemFrameClock()`. |
| `PacingConfig` | Budget/batch tuning. `PacingConfig.forDevice(caps)` derives it from the display. |
| `DeviceProfiler` | `AndroidDeviceProfiler(context)` / `IosDeviceProfiler()` → `DeviceCapabilities`. |
| `DeviceCapabilities` | `refreshRateHz`, `cpuCores`, `frameBudgetMs`, `tier`, `recommendedChunkSize`, … |
| `PacingStats` | Per-run result: frames, FPS, succeeded/failed counts, timing. |

### Device tiers

| Tier | Starting batch |
|---|---|
| `FLAGSHIP` | 100 |
| `HIGH_END` | 75 |
| `MID_RANGE` | 50 |
| `LOW_MID` | 30 |
| `LOW_END` | 15 |

(Only a starting point — the pacer adapts from here.)

---

## Platform support

| | Frame clock | Device profiler |
|---|---|---|
| **Android** (XML, Compose, KMP) | `Choreographer` ✅ | full score (CPU/RAM/GPU/OS/storage) ✅ |
| **iOS** | `CADisplayLink` ✅ | coarser estimate (CPU/RAM/refresh) — iOS hides GPU/frequency/storage |

The adaptive engine is identical everywhere; only the frame clock and profiler are platform code.

---

## Samples

- **Android:** the `:app` module — `./gradlew :app:installDebug`
- **iOS:** [`iosApp/`](iosApp/) — SwiftUI, see its README

## License

[Apache License 2.0](LICENSE) © 2026 Tejbhan Sahu
