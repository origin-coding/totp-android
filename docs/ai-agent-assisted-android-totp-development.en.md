# Building an Android TOTP App with an AI Agent: From Scope to v1.0.0

I recently completed a small native Android TOTP authenticator. It has the following features:

- It manages multiple accounts and supports importing them from a secret, an `otpauth://` URI, or a QR code.
- It supports SHA-1, SHA-256, SHA-512, six- and eight-digit codes, and configurable periods.
- It uses Room, Android Keystore, and AndroidX Biometric for local persistence, secret protection, and app locking.
- The final product is a signed APK distributed through GitHub Releases.

From the initial project scaffold to `v1.0.0`, the commit history spans seven calendar days. That speed did not come entirely from my familiarity with Android APIs. An AI Agent participated throughout the project, from requirements analysis and technology selection to incremental implementation, code review, and testing.

This article uses the project to document how I collaborated with an AI Agent to complete a real Android app. It also covers the Android, Jetpack Compose, security, and release work involved, along with several pitfalls that were easier to fall into than I expected.

Project repository: [origin-coding/totp-android](https://github.com/origin-coding/totp-android)

## Why I Chose TOTP

I wanted a reasonably small Android project with a clear definition of done, but one that involved more than UI practice. A TOTP authenticator fit those requirements well.

On the surface, it seems to do little more than “calculate a six-digit number every 30 seconds.” Turning that idea into an app people can install and use, however, touches many core concerns in modern Android development:

- implementing independently verifiable protocol logic in Kotlin;
- building a stateful UI with Jetpack Compose;
- managing screens and the back stack with Navigation 3;
- organizing persistent data with Room, Flow, and ViewModel;
- protecting sensitive data with Android Keystore;
- scanning QR codes with CameraX and ML Kit;
- implementing an app lock with AndroidX Biometric;
- writing JVM, repository, ViewModel, and Compose tests;
- building, signing, and publishing an APK with GitHub Actions.

At the same time, the project has a clear finish line: users on Android 12 and later can download and install the app, add accounts, generate correct codes, and keep their secrets on the device. Once those goals are met, the project can enter maintenance mode instead of growing indefinitely.

That clarity is especially important when collaborating with AI. Without explicit boundaries, an Agent can easily continue extending a project according to familiar “best practices”: adding more modules, introducing a dependency injection framework, building elaborate layers, or even planning cloud sync, multiplatform support, and an account system. None of those ideas are necessarily wrong, but none belongs in this project.

Before development began, it was therefore essential to define the project goals, optional supported features, and everything the project would explicitly leave out.

## Write Down the Boundaries Before Writing Code

The repository root contains an `AGENTS.md` file. It is neither marketing copy nor user documentation. It is a set of durable collaboration constraints.

The file includes both functional goals and explicit non-goals. For example, the app supports multiple standard TOTP accounts, QR-code imports, secure storage, and app locking. It does not support cloud sync, password management, passkeys, push authentication, desktop platforms, or Google Play distribution. It defaults to constructor injection instead of adopting Hilt or Koin preemptively. Its core protocol logic has no third-party TOTP runtime dependency. Tests use the official Android ecosystem and handwritten fakes, with MockK or Mockito reserved for a concrete need.

This file solves two problems.

First, it reduces repeated communication. I do not have to restate “do not add another module” or “do not introduce an entire Clean Architecture stack” in every task. The Agent can obtain stable project context as soon as it inspects the repository.

Second, it gives reviews a shared basis. When a proposal looks comprehensive but exceeds the project scope, I do not have to reject it on intuition alone. I can return to the agreed goals and ask: what existing problem does this complexity solve? If there is no answer, it should not be added.

The project eventually evolved from the original `:app` and `:core` modules into `:app`, `:core`, and `:data`. This happened because Room, Keystore, and DataStore had formed a clear set of Android data infrastructure that needed to be separated from the UI layer—not because “a three-layer architecture looks more professional.” The project stopped at three modules instead of continuing to split out feature, domain, or use-case layers.

This became the most important principle in the project:

> Introduce complexity only when it solves an existing correctness, maintainability, or platform-boundary problem.

## What Did the AI Agent Actually Do?

In this project, the AI Agent did more than complete code. Its main contributions included:

1. breaking the goals into development phases and acceptance criteria;
2. reviewing official Android approaches and existing dependencies to compare API choices;
3. reading the existing code before implementing focused changes;
4. adding tests for protocol logic, repositories, and ViewModels;
5. running tests, Lint, and builds, then fixing issues based on actual output;
6. reviewing module responsibilities, redundant code, and release configuration;
7. refining the README, signing instructions, and GitHub Actions workflows.

The human responsibilities did not disappear. They became more concentrated: setting the scope, making security tradeoffs, deciding whether to accept a dependency, checking the Agent’s conclusions, confirming UI behavior, and deciding when the app was ready to ship.

Over time, I arrived at a more precise division of labor:

| Area | What the AI Agent does well | What a human must own |
|---|---|---|
| Exploration | Search the codebase, enumerate options, locate relevant APIs | Decide which problems are worth solving |
| Implementation | Make focused changes with clear boundaries | Confirm that behavior matches product intent |
| Verification | Run tests, Lint, and builds, then analyze the output | Decide whether the tests prove the right things |
| Architecture | Identify dependency and responsibility problems | Control complexity and project scope |
| Security | Check common risks and platform capabilities | Accept risk and take responsibility for the release |

Treating an Agent as “someone who writes code automatically” makes it easy to focus on how many files it produces. Treating it as a collaborator shifts the useful question to whether each change has clear inputs, observable results, and a closed verification loop.

## Development Order: Prove the Core First, Then Connect Android

The project began with the algorithm layer: the `:core` module. It is pure Kotlin/JVM with no Android dependency and owns:

- Base32 encoding and decoding;
- HOTP and TOTP generation;
- SHA-1, SHA-256, and SHA-512 algorithm selection;
- six- and eight-digit codes;
- parsing, formatting, and validating `otpauth://` URIs.

This order works particularly well for protocol features. Code correctness should not be judged by installing the app and seeing whether it appears to work. It should first be verified quickly and deterministically on the JVM with RFC test vectors.

TOTP itself is not complicated. It calculates a counter from the current Unix time and the configured period, then calls HOTP:

```text
counter = floor(unixTimeSeconds / periodSeconds)
TOTP = HOTP(secret, counter)
```

HOTP uses the JCA-provided `Mac` and `SecretKeySpec` classes to calculate an HMAC, applies the dynamic truncation defined in RFC 4226, takes the result modulo `10^digits`, and pads it with leading zeroes. The project neither implements HMAC itself nor depends on a third-party TOTP library.

This led to an important collaboration rule: for a standardized protocol, I do not accept “the AI remembers roughly how it works” as a completion criterion. The implementation must be checked against RFC test vectors. The project tests include the RFC 4226 HOTP vectors, the RFC 6238 TOTP vectors for all three HMAC algorithms, period boundaries, custom periods, and invalid inputs.

AI can quickly produce an algorithm that looks plausible. Only authoritative test vectors can establish whether it is actually standards-compliant.

## The First Pitfall: `otpauth://` Is More Than Ordinary URL Parsing

A QR-code scan usually produces an `otpauth://` URI such as:

```text
otpauth://totp/Example:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Example
```

It is tempting to think of this as simply reading a few query parameters. In practice, most compatibility details are hidden in the URI semantics:

- the type is in the host and may be `totp` or `hotp`;
- the label is a single path segment that may contain both an issuer and an account name;
- when the issuer appears in both the label and the query, the two values must agree;
- the Base32 secret may be lowercase and may include or omit padding;
- algorithm names may use different letter cases, but formatting should emit a canonical form;
- `%xx` sequences must be decoded strictly as UTF-8;
- in an ordinary URI query, `+` is a literal plus sign and must not be converted to a space as it would be in an HTML form;
- duplicate parameters, invalid escapes, unsupported digit counts, non-positive periods, and issuer mismatches must be rejected.

The project ultimately avoided a query decoder with form semantics. Instead, it implemented strict percent encoding and decoding, with tests for preserving literal plus signs, Unicode round trips, duplicate parameters, invalid URI structures, and related cases.

AI is well suited to helping enumerate edge cases like these, but asking only for a “common implementation” is insufficient. A more effective request is:

> Review the parser against RFC 3986 and the commonly used Key URI Format. Enumerate the input rules, defaults, rejection conditions, and canonicalization rules. Extend the existing tests before changing the implementation.

That reframes the task from “write a parser” to “satisfy a reviewable set of compatibility constraints.”

## The Data Layer: Room Does Not Store Plaintext Secrets

After the protocol core was complete, the project added the `:data` module. The UI interacts only with `TotpAccountRepository`, which coordinates Room and the secret protector.

Account names, issuers, algorithms, digit counts, and periods can be persisted directly. TOTP secrets, however, never enter the database as plaintext. When an account is saved, its secret is first encrypted with AES-GCM. Room stores the ciphertext and the corresponding random IV. When a code must be generated, the repository decrypts the secret temporarily and invokes `:core`.

The encryption key is generated and held by Android Keystore. The application can use it only through cryptographic operations provided by the system and cannot export the raw key material. As a result, obtaining the database file alone is not sufficient to recover the TOTP secrets.

Two concepts are easy to confuse here:

- a TOTP secret is the application-level secret received from a service provider and used to calculate codes;
- the AES key in Keystore is a device-local protection key generated to encrypt TOTP secrets.

They are not the same thing. “Storing TOTP secrets in Keystore” is also not a general-purpose database strategy that scales without limit. This project uses one non-exportable Keystore AES key to encrypt any number of account secrets, then lets Room manage the ciphertext.

The app also disables Android backups. This is not an optional detail: the ciphertext in the Room database and the device-bound key in Keystore must be considered together. If only the ciphertext is restored to another device without the original protection key, the restored data cannot be decrypted. Since the project has no cloud-sync or cross-device recovery goal, disabling backups is the clearer choice.

It is worth emphasizing that this remains a learning project and has not undergone an independent security audit. Platform APIs, the absence of plaintext storage, and reduced permissions can narrow the risk surface, but using Keystore does not by itself make an application absolutely secure.

## The Hard Part of Compose Is State and Lifecycle

The UI uses Jetpack Compose and Material 3, with Navigation 3 for navigation. Routes are modeled as serializable types that implement `NavKey`. A `NavDisplay` creates screens for the account list, adding and editing accounts, changing secrets, scanning QR codes, and configuring settings based on the back stack.

Compose makes declarative UI straightforward, but the code screen is not a static list. Every account has a period countdown, and its code changes at the period boundary. Starting a continuous refresh job for every account in the list would waste resources and make state and lifecycle management more difficult.

The final implementation uses a smaller behavioral model: only the account expanded by the user generates and refreshes a code. When the user switches accounts or collapses the current one, the previous refresh coroutine is cancelled. On each iteration, the refresh loop reads the current time and uses `floorDiv` to determine whether the TOTP counter has changed. It recalculates the code only after entering a new period, while the seconds remaining can update on every tick.

The corresponding test name states the intended behavior directly: `only expanded account generates and refreshes a code`. The test does not inspect how many coroutines exist internally. It checks the observable result: only the expanded account generates and refreshes a code.

I used this style repeatedly when working with AI: specify what the user can observe, when it should happen, and what must not happen, instead of prescribing a particular private function call. Tests written at that level survive refactoring better and correspond more closely to real requirements.

Navigation 3 raised a similar concern. A screen’s ViewModel must retain state with its navigation entry instead of being recreated arbitrarily during recomposition. The project configures saved-state and ViewModel Store decorators for navigation entries so that screen and state lifecycles remain aligned with the back stack.

## QR-Code Scanning Does Not End When the Camera Is Connected

QR-code imports use CameraX, `LifecycleCameraController`, `MlKitAnalyzer`, and ML Kit Barcode Scanning. After a scan, the app accepts only `otpauth://totp/` content and sends the URI through the existing account editing flow for parsing and confirmation. It does not duplicate the saving logic on the scanner screen.

Recognizing a QR code is relatively simple. Most of the complexity lies in the surrounding state:

- a device may have no camera, so the camera feature cannot be declared as required;
- permission may be denied on the first request or re-enabled later in system settings;
- permission must be checked again after returning from settings;
- the analyzer, scanner, and camera controller must all be released correctly with the Compose lifecycle;
- the same QR code may appear in consecutive frames, but a successful result must be handled only once;
- scanning a normal URL or another type of QR code should show a clear error rather than sending the content directly into the account-saving flow.

The implementation uses `DisposableEffect` to bind and unbind the camera, clear the analyzer, and close the scanner when disposed. A lifecycle observer rechecks permission on `ON_RESUME`, while a state flag prevents duplicate handling of successful scans.

This demonstrates why “implement QR-code scanning” is not a precise enough task. Later, I preferred wording the request like this:

> Add a QR-code entry point that reuses the existing account import flow. Handle devices without a camera, the first permission request, opening settings after denial, returning from settings, duplicate frames, and non-TOTP QR codes. Camera resources must be released with the lifecycle.

The additional specificity does not design every line of code for the AI. It turns commonly overlooked product behaviors into acceptance criteria before implementation begins.

## App Locking and Secret Encryption Are Two Different Layers of Protection

The project also adds an optional app lock with AndroidX Biometric, allowing authentication with strong biometrics or the device screen lock.

It addresses a different problem from encrypting secrets in Room:

- Keystore encryption protects TOTP secrets at rest;
- the app lock prevents someone with access to an unlocked device from opening the app and viewing codes directly.

The app-lock setting is stored in DataStore. Enabling it requires a successful authentication before the setting is persisted. Once enabled, the app locks again after it has remained in the background for more than 30 seconds. `AppLockViewModel` models whether locking is enabled, whether the app is currently locked, whether authentication is in progress, and any error state. Coroutine tests cover behaviors such as keeping the current session unlocked immediately after enabling the feature and relocking after 30 seconds in the background.

Biometric authentication is not pushed into the repository. `BiometricPrompt` depends on an Activity and user interaction, so it belongs at the Android UI boundary. Setting persistence belongs in the data layer, while lock state and timeout logic belong in the ViewModel. Each part has a small responsibility, but the separation is enough to test the core state logic without a physical sensor.

## Testing Is Not a Patch Applied After the AI Finishes

If AI generates all the code and a human adds tests at the end, two problems appear easily: testing is postponed, or tests merely repeat the current implementation.

This project followed a more incremental verification process. RFC vectors and boundary tests were added as soon as the protocol layer was complete. Repository tests use handwritten fake DAOs and a fake `SecretProtector` to verify that secrets are encrypted when saved, decrypted when codes are generated, and left unchanged when metadata is updated. ViewModel tests use controllable time and test schedulers to verify refresh, save, delete, and app-lock behavior. Compose tests cover only critical interaction entry points.

The project does not repeat the same behavior at every layer. All TOTP algorithm boundaries are tested in `:core`, so account UI tests do not rerun every RFC vector. Room and Android Keystore integration are best checked on a device or emulator, while repository coordination can be verified quickly on the JVM with fakes.

Routine verification remains simple:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

Before an important release, I also connect an emulator or device to run instrumented tests and manually check account management, QR scanning, app locking, and other critical flows.

For an AI Agent, a successful command is not the only goal. It must also consider:

- whether a test verifies a specification or user-visible behavior;
- whether correct validation was weakened merely to make a test pass;
- whether a new test file duplicates an existing suite;
- whether a Lint warning indicates a compatibility issue, redundant code, or intentional behavior that should remain;
- whether the successful build produced the variant that will actually be released.

Before a change, I ask the Agent to find the test suite that owns the behavior and extend it where possible instead of creating a new regression-test file for every bug. This prevents the test directory from becoming fragmented as AI increases the volume of output.

## The Last Mile: Local Success Does Not Mean the App Can Ship

After the application features were complete, the project added two GitHub Actions workflows:

- CI runs unit tests, Android Lint, and a Debug APK build on pushes and pull requests;
- Release restores signing material for `v*` tags, reruns verification, builds the Release APK, verifies its signing certificate, generates a SHA-256 file, and publishes both through GitHub Releases.

This stage produced several of the most concrete pitfalls in the project. It also best illustrates why an Agent must iterate against actual output.

### `sdkmanager` Is Not Necessarily on `PATH`

The initial workflow invoked `sdkmanager` directly, but that assumption does not always hold in the GitHub-hosted runner environment. The revised workflow reads `ANDROID_HOME` or `ANDROID_SDK_ROOT`, checks `cmdline-tools/latest/bin/sdkmanager` first, and falls back to selecting an installed Command-line Tools version.

### Android SDK Package Names Are Not Always Obvious

The project uses API 37. The workflow initially tried to install `platforms;android-37`, but the actual package identifier was `platforms;android-37.0`. Experience with earlier releases cannot guarantee the right answer here; the runner output is the source of truth.

### The Gradle Wrapper Needs Execute Permission on Linux Runners

Local Windows builds always use `gradlew.bat`, so they do not expose a missing executable bit on `gradlew`. The problem surfaced only when `./gradlew` failed on the Ubuntu runner. The fix was to record the Wrapper’s execute permission correctly in Git.

### Do Not Make Narrow Assumptions About Tool Output

The release workflow uses `apksigner verify --print-certs` to check the APK signing certificate. The initial parser assumed that an output line began with an exact string. The real output differed from that assumption, so the workflow failed to extract the digest even though the signature was valid. The parser was changed to find the line containing `certificate SHA-256 digest:` and read its final field.

None of these fixes was complicated. They shared a common pattern: the initial script looked logically sound, and local static checks could not prove that it would fail. Only execution in the real environment exposed the hidden assumptions.

I therefore did not consider the workflow complete when the Agent finished writing it. Completion meant green GitHub Actions runs, installation of the signed APK, and verification of the certificate fingerprint.

## How I Describe Tasks to AI

This project made one lesson especially clear: the most important property of a prompt is neither politeness, length, nor adherence to a fixed template. It is the structure of the information.

An effective task usually contains five parts:

1. **Goal**: what the user will ultimately be able to do;
2. **Context**: the existing modules, code ownership, and relevant constraints;
3. **Boundaries**: what is explicitly out of scope and what must not be introduced;
4. **Acceptance criteria**: which tests, commands, or real behaviors will demonstrate completion;
5. **Working method**: inspect the existing implementation first, then make changes and report key tradeoffs.

For example, this request lacks enough information:

> Add secure storage to the app.

A more actionable version is:

> Implement local protection for TOTP secrets in `:data`. Use Android Keystore to hold a non-exportable AES-GCM key, and store only ciphertext and IVs in Room; account metadata must remain queryable. Do not introduce a third-party cryptography library or another module. Add repository tests verifying that new accounts have their secrets encrypted, secrets are decrypted when generating codes, and metadata edits do not accidentally replace secrets. Finally, run the relevant tests, Lint, and the Debug build.

The second version does not dictate every class name, but it defines the security boundary and observable results. The Agent retains room to choose an implementation, while being much less likely to interpret the task as “Base64-encode the string” or “build another security framework.”

I also adapt the communication style to the type of task.

### For Protocol and Security Logic, Ask for Evidence First

I ask for standards, official platform capabilities, or existing tests, and distinguish normative requirements from design choices. For TOTP, RFC test vectors are authoritative. For Android Keystore, Biometric, and app signing, platform APIs and behavior observed on real devices are more reliable than the Agent’s memory.

### For UI Work, Describe State Transitions

Instead of asking for “a good-looking account list,” describe the empty, expanded, loading, and error states, along with click behavior. Most Compose UI problems are ultimately about who owns state, when it changes, and whether work continues after the user leaves the screen—not whether the Agent knows how to draw a component.

### For Failures, Provide the Complete Output

Telling an Agent only that “CI failed” leaves it guessing. A failed step, the command, the error output, and the most recent changes give it enough information to locate the environmental assumption. For toolchain failures, I ask it to propose the smallest fix supported by the output, then rerun the workflow instead of changing several configuration areas at once.

### For Architecture Advice, Ask Which Concrete Problem It Solves

AI readily suggests common industry patterns, but “common” does not guarantee that a pattern fits this project. I ask: what actual problem appears if we do not add this abstraction? Has the existing constructor injection become insufficient? Does this dependency replace existing code, or merely add another way to write it?

Questions like these often reduce an ambitious refactoring proposal to a few direct changes.

## Where AI Is Most Valuable—and Most Dangerous

In this project, the AI Agent was most valuable in reducing the cost of context switching. It could read protocol code, Gradle configuration, Compose state, and CI scripts continuously; locate related files quickly; perform repetitive verification; and carry failure output into the next iteration. For a learning project, this allowed me to spend more time understanding tradeoffs and less time moving between documentation, commands, and boilerplate.

The greatest risk comes from the same capability: AI can produce a comprehensive, plausible, and even test-passing solution very quickly. That sense of completeness can lower a reviewer’s guard.

Common risks include:

- treating a remembered API version or environmental behavior as a current fact;
- adding layers the project does not need in pursuit of “architectural completeness”;
- covering only the happy path while overlooking permissions, lifecycle, and recovery scenarios;
- writing implementation-coupled tests that do not prove user behavior;
- equating the use of a security API with overall application security;
- changing unrelated code within a task and expanding the review surface.

The most effective controls are concrete engineering constraints, not a request to “be more careful”: small commits, explicit scope, existing test ownership, authoritative test vectors, real command output, Lint, builds, device verification, and final human review.

## What I Would Do Earlier If I Started Again

Looking back, several decisions would be worth making earlier.

First, define the completion criteria and non-goals as soon as possible. They help the AI and also help me resist feature creep. A TOTP app can easily grow into a password manager or synchronization service, turning a finishable learning project into a long-running platform effort.

Second, identify test sources before implementing the protocol. Once RFC test vectors are established as the final authority, the Agent follows a more stable implementation path and avoids the circular dependency of validating one library with another library’s output.

Third, distinguish the local development environment from CI from day one. This project uses China-hosted Gradle and Maven mirrors locally, while GitHub Actions uses the official repositories and Gradle distribution. Encoding that distinction explicitly in configuration is more reliable than switching it manually before a release.

Fourth, schedule device verification earlier. JVM and Compose tests cover a great deal of logic, but camera permissions, `BiometricPrompt`, round trips through system settings, background app-lock timeouts, and APK update signing ultimately need to be observed on an Android device or emulator.

Fifth, conduct a subtractive review at the end of every phase: which classes only forward calls? Which parameters never vary? Which tests are redundant? Which dependencies are no longer used? AI is highly capable of adding material, and it should also be asked explicitly to help remove it.

## Conclusion: AI Accelerates Iteration; Engineering Judgment Remains Human

The project ultimately reached its intended completion state. It generates standards-compliant TOTP codes, manages and scans multiple accounts, protects secrets locally with Room and Android Keystore, guards app access with device authentication, and publishes a signed APK through an automated release process.

The AI Agent contributed substantially. It reduced the time spent searching for information, implementing features, adding tests, and resolving toolchain issues. It also enabled me to explore several important areas of modern Android development within a relatively short period.

What ultimately determined the quality of the project were familiar engineering practices: defining the problem, controlling scope, establishing verifiable standards, reading failure output carefully, checking edge cases, understanding the security model, and taking responsibility for the final deliverable.

At this stage, an AI Agent is best treated as an engineering collaborator with strong execution skills and a large context window. Engineering judgment and final responsibility remain with the developer. Effective collaboration does not depend on finding a magical prompt. It depends on expressing engineering intent clearly: what problem needs to be solved, why this approach was chosen, what is explicitly out of scope, and what evidence will establish that the work is complete.

AI amplifies execution, and it also amplifies ambiguity. Without clear goals, boundaries, and acceptance criteria, it merely gets a project to “usable” faster. When those foundations are explicit, it can turn human engineering judgment into reliable, verifiable, and genuinely shippable results more quickly.
