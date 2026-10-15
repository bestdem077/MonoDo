# MonoDo — Modular Multi-Screen Architecture & Custom Docked Navigation

A comprehensive architectural plan to evolve MonoDo from a single-screen feed into a modular, multi-tab productivity experience featuring a custom bottom navigation bar with a centered elevated '+' action button (matching user reference mockup), dedicated Analytics, Productivity Tools, and Settings.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The architectural foundation and navigation structure have been calibrated against your clarifying answers:

- **Confirmed Navigation Layout (5-Item Structure)**:
  1. **Tasks Tab**: Pure, uncluttered task feed with category chips, search/priority filtering, and collapsible completed items.
  2. **Stats Tab**: Dedicated analytics hub featuring the Weekly Productivity Matrix, animated Circular Completion Rate Gauge, and Category Distribution Breakdown.
  3. **Center '+' Button**: Floating elevated circular black button centered in the bottom dock, opening the full interactive Task & Subtask Creation Sheet.
  4. **Tools Tab**: Modular utility hub containing a Focus Pomodoro Timer (with start/pause/reset and sound/haptic ticks) and a Quick Scratchpad Notes tool for instant thoughts.
  5. **Settings Tab**: Comprehensive hub with Developer Profile (Vishal Dhangar), Dynamic App Versioning (`BuildConfig`), Daily Notification Reminders toggle, and Local JSON Data Backup & Restore.

- **Confirmed Center '+' Button Behavior**: Tapping the center elevated button opens the full bottom sheet to configure task titles, descriptions, categories, priorities, due dates, and subtask checklists.

- **Confirmed Analytics Scope**: Weekly matrix, real-time completion gauge, lifetime streak record, and category task distribution.

---

### 1. Overview & Core Concept

- **What It Delivers**: Transforms MonoDo from a single long scrollable page into a 4-tab powerhouse. Heavy components like the productivity matrix no longer crowd the task list; instead, tasks, deep analytics, productivity tools, and preferences each have their own dedicated space.
- **Target Persona**: Focused creators, students, and professionals who demand a distraction-free, 100% offline-first productivity tool with immediate tactile feedback and zero latency.
- **Key Value**:
  - Eliminates visual clutter on the home screen.
  - Adds real productivity utilities (Focus Timer + Scratchpad Notes).
  - Empowers users with complete offline backup/export and profile customization.

---

### 2. User Experience & Visual Design

#### Bottom Navigation Bar Architecture (Matching User Reference Image)
The bottom bar mirrors the uploaded mockup:
- **Floating Dock Height**: 64.dp container with subtle hairlines and high-contrast monochrome tokens.
- **Docked Elevated Center Button**: A prominent 56.dp circular black button (`ObsidianBlack`) with a crisp white `+` icon, slightly elevated with a soft drop shadow, centered between the first two tabs and the last two tabs.
- **Safe Area Insets**: Explicit `.navigationBarsPadding()` so the bar floats gracefully above Android 3-button navigation and gesture pills on all device screen ratios.
- **Haptic Tactile Feedback**: Tapping any tab or the center FAB triggers instantaneous mechanical haptic feedback (`HapticFeedbackConstants.GESTURE_START` / `CONFIRM`).

```
┌──────────────────────────────────────────────────────────────┐
│                      Screen Content Area                     │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  [📋 Tasks]    [📊 Stats]      ( + )      [🧰 Tools]   [⚙️ User] │
└──────────────────────────────────────────────────────────────┘
```

#### Detailed Screen Experiences

1. **Tasks Screen (`MonoDoTasksScreen`)**:
   - Header with Date, Streak Capsule, and Active/Completed counters.
   - Horizontal Category Pills (`All`, `Focus`, `Work`, `Personal`, `Ideas`).
   - Search bar and priority filtering pills.
   - Smooth animated task cards with expandable subtasks and tactile checkboxes.

2. **Stats Screen (`MonoDoStatsScreen`)**:
   - **Hero Metric**: Large Streak Milestone Card with lifetime completion numbers.
   - **Weekly Matrix Grid**: 7-day productivity density blocks with day labels.
   - **Completion Gauge**: Circular progress visual displaying overall completion percentage (`done / total`).
   - **Category Distribution**: Clean monochrome progress bars showing where time and energy are spent (e.g. Work 45%, Focus 30%, Personal 25%).

3. **Tools Screen (`MonoDoToolsScreen`)**:
   - **Focus Pomodoro Timer**:
     - Circular countdown timer (25 min focus / 5 min break).
     - Start / Pause / Reset tactile controls with animated progress ring.
     - Session completion counter.
   - **Quick Scratchpad Notes**:
     - Fast, distraction-free markdown/plain-text scratchpad stored locally.
     - Auto-saves as you type without friction.

