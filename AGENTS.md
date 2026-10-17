# MonoDo — Agent Context & Operational Guidelines

This document serves as the single source of truth for AI agents and developers working on the **MonoDo** codebase. Read this file to understand the architecture, design rules, feature specifications, and constraints before making code changes.

---

## 1. Project Overview & Philosophy

- **App Name:** MonoDo
- **Version:** 2.0.0 (versionCode: 3)
- **Creator / Branding:** Created by Vishal D. (Vishal Dhangar)
- **Platform:** Android (Jetpack Compose, Material 3, Kotlin Coroutines & Flow, Room Database)
- **Core Value Proposition:** 100% offline, privacy-focused, distraction-free minimalist productivity workspace.
- **Visual Design Identity:** Swiss paper minimalist monochrome aesthetic elevated with refined **Royal Blue** (`#2563EB`) accents for primary CTAs, active highlights, badges, and creation affordances.

---

## 2. Inviolable Core Constraints & Rules

1. **NO LOGIN / NO SIGNUP:**
   - MonoDo is strictly 100% offline and local-first.
   - **NEVER** implement authentication flows, OAuth, Firebase Auth, Google Sign-In, or account creation screens. If an image or design reference shows Login/Signup buttons (e.g. "Continue with Google", "Sign In"), **ignore those options entirely**.

2. **ONBOARDING & WELCOME SCREEN (ONE-TIME ONLY):**
   - The Welcome/Onboarding screen (`MonoDoOnboardingScreen.kt`) collects the user's name and optional workspace title on first launch.
   - Once saved or skipped, user profile is committed to disk via `UserProfileManager.saveProfile()` using `commit()` (synchronous disk write).
   - `isOnboarded` is checked on launch: if `true` or if user name is present, the welcome screen **MUST NEVER** appear again on subsequent app opens.
   - The onboarding screen only re-appears if the user explicitly triggers **"Reset All Data & Setup"** in Settings or if app storage is cleared.
   - Onboarding button text is **"Enter MonoDo"** (or dynamically `Enter <WorkspaceName>`), **NEVER** generic "Enter Workspace".
   - The logo displayed on the onboarding screen is the emboldened MonoDo emblem vector (`R.drawable.ic_monodo_logo`), **NEVER** a bare letter "M".

3. **NAVIGATION TABS (4 PRIMARY DESTINATIONS):**
   - **Tasks:** Inline Quick-Add bar + 1-Tap Quick Deadline Chips (`Today`, `Tomorrow`, `This Weekend`, `Next Week`), 7-Day Habit Dot Matrix Tracker, active tasks with `pointerInput` horizontal swipe-to-complete and swipe-for-actions (`Today`, `Tomorrow`, `Delete`), Zen Focus Mode fullscreen canvas, subtasks checklist, custom category filter bar with semantic palette picker, smart sorting & filtering sheet, daily streak counter, Swiss ink-stamp celebration badge, collapsible completed section.
   - **Stats:** Weekly completion bar chart (7-day activity), daily totals, productivity analytics.
   - **Center Floating '+' Button:** Royal Blue elevated circular pill that opens the task creation bottom sheet.
   - **Tools:** Replaces the old "Focus Tools" / "Scratchpad". Contains:
     1. **Notes Application:** Room database-backed notes with category pills, pinning, instant title/body search, note detail viewer/editor, and deletion.
     2. **Pomodoro / Focus Timer:** 25/5 min focus sessions, audio tick toggles, full focus mode.
     3. **Home Widget (`MonoDo Today`):** Live interactive widget preview and 1-tap launcher pinning.
   - **Settings:** Profile customization (User Name, Workspace Title), Daily Reminder alarm scheduler, Data Management (1-Tap Offline Backup & Restore `.json`, Clear Completed Tasks, Reset All Data & Setup), and Developer credits.

4. **STORAGE & PERSISTENCE:**
   - **Room Database (`AppDatabase.kt`, Version 6):**
     - `TaskEntity` + `TaskDao`: Primary task and subtask persistence.
     - `NoteEntity` + `NoteDao`: Full Notes application storage (`notes` table).
     - `HabitEntity` + `HabitLogEntity` + `HabitDao`: 7-Day Habit Dot Matrix persistence.
     - `CustomCategoryEntity` + `CustomCategoryDao`: Custom user categories with semantic color hexes.
   - **SharedPreferences (`UserProfileManager.kt`):**
     - User's name, custom workspace title, and `isOnboarded` boolean flag with `commit()`.
   - **Daily Reminder Preferences (`DailyReminderPreferences.kt`):**
     - Scheduled notification hour, minute, and enabled status.

