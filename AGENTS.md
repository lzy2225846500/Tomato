# AGENTS.md

## Project Context

This repository is a personal modified fork of `nsh07/Tomato`.

- Upstream project: https://github.com/nsh07/Tomato
- Personal fork: https://github.com/lzy2225846500/Tomato
- License: GNU GPLv3. Keep the original `LICENSE`, copyright headers, and fork attribution.
- Current product direction: a lightweight Android TODO + Pomodoro app for personal use and a few friends.

## Repository Rules

- Do not push to `upstream`. Push user work to `origin`.
- Keep `upstream` available for future syncs from the original project.
- Preserve GPLv3 notices in source files.
- Avoid removing original attribution unless the user explicitly asks and the legal/attribution impact is considered.
- Keep changes small and committed in coherent steps.
- Do not revert unrelated user changes.

## Tech Stack

- Android app with Kotlin and Compose Multiplatform.
- Shared KMP module under `shared`.
- Room database with schemas in `shared/schemas`.
- Jetpack Glance widgets under `androidApp/src/main/java/org/nsh07/pomodoro/widget`.
- Koin for dependency injection.
- Gradle wrapper is the expected build entry point.

## Common Commands

Use the local Android SDK bundled in this workspace when needed:

```powershell
$env:ANDROID_HOME='C:\Users\Administrator\Documents\Codex\2026-07-01\xian\work\android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
```

Run unit tests:

```powershell
.\gradlew.bat :androidApp:testFossDebugUnitTest --console=plain
```

Compile shared Android code:

```powershell
.\gradlew.bat :shared:compileAndroidMain --console=plain
```

Build the FOSS debug APK:

```powershell
.\gradlew.bat :androidApp:assembleFossDebug --console=plain
```

Check whitespace before committing:

```powershell
git diff --check
```

## Current Feature Notes

- TODO data lives in `TaskItem`, `TaskDao`, and `TaskRepository`.
- Today task order is persisted through `sortOrder`.
- Due dates are optional and do not automatically move tasks between Today and Later.
- The Today page current task is the first unfinished Today task.
- The Today home-screen widget is read-only and lists unfinished Today tasks.
- Widget refresh is handled by `WidgetUpdatingTaskRepository`.

## Verification Expectations

Before claiming work is complete, run the narrowest relevant verification plus the broader build when the change affects shared app behavior.

For UI/task/widget changes, prefer:

```powershell
.\gradlew.bat :androidApp:testFossDebugUnitTest --console=plain
.\gradlew.bat :shared:compileAndroidMain --console=plain
.\gradlew.bat :androidApp:assembleFossDebug --console=plain
git diff --check
```

If an emulator or Android device is unavailable, say so clearly and provide the APK path after a successful build.
