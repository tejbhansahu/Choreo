# Changelog

All notable changes to Choreo are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-11-19

Initial release.

### Added
- `FramePacer` — adaptive, frame-budgeted scheduler that spreads work across display frames and
  auto-tunes the batch size to the measured frame time.
- `PacingConfig` with `forDevice(caps)` — derives the frame budget from the real refresh rate.
- `DeviceProfiler` / `DeviceCapabilities` — `AndroidDeviceProfiler` (CPU/RAM/GPU/OS/storage tiering,
  refresh rate, cores) and `IosDeviceProfiler` (coarser iOS estimate).
- `FrameClock` abstraction with `ChoreographerFrameClock` (Android), `DisplayLinkFrameClock` (iOS),
  and `systemFrameClock()` for shared code.
- Single-active-run semantics, per-item error capture via `onError`, `cancel()` / `isRunning`,
  and `PacingStats` (frames, FPS, succeeded/failed counts, timing).
- Android Compose demo app and iOS SwiftUI demo sources.

[Unreleased]: https://github.com/tejbhansahu/Choreo/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/tejbhansahu/Choreo/releases/tag/v0.1.0
