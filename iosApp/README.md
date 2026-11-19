# Choreo — iOS demo

A SwiftUI mirror of the Android demo. It uses the `Choreo` Kotlin Multiplatform framework produced
by the `:choreo` module to pace rendering of many dots across `CADisplayLink` frames.

- `iosApp/iOSApp.swift` — app entry point
- `iosApp/ContentView.swift` — the demo screen (device info + paced render + Canvas)

> These Swift files are a ready-to-use starting point. Because this project was created as an
> Android app (no Xcode project yet), you need to create an Xcode app target and link the framework,
> as below. A few Kotlin→Swift names may need Xcode autocomplete fix-ups (e.g. `KotlinInt`, companion
> accessors).

## 1. Build the framework from the `:choreo` module

```bash
# Simulator (Apple Silicon):
./gradlew :choreo:linkDebugFrameworkIosSimulatorArm64
# Device:
./gradlew :choreo:linkDebugFrameworkIosArm64
```

The framework is emitted at:
`choreo/build/bin/iosSimulatorArm64/debugFramework/Choreo.framework`

## 2. Create the Xcode app and link it

1. In Xcode: **File ▸ New ▸ Project ▸ iOS App** (SwiftUI, name `iosApp`), saved under this `iosApp/`
   folder. Replace the generated `App`/`ContentView` with the two Swift files here.
2. Add a **Run Script** build phase (before "Compile Sources") so the framework is rebuilt and
   embedded on each build:
   ```bash
   cd "$SRCROOT/.."
   ./gradlew :choreo:embedAndSignAppleFrameworkForXcode
   ```
3. In **Build Settings ▸ Framework Search Paths**, add:
   `$(SRCROOT)/../choreo/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`
4. In the target's **Frameworks, Libraries, and Embedded Content**, add `Choreo.framework`
   (Embed & Sign).
5. Select an iPhone simulator and **Run**.

## What you should see

The dots fill in smoothly and the frame pacing keeps the UI responsive — the same adaptive engine
and device profiling as Android, from the shared `commonMain` code.

> Tip: the whole thing can also be scaffolded automatically by the Kotlin Multiplatform plugin in
> Android Studio (adds an `iosApp.xcodeproj` and the embed script for you).
