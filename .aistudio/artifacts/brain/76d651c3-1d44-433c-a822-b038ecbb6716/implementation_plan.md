# MonoDo — Quick Deadline Chips & PointerInput Horizontal Swipe Actions Plan

## 1. Overview & Goals
This update refines task creation and task card gesture interactions to make managing daily tasks effortless:
1. **1-Tap Quick Deadline Chips (`Today`, `Tomorrow`, `This Weekend`, `Next Week`):**
   - Reusable `QuickDateChipsRow` component placed directly below the task title input field in **both** the **Create Task Bottom Sheet** (`CreateTaskSheet.kt`) and a new **Inline Quick-Add Task Bar** on the main **Tasks Screen** (`MonoDoTasksScreen.kt`).
2. **Custom `Modifier.pointerInput` Horizontal Swipe Gestures with Haptics (`TaskCard.kt`):**
   - **Left-to-Right Swipe (`offsetX > 0`):** Drags the card right to reveal a Royal Blue completion backdrop; crossing the threshold fires a crisp haptic tick (`HapticFeedbackHelper`) and toggles task completion before springing back to `0f`.
   - **Right-to-Left Swipe (`offsetX < 0`):** Slides the foreground card left using `Modifier.pointerInput(task.id) { detectHorizontalDragGestures(...) }` and snaps open (`-actionPanelWidthPx`) to reveal **inline action buttons behind the card** (`Today`, `Tomorrow`, `Delete`), with threshold haptic feedback on reveal and action execution.

---

## 2. Detailed Component Design

### A. Reusable `QuickDateChipsRow` & Inline Quick-Add Bar
- **`QuickDateChipsRow` Composable (`app/src/main/java/com/example/ui/components/QuickDateChipsRow.kt`):**
  - Renders a horizontally scrollable row of 4 primary deadline pills:
    - **Today** (Today at 23:59:59)
    - **Tomorrow** (+1 day at 23:59:59)
    - **This Weekend** (Upcoming Saturday at 23:59:59)
    - **Next Week** (Upcoming Monday at 23:59:59)
    - Optional **Custom** date picker pill and clear (`×`) affordance when a date is active.
  - Highlights the active chip in `RoyalBlue` (`#2563EB`) with white bold text and triggers `HapticFeedbackHelper.performSelectionHaptic` on tap (tapping an already-selected chip toggles it off).
- **Integration in `CreateTaskSheet.kt`:**
  - Placed directly beneath `create_task_title_input` so users can set a deadline in one tap without opening a calendar dialog.
- **Inline Quick-Add Task Bar in `MonoDoTasksScreen.kt`:**
  - A compact Swiss paper input card at the top of the Tasks workspace with a task title field, submit (`+`) button, and the `QuickDateChipsRow` (`Today`, `Tomorrow`, `This Weekend`, `Next Week`) directly underneath the input field for instant 1-tap task creation with deadlines.

### B. `Modifier.pointerInput` Swipe-to-Complete & Swipe-for-Actions (`TaskCard.kt`)
- **Gesture Mechanics (`Modifier.pointerInput` + `detectHorizontalDragGestures`):**
  - Uses `Animatable(0f)` for smooth horizontal translation (`IntOffset(offsetX.value.roundToInt(), 0)`).
  - Tracks `hasTriggeredThresholdHaptic` during the drag gesture so crossing the completion threshold (`+completeThresholdPx`) or action reveal threshold (`-revealThresholdPx`) emits a single crisp haptic pulse (`HapticFeedbackHelper.performSwipeActionHaptic`).
- **Background Layer (Revealed Behind the Card):**
  - **When `offsetX > 0` (Swiping Right — Complete):**
    - Displays a `RoyalBlueSoft` (`#EFF6FF`) surface on the left with a `RoyalBlue` check icon and `"Complete"` / `"Reopen"` label that scales up as the user drags past the threshold.
    - Releasing past `+completeThresholdPx` fires `HapticFeedbackHelper.performTaskCompletionHaptic`, calls `onToggleCompletion()`, and animates the card back to `0f` with a spring specification.
  - **When `offsetX < 0` or Snapped Open (Swiping Left — Inline Actions):**
    - Displays three inline action buttons aligned to the right edge behind the card:
      1. **Today:** Reschedules task due date to Today (23:59:59), triggers selection haptic, and snaps the card closed (`0f`).
      2. **Tomorrow:** Reschedules task due date to Tomorrow (23:59:59), triggers selection haptic, and snaps the card closed (`0f`).
      3. **Delete:** Triggers delete haptic (`performDeleteHaptic`), snaps closed, and invokes `onDeleteTask()`.
    - Releasing a right-to-left drag past `-revealThresholdPx` snaps the card open to `-maxLeftSwipePx` so the user can tap any of the revealed inline buttons; dragging right or tapping the card closes the revealed actions smoothly.

---

## 3. Files to Create & Modify
1. **Create** `app/src/main/java/com/example/ui/components/QuickDateChipsRow.kt` — Reusable composable for `'Today'`, `'Tomorrow'`, `'This Weekend'`, and `'Next Week'` deadline chips + helper timestamp calculators.
2. **Modify** `app/src/main/java/com/example/ui/components/CreateTaskSheet.kt` — Use `QuickDateChipsRow` directly below the task title input field.
3. **Modify** `app/src/main/java/com/example/ui/screens/MonoDoTasksScreen.kt` — Add the inline Quick-Add Task Bar with `QuickDateChipsRow` directly below the input field so deadlines and tasks can be added in 1 tap from the Tasks screen.
4. **Modify** `app/src/main/java/com/example/ui/components/TaskCard.kt` — Replace `SwipeToDismissBox` with a custom `Modifier.pointerInput` (`detectHorizontalDragGestures`) layout that slides the foreground card right to complete and slides/snaps left to reveal inline `Today`, `Tomorrow`, and `Delete` action buttons behind the card with full haptic feedback integration.
5. **Modify** `app/src/test/java/com/example/ExampleRobolectricTest.kt` — Add unit tests verifying the quick deadline timestamp calculations (`Today`, `Tomorrow`, `This Weekend`, `Next Week`).
