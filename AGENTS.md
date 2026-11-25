# MonoDo — Agent Context & Operational Guidelines

This document serves as the single source of truth for AI agents and developers working on the **MonoDo** codebase. Read every rule carefully before making any code, Gradle, Manifest, or workflow changes.

---

## 1. Project Overview & Official Identity

- **App Name (`app_name`):** `MonoDo`
- **Application ID (`applicationId`):** `com.aistudio.monodo.kxmpzq`
- **Android Namespace (`namespace`):** `com.example`
- **Official Public GitHub Repository:** `https://github.com/bestdem077/MonoDo`
- **Latest Release API Endpoint:** `https://api.github.com/repos/bestdem077/MonoDo/releases/latest`
- **Base Version:** `2.1.0` (`baseVersionCode = 10`, auto-incremented on GitHub Actions via `GITHUB_RUN_NUMBER`)
- **Creator / Branding:** Created by Vishal D. (Vishal Dhangar)
- **Platform:** Android (Jetpack Compose, Material 3, Kotlin Coroutines & Flow, Room Database v10)
- **Core Value Proposition:** 100% offline, privacy-focused minimalist productivity workspace (Tasks, Monthly Calendar, Habits, Birthdays, Notes, Pomodoro, Widgets, Stats) with optional in-app GitHub Release updates.
- **Visual Design Identity:** Swiss paper minimalist monochrome aesthetic (Light Paper, Dark Slate, Pure Black AMOLED) elevated with refined **Royal Blue** (`#2563EB`) accents.

---

## 2. CRITICAL IN-PLACE UPDATE & PACKAGE IDENTITY LOCKS (NEVER BREAK!)

These rules exist to guarantee that every new APK built via GitHub Actions installs **cleanly over the user's existing MonoDo app WITHOUT installing a duplicate app and WITHOUT losing any user data**:

1. **NEVER CHANGE `applicationId`:**
   - `applicationId` in `app/build.gradle.kts` **MUST ALWAYS remain `"com.aistudio.monodo.kxmpzq"`**.
   - Changing `applicationId` causes Android to treat the update as a completely separate second app, leaving the user's tasks, notes, habits, and birthdays stranded in the old app!
2. **NEVER CHANGE `namespace` OR `app_name`:**
   - Keep `namespace = "com.example"` in `app/build.gradle.kts` and `<string name="app_name">MonoDo</string>` in `strings.xml`.
3. **NEVER MODIFY OR REGENERATE SIGNING KEYSTORES:**
   - **NEVER** modify, delete, or regenerate `debug.keystore`, `debug.keystore.base64`, `signingConfigs` in `app/build.gradle.kts`, or the fixed canonical keystore block in `.github/workflows/build-apk.yml`.
   - Changing the signing certificate causes Android to reject in-place updates with *"App not installed as package conflicts with an existing package"*.
4. **MONOTONICALLY INCREASING `versionCode`:**
   - Never lower `baseVersionCode` below `10` (and keep `baseVersionName` strictly above the last published GitHub release so in-development builds never prompt to install older releases).
   - Preserve the CI `GITHUB_RUN_NUMBER` + `MONODO_RELEASE_TAG` logic in `app/build.gradle.kts` and `.github/workflows/build-apk.yml` so every GitHub Release APK has a strictly higher `versionCode` and matching `BuildConfig.GITHUB_RELEASE_TAG`.
5. **NON-DESTRUCTIVE ROOM MIGRATIONS ONLY:**
   - `AppDatabase` is currently **Version 10**.
   - Whenever modifying or adding Room tables/columns, **ALWAYS** increment the database version and provide an explicit non-destructive `Migration(oldVersion, newVersion)`. **NEVER** enable `fallbackToDestructiveMigration()` — doing so wipes user data on update.
6. **APP LAUNCHER ICON GUARD:**
   - Do **NOT** modify `ic_launcher*` or `ic_monodo_logo` unless the user explicitly asks to redesign/change the icon.
7. **GITHUB REPOSITORY LOCK:**
   - Keep `BuildConfig.GITHUB_REPO = "bestdem077/MonoDo"` so the in-app GitHub Release updater always points to the official public repository.

---

## 3. Inviolable Product Constraints

1. **NO LOGIN / NO SIGNUP / NO CLOUD DATA SYNC:**
   - MonoDo's user data (tasks, notes, habits, birthdays, templates, stats) is strictly 100% offline and local-first.
   - **NEVER** implement authentication flows, OAuth, Firebase Auth, Google Sign-In, Firestore sync, or account creation screens.
   - Network access (`INTERNET`, `ACCESS_NETWORK_STATE`) is used **exclusively** for checking/downloading official public GitHub Releases (`bestdem077/MonoDo`). When offline, the app must continue working 100% silently with zero errors.

