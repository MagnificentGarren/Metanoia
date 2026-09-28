# Implementation Plan - Responsive Design, Database Performance, and Persistent Auth Validation

This document outlines the detailed implementation plan to address the three project requirements:
1. Audit and fix responsive design across mobile, tablet, and desktop viewports (eliminating text wrapping, element overlap, and viewport clipping).
2. Optimise database schema, query performance, and app architecture for low latency and concurrent user load.
3. Replace auto-dismissing toast popups on authentication forms with persistent inline validation.

---

## User Review Required

- **No Breaking API Changes**: All public data models (`Session`, `Project`) remain fully backward-compatible.
- **Verification Strategy**: Automated unit test execution via `./gradlew testDebugUnitTest` and manual layout inspection across viewports.

---

## Proposed Changes

### 1. Responsive Design & Viewport Adaptations

#### [activity_main.xml](file:///C:/projects/metanoia/app/src/main/res/layout/activity_main.xml)
- Update `layoutBottomNav` height to `wrap_content` with `minHeight="56dp"`, set singleLine/ellipsize on navigation items, and constrain maximum container width for wide tablet and desktop screens.

#### [fragment_dashboard.xml](file:///C:/projects/metanoia/app/src/main/res/layout/fragment_dashboard.xml)
- Wrap `layoutPresetChips` in a `HorizontalScrollView` (`scrollbars="none"`) to prevent preset chip clipping or awkward wrapping on narrow screens (< 360dp).
- Add max-width constraints for dashboard cards on wide tablet/desktop viewports.

#### [fragment_sessions.xml](file:///C:/projects/metanoia/app/src/main/res/layout/fragment_sessions.xml)
- Adjust `layoutTopHeader` to prevent overlap between "SESSION HISTORY" title and the "Week/Month" toggle bar on narrow viewports.
- Format worked hours/minutes display inside `cardWorkedBlock` to avoid vertical line wrapping or clipping on mobile devices.

#### [fragment_achievements.xml](file:///C:/projects/metanoia/app/src/main/res/layout/fragment_achievements.xml)
- Wrap `layoutFilters` in a `HorizontalScrollView` or use flexible auto-sizing text so tab titles like "MILESTONES" never wrap awkwardly or clip.

---

### 2. Database Schema, Query Performance & Architecture Optimizations

#### [Session.kt](file:///C:/projects/metanoia/app/src/main/java/com/example/myapplicationtoday/Session.kt)
- **Thread Safety**: Synchronize memory collections (`memorySessions` and `memoryProjects`) in `SessionRepository` using thread-safe synchronization blocks to prevent race conditions and `ConcurrentModificationException` under concurrent loads.
- **Query Performance & Regex Caching**: Pre-compile Regex patterns (`hourRegex`, `minRegex`) into top-level / companion object constants instead of recompiling on every `parseDurationToMinutes()` invocation.
- **Duration Caching**: Cache `parsedMinutes` in `Session` objects to reduce repetitive regex evaluation during filtering, sorting, and aggregate sum calculations.
- **Project Persistence**: Implement `updateProject(context, project)` directly in `SessionRepository`.

#### [MainViewModel.kt](file:///C:/projects/metanoia/app/src/main/java/com/example/myapplicationtoday/MainViewModel.kt)
- Refactor `updateProject()` to delegate directly to `SessionRepository.updateProject()`, eliminating duplicate JSON string manipulation in ViewModel.

#### [AppDatabase.kt](file:///C:/projects/metanoia/app/src/main/java/com/example/myapplicationtoday/AppDatabase.kt)
- Update `SessionDaoReplacement.getAllSessions()` to return all sessions safely instead of restricting output to today's sessions.

---

### 3. Persistent Inline Validation on Auth Forms

#### [activity_auth.xml](file:///C:/projects/metanoia/app/src/main/res/layout/activity_auth.xml)
- Add dedicated inline error `TextView`s:
  - `tvErrorAuthName` under `etAuthName`
  - `tvErrorAuthEmail` under `etAuthEmail`
  - `tvErrorAuthPassword` under `etAuthPassword`
  - `tvAuthGlobalError` above the submission button for authentication failure messages

#### [AuthActivity.kt](file:///C:/projects/metanoia/app/src/main/java/com/example/myapplicationtoday/AuthActivity.kt)
- Remove `Toast.makeText` calls for form validation and auth error display.
- Set persistent inline error text on target fields when validation fails or Firebase operations return exceptions.
- Clear inline errors dynamically when switching tabs or editing input fields via `TextWatcher`s.

---

## Verification Plan

### Automated Tests
- Run full unit test suite:
  `gradle_build("app:testDebugUnitTest")`
- Add new unit tests covering:
  - `AuthActivity` validation rules and error clearing.
  - Thread-safe `SessionRepository` operations under concurrent reads/writes.
  - Pre-compiled regex and duration parsing logic.

### Manual Verification
- Verify layout rendering across small mobile screens, tablets, and desktop/landscape viewports.
- Validate persistent inline error messages on `AuthActivity` for missing name, invalid email, short password, and Firebase auth errors.