5. **CLEAN PRODUCTION & ZERO DUMMY DATA:**
   - Production builds start with a 100% pristine, empty database.
   - Do **NOT** auto-inject dummy, mock, or sample tasks/notes on launch.
   - Any legacy test fixtures are purged via `removeSampleDataIfPresent()`.
   - Empty states provide elegant minimalist guidance ("Tap the + button below to add your first task").

---

## 3. Design System & Color Palette (`com.example.ui.theme`)

| Token Name | Hex Value | Purpose |
| :--- | :--- | :--- |
| `PaperCanvas` | `#FAF9F6` | Warm, clean paper background for screens |
| `PaperSurface` | `#FFFFFF` | Pure white card surfaces, bottom sheets, dialogs |
| `PaperSurfaceVariant` | `#F2F1ED` | Elevated secondary surface |
| `ObsidianBlack` | `#111111` | Primary high-contrast typography & dark badges |
| `CharcoalDark` | `#1E1E1E` | Deep charcoal for secondary dark components |
| `SlateGraphite` | `#6B6B6B` | Captions, secondary labels, metadata, placeholders |
| `HairlineBorder` | `#E5E5DF` | Card dividers, outlines, hairline borders |
| `RoyalBlue` | `#2563EB` | **Primary curated accent** for CTAs, active tabs, center FAB, highlights |
| `RoyalBlueHover`| `#1D4ED8` | Pressed / active state for Royal Blue elements |
| `RoyalBlueSoft` | `#EFF6FF` | Soft background tint for pills, avatar frames, badges |
| `RoyalBlueBorder`| `#BFDBFE` | Pastel blue delicate outline for chips and inputs |
| `RoyalBlueMuted`| `#3B82F6` | Secondary vibrant blue highlight |

---

## 4. Key Codebase Directories & Files

```
app/src/main/java/com/example/
├── MainActivity.kt                        # Edge-to-edge entry point
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt                # Room DB with TaskDao & NoteDao
│   │   ├── Converters.kt                 # SubTask JSON converters
│   │   ├── NoteDao.kt                    # Note CRUD, queries, search, counts
│   │   ├── TaskDao.kt                    # Task CRUD, queries, search, counts
│   │   └── TaskRepository.kt             # Task repository abstractions
│   ├── model/
│   │   ├── NoteEntity.kt                 # Note data entity (id, title, content, category, isPinned, createdAt, updatedAt)
│   │   ├── SubTask.kt                    # Subtask item model
│   │   ├── TaskEntity.kt                 # Task data entity
│   │   └── TaskPriority.kt               # LOW, MEDIUM, HIGH, URGENT
│   └── user/
│       ├── DailyReminderPreferences.kt   # Reminder alarm settings
│       └── UserProfileManager.kt         # Synchronous profile & onboarding state
├── ui/
│   ├── components/
│   │   ├── CategoryChipsRow.kt           # Category filter bar with color styles
│   │   ├── MonoDoBottomBar.kt            # 4-tab bottom navigation with center '+' FAB
│   │   ├── MonochromeHeader.kt           # Date, greeting, streak badge, reminder bell
│   │   └── TaskCard.kt                   # Task item with checkbox, subtasks, delete
│   ├── navigation/
│   │   └── NavigationTab.kt              # TASKS, STATS, TOOLS, SETTINGS
│   ├── screens/
│   │   ├── MonoDoHomeScreen.kt           # Primary scaffold container & tab coordinator
│   │   ├── MonoDoNotesScreen.kt          # Complete Room-backed Notes manager
│   │   ├── MonoDoOnboardingScreen.kt     # First-run welcome & profile setup
│   │   ├── MonoDoSettingsScreen.kt       # Profile edit, reminders, reset data
│   │   ├── MonoDoStatsScreen.kt          # Productivity analytics & 7-day charts
│   │   ├── MonoDoTasksScreen.kt          # Active tasks list, category filter, search
│   │   └── MonoDoToolsScreen.kt          # Tools tab host (Notes, Timer, Utilities)
│   ├── theme/
│   │   ├── Color.kt                      # Monochrome + Royal Blue palette
│   │   ├── CategoryTheme.kt              # Semantic color tokens for categories
│   │   └── Theme.kt                      # MaterialTheme setup
│   └── viewmodel/
│       └── TaskViewModel.kt              # Central ViewModel for Tasks, Notes & Analytics
└── res/
    └── drawable/
        ├── ic_monodo_logo.xml            # Official emboldened MonoDo emblem vector
        ├── ic_launcher_background.xml    # Adaptive launcher background
        └── ic_launcher_foreground.xml    # Adaptive launcher foreground
```

---

## 5. Testing & Verification

- **Robolectric JVM Tests:** Located in `app/src/test/java/com/example/ExampleRobolectricTest.kt`.
- **Command:** `gradle :app:testDebugUnitTest`
- **Compiler Check:** Use `compile_applet` tool to ensure pristine Gradle compilation without errors.
