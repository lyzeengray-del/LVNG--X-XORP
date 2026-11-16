# LVNG -X · Android Game Space

A Kotlin + Jetpack Compose Android project using the supplied official LVNG -X logo.

## Included in this build

- Black / crimson visual identity and supplied launcher icon.
- Home designed as a game launcher, not a feature directory.
- Installed-app library with add/remove selection and launch action.
- Floating LVNG -X overlay bubble, draggable and edge-dockable.
- Left-side overlay panel with the agreed feature groups.
- Permission/diagnostics screen that reports actual overlay permission state.
- Local game library persistence.
- GitHub Actions workflow to build a debug APK.

## Honest implementation status

This is a functional first implementation of the launcher and overlay foundation, not a claim that every item in the 100+ feature specification is already complete.

The following require additional dedicated implementation and device testing before they can be marked working:
- Shizuku API/Binder integration and privileged operations.
- Macro touch injection through an AccessibilityService, including hold-to-repeat and emergency cancellation.
- Game FPS and advanced performance telemetry (device/API-dependent).
- Resolution control, key mapping, virtual crosshair, task management and screen-off actions (permission/OEM/Android-dependent).
- MediaProjection recording flow.
- Full `.lvngx` selective backup/restore, integrity validation, migration and rollback.
- Complete diagnostics for each service and feature.

The overlay is shown only after the user grants Android's "Display over other apps" permission. The app does not silently grant permissions or pretend unsupported capabilities work.

## Build on GitHub

1. Create a GitHub repository.
2. Extract the contents of this ZIP into the repository root.
3. Commit and push to `main` (or `master`).
4. Open **Actions** and wait for **Android Build**.
5. Download `LVNG-X-debug-apk` from the successful workflow run.

The workflow uses JDK 17 and Gradle 8.9.

## Local build

Open in Android Studio with JDK 17, sync Gradle, then run the `app` configuration.

- Application ID: `com.lvngx.app`
- Minimum Android: 8.0 (API 26)
- Compile / target SDK: 35
