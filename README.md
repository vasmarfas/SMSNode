# SMSNode Client Application

This is a Kotlin Multiplatform project targeting Android, iOS, Web (WasmJS), and Desktop (JVM). It serves as the primary user interface for the SMSNode backend, allowing users to send, receive, and manage SMS messages using hardware GSM gateways.

## Features

### 🔹 Modern & Adaptive UI
- **Responsive Layout:** Automatically switches between a bottom `NavigationBar` on mobile devices (width < 600dp) and a side `NavigationRail` on tablets/desktops to optimize screen space.
- **Dynamic Theming:** Fully supports system Dark and Light modes based on Material 3 guidelines.
- **Optimized Content Width:** Content is centered and bounded by a maximum width (840dp) to prevent unreadable stretching on ultra-wide monitors.

### 🔹 Core User Functionality
- **Seamless Onboarding:** Users are automatically logged in immediately after successful registration. No need to re-enter credentials.
- **Dialogs & Chat:** WhatsApp/Telegram-style messenger interface.
    - **Filters:** Easily filter dialogs by "All", "Unread", "Incoming", or "Outgoing".
    - **Polling:** Real-time message updates without needing to pull-to-refresh.
- **SMS Templates:**
    - Save frequently used messages as templates.
    - Insert a template into the chat box with one click.
    - **Global Templates:** Administrators can create and edit templates that are available to all users.
- **My SIMs:** View SIM cards assigned to your account and attach custom text labels (e.g., "Work", "Personal") to easily identify them.
- **Contacts & Groups:**
    - Save and edit contacts. External phone numbers are automatically replaced with contact names throughout the app.
    - Organize contacts into groups for easy management.
    - **Mass Sending:** Send SMS messages to an entire group of contacts at once, with the ability to choose a specific SIM card or use automatic load balancing.

### 🔹 Administrator Panel
- **Gateway Management:** Add, edit, test (ping), and delete GSM gateways (GOIP, Skyline, etc.).
    - **Safe Deletion:** To prevent database integrity issues, attempting to delete a gateway with active SIM cards returns an error. The app then offers a "Force Delete" option to cascade delete the gateway and its resources.
    - **Auto-Discovery:** Automatically detect newly connected SIM channels based on UDP keepalive packets and add them with one click.
- **User Management:** Change user roles, deactivate accounts, view Telegram IDs, and assign/revoke specific SIM cards.
- **Registration Mode:** Switch between "Open", "Closed", and "Semi-Open (Approval required)" directly from the app.
- **Pending Approvals:** Approve or reject user registrations when in semi-open mode.
- **Global Message Log:** View all SMS traffic across the entire system for auditing.

---

## 🛠 Project Structure

* [/composeApp](./composeApp/src) contains the shared Compose Multiplatform UI and logic.
  * [commonMain](./composeApp/src/commonMain/kotlin) is where 99% of the code lives (ViewModel, UI screens, Ktor HTTP Client, Models).
  * Platform-specific folders (e.g., `androidMain`, `jvmMain`, `wasmJsMain`) contain platform bindings and entry points.
* [/iosApp](./iosApp/iosApp) contains the Xcode project for iOS deployment.

## 🚀 Build and Run

### Android
To build and run the development version of the Android app, use the run widget in Android Studio / IntelliJ or run:
```shell
# macOS/Linux
./gradlew :composeApp:assembleDebug

# Windows
.\gradlew.bat :composeApp:assembleDebug
```

### Desktop (JVM)
To run the Desktop version locally (Windows/macOS/Linux):
```shell
# macOS/Linux
./gradlew :composeApp:run

# Windows
.\gradlew.bat :composeApp:run
```

### Web (WasmJS)
To run the web app in a browser (supports modern browsers for faster execution):
```shell
# macOS/Linux
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Windows
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
```

---

*Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) and [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform).*
