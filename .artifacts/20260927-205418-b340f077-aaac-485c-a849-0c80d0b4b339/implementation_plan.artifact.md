# Implementation Plan - Metanoia Features & Fixes

This plan details the technical changes required to implement all 6 user requirements across the Metanoia Android application.

## User Review Required

> [!NOTE]
> All changes maintain backward compatibility with existing local session database and SharedPreferences schemas.

## Proposed Changes

### 1. Daily Focus Goal Synchronization

#### [MainViewModel.kt](file:///C:/projects/metanoia/app/src/main/java/com/example/myapplicationtoday/MainViewModel.kt)
- Standardize default daily focus goal to 7,200,000 ms (2 hours = 120 mins).
- Ensure `saveDailyFocusGoal` emits updated goal state via StateFlow.

#### [ProfileFragment.kt](file:///C:/projects/metanoia/app/src/main/java/com/example/myapplicationtoday/ui/ProfileFragment.kt)
- Use `activityViewModels<MainViewModel>()` to access `MainViewModel`.
- When daily focus goal is changed in Profile settings, call `viewModel.saveDailyFocusGoal(newGoalMillis)`.
- Observe `viewModel.dailyFocusGoalMillis` to update UI dynamically.

#### [DashboardFragment.kt](file:///C:/projects/metanoia/app/src/main/java/com/example/myapplicationtoday/DashboardFragment.kt)
- Update default fallback value to align with 2 hours (120 mins).

---

### 2. Tag Management & Deletion (10 Tag Max Limit)

#### [MainViewModel.kt](file:///C:/projects/metanoia/app/src/main/java/com/example/myapplicationtoday/MainViewModel.kt)
- Store active tags list in `metanoia_tags_pref`. Default tags list: `["Deep Work", "Study", "Workout", "Coding", "Reading"]`.
- Implement `getTags(): List<String>`, `addTag(tag: String): Boolean` (with max limit 10), `deleteTag(tag: String)`.

#### [item_category_row.xml](file:///C:/projects/metanoia/app/src/main/res/layout/item_category_row.xml)
- Change layout from plain TextView to a horizontal LinearLayout with tag name TextView and a Delete Button (`ImageView` / `TextView` '✕').

#### [DialogHelper.kt](file:///C:/projects/metanoia/app/src/main/java/com/example/myapplicationtoday/ui/DialogHelper.kt)
- Update `showCategoryPicker` to accept `onDeleteTag: (String) -> Unit` and `onAddTag: (String) -> Unit`.
- Show delete icon next to each tag; tapping delete calls `onDeleteTag` and refreshes the tag list view.
- Enforce max 10 tag limit when adding new custom tags.

---

### 3. Active Streak Calculation Fix on Profile Page

#### [ProfileFragment.kt](file:///C:/projects/metanoia/app/src/main/java/com/example/myapplicationtoday/ui/ProfileFragment.kt)
- Replace static SharedPreferences lookup `prefs.getInt("streak_count", 0)` with dynamic calculation using `AchievementsEngine.calculateStreak(sessions)` / `viewModel.calculateStreak(sessions)`.
- Ensure Profile tab and Enlarged Stats dialog accurately reflect the active streak.

---

### 4. Log In & Sign Up Page + Production Email Verification Integration

#### [NEW] [AuthActivity.kt](file:///C:/projects/metanoia/app/src/main/java/com/example/myapplicationtoday/AuthActivity.kt)
- Log In / Sign Up Activity featuring:
  - Tabbed interface (Log In vs Sign Up).
  - Validation for email address, password, and username.
  - "Continue as Guest" option.
  - Persistence of authentication state in `metanoia_prefs`.

#### [NEW] [activity_auth.xml](file:///C:/projects/metanoia/app/src/main/res/layout/activity_auth.xml)
- UI layout for authentication screen with Metanoia dark theme and gold accents.

#### [ProfileFragment.kt](file:///C:/projects/metanoia/app/src/main/java/com/example/myapplicationtoday/ui/ProfileFragment.kt)
- Display logged-in account email and verification status.
- Add "Log In / Sign Up" or "Log Out" action button.

#### Production Email Connection Guide
- Detailed step-by-step documentation for integrating Firebase Authentication / REST API with `user.sendEmailVerification()` for production deployment.

---

### 5. Separate Haptic Feedback & Sound Options

#### [fragment_profile.xml](file:///C:/projects/metanoia/app/src/main/res/layout/fragment_profile.xml)
- Split combined switch into two distinct preference rows:
  - **Haptic Feedback** (`switchHapticFeedback`) - Key: `"haptic_feedback_enabled"`
  - **Sound Effects** (`switchSoundEffects`) - Key: `"sound_effects_enabled"`
  - **Alert Chime** (`llAlertSound`) - Selector for alert tone.

#### [ProfileFragment.kt](file:///C:/projects/metanoia/app/src/main/java/com/example/myapplicationtoday/ui/ProfileFragment.kt)
- Read, save, and toggle `"haptic_feedback_enabled"` and `"sound_effects_enabled"` independently.

#### [TimerService.kt](file:///C:/projects/metanoia/app/src/main/java/com/example/myapplicationtoday/TimerService.kt)
- Check `haptic_feedback_enabled` for vibration and `sound_effects_enabled` for audio playback.

---

### 6. Focus Music Tracks & Playback Options (Loop / Shuffle / Silence)

#### [NEW Audio Resources]
- Add 3 ambient focus audio files in `app/src/main/res/raw/`:
  - `focus_track_1.ogg` ("Deep Focus Ambient")
  - `focus_track_2.ogg` ("Zen Rain Stream")
  - `focus_track_3.ogg` ("Celestial Binaural")

#### [TimerService.kt](file:///C:/projects/metanoia/app/src/main/java/com/example/myapplicationtoday/TimerService.kt)
- Add `MediaPlayer` for focus music playback during session countdown / countup.
- Implement modes:
  - **Silence (None)**
  - **Track 1, 2, or 3**
  - **Loop Mode**: Seamless re-play upon completion.
  - **Shuffle Mode**: Picks a random track upon completion.
- Integrate with Service lifecycle (play on start/resume, pause on pause, stop on finish/cancel).

#### [fragment_profile.xml & ProfileFragment.kt](file:///C:/projects/metanoia/app/src/main/res/layout/fragment_profile.xml)
- Add Focus Music selector in Profile Settings allowing users to set default music track and playback mode (Loop, Shuffle, or Silence).

---

## Verification Plan

### Automated Unit Tests
- Execute `:app:testDebugUnitTest` using `gradle_build`:
  ```bash
  ./gradlew :app:testDebugUnitTest
  ```
- Write new unit tests in `ProfileAnalyticsTest.kt` or `DashboardUnitTest.kt` covering:
  - Tag deletion and 10-tag limit enforcement.
  - Dynamic streak calculation.
  - Daily goal sync state flow updates.

### Manual Verification
- Deploy/run app build or verify UI rendering and layout components.
- Verify daily focus goal changes in Profile settings immediately reflect on Dashboard card.
- Verify deleting default or custom tags reduces tag count and allows adding up to 10 total tags.
- Verify Profile streak displays correct active consecutive day count.
- Verify Auth Activity UI, input validation, guest login, and logout.
- Verify separate Haptic vs Sound effect switches in Profile settings.
- Verify Focus Music playback modes (Loop, Shuffle, Silence).
