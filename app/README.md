# Allium Tutor Application

This directory contains the Kotlin Multiplatform vertical slice governed by:

- `../docs/decisions/0001-story-driven-tutoring.md`
- `../docs/decisions/0002-kmp-application-platform.md`
- `../layer/allium/lesson-lifecycle.allium`

The `shared` KMP library contains lesson behaviour, shared Compose presentation, resources, tests, and the Kotlin framework consumed by iOS. Runnable platform applications remain separate: `desktopApp` owns the JVM entry point, desktop persistence adapter, and distribution configuration, while `iosApp` is the Swift/Xcode host. Android and web follow after the Apple/desktop lifecycle is understood.

## Lesson package

The authored temporal-overdue lesson lives at:

```text
shared/src/commonMain/composeResources/files/lessons/temporal-overdue.json
```

The package contains lifecycle copy, learner-facing actions, starter text, and deterministic repair and transfer requirements. Its schema and validation rules are documented in [`docs/lesson-packages.md`](docs/lesson-packages.md).

Learner progress is serialized independently of lesson content. Desktop stores it in Java Preferences; iOS stores it in `NSUserDefaults`. Saved state is accepted only when its lesson ID, package revision, lifecycle invariants, and evaluation state are consistent.

## Desktop

```bash
cd app
./gradlew :desktopApp:run
```

Run shared tests:

```bash
./gradlew :shared:desktopTest
```

Build the macOS application and DMG:

```bash
./gradlew :desktopApp:packageDmg
```

Gradle resolves an Adoptium JDK 21 toolchain for packaging. Local packages are unsigned; release distribution requires Apple signing and notarisation credentials in the native distribution configuration.

## iOS

Select the full Xcode developer directory and open the project:

```bash
sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
open app/iosApp/iosApp.xcodeproj
```

The Xcode build phase invokes:

```text
:shared:embedAndSignAppleFrameworkForXcode
```

Development signing uses `TEAM_ID` from `iosApp/Configuration/Config.xcconfig` with Xcode's automatic signing. Verify the shared framework with:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

## Scope

The application develops platform capability through one complete lesson vertical slice. It exercises:

- the full story-driven lesson lifecycle;
- explicit application ownership;
- shared state and deterministic evaluation;
- real multiline editing;
- compact and expanded presentation; and
- JVM/iOS framework boundaries.

The Bash Alliumlings runner provides terminal exercises and deterministic CLI checks.
