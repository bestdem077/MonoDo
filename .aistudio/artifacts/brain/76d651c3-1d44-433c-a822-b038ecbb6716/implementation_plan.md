# MonoDo - UI Polish & Input Styling Fix Plan

Fix text visibility in text inputs, eliminate harsh black focus borders, render clear calendar date numbers in the weekly activity strip, and lock the pure paper light theme to prevent dark-mode contrast conflicts.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following adjustments have been confirmed based on your preferences and screenshots:
> - **Input Field Focus Border & Colors**: Remove the harsh thick black outline when focusing/clicking text fields. Replace it with a soft, refined neutral grey outline (`#CCCCCC` / `#BDBDBD`) with subtle tinted background (`#F5F4F0`). Explicitly lock `focusedTextColor`, `unfocusedTextColor`, and cursor to pitch black (`#111111`) so entered text is always 100% visible.
> - **Weekly Activity Strip**: Update the 7-day strip so each tile prominently displays the **calendar date number** (e.g. 14, 15, 16) alongside the weekday label (`WED`, `THU`, etc.), with clear rounded tile borders and high-contrast indicators for today and completed counts.
> - **Theme Enforcement**: Lock the app to **Pure Paper Light Theme** (`#FAF9F6`) across all device system settings, eliminating inverted white-on-white text issues caused by Android system dark mode.

---

## 1. Overview & Core Concept

- **Problem Addressed**:
  1. Typed text in `Task Title`, `Description`, and `Subtask` input fields was rendering invisible or near-invisible due to system dark mode overriding M3 content colors.
  2. Clicking text areas triggered an abrupt, heavy 2dp black border that felt harsh against the minimalist aesthetic.
  3. Weekly activity boxes lacked visible date numbers and day indicators on certain screen densities.
- **Goal**: Deliver a tranquil, ultra-polished editorial paper UI where every text element has maximum legibility, inputs feel soft and natural to interact with, and date numbers are crisp and prominent.

---

## 2. User Experience & Visual Design

### 1. Soft Input Experience
- **Unfocused State**: Warm subtle fill (`#F7F6F3`), hairline border (`#E4E3DE`), placeholder text in refined slate (`#787878`).
- **Focused State**: No thick black outline. Instead, a gentle 1dp soft graphite/grey outline (`#B0B0A8`), slightly deeper background (`#F3F2EE`), and stark black typed text (`#111111`).
- **Cursor**: Obsidian black (`#111111`) matching the typography.

### 2. Enhanced Weekly Calendar Strip
- Each day tile renders:
  - **Date Number** (e.g., `14`, `15`) in bold typography (14sp).
  - **Weekday Abbreviation** (e.g., `WE`, `TH`, `FR`) in 10sp uppercase.
  - **Completion State**: Subtle filled dot or solid tile fill when tasks were finished on that day.
  - **Today Marker**: Distinct high-contrast obsidian border highlight.

### 3. Theme Consistency
- Fix `MonoDoTheme` to enforce the paper light color scheme (`PaperCanvas`, `PaperSurface`, `ObsidianBlack`), preventing system dark mode from setting white-on-white text in Composables and text fields.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Explicit Text & Border Colors in OutlinedTextField**:
  - *Chosen Approach*: Explicitly pass `focusedTextColor = ObsidianBlack`, `unfocusedTextColor = ObsidianBlack`, `focusedBorderColor = SoftBorderGrey (1.dp)`, and `unfocusedBorderColor = HairlineBorder` in `OutlinedTextFieldDefaults.colors(...)` across all input fields.
  - *Why*: Eliminates system theme ambiguity and directly solves the issue where typed text was invisible.

- **Decision 2: Calendar Date Integration in DayActivity**:
  - *Chosen Approach*: Add `dayOfMonth: Int` to `DayActivity` (calculated via `Calendar.DAY_OF_MONTH`) so each tile displays its real calendar date number.
  - *Why*: Provides immediate spatial clarity and matches user expectations for a modern task scheduler.

---

## 4. Technical Architecture & Component Mapping

```
┌───────────────────────────────────────────────────────────────┐
│                      Theme & Input Polish                     │
├───────────────────────────────────────────────────────────────┤
│ Theme.kt                                                      │
│ └── MonoDoTheme: Enforces MonochromeLightColorScheme         │
│     (PaperCanvas, PaperSurface, ObsidianBlack text)          │
├───────────────────────────────────────────────────────────────┤
│ WeeklyMatrixView.kt & TaskViewModel.kt                        │
│ └── DayActivity: adds dayOfMonth (e.g., 14)                  │
│ └── DayMatrixTile: displays weekday label + prominent date   │
│     number + completion dot badge                             │
├───────────────────────────────────────────────────────────────┤
│ CreateTaskSheet.kt & TaskCard.kt & SearchBarAndFilter.kt      │
│ └── Soft border colors on focus (no harsh black border)      │
│ └── Explicit focusedTextColor & unfocusedTextColor (black)   │
└───────────────────────────────────────────────────────────────┘
```

---

## 5. Verification Plan

- Run `compile_applet` to verify compilation.
- Execute unit and Robolectric tests (`gradle :app:testDebugUnitTest`).
- Verify text visibility, soft border colors, and date numbering in the weekly strip.
