# MonoDo — Minimalist Task & Focus Workspace (v2.0.0)

<p align="center">
  <b>Crafted with Focus by Vishal Dhangar</b><br>
  <i>A distraction-free, 100% offline personal task manager, habit tracker, and focus workspace.</i>
</p>

---

## 🌟 Key Features (v2.0.0 Major Update)

- **🖤 Swiss Minimalist UI with Royal Blue Accents:** High-contrast paper aesthetic elevated with refined Royal Blue (`#2563EB`) highlights built with Jetpack Compose and Material 3.
- **👆 Smooth Swipe Gestures (`pointerInput`) & Haptics:** Swipe left-to-right on any task card to complete/reopen with a crisp haptic tick; swipe right-to-left to reveal inline **Today**, **Tomorrow**, and **Delete** quick actions.
- **⚡ 1-Tap Quick Deadline Chips & Inline Quick-Add Bar:** Instant **`Today`**, **`Tomorrow`**, **`This Weekend`**, and **`Next Week`** chips directly below the task input field in both the Create Task sheet and the inline Quick-Add bar on the Tasks screen.
- **🔵 Minimalist 7-Day Habit Dot Matrix:** Collapsible daily habit tracker with interactive 7-day completion dots and 1-tap starter habits.
- **🧘 Zen Focus Mode (Single-Task Fullscreen Canvas):** Long-press any task or tap the `Focus` pill to enter a distraction-free single-task canvas with an integrated timer and interactive subtask checklist.
- **🗂️ Smart Sorting, Filtering & Custom Categories:** Sort tasks by Priority, Due Date, Creation Date, or Alphabetical, and create custom categories with semantic palette accent dots.
- **💮 Swiss Ink-Stamp Celebration (`DAY COMPLETE • 100%`):** Subtle geometric ink-stamp badge with spring animation and haptic pulse when all daily tasks are completed.
- **💾 1-Tap Offline Backup & Restore (`.json`):** Export and import full JSON backups (tasks, notes, habits, custom categories) directly to device storage.
- **📱 Minimalist Android Home Screen Widget (`MonoDo Today`):** Paper-styled home screen widget showing your top 3 priority tasks with direct checkoff.
- **📝 Local Notes Workspace:** Complete Room-backed notes manager with title/content search, categories, and pinning.
- **🚀 Automated Continuous Integration:** Pre-configured GitHub Actions workflow automatically builds, versions, and releases APKs on every `git push`.

---

## 📲 Download & Install APK

Whenever code is pushed to this repository, a new versioned APK is automatically generated!

### How to get the latest APK:
1. Go to the **[Releases](../../releases)** tab of this GitHub repository.
2. Under the latest release, download **`MonoDo-latest.apk`** or the specific version file (e.g. `MonoDo-v2.0.0.X.apk`).
3. Open the downloaded `.apk` file on your Android device.
4. If prompted, tap **Settings** and enable **"Allow from this source" / "Install unknown apps"**.
5. Tap **Install** and launch MonoDo!

---

## 🛠️ Tech Stack & Architecture

- **Platform:** Native Android (API 24 to 36)
- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose + Material Design 3
- **Local Persistence:** Room Database (v6) + SharedPreferences + Kotlin Coroutines & Flow
- **Architecture:** Clean MVVM (Model - View - ViewModel)
- **CI/CD:** GitHub Actions (Automated multi-version APK building and GitHub Releases)

---

## 🤖 GitHub Actions Workflow (`.github/workflows/build-apk.yml`)

The repository includes a ready-to-run GitHub Actions pipeline:
- **Triggers:** Automatically on every `git push` to `main`/`master`, or any tag (`v*`), or via manual trigger in the Actions tab.
- **Builds:** Compiles the project using Gradle and outputs signed, ready-to-install `.apk` packages.
- **Versioning:** Automatically assigns dynamic version tags (e.g., `v2.0.0.1`, `v2.0.0.2`...) based on `versionName = "2.0.0"` in `app/build.gradle.kts` and the GitHub Actions run number.
- **Releases:** Automatically creates a GitHub Release with direct download links and SHA-256 checksums.

---

## 👤 Developer & Credits

- **Creator & Lead Developer:** **Vishal Dhangar**
- **Project:** MonoDo Task Manager (`v2.0.0`)
- **License:** Open Source

---
*Created with simplicity, speed, and focus.*
