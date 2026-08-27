# TOTP for Android

[English](README.md) | [简体中文](README.zh-CN.md)

A small, native Android authenticator for generating standards-compliant time-based one-time passwords (TOTP).

This repository is a focused learning project for modern Android development, Android security APIs, automated testing, and APK distribution. It is currently being prepared for its first public release.

## Project goals

The project aims to provide a compact and understandable TOTP authenticator that:

- supports the common TOTP formats used by standard authenticator applications;
- keeps account data and secrets on the device;
- uses Android platform and Jetpack APIs where practical;
- demonstrates a simple, maintainable architecture without unnecessary framework layers; and
- can be built, tested, signed, and distributed as an APK through GitHub Releases.

The scope is intentionally narrow. This is not an identity platform, password manager, cloud synchronization service, passkey provider, or proprietary authentication client.

## AI-assisted development

This project was developed with guidance from AI agents throughout requirements analysis and implementation. AI agents were used as collaborative tools for decomposing requirements, evaluating architectural and API choices, guiding incremental implementation, and reviewing code and tests.

The project remains human-directed: scope, product decisions, security trade-offs, and changes to the repository are reviewed by the maintainer. AI suggestions are checked against the relevant standards, official Android APIs, source review, and automated tests.

## Features

- Store and manage multiple TOTP accounts.
- Add accounts from a setup key, an `otpauth://` URI, or a QR code.
- Edit account metadata and TOTP parameters, replace secrets, and delete accounts.
- Generate 6-digit or 8-digit codes using SHA-1, SHA-256, or SHA-512.
- Support configurable TOTP periods.
- Display the remaining validity period and copy a generated code to the clipboard.
- Encrypt TOTP secrets before storing them in the local Room database.
- Protect access to the application with strong biometrics or the device screen lock.
- Operate locally without an Internet permission or a backend service.

The TOTP implementation is based on RFC 2104, RFC 4226, RFC 6238, RFC 4648, RFC 3986, and the commonly used Key URI Format (`otpauth://`). Cryptographic operations use JCA APIs rather than custom cryptographic primitives or a third-party TOTP runtime library.

## Architecture

The application uses a small three-module architecture with explicit dependencies:

```text
Compose UI / ViewModel
          |
          v
      Repository
          |
          v
 Room / Android Keystore / DataStore
```

### `:core`

A platform-independent Kotlin/JVM module containing:

- Base32 encoding and decoding;
- HOTP and TOTP generation;
- OTP domain models and validation; and
- `otpauth://` URI parsing and formatting.

The module has no Android dependency and is tested entirely on the JVM, including RFC-backed test vectors.

### `:data`

An Android library containing:

- the Room 3 database, entities, and DAO;
- the TOTP account repository;
- Android Keystore-backed secret protection; and
- DataStore-backed application settings.

TOTP secrets are encrypted with an AES-GCM key generated and held by Android Keystore before the encrypted payload is persisted by Room.

### `:app`

The Android application containing:

- the Jetpack Compose and Material 3 UI;
- Navigation 3 routes;
- ViewModels and lifecycle integration;
- QR scanning with CameraX and ML Kit; and
- application locking with AndroidX Biometric.

Dependencies are assembled with constructor injection and a small application-level container. The project deliberately does not use a dependency injection framework.

## Technology

- Kotlin and Gradle Kotlin DSL
- Jetpack Compose and Material 3
- Navigation 3
- Coroutines, Flow, StateFlow, and ViewModel
- Room 3 and DataStore
- Android Keystore and AndroidX Biometric
- CameraX and ML Kit barcode scanning
- JUnit, AndroidX Test, and Compose testing APIs

The minimum supported version is Android 12 (`minSdk 31`). The current project configuration compiles against Android API 37.

## Getting started

### Requirements

- A current stable version of Android Studio compatible with the Android Gradle Plugin used by the project
- The Gradle JDK bundled with Android Studio
- Android SDK Platform 37
- An Android 12 or newer device or emulator

The Gradle Wrapper is included, so a separate Gradle installation is not required.

### Open and run the project

1. Clone the repository and open its root directory in Android Studio.
2. Let Android Studio install any missing SDK components and complete the Gradle sync.
3. Select the `app` run configuration.
4. Choose an Android 12 or newer device or emulator and run the application.

To build a debug APK from a terminal on macOS or Linux:

```bash
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

The APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

You can also install the debug build on a connected device with:

```bash
./gradlew installDebug
```

Use `.\gradlew.bat` instead of `./gradlew` when running Gradle commands from Windows PowerShell.

## Development and testing

Run the JVM unit tests:

```bash
./gradlew test
```

Run Android Lint:

```bash
./gradlew lint
```

Build the debug APK:

```bash
./gradlew assembleDebug
```

With an emulator or device connected, run the instrumented and Compose tests:

```bash
./gradlew connectedDebugAndroidTest
```

The normal local and CI verification sequence is:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

Before an important release, the instrumented tests and the critical account, QR scanning, and application-lock flows should also be verified on an Android device or emulator.

## Security notes

- TOTP secrets are encrypted before database persistence using AES-GCM and a non-exportable Android Keystore key.
- Android application backup is disabled to avoid separating encrypted data from its device-bound key.
- The application requests camera access only for QR code scanning and does not request Internet access.
- Optional application locking accepts a strong biometric or the configured device credential.
- Signing keys and local signing configuration must never be committed to the repository.

This is an educational project and has not undergone an independent security audit. Review the implementation and understand the risks before using it to protect critical accounts.

## Distribution

Continuous integration runs the unit tests, Android Lint, and the debug APK build on pushes and pull requests. Version tags trigger a separate workflow that builds and verifies a signed APK, generates a SHA-256 checksum, and publishes the files through GitHub Releases.

Release signing material is kept outside the repository. See [Release signing](docs/release-signing.md) for the keystore, local build, and GitHub Actions secret setup.

Google Play distribution and other platforms are outside the scope of this project.

## License

This project is released under the [MIT License](LICENSE).