4. **Settings Screen (`MonoDoSettingsScreen`)**:
   - **Creator Card**: Prominent "Crafted by Vishal Dhangar" card with badges.
   - **Version & Build**: Dynamic version name (`v1.0.X`) and build code from `BuildConfig`.
   - **Daily Reminder Settings**: Time picker and notification switch.
   - **Data Management (Backup & Restore)**:
     - "Export JSON Backup" — exports all tasks, subtasks, and streaks to a JSON file.
     - "Import / Restore Backup" — restores database records seamlessly.
     - "Clear Completed Tasks" with confirmation dialog.

---

### 3. Key Product Decisions & Trade-Offs

- **Custom Compose Dock vs Standard BottomSheet Navigation**:
  - *Decision*: Build a purpose-tailored custom dock layout using Compose `Row`, `Box`, and `FloatingActionButton`.
  - *Why*: The standard Android `NavigationBar` places the FAB separately in a floating corner or requires complex scaffold nesting. A custom dock guarantees pixel-perfect replication of the user's mockup with the centered circular FAB.
- **Navigation State Management (Internal Screen Enum vs Navigation Compose)**:
  - *Decision*: Clean sealed `NavigationTab` state in `TaskViewModel` with `BackHandler` integration.
  - *Why*: Retains instant tab switching with zero flicker, preserves shared ViewModel state (e.g. task list, filters, and timer ticks) across tabs, and complies with Android back-press navigation.
- **Local Persistence Expansion**:
  - *Decision*: Keep Room as the single source of truth for tasks, and utilize fast `SharedPreferences` / Room for scratchpad notes and timer state.
  - *Why*: Guarantees 100% offline security, instant cold-starts, and no cloud account overhead.

---

### 4. Technical Architecture & System Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              MainActivity                               │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                     ┌───────────────┴───────────────┐
                     ▼                               ▼
            TaskViewModel                   ReminderPreferences
                     │
         ┌───────────┴───────────────────────────────┐
         ▼                                           ▼
   AppDatabase (Room)                       UI State (StateFlow)
   • TaskDao (Tasks & Subtasks)              • activeTab: NavigationTab
   • Local SQLite Database                   • tasks, stats, query, timer
         │
┌────────┴────────────────────────────────────────────────────────────────┐
│                         MonoDoRootApp (Scaffold)                         │
├─────────────────────────────────────────────────────────────────────────┤
│ Active Screen Body:                                                     │
│   ├── [Tab: TASKS]    ➔ TasksScreen (Feed, Categories, Search, Cards)    │
│   ├── [Tab: STATS]    ➔ StatsScreen (Weekly Matrix, Gauge, Distribution)│
│   ├── [Tab: TOOLS]    ➔ ToolsScreen (Pomodoro Timer & Scratchpad)       │
│   └── [Tab: SETTINGS] ➔ SettingsScreen (Profile, Reminders, Backup)     │
├─────────────────────────────────────────────────────────────────────────┤
│ Floating Overlay:                                                       │
│   └── CreateTaskSheet (Full modal for task + subtasks + priority)       │
├─────────────────────────────────────────────────────────────────────────┤
│ Bottom Custom Navigation Dock:                                          │
│   [📋 Tasks]   [📊 Stats]   ( ➕ Center Elevated FAB )   [🧰 Tools]  [⚙️ User]│
└─────────────────────────────────────────────────────────────────────────┘
```

---

### 5. Implementation Stages (Step-by-Step)

1. **Navigation Core & Data Structures**:
   - Define `NavigationTab` sealed class/enum (`Tasks`, `Stats`, `Tools`, `Settings`).
   - Create custom `MonoDoBottomBar` composable with centered elevated FAB and safe-area window insets.
2. **Dedicated Stats Screen**:
   - Extract `WeeklyMatrixView` and expand it into a full analytics dashboard with animated completion rate gauge and category distribution cards.
3. **Dedicated Tools Screen**:
   - Implement the Focus Pomodoro Timer with start/pause/reset states.
   - Implement the Quick Scratchpad Notes area with persistence.
4. **Dedicated Settings Screen**:
   - Create Vishal Dhangar developer attribution card.
   - Add JSON export/import handlers for full offline data backup.
   - Add notification reminder controls and build version telemetry.
5. **Home Scaffold Integration**:
   - Connect all screens inside `MainActivity` / `MonoDoRootScreen` with smooth crossfade transitions.
   - Hook up `BackHandler` so pressing Android back returns to the Tasks tab before exiting.
6. **Tactile & Visual Polish**:
   - Verify M3 dynamic insets, haptic vibration triggers on all buttons, and compile verification.
