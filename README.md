# SOUNDROOT

Android app scaffold for Redmi Note 9 Pro / rooted Android. Kotlin + Jetpack Compose.

## Build

Requires JDK 17 and Gradle 8.9, Android SDK platform 35 and build tools installed.

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions

Push this project to the `main` or `master` branch, or run **Build SOUNDROOT APK** manually from Actions. Download the `SOUNDROOT-debug-apk` artifact after a successful run.

## Safety notes

- Root is used only to check `id -u` and query TinyMix.
- This starter does not write mixer values, change speaker amplifier switches, or modify the EAR PA GAIN enum.
- Android media volume is constrained by the system's own maximum.
- TinyMix control IDs and ranges must be verified on-device before any write controls are added.
