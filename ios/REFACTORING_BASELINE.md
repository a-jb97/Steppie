# iOS Maintainability Refactoring Baseline

## Purpose

This document records the build and test baseline before the
`refact/ios-maintainability` refactoring changes begin. Refactoring results must
preserve the behavior defined by the product, data, backup, accessibility,
localization, test, and release contracts.

No application or test source code was changed while establishing this
baseline.

## Recorded State

- Recorded: 2026-07-29 (Asia/Seoul)
- Branch: `refact/ios-maintainability`
- Commit: `4bd5bae558b8df376052f8cd34c0c5dd1fddcc78`
- Xcode: 26.6 (`17F113`)
- Simulator: iPhone 17 Pro
- Simulator runtime: iOS 26.5 (`23F77`)
- App deployment target: iOS 18.6

## Automated Baseline

### App Build

```sh
xcodebuild \
  -project Steppie/Steppie.xcodeproj \
  -scheme Steppie \
  -sdk iphonesimulator \
  -derivedDataPath /private/tmp/steppie-maintainability-build \
  build
```

Result: passed (`** BUILD SUCCEEDED **`).

Observed diagnostic:

- App Intents metadata extraction was skipped because the target has no
  `AppIntents.framework` dependency. This did not fail the build.

### Unit Tests

```sh
xcodebuild \
  -project Steppie/Steppie.xcodeproj \
  -scheme Steppie \
  -destination 'platform=iOS Simulator,id=AE089B4A-EA34-446D-B5B7-140E562FB278' \
  -derivedDataPath /private/tmp/steppie-maintainability-tests \
  -only-testing:SteppieTests \
  test
```

Result: passed (`** TEST SUCCEEDED **`). All selected `SteppieTests` and
`TutorialCoordinatorTests` cases passed.

### First-Launch Tutorial UI Test

```sh
xcodebuild \
  -project Steppie/Steppie.xcodeproj \
  -scheme Steppie \
  -destination 'platform=iOS Simulator,id=AE089B4A-EA34-446D-B5B7-140E562FB278' \
  -derivedDataPath /private/tmp/steppie-maintainability-tests \
  -only-testing:SteppieUITests/SteppieUITests/testFirstLaunchTutorialAppearsAndAdvances \
  test
```

Result: passed (`** TEST SUCCEEDED **`).

### Launch UI Test

```sh
xcodebuild \
  -project Steppie/Steppie.xcodeproj \
  -scheme Steppie \
  -destination 'platform=iOS Simulator,id=AE089B4A-EA34-446D-B5B7-140E562FB278' \
  -derivedDataPath /private/tmp/steppie-maintainability-tests \
  -only-testing:SteppieUITests/SteppieUITestsLaunchTests/testLaunch \
  test
```

Result: passed (`** TEST SUCCEEDED **`). The launch test passed in all four
executed configurations.

Observed environment diagnostics:

- Xcode repeatedly reported that no LLDB debugger version was available while
  capturing launch parameters.
- One cloned simulator runner launch reported `NSMachErrorDomain Code=-308`
  after the test result had been produced.
- These diagnostics did not fail the command or any executed launch test.

## Manual Regression Baseline

The automated commands above do not fully verify the following behavior:

- iPhone portrait and iPad portrait/landscape Split View layouts
- VoiceOver reading order, labels, hints, and focus
- Dynamic Type at supported accessibility sizes
- Reduce Motion behavior
- Minimum 80x80pt touch targets
- Notification delivery and routing on a device
- Audible TTS, physical haptics, photo capture, and photo-library permissions
- PIN and recovery-code flows involving system authentication behavior
- Backup interoperability with previously exported real-world files
- SwiftData migration from previously shipped persistent stores

These items remain required acceptance checks. They must be exercised when a
refactoring stage changes the corresponding UI, state, service, backup, or
persistence boundary.

## Comparison Rule

After each commit unit, run the smallest relevant build and test set. Run the
complete automated baseline after the planned Guardian UI, Guardian state,
Child Routine policy, and SwiftData cleanup checkpoints. A result is a
regression when it introduces a new build or test failure, changes an existing
user flow or accessibility behavior, or breaks data and backup compatibility
relative to this recorded state.
