# AGENTS.md

## Project Overview

Steppie is a routine visualization app for users with developmental disabilities and users on the autism spectrum. The project develops native iOS/iPadOS and Android apps in parallel.

Primary technical direction:

- iOS / iPadOS: SwiftUI, SwiftData, MVVM, `@Observable`
- Android: Kotlin, Jetpack Compose, Room, MVVM, StateFlow, AAC ViewModel
- Shared source of truth: common contract documents under `docs/`

## Repository Structure

- `docs/`: common product requirements, data contracts, design tokens, accessibility standards, test scenarios, release criteria, and planning references.
- `ios/`: native iOS/iPadOS implementation.
- `android/`: native Android implementation.

The documents under `docs/` define the shared contracts that both platform implementations must follow. iOS and Android behavior, data shape, accessibility expectations, localization rules, and design tokens should be derived from these documents instead of platform-specific assumptions.

## Required Reading Before Work

Before making changes, read the documents relevant to the requested task. Prefer document path references over copying document content into code or prompts.

Core documents:

- `docs/product-spec.md`
- `docs/data-contract.md`
- `docs/design-tokens.md`
- `docs/accessibility-checklist.md`
- `docs/test-scenarios.md`
- `docs/localization-contract.md`
- `docs/backup-contract.md`
- `docs/release-checklist.md`

Planning reference:

- `docs/planning/steppie_planning_document.md`

## Docs To Read By Task Type

- Product behavior, user flow, screen logic: read `docs/product-spec.md` and `docs/test-scenarios.md`.
- Data model, persistence, import/export, backup behavior: read `docs/data-contract.md` and `docs/backup-contract.md`.
- UI styling, spacing, typography, color, component tokens: read `docs/design-tokens.md`.
- Accessibility behavior or interaction sizing: read `docs/accessibility-checklist.md`.
- Localization, copy keys, language behavior: read `docs/localization-contract.md`.
- Release readiness, QA, acceptance checks: read `docs/release-checklist.md` and `docs/test-scenarios.md`.
- Planning or scope clarification: read `docs/planning/steppie_planning_document.md` plus the relevant contract documents above.

## iOS Work Rules

When working in the iOS worktree or when the user asks for iOS work:

- Modify only `ios/`.
- Treat `docs/` as read-only reference material.
- Do not modify `android/`.
- Use SwiftUI, SwiftData, MVVM, and `@Observable`.
- Support iPhone portrait orientation.
- Support iPad portrait and landscape Split View.
- Follow Dynamic Type, VoiceOver, Reduce Motion, and minimum 80x80pt touch target requirements.
- Keep implementation aligned with the shared contracts in `docs/`.
- Run relevant iOS build/tests when feasible and report the result.

## Android Work Rules

When working in the Android worktree or when the user asks for Android work:

- Modify only `android/`.
- Treat `docs/` as read-only reference material.
- Do not modify `ios/`.
- Use Kotlin, Jetpack Compose, Room, MVVM, StateFlow, and AAC ViewModel.
- Support Android phone portrait orientation.
- Support Android tablet portrait and landscape Split View.
- Follow Font Scale, TalkBack, animation scale 0, and minimum 80x80pt touch target requirements.
- Keep implementation aligned with the shared contracts in `docs/`.
- Run relevant Android build/tests when feasible and report the result.

## Docs Work Rules

When changing common requirements, data contracts, design tokens, accessibility standards, localization rules, backup rules, test scenarios, or release criteria:

- Modify only `docs/`.
- Do not change platform code in `ios/` or `android/` in the same task by default.
- Keep docs changes focused on shared contracts and cross-platform behavior.
- If platform implementation changes are also needed, do them in a separate follow-up task unless the user explicitly requests a combined change.

## Git And Worktree Rules

Respect each worktree's intended scope:

- `Steppie`: `main` branch, docs and merge management.
- `Steppie-ios`: iOS feature branch work.
- `Steppie-android`: Android feature branch work.

Do not mix platform scopes across worktrees. Keep iOS changes in the iOS worktree, Android changes in the Android worktree, and shared contract changes in the main/docs worktree unless the user gives explicit instructions otherwise.

Never revert user changes unless explicitly requested. Before committing, review the diff and ensure unrelated files are not included.

## Short Role Activation Prompts

The user may activate a platform-specific work mode with a short Korean prompt.

### iOS 작업 규칙으로 진행해

When the user says `iOS 작업 규칙으로 진행해`, treat the current session as an iOS worktree session unless the user later changes the scope.

Apply these rules by default:

- Follow all rules in `iOS Work Rules`.
- Read relevant `docs/` files before implementation.
- Treat `docs/` as read-only reference material.
- Modify only `ios/`.
- Do not modify `android/`.
- Use SwiftUI, SwiftData, MVVM, and `@Observable`.
- Preserve iPhone portrait support.
- Preserve iPad portrait and landscape Split View support.
- Respect Dynamic Type, VoiceOver, Reduce Motion, and minimum 80x80pt touch targets.
- Run relevant iOS build/tests when feasible and report results.

### Android 작업 규칙으로 진행해

When the user says `Android 작업 규칙으로 진행해`, treat the current session as an Android worktree session unless the user later changes the scope.

Apply these rules by default:

- Follow all rules in `Android Work Rules`.
- Read relevant `docs/` files before implementation.
- Treat `docs/` as read-only reference material.
- Modify only `android/`.
- Do not modify `ios/`.
- Use Kotlin, Jetpack Compose, Room, MVVM, StateFlow, and AAC ViewModel.
- Preserve Android phone portrait support.
- Preserve Android tablet portrait and landscape Split View support.
- Respect Font Scale, TalkBack, animation scale 0, and minimum 80x80pt touch targets.
- Run relevant Android build/tests when feasible and report results.

## Short Follow-up Prompt Examples

```text
iOS 작업 규칙으로 진행해. Routine 모델을 구현해.
```

```text
Android 작업 규칙으로 진행해. 포커스 화면을 구현해.
```

## Build, Test, And Reporting Rules

After code changes, run the most relevant build and test commands that are available for the affected platform. In the final report, include:

- What changed.
- Which docs were used as references.
- Which build/test commands were run.
- Whether each command passed or failed.
- Any failures, skipped checks, or environment limitations.

If no code changed, state that build/test commands were not run and why.

## Files That Must Not Be Committed

Do not commit generated files, build artifacts, secrets, signing files, or local environment files.

Examples include:

- Build outputs and caches such as `.build/`, `DerivedData/`, `build/`, `.gradle/`
- Local IDE state such as `.idea/workspace.xml`, `xcuserdata/`, `.DS_Store`
- Local environment files such as `local.properties`, `.env`, `.env.*`
- Signing materials such as keystores, provisioning profiles, certificates, and private keys
- Generated files unless they are intentionally part of the reviewed source contract
