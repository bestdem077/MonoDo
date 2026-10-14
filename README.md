# MonoDo - Minimalist Monochrome Task Manager

<p align="center">
  <b>Crafted with Focus by Vishal Dhangar</b><br>
  <i>A distraction-free, offline-first personal task and productivity tracker.</i>
</p>

---

## 🌟 Key Features

- **🖤 Monochrome Minimalist Design:** High-contrast, clean black & white aesthetic built with Jetpack Compose and Material 3.
- **⚡ 100% Offline & Private:** Built on **Android Room Database (SQLite)**. Your data stays securely on your phone without cloud tracking.
- **🔥 Streak & Productivity Analytics:** Tracks daily completions, continuous habit streaks, and weekly productivity matrices.
- **📋 Subtask Checklists:** Break down complex goals into simple checkable subtasks with live progress meters.
- **🏷️ Smart Categorization & Priority Scheduling:** Tag tasks as Work, Personal, Focus, or Urgent with High/Medium/Low priority flags.
- **⏰ Daily Local Notifications:** Built-in daily reminder service to keep you accountable without battery drain.
- **🚀 Automated Continuous Integration:** Pre-configured GitHub Actions workflow automatically builds, versions, and releases APKs on every `git push`.

---

## 📲 Download & Install APK

Whenever code is pushed to this repository, a new versioned APK is automatically generated!

### How to get the latest APK:
1. Go to the **[Releases](../../releases)** tab of this GitHub repository.
2. Under the latest release, download **`MonoDo-latest.apk`** or the specific version file (e.g. `MonoDo-v1.0.X.apk`).
3. Open the downloaded `.apk` file on your Android device.
4. If prompted, tap **Settings** and enable **"Allow from this source" / "Install unknown apps"**.
5. Tap **Install** and launch MonoDo!

---

## 🛠️ Tech Stack & Architecture

- **Platform:** Native Android (API 24+)
- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose + Material Design 3
- **Local Persistence:** Room Database + Kotlin Coroutines & Flow
- **Architecture:** Clean MVVM (Model - View - ViewModel)
- **CI/CD:** GitHub Actions (Automated multi-version APK building and GitHub Releases)

---

## 🤖 GitHub Actions Workflow (`.github/workflows/build-apk.yml`)

The repository includes a ready-to-run GitHub Actions pipeline:
- **Triggers:** Automatically on every `git push` to `main`/`master`, or any tag (`v*`), or via manual trigger in the Actions tab.
- **Builds:** Compiles the project using Gradle and outputs signed, ready-to-install `.apk` packages.
- **Versioning:** Automatically assigns dynamic version tags (e.g., `v1.0.1`, `v1.0.2`...) based on the run number and git tags.
- **Releases:** Automatically creates a GitHub Release with direct download links for users.

---

## 👤 Developer & Credits

- **Creator & Lead Developer:** **Vishal Dhangar**
- **Project:** MonoDo Task Manager
- **License:** Open Source

---
*Created with simplicity, speed, and focus.*
