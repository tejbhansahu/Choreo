# Contributing to Choreo

Thanks for your interest! Choreo is a small, focused library — bug fixes, tests, docs and
platform-parity improvements are all welcome.

## Getting started

```bash
git clone https://github.com/tejbhansahu/Choreo.git
cd Choreo
./gradlew :choreo:testAndroidHostTest          # common + Android unit tests
./gradlew :choreo:compileKotlinIosSimulatorArm64  # iOS compile (needs macOS)
./gradlew :app:installDebug                     # run the Android demo
```

## Guidelines

- **Keep the core dependency-free.** `commonMain` should not pull in Compose, coroutines, or other
  runtime libraries — the only platform code is the frame clock and the device profiler.
- **Put logic in `commonMain`.** Platform source sets (`androidMain` / `iosMain`) should hold only
  what truly needs platform APIs.
- **Add a test** for behavior changes. The adaptive loop is unit-tested with a fake `FrameClock` and
  a `TestTimeSource` — no device required.
- **Run the checks** above before opening a PR; CI must be green.
- **Update `CHANGELOG.md`** under `## [Unreleased]`.

## Reporting issues

Please include the device/OS, the library version, and a minimal snippet. For pacing issues, the
`PacingStats` from `onComplete` (frames, fps, average frame time) is very helpful.
