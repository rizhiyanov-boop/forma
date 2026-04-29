# LiveCoach: Change Log After Review

Date: 2026-04-28

## Fixed From Review

1. JSON field mismatch in coach content pool:
- `CoachContentPool.exerciseId` changed to `CoachContentPool.contentKey`.
- This aligns parser with assets (`contentKey`) and unblocks content rendering on device.

2. Missing content file for light biceps:
- Preset mapping changed from `biceps-curl-light` to `biceps-curl`.
- This ensures Wednesday light biceps now resolves to an existing pool file.

3. Minor cleanup:
- Removed redundant `open` from `CoachContentAssetLoader` (tests rely on `CoachContentPoolSource` interface).

## UX/Feature Changes (This Iteration)

1. Rest coach content is now embedded on screen (no bottom popup overlay):
- Removed animated bottom popup behavior from `WorkoutScreen`.
- Rest timer and coach content are shown inline in the main screen flow.

2. Three simultaneous coach cards during rest:
- `FACT`, `TECHNIQUE`, and `MOTIVATION` are shown together with timer.
- Cards are displayed with current design-system components (`FormaCard`, theme colors, typography).

3. Per-card tap-to-next behavior:
- Tapping each card cycles only that card’s content.
- Fact, technique, and motivation have independent decks and indices.

4. Content selection improvements:
- Repository now supports type-aware picks (`preferredType`) and local exclusions (`excludedIds`) to reduce duplicates within the same rest block.
- ViewModel builds per-type decks and tracks shown items.

## Workout Flow Refresh (Current Iteration)

1. Manual table-like inputs removed from the workout plan screen:
- `WorkoutScreen` now shows flow cards and set status, without inline text fields.

2. New dedicated set-entry screen:
- Added `SetEntryScreen` for one specific set (`setId`) with tap controls for reps, weight, and RIR.
- Save action completes the set and transitions into rest flow.

3. Rest moved to dedicated screen:
- Added `RestScreen` with timer + three coach cards in one view.
- Tapping each card cycles independently.
- User can return to plan at any moment and continue flow without hard context switch.

4. Navigation updates:
- Added new routes: `SetEntry` and `Rest`.
- Seamless transitions:
  - `Workout -> SetEntry -> Rest -> next SetEntry` or back to `Workout`.

## Stage 2 (Wellness + UI) Implementation

Date: 2026-04-28

1. Block A: UI/UX fixes
- Set entry weight step is now dynamic via `ProgressionEngine.stepFor(...)` and user level.
- RIR default now falls back to plan target (`targetSetsDetailed.rirTarget`) before hardcoded `2`.
- Rest timer redesigned as a full-size hero (`RestTimerHero`) with large centered time.
- Added haptic feedback to high-frequency and primary controls:
  - soft: +/- taps, coach-card taps, chips
  - strong: primary actions (save set, next set, finish workout)

2. Block B: Wellness data layer
- Added domain models and repository:
  - `domain/wellness/WellnessModels.kt`
  - `domain/wellness/WellnessRepository.kt`
- Added Room storage:
  - `WellnessEntity`, `WellnessDao`
  - `FormaDatabase` version bump `6 -> 7`
  - DAO provider + DI binding (`WellnessRepositoryImpl`)
- Added backup support:
  - `BackupSnapshot.CURRENT_VERSION` bumped `3 -> 4`
  - `wellnessLog` included in export/import pipeline

3. Block B: Wellness UI flow
- Pre-workout energy picker integrated directly into `WorkoutScreen` as overlay.
- Picker stores `PRE_WORKOUT` entry once per session and does not reappear for same session.
- Post-workout questionnaire added as dedicated screen:
  - route: `post-wellness/{sessionId}`
  - sections: energy, sleep, stress, mood
  - submit disabled until energy is selected
  - on submit: save `POST_WORKOUT` entry, then run review and navigate to Review/Home

4. Block C (tech debt)
- Added in-VM de-duplication for `markShown` via `shownInThisSession` + `markShownOnce(...)`.
- Unified shared route args with `Route.ARG_SESSION_ID` and `Route.ARG_EXERCISE_ID`.

5. Tests
- Added `WellnessRepositoryTest` with 4 cases:
  - save + get pre-workout
  - null when missing
  - average energy
  - recent-window filtering
