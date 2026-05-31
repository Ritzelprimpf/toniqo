# Phase 7.4 — Requirements & Acceptance Criteria

Phase 7.4 completes the module. It renders 7.3 state to `DESIGN.md` §8.3 and adds no business logic.

## Agent Responsibilities

### Screen & structure
- [ ] `KeyFinderScreen` collects `KeyFinderUiState` via lifecycle-aware collection and forwards intents to the 7.3 ViewModel; no business logic in composables.
- [ ] Header (kicker `KEY FINDER`, title `Match notes`, info button), `NOTES · n` / `TONIC · x` line, input rail, results header, top-7 list — matching the mockup layout.
- [ ] `TONIC PREFERRED` shows only when a root is marked.

### Input rail & chips
- [ ] Input rail: `bg.inset`, `r.md`, min-height 56dp; chips wrap across rows.
- [ ] Note chip: 30dp, `kicker`-mono 12sp; root chip uses the 22% mint mix + `· TONIC` mint suffix.
- [ ] Add-note button: 30×30dp dashed circle with `plus`; opens the picker; present notes are disabled there.
- [ ] Mark-root and remove gestures implemented, discoverable, and non-conflicting.
- [ ] Tap targets ≥ 44×44dp on chips, add button, chevron.

### Result cards
- [ ] Row: 12dp padding, `r.md`; 2-digit zero-padded rank (mono), primary label, badges (`TONIC` mint-outlined, `FULL` neutral-outlined), `mono.micro` subtitle, percent (`h2`; mint for top match, `fg.primary` otherwise), `chevron-right`.
- [ ] First result: 6% mint over `bg.elev1` + mint-mixed border.
- [ ] Labels/subtitles resolved from `ScaleType` resource keys + `ScaleSpeller` root spelling — the only place display strings are assembled.
- [ ] List shows the top 7 from state; updates live with no search/start control.

### Detail view
- [ ] Opens on row tap; self-contained; shows the scale's conventionally-spelled 7 notes with degree labels, the primary label + subtitle, percent, and badges.
- [ ] No navigation to Chord Finder.

### States
- [ ] Idle/empty (< 3 notes): quiet `fg.tertiary` prompt, no spinner, no illustration.
- [ ] Mic toggle with a listening indicator; wires `startListening`/`stopListening`.
- [ ] Mic permission-denied: single card with `mic` icon, explanation, "Grant access" → system settings (reuse tuner pattern).

### Design conformance
- [ ] All colour/type/spacing/radii via `Tq` tokens / `MaterialTheme.colorScheme.*`; no raw hex, sp, or dp literals outside tokens.
- [ ] Respects the `DESIGN.md` §12 don'ts (no emoji, no illustrations, no extra glows, no gradients) and §9 motion / reduced-motion.
- [ ] All user-visible text in `strings.xml`.

### Tests
- [ ] Label-mapping unit test for representative scale types.
- [ ] Compose UI tests: 3 notes → results appear; first-card top-match styling + mint percent; marking root → `TONIC` badge + `TONIC PREFERRED`; card tap → correct detail; < 3-note idle prompt; permission-denied card.

### Documentation Updates
- [ ] `DECISIONS.md`: result row opens a self-contained detail view (no Chord Finder nav); plus the locked UX gesture/picker/detail-form choices.

### Code Quality
- [ ] No `TODO()`. KDoc on public composables' contracts where non-obvious. No magic values.

### Handoff
- [ ] Summary confirms the module is end-to-end and lists the device checks for the user.

## User Responsibilities (Verification in Android Studio / device)
- [ ] Gradle sync + Build succeed; Run All Tests green (incl. tuner regression still green).
- [ ] On device, the full Phase 7 happy path works: add notes by dropdown and by mic; remove a note; mark/unmark a root; results update live with correct percentages, badges, and top-match styling; tapping a result shows the right notes/degrees; permission-denied routes to settings.
- [ ] The screen visually matches `DESIGN.md` §8.3 / the mockup.
- [ ] No crashes or Logcat errors on the happy path.

## Decision Log
- [ ] The detail-view and UX-gesture decisions are recorded in `DECISIONS.md` before the module is closed.

---

## Phase 7 close-out

When 7.4 passes, re-verify the module-level **Completion Criteria** in `Phase7-PLAN.md`, confirm all nine `DECISIONS.md` entries from the phase are present, and update `PROJECT_PLAN.md` to mark Key Finder done and Chord Finder (Phase 8) next.
