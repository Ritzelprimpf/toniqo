# Implementation Prompt — Phase 7.4 (Key Finder UI)

> Paste this to start a fresh implementation session for **Phase 7.4**, the final sub-phase. Phases 7.1 (engine), 7.2 (audio + `NoteDetector`), and 7.3 (ViewModel + `KeyFinderUiState`) must already be merged.

---

You are implementing **Phase 7.4 — Key Finder UI** of Toniqo, a native Android guitar toolkit. You write code; the user owns Android Studio, the build, the emulator/device, and Git. Do not run Gradle, launch emulators, or invoke git. Propose complete file contents the user can apply.

**Before writing any code, read, in this order:**
1. `PROJECT_PLAN.md`, `CLAUDE.md`, `IMPLEMENTATION_NOTES.md`, `DECISIONS.md` (always-read set; note the Phase 7.1–7.3 entries).
2. `DESIGN.md` → **§8.3 (Key Finder)**, §6.1–6.3 (buttons, chips, note pills), §6.6 (cards), §2 (colour), §3 (type), §7 (icons), §9 (motion / reduced motion), §12 (don'ts), §13 (accessibility), §14 (token usage).
3. The **`Key Finder — result list` mockup**.
4. `Phase7-PLAN.md` → "Ranking & Display Rules" and "Conventional Spelling Rule".
5. `Phase7_3-PLAN.md` (the `KeyFinderUiState` contract) and `Phase7_1-PLAN.md` (`ScaleType` label resource keys + `ScaleSpeller`).
6. `Phase7_4-PLAN.md` and `Phase7_4-REQUIREMENTS.md` — your scope for this session. Reuse the tuner's permission-denied pattern from Phase 5.

**Hard constraints (from `CLAUDE.md` / `DESIGN.md`):**
- Kotlin, Jetpack Compose (Material 3). This layer **renders state and forwards intents only — no business logic** in composables.
- All colour/type/spacing/radii via the `Tq` tokens / `MaterialTheme.colorScheme.*`. **No raw hex, sp, or dp literals outside tokens.** All user-visible text in `res/values/strings.xml`.
- Respect `DESIGN.md` §12 (no emoji, no illustrations, no extra glows, no gradients) and §9 motion / reduced-motion. Tap targets ≥ 44×44dp even where visuals are smaller.
- Collect state with `collectAsStateWithLifecycle()`; no local state beyond ephemeral UI (sheet open/closed). KDoc where non-obvious. Tests alongside the code.
- **Stop and ask** rather than guess if anything is ambiguous.

**Lock these UX decisions at the start of the session and record them in `DECISIONS.md` before coding:**
1. **Add-note picker form** — recommended: a compact bottom sheet / menu of the 12 pitch classes, tap to add, present notes disabled. (Sheet vs inline grid.)
2. **Mic affordance placement** — a listen toggle (mic icon, active indicator mirroring the tuner's §8.1 mic language). Where it lives (rail header vs screen header).
3. **Mark-root gesture** — how a chip is marked as root (e.g. tap cycles role, or long-press to mark + tap to remove). Keep it discoverable.
4. **Remove-note gesture** — e.g. an `×` on the chip, or tap-to-remove from a chip menu; must not collide with the mark-root gesture.
5. **Detail view form** — recommended: a bottom sheet (keeps list context). Sheet vs full screen.

**Your task this session:**
1. `keyfinder/presentation/ui/KeyFinderScreen.kt` and sub-composables, rendering `KeyFinderUiState`:
   - Header: kicker `KEY FINDER`, title `Match notes`, info button; the `NOTES · n` / `TONIC · x` line; the `N MATCHES` / `TONIC PREFERRED` results header (`TONIC PREFERRED` only when a root is marked).
   - **Input rail** (`bg.inset`, `r.md`, min-height 56dp, chips wrap across rows). **Note chip** (30dp, `kicker`-mono 12sp); **root chip** = 22% mint mix + `· TONIC` mint suffix. **Add-note button** = 30×30dp dashed circle with `plus`.
   - **Result cards** (top 7 from state): 12dp padding, `r.md`; 2-digit zero-padded rank (mono), primary label, badges (`TONIC` mint-outlined, `FULL` neutral-outlined), `mono.micro` subtitle, percent (`h2`; mint for the top match, `fg.primary` otherwise), `chevron-right`. **First result** = 6% mint over `bg.elev1` + mint-mixed border.
   - **Idle/empty state** (< 3 notes): a quiet `fg.tertiary` prompt — no spinner, no illustration.
2. The add-note picker; wire `addNoteFromPicker`. The mic toggle + listening indicator; wire `startListening`/`stopListening`. The mark-root and remove gestures; wire `toggleRoot` / `removeNote`.
3. **Result detail view** (self-contained): opens on row tap; shows the scale's conventionally-spelled 7 notes with degree labels, the primary label + subtitle, percent, and badges. **No navigation into Chord Finder.**
4. **Mic permission-denied state**: a single `ToniqoCard` with the `mic` icon, an explanation, and a "Grant access" button opening system app settings — reuse the tuner's Phase 5 pattern/component.
5. **Label rendering**: resolve each card's primary label + subtitle from `match.candidate.type`'s string-resource keys with `ScaleSpeller.rootName(...)`; the detail view uses `ScaleSpeller.scaleNoteNames(...)`. This is the **only** place display strings are assembled.

**Tests:**
- A **label-mapping** unit test (pure mapping `ScaleMatch` → primary label + subtitle) for representative types (Major, Natural Minor, Phrygian Dominant, Altered).
- **Compose UI tests** for the key flows: adding 3 notes surfaces results; the first card has top-match styling + mint percent; marking a root adds the `TONIC` badge to the matching card and shows `TONIC PREFERRED`; tapping a card opens the detail view with the correct notes/degrees; the < 3-note idle prompt shows; the permission-denied card renders when permission is absent.

**When done:**
- Append the `DECISIONS.md` entries (result row → self-contained detail view, no Chord Finder nav; plus the locked picker / mic / mark-root / remove / detail-form choices), dated and append-only.
- **Re-verify the module-level Completion Criteria in `Phase7-PLAN.md`** (the full end-to-end happy path) and confirm all nine Phase 7 `DECISIONS.md` entries are present. Update `PROJECT_PLAN.md` to mark Key Finder done and Chord Finder (Phase 8) next.
- Summary: files added/modified and the device checks the user should run (add notes by dropdown and mic; remove; mark/unmark root; live results with correct percentages/badges/top-match styling; detail view; permission-denied → settings; tuner still works; no Logcat errors).
- Organise the proposal to map cleanly to commits (e.g. `feat: add Key Finder screen and result cards`, `feat: add Key Finder detail view and permission-denied state`). Do not commit yourself.

Confirm you have read the listed docs and studied the mockup, have locked the start-of-phase UX decisions, and have no blocking questions — then proceed composable by composable with tests.
