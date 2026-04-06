# SMSNode Client Application

**Русский:** [`README.md`](README.md)

## Purpose

Client application for the SMSNode backend: sending and receiving SMS, managing dialogs, contacts, and settings related to GSM gateways.

## Target platforms

Implemented with Kotlin Multiplatform (Compose Multiplatform):

| Platform | Module |
|----------|--------|
| Android | `composeApp` |
| iOS | `composeApp` + Xcode project under `iosApp` |
| Desktop | JVM |
| Web | WasmJS |

## Functionality

**UI.** Adaptive layout: bottom navigation on narrow viewports, side navigation on wide screens; light and dark themes (Material 3); content max width capped at 840 dp.

**User.** Registration and sign-in; dialog list with filters (all / unread / incoming / outgoing); chat with periodic server polling; message templates (including global templates defined by administrators); assigned SIM cards with custom labels; contacts, contact groups, bulk SMS to groups.

**Administrator.** Gateways: create, read, update, delete, connectivity check, conflict-aware deletion (optional forced delete); channel discovery from keepalive data. Users: roles, deactivation, SIM assignment. Registration mode (open / closed / approval-based). Pending registration handling. System-wide message log for auditing.

## Repository layout

- `composeApp/src/commonMain` — shared code (UI, ViewModel, Ktor HTTP client, data models).
- `composeApp/src/androidMain`, `jvmMain`, `iosMain`, `jsMain`, `wasmJsMain` — platform-specific code and entry points.
- `iosApp` — Xcode project for iOS builds.

## Build and run

**Android**

```shell
./gradlew :composeApp:assembleDebug
```

```shell
.\gradlew.bat :composeApp:assembleDebug
```

**Desktop (JVM)**

```shell
./gradlew :composeApp:run
```

```shell
.\gradlew.bat :composeApp:run
```

**Web (WasmJS, development)**

```shell
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

```shell
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
```

References: [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html), [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform).