2. **ONBOARDING & WELCOME SCREEN (ONE-TIME ONLY):**
   - `MonoDoOnboardingScreen.kt` collects the user's name, date of birth, and morning briefing preference on first launch.
   - Committed synchronously via `UserProfileManager.saveProfile()` (`commit()`).
   - Never shown again on subsequent opens unless the user triggers **"Reset All Data & Setup"** in Settings.

3. **IN-APP GITHUB UPDATE FLOW (`data/update/` + `AppUpdateSheet.kt`):**
   - Queries `https://api.github.com/repos/bestdem077/MonoDo/releases/latest`.
   - Compares remote `tag_name` against `BuildConfig.GITHUB_RELEASE_TAG` via `VersionComparator`.
   - Downloads the APK with live progress into `cacheDir/updates/` **AND** exports a copy to the public **Downloads** folder (`MediaStore.Downloads` on Android 10+, zero storage permission needed).
   - Supports BOTH:
     - **Direct In-App Install** (via `FileProvider` + `REQUEST_INSTALL_PACKAGES`), and
     - **Zero-Permission Fallback** ("Open Downloads Folder") for users who prefer installing manually from `Downloads/MonoDo-vX.Y.Z.apk`.

4. **NAVIGATION TABS (4 PRIMARY DESTINATIONS + CENTER FAB):**
   - **Tasks:** Inline Quick-Add + 1-Tap Deadline Chips, Smart Sections (Overdue, Today, Upcoming), Monthly Calendar toggle (Tasks + Birthdays), 7-Day Habit Dot Matrix, swipe gestures, tactile checkbox & strikethrough animation, reusable Task Templates, Zen Focus Mode, user's own birthday celebration banner, dismissible update banner.
   - **Stats:** 7-day weekly bar chart & 28-day heatmap with Tap-to-Inspect tooltips, focus minutes, streak & completion analytics.
   - **Center Floating '+' Button:** Royal Blue pill opening the Create/Edit Task & Templates bottom sheet.
   - **Tools:**
     1. **Notes:** Room-backed notes with pinning, search, category pills, Note-to-Task conversion.
     2. **Pomodoro Timer:** Persistent background countdown with ongoing notification + exact alarm completion alert + focus stats logging.
     3. **Birthdays:** Friends/family birthday tracker with 3h/4h pre-midnight & 12 AM exact alarms, search + Countdown/By-Month toggle, 1-Tap Wish Templates (Friend/Family/Formal), notification Copy/Share wish actions.
     4. **Widgets:** Scrollable Agenda List widget (`MonoDoWidgetProvider`) & Focus Glance tile (`MonoDoQuickWidgetProvider`).
   - **Settings:** Profile, 3 Themes (Light Paper / Dark Slate / Pure Black AMOLED), Morning Briefing & Doze-proof exact alarms, Pinned Today's Tasks card (opt-in), Biometric/PIN App Lock, JSON Backup & Restore (v6), **App Updates (GitHub)**, Clear Completed, Reset All.

5. **CLEAN PRODUCTION & ZERO DUMMY DATA:**
   - Start with a pristine empty database. Never inject dummy/mock tasks, notes, or birthdays on launch.

---

## 4. Storage & Persistence Overview

- **Room Database (`AppDatabase.kt`, Version 10):**
  - `tasks` (`TaskEntity` + `TaskDao`)
  - `notes` (`NoteEntity` + `NoteDao`)
  - `habits` & `habit_logs` (`HabitEntity`, `HabitLogEntity`, `HabitDao`)
  - `custom_categories` (`CustomCategoryEntity`, `CustomCategoryDao`)
  - `focus_sessions` (`FocusSessionEntity`, `FocusSessionDao`)
  - `birthdays` (`BirthdayEntity`, `BirthdayDao`)
  - `routine_templates` (`RoutineTemplateEntity`, `RoutineTemplateDao`)
- **SharedPreferences:**
  - `UserProfileManager.kt`: User profile, theme mode, App Lock toggle, Pinned Today notification toggle, Pomodoro persistence, dismissed update tag & last update check timestamp.
  - `ReminderPreferences.kt`: Daily briefing alarm schedule.

---

## 5. Testing & Verification

- **Pure JVM Unit Tests:** `app/src/test/java/com/example/ExampleUnitTest.kt`
  - Command: `gradle :app:testDebugUnitTest --tests "com.example.ExampleUnitTest"`
- **Compiler Verification:** Always run `compile_applet` after code changes.
