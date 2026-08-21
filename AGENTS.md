# TOTP Android — Agent Guidelines

## Project

This repository contains a small native Android TOTP authenticator.

The project is primarily a learning project for modern Android development, Android security APIs, testing, and APK distribution.

The project has a fixed scope and a clear completion point. Do not expand it into a general authentication platform.

## Goals

The final application should:

* Support multiple TOTP accounts.
* Generate standards-compliant TOTP codes.
* Support SHA-1, SHA-256, and SHA-512.
* Support 6-digit and 8-digit codes.
* Support configurable TOTP periods.
* Import standard `otpauth://` URIs.
* Import accounts from QR codes.
* Allow accounts to be added, edited, and deleted.
* Persist accounts locally with Room.
* Protect TOTP secrets using Android Keystore.
* Support biometric app locking.
* Provide a small settings surface where required.
* Be distributed as a signed APK through GitHub Releases.

## Non-goals

Do not add the following unless the project scope is explicitly changed:

* Google Play distribution.
* iOS, Web, Desktop, Wear OS, or Kotlin Multiplatform support.
* Backend services.
* User accounts.
* Cloud synchronization.
* Cross-device synchronization.
* Password-manager functionality.
* Passkeys.
* Push authentication.
* Proprietary authenticator protocols.
* Google or Microsoft account integration.
* Home-screen widgets.
* Vendor-specific Android APIs.
* OEM background-service workarounds.
* Foreground services for OTP refresh.
* Internationalization.
* Complex theming systems.
* Feature-per-module architecture.
* Clean Architecture ceremony or unnecessary abstraction layers.
* Hilt, Koin, or another DI framework unless manual DI becomes demonstrably insufficient.
* BouncyCastle or another cryptography provider without a concrete platform limitation.
* Third-party TOTP runtime libraries.
* PyOTP as a runtime, test, or CI dependency.

## Technology

Use the Android ecosystem and official Android recommendations whenever practical.

Primary stack:

* Kotlin
* Gradle Kotlin DSL
* Jetpack Compose
* Material 3
* Navigation 3
* Kotlin Coroutines
* Flow / StateFlow
* ViewModel
* Room 3
* DataStore
* Android Keystore
* AndroidX Biometric
* CameraX
* AndroidX Test
* Compose Testing
* Android Lint
* GitHub Actions

Use the Android Studio / Android Gradle Plugin recommended JDK configuration. Do not require the developer's system JDK version.

Minimum Android version:

* `minSdk = 31`

Use the current appropriate `compileSdk` and `targetSdk` when the project is created or upgraded.

## Modules

The initial project contains:

```text
:app
:core
```

### `:core`

Keep this module Android-independent whenever practical.

It owns:

* HOTP/TOTP algorithms
* Base32 handling
* OTP domain models
* `otpauth://` parsing and formatting
* RFC-related validation and pure logic

The module should be testable entirely on the JVM.

### `:app`

Initially owns all Android-specific functionality:

* Compose UI
* Navigation
* ViewModels
* Room integration
* Android lifecycle integration

During Phase 2, extract Android data infrastructure into:

```text
:data
```

### `:data`

When introduced, it owns:

* Room database and DAO implementations
* Repositories
* Android Keystore-backed secret protection
* DataStore

The final intended module structure is:

```text
:app
:core
:data
```

Do not add additional modules without a concrete need.

## Architecture

Prefer simple explicit dependencies.

Default to constructor injection and explicit parameter passing.

A small application-level container may be used to assemble dependencies.

Do not introduce a DI framework preemptively.

Kotlin context parameters may be experimented with locally when they materially improve clarity, but they are not an architectural requirement.

Prefer:

```text
UI / ViewModel
      ↓
Repository
      ↓
Room / Keystore / DataStore
```

Avoid unnecessary layers such as use cases, interactors, managers, factories, or providers when they only forward calls.

## TOTP implementation

Implement HOTP/TOTP protocol logic in `:core`.

Follow the relevant standards, especially:

* RFC 2104 — HMAC
* RFC 4226 — HOTP
* RFC 6238 — TOTP
* RFC 4648 — Base32
* RFC 3986 — URI syntax

Support the commonly used `otpauth://` URI format.

Do not implement cryptographic primitives manually.

Use platform/JCA APIs such as:

```text
javax.crypto.Mac
javax.crypto.spec.SecretKeySpec
```

Use Android Keystore for Android-specific key protection.

PyOTP may be read during development as a reference implementation or used manually for result comparison, but it must not become part of the project dependency graph or automated test environment.

RFC test vectors are the authoritative automated test source.

## Testing

Follow the Android documentation and official Android testing ecosystem.

Use:

* JUnit for JVM tests.
* `kotlinx-coroutines-test` for coroutine tests.
* AndroidX Test for Android instrumented tests.
* Compose testing APIs for Compose UI tests.
* Room-supported testing approaches for database behavior.

Prefer handwritten fakes over mocking frameworks.

Do not add MockK or Mockito unless a concrete testing problem justifies them.

### JVM tests

Run frequently and in CI.

They should cover, at minimum:

* HOTP
* TOTP
* RFC test vectors
* Base32
* `otpauth://`
* coroutine-based pure/application logic where applicable

### Instrumented tests

Use a real Android device or emulator.

Use them selectively for:

* Room behavior
* Android framework integration
* critical Compose UI flows
* critical navigation flows

Instrumented tests are not required in GitHub Actions by default.

Before important releases, run them manually with an emulator or connected Android device.

## CI/CD

GitHub Actions CI should remain lightweight.

CI should normally run:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

Do not add an Android emulator to CI unless the instrumented test suite becomes large enough to justify the additional complexity.

Release builds should produce a signed APK and publish it through GitHub Releases.

Signing keys must never be committed to the repository.

Local development may use China-hosted Gradle and Maven mirrors. GitHub Actions should prefer official Gradle and Maven repositories; switch the Gradle Wrapper URL before invoking Gradle and select dependency/plugin repositories based on the CI environment.

## Lint and formatting

Use Android Lint:

```bash
./gradlew lint
```

Formatting is primarily handled by Android Studio and project editor settings.

Do not add ktlint, detekt, Spotless, or similar tools unless a concrete problem appears.

## Language

Use English for:

* source code
* identifiers
* UI text
* comments
* README and repository documentation
* commits
* issues and pull requests

The application does not implement i18n.

Blog posts about the project may be written in Chinese first, with an optional English version.

## Development principles

Prefer Android platform and Jetpack APIs over third-party libraries.

Add a dependency only when it solves a concrete problem that is not reasonably handled by the platform or existing project dependencies.

Prefer straightforward code over speculative abstractions.

Do not create infrastructure for hypothetical future requirements.

Do not increase module count, abstraction depth, or dependency count merely to imitate large Android applications.

When choosing between:

```text
more architecture
```

and:

```text
a simpler implementation sufficient for this project
```

prefer the simpler implementation unless there is a demonstrated maintenance or correctness problem.

## Completion

The project is considered complete when an Android 12+ user can:

1. Download a signed APK from GitHub Releases.
2. Install and launch the application.
3. Add or scan multiple standard TOTP accounts.
4. Generate correct TOTP codes.
5. Store secrets securely on the device.
6. Protect access using supported Android biometric/device authentication.
7. Use the core application flows reliably.

The project should also have:

* RFC-backed unit tests.
* appropriate Room and Compose tests.
* Android Lint passing.
* GitHub CI.
* signed release automation.
* a concise README.

After `v1.0.0`, default to maintenance mode.

New features should only be added when they fix a real usability, compatibility, security, or correctness problem within the existing scope.
