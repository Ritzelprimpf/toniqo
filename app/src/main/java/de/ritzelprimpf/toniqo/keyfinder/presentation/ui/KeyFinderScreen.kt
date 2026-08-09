package de.ritzelprimpf.toniqo.keyfinder.presentation.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.common.model.Note
import de.ritzelprimpf.toniqo.common.model.NoteName
import de.ritzelprimpf.toniqo.common.model.ScaleType
import de.ritzelprimpf.toniqo.common.util.ScaleSpeller
import de.ritzelprimpf.toniqo.keyfinder.domain.model.ScaleCandidate
import de.ritzelprimpf.toniqo.keyfinder.domain.model.ScaleMatch
import de.ritzelprimpf.toniqo.keyfinder.domain.usecase.MatchScalesUseCase
import de.ritzelprimpf.toniqo.keyfinder.presentation.scaleDegreeLabel
import de.ritzelprimpf.toniqo.keyfinder.presentation.scaleLabelData
import de.ritzelprimpf.toniqo.keyfinder.presentation.util.findActivity
import de.ritzelprimpf.toniqo.keyfinder.presentation.util.handleKeyFinderMicAccess
import de.ritzelprimpf.toniqo.keyfinder.presentation.util.hasMicPermission
import de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel.KeyFinderScreenViewModel
import de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel.KeyFinderUiState
import de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel.KeyFinderViewModel
import de.ritzelprimpf.toniqo.keyfinder.presentation.viewmodel.NoteChip
import de.ritzelprimpf.toniqo.ui.components.PulsingDot
import de.ritzelprimpf.toniqo.ui.components.ToniqoCard
import de.ritzelprimpf.toniqo.ui.theme.Tq
import de.ritzelprimpf.toniqo.ui.theme.ToniqoTheme

// ─── Screen entry point ───────────────────────────────────────────────────────

/**
 * Key Finder screen. Collects [KeyFinderUiState] and forwards user intents to [viewModel].
 * No business logic here — this layer renders state only.
 *
 * Permission state is managed locally: the screen requests `RECORD_AUDIO` when the user taps
 * the mic toggle and uses [rememberSaveable] to track whether a request has already been made
 * (needed for the permanently-denied heuristic).
 *
 * @param testMicPermissionDenied Forces the mic-permission-denied card for Compose UI tests.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyFinderScreen(
    viewModel: KeyFinderScreenViewModel = hiltViewModel<KeyFinderViewModel>(),
    @VisibleForTesting testMicPermissionDenied: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── Permission tracking ──────────────────────────────────────────────────
    var hasMicPerm by rememberSaveable { mutableStateOf(hasMicPermission(context)) }
    var hasRequestedMicPerm by rememberSaveable { mutableStateOf(false) }

    // Re-check on every RESUME so a permission grant from system settings takes effect.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hasMicPerm = hasMicPermission(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasRequestedMicPerm = true
            hasMicPerm = granted
            if (granted) viewModel.startListening()
        },
    )

    val showMicPermCard = testMicPermissionDenied || (hasRequestedMicPerm && !hasMicPerm)
    val activity = context.findActivity() as? Activity

    fun onMicToggle() {
        if (uiState.isListening) {
            viewModel.stopListening()
        } else if (hasMicPerm) {
            viewModel.startListening()
        } else {
            handleKeyFinderMicAccess(activity, permissionLauncher, hasRequestedMicPerm)
        }
    }

    // ── Ephemeral UI state ───────────────────────────────────────────────────
    var showPickerSheet by rememberSaveable { mutableStateOf(false) }
    var selectedResult by remember { mutableStateOf<ScaleMatch?>(null) }
    var showInfoDialog by rememberSaveable { mutableStateOf(false) }

    // ── Layout ───────────────────────────────────────────────────────────────
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Tq.Color.BgBase)
            .padding(top = Tq.Sp.s5),
        contentPadding = PaddingValues(start = Tq.Sp.s5, end = Tq.Sp.s5, bottom = Tq.Sp.s4),
    ) {
        item {
            KeyFinderHeader(
                noteCount = uiState.notes.size,
                rootPitchClass = uiState.rootPitchClass,
                notes = uiState.notes,
                isListening = uiState.isListening,
                onMicToggle = ::onMicToggle,
                onClearAll = { viewModel.clearAll() },
                onInfoClick = { showInfoDialog = true },
            )
            Spacer(Modifier.height(Tq.Sp.s3))
        }
        item {
            NoteInputRail(
                notes = uiState.notes,
                hasRoot = uiState.rootPitchClass != null,
                onToggleRoot = { viewModel.toggleRoot(it) },
                onRemove = { viewModel.removeNote(it) },
                onAddNote = { showPickerSheet = true },
            )
            Spacer(Modifier.height(Tq.Sp.s4))
        }
        when {
            showMicPermCard -> item { MicPermissionDeniedCard(
                onGrantAccess = {
                    handleKeyFinderMicAccess(activity, permissionLauncher, hasRequestedMicPerm)
                }
            ) }
            uiState.results.isEmpty() -> item {
                IdlePrompt(
                    hasEnoughNotesToMatch = uiState.notes.size >= MatchScalesUseCase.MIN_NOTES_TO_MATCH,
                    onAddNote = { showPickerSheet = true },
                )
            }
            else -> {
                item {
                    ResultsHeader(
                        matchCount = uiState.matchCount,
                        hasRoot = uiState.rootPitchClass != null,
                    )
                    Spacer(Modifier.height(Tq.Sp.s2))
                }
                items(uiState.results) { match ->
                    ResultCard(
                        match = match,
                        isFirst = match.rank == 1,
                        onTap = { selectedResult = match },
                    )
                    Spacer(Modifier.height(Tq.Sp.s2))
                }
            }
        }
        // Bottom breathing room for the bottom nav bar
        item { Spacer(Modifier.height(Tq.Sp.s12)) }
    }

    // ── Bottom sheets ────────────────────────────────────────────────────────
    if (showPickerSheet) {
        NotePickerSheet(
            presentPitchClasses = uiState.notes.map { it.pitchClass }.toSet(),
            onAddNote = { pitchClass ->
                viewModel.addNoteFromPicker(Note(name = NoteName.entries[pitchClass], octave = 4))
            },
            onDismiss = { showPickerSheet = false },
        )
    }

    selectedResult?.let { match ->
        ScaleDetailSheet(match = match, onDismiss = { selectedResult = null })
    }

    if (showInfoDialog) {
        InfoDialog(onDismiss = { showInfoDialog = false })
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

/**
 * Screen header: kicker "KEY FINDER", H1 title "Match notes", info button, sub-header line
 * with "NOTES · n" / "TONIC · x", and the mic toggle.
 */
@Composable
private fun KeyFinderHeader(
    noteCount: Int,
    rootPitchClass: Int?,
    notes: List<NoteChip>,
    isListening: Boolean,
    onMicToggle: () -> Unit,
    onClearAll: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Kicker + H1 title, with info button floating at top-end so its 48dp
        // touch target does not inflate the row and push the H1 down.
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = stringResource(R.string.keyfinder_kicker),
                    style = Tq.Type.Kicker,
                    color = Tq.Color.FgTertiary,
                )
                Spacer(Modifier.height(Tq.Sp.s2))
                Text(
                    text = stringResource(R.string.keyfinder_screen_title),
                    style = Tq.Type.H1,
                    color = Tq.Color.FgPrimary,
                )
            }
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.keyfinder_cd_info),
                    tint = Tq.Color.FgTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(Tq.Sp.s2))
        // Sub-header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // NOTES · n
            Text(
                text = stringResource(R.string.keyfinder_notes_count, noteCount),
                style = Tq.Type.MonoMicro,
                color = Tq.Color.FgTertiary,
            )
            // TONIC · x (only when a root is marked)
            rootPitchClass?.let { pc ->
                val tonicName = notes.firstOrNull { it.pitchClass == pc }?.displayName ?: ""
                Spacer(Modifier.width(Tq.Sp.s3))
                Text(
                    text = stringResource(R.string.keyfinder_tonic_marker, tonicName),
                    style = Tq.Type.MonoMicro,
                    color = Tq.Color.SignalMint,
                )
            }
            Spacer(Modifier.weight(1f))
            // CLEAR button — only shown when there are notes to clear
            if (noteCount > 0) {
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .border(1.dp, Tq.Color.LineFaint, RoundedCornerShape(Tq.Radius.Pill))
                        .clip(RoundedCornerShape(Tq.Radius.Pill))
                        .clickable(onClick = onClearAll)
                        .padding(horizontal = Tq.Sp.s3),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.keyfinder_clear_all),
                        style = Tq.Type.MonoMicro,
                        color = Tq.Color.FgSecondary,
                    )
                }
                Spacer(Modifier.width(Tq.Sp.s2))
            }
            // MIC LIVE indicator (shown when listening)
            if (isListening) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s1),
                ) {
                    PulsingDot(color = Tq.Color.SignalMint, dotSize = 6.dp)
                    Text(
                        text = stringResource(R.string.keyfinder_mic_live),
                        style = Tq.Type.MonoMicro,
                        color = Tq.Color.SignalMint,
                    )
                    Spacer(Modifier.width(Tq.Sp.s1))
                }
            }
            // Mic toggle button (44×44dp tap target via IconButton)
            IconButton(
                onClick = onMicToggle,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = stringResource(
                        if (isListening) R.string.keyfinder_cd_mic_on else R.string.keyfinder_cd_mic_off
                    ),
                    tint = if (isListening) Tq.Color.SignalMint else Tq.Color.FgSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ─── Note input rail ──────────────────────────────────────────────────────────

/**
 * The chip rail where the user's entered notes are shown. `bg.inset`, `r.md`, min-height 56dp.
 * Chips wrap to a new row when the rail is full. The add-note button always follows the last chip.
 *
 * When notes are present but none is marked as root, a short hint line is shown below the rail
 * so the user knows tapping a chip body marks it as the tonic key.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteInputRail(
    notes: List<NoteChip>,
    hasRoot: Boolean,
    onToggleRoot: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAddNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .background(Tq.Color.BgInset, RoundedCornerShape(Tq.Radius.Md))
                .padding(Tq.Sp.s2),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2),
                verticalArrangement = Arrangement.spacedBy(Tq.Sp.s2),
            ) {
                notes.forEach { chip ->
                    NoteChipItem(
                        chip = chip,
                        onToggleRoot = { onToggleRoot(chip.pitchClass) },
                        onRemove = { onRemove(chip.pitchClass) },
                    )
                }
                AddNoteButton(onClick = onAddNote)
            }
        }
        // Hint: visible when there are notes but no tonic is set yet
        if (notes.isNotEmpty() && !hasRoot) {
            Spacer(Modifier.height(Tq.Sp.s1))
            Text(
                text = stringResource(R.string.keyfinder_hint_set_tonic),
                style = Tq.Type.MonoMicro,
                color = Tq.Color.FgQuaternary,
            )
        }
    }
}

/**
 * A single note chip in the input rail.
 *
 * - **Tap** → [onRemove] (remove the note; matches the Material InputChip convention).
 * - **Long-press** → [onToggleRoot] (mark/unmark as the tonic key).
 *
 * No dedicated × button — tapping the whole chip removes it. The hint text below the rail
 * ("TAP TO REMOVE · HOLD TO SET AS TONIC") explains both gestures. The outer [Box] ensures
 * the ≥44dp tap target height even though the chip visual is 30dp.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteChipItem(
    chip: NoteChip,
    onToggleRoot: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Not wrapped in remember{} — Tq.Color is a theme-reactive composable getter, and lerp() on
    // two colours is cheap enough that memoizing it isn't worth losing that reactivity for.
    val rootChipBg = if (chip.isRoot) lerp(Tq.Color.BgElev2, Tq.Color.SignalMint, 0.22f)
        else Tq.Color.BgElev2
    val longClickLabel = stringResource(
        if (chip.isRoot) R.string.keyfinder_cd_unmark_tonic else R.string.keyfinder_cd_mark_tonic
    )

    Box(
        modifier = modifier.heightIn(min = 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(30.dp)
                .clip(RoundedCornerShape(Tq.Radius.Pill))
                .background(rootChipBg)
                .combinedClickable(
                    onClick = onRemove,
                    onLongClick = onToggleRoot,
                    onLongClickLabel = longClickLabel,
                )
                .padding(horizontal = Tq.Sp.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = chip.displayName,
                style = Tq.Type.NumericS,
                color = Tq.Color.FgPrimary,
            )
            if (chip.isRoot) {
                Text(
                    text = stringResource(R.string.keyfinder_tonic_suffix),
                    style = Tq.Type.MonoMicro,
                    color = Tq.Color.SignalMint,
                )
            }
        }
    }
}

/**
 * 30×30dp dashed-border circle with a `plus` icon. Tap target extended to 44×44dp by the
 * surrounding [Box].
 */
@Composable
private fun AddNoteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Captured here — Tq.Color is a theme-reactive composable getter, not readable from inside
    // the non-composable Canvas draw lambda below.
    val dashColor = Tq.Color.Line

    Box(
        modifier = modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(30.dp)) {
            val strokeWidth = 1.25.dp.toPx()
            drawCircle(
                color = dashColor,
                radius = size.minDimension / 2f - strokeWidth / 2f,
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f),
                ),
            )
        }
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.keyfinder_cd_add_note),
            tint = Tq.Color.FgTertiary,
            modifier = Modifier.size(14.dp),
        )
    }
}

// ─── Results ─────────────────────────────────────────────────────────────────

/**
 * Results header: "N MATCHES" on the left, "TONIC PREFERRED" on the right when [hasRoot].
 */
@Composable
private fun ResultsHeader(
    matchCount: Int,
    hasRoot: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.keyfinder_matches_header, matchCount),
            style = Tq.Type.MonoMicro,
            color = Tq.Color.FgTertiary,
        )
        if (hasRoot) {
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.keyfinder_tonic_preferred),
                style = Tq.Type.MonoMicro,
                color = Tq.Color.SignalMint,
            )
        }
    }
}

/**
 * A single result card row.
 *
 * Layout: zero-padded rank (mono) + primary label + badges on the left column, `mono.micro`
 * subtitle below; percent (`h2`, mint for the top match) + chevron on the right.
 *
 * The first result ([isFirst]) gets a 6% mint-mixed background and mint-mixed border.
 * All colours, spacing, and radii come from `Tq` tokens.
 */
@Composable
private fun ResultCard(
    match: ScaleMatch,
    isFirst: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val labelData = remember(match) { scaleLabelData(match) }
    val primaryLabel = resolveScaleString(labelData.primaryLabelKey, labelData.spelledRoot)
    val subtitle = resolveScaleString(labelData.subtitleKey, labelData.spelledRoot)

    // Not wrapped in remember{} — Tq.Color is a theme-reactive composable getter, and lerp() on
    // two colours is cheap enough that memoizing it isn't worth losing that reactivity for.
    val cardBg = if (isFirst) lerp(Tq.Color.BgElev1, Tq.Color.SignalMint, 0.06f)
        else Tq.Color.BgElev1
    val borderColor = if (isFirst) Tq.Color.SignalMint.copy(alpha = 0.30f) else Tq.Color.LineFaint
    val percentColor = if (isFirst) Tq.Color.SignalMint else Tq.Color.FgPrimary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(Tq.Radius.Md))
            .border(1.dp, borderColor, RoundedCornerShape(Tq.Radius.Md))
            .clickable(onClick = onTap)
            .padding(Tq.Sp.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2),
            ) {
                // Zero-padded rank
                Text(
                    text = "%02d".format(match.rank),
                    style = Tq.Type.MonoMicro,
                    color = Tq.Color.FgTertiary,
                )
                Text(
                    text = primaryLabel,
                    style = Tq.Type.BodyStrong,
                    color = Tq.Color.FgPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (match.isRootMatch) ScaleBadge(stringResource(R.string.keyfinder_badge_tonic), isMint = true)
                if (match.isFull) ScaleBadge(stringResource(R.string.keyfinder_badge_full), isMint = false)
            }
            Spacer(Modifier.height(Tq.Sp.s1))
            Text(
                text = subtitle,
                style = Tq.Type.MonoMicro,
                color = Tq.Color.FgTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(Tq.Sp.s2))
        // Percent + chevron
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s1),
        ) {
            Text(
                text = "${match.percent}%",
                style = Tq.Type.H2,
                color = percentColor,
            )
            // 44dp tap target wrapping the 18dp chevron icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onTap),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Tq.Color.FgTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * Small outlined badge used in result rows and the detail sheet.
 *
 * @param isMint `true` → mint outline + mint text (`TONIC`); `false` → neutral (`FULL`).
 */
@Composable
private fun ScaleBadge(label: String, isMint: Boolean, modifier: Modifier = Modifier) {
    val borderColor = if (isMint) Tq.Color.SignalMint.copy(alpha = 0.5f) else Tq.Color.Line
    val textColor = if (isMint) Tq.Color.SignalMint else Tq.Color.FgSecondary
    Box(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(Tq.Radius.Sm))
            .padding(horizontal = Tq.Sp.s1, vertical = Tq.Sp.s1),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = Tq.Type.MonoMicro, color = textColor)
    }
}

// ─── Empty / idle state ───────────────────────────────────────────────────────

/**
 * Shown when there are no scale matches yet. While the note count is still below
 * [MatchScalesUseCase.MIN_NOTES_TO_MATCH] ([hasEnoughNotesToMatch] false), a large mint CTA
 * button is shown above the hint text — the small dashed add-button in the chip rail above isn't
 * a strong enough call-to-action on an otherwise-empty screen, and the user has more notes to add
 * before a match is even possible. Once enough notes are present, only the hint text is shown
 * (results stay empty here only because none of them matched), matching the original design.
 */
@Composable
private fun IdlePrompt(
    hasEnoughNotesToMatch: Boolean,
    onAddNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Tq.Sp.s8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!hasEnoughNotesToMatch) {
            BigAddNoteButton(onClick = onAddNote)
            Spacer(Modifier.height(Tq.Sp.s4))
        }
        Text(
            text = stringResource(R.string.keyfinder_idle_prompt),
            style = Tq.Type.Body,
            color = Tq.Color.FgTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Large first-visit call-to-action: a filled `signal.mint` circle with a `+` icon. Deliberately
 * does not use the 24dp glow reserved for `btn.primary` at its 52dp variant / the bottom-nav
 * indicator (DESIGN.md §10) — those are the only two glows the design language permits (§12).
 */
@Composable
private fun BigAddNoteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Tq.Color.SignalMint)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.keyfinder_cd_add_note),
            tint = Tq.Color.BgBase,
            modifier = Modifier.size(28.dp),
        )
    }
}

// ─── Info dialog ────────────────────────────────────────────────────────────

/**
 * Explains what the matcher needs from the user, shown via the header's info button.
 * Mirrors Chord Finder's `InfoDialog` (`ChordFinderScreen.kt`) — same `AlertDialog` structure.
 */
@Composable
private fun InfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.keyfinder_info_dialog_title),
                style = Tq.Type.H2,
                color = Tq.Color.FgPrimary,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.keyfinder_info_dialog_body),
                style = Tq.Type.Body,
                color = Tq.Color.FgSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.keyfinder_info_dialog_ok),
                    style = Tq.Type.Body,
                    color = Tq.Color.SignalMint,
                )
            }
        },
    )
}

// ─── Mic permission denied card ───────────────────────────────────────────────

/**
 * Shown when the user taps the mic toggle but `RECORD_AUDIO` is denied.
 *
 * Structure mirrors the tuner's PermissionDeniedCard (DESIGN.md §8.1): mic icon + slash,
 * H2 heading, body, `btn.primary` → "Grant access". The tuner's component uses tuner-specific
 * strings, so a separate composable is created here with Key Finder–specific copy.
 */
@Composable
private fun MicPermissionDeniedCard(
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ToniqoCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Tq.Sp.s3),
        ) {
            Spacer(Modifier.height(Tq.Sp.s2))
            // Captured here — Tq.Color is a theme-reactive composable getter, not readable from
            // inside the non-composable Canvas draw lambda below.
            val slashColor = Tq.Color.FgSecondary
            Box(modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = null,
                    tint = Tq.Color.FgSecondary,
                    modifier = Modifier.size(28.dp),
                )
                Canvas(modifier = Modifier.size(28.dp)) {
                    drawLine(
                        color = slashColor,
                        start = Offset(size.width * 0.75f, size.height * 0.10f),
                        end = Offset(size.width * 0.25f, size.height * 0.90f),
                        strokeWidth = 1.25.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            Text(
                text = stringResource(R.string.keyfinder_permission_heading),
                style = Tq.Type.H2,
                color = Tq.Color.FgPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.keyfinder_permission_body),
                style = Tq.Type.Body,
                color = Tq.Color.FgSecondary,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
            Button(
                onClick = onGrantAccess,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Tq.Color.SignalMint,
                    contentColor = Tq.Color.BgBase,
                ),
            ) {
                Text(
                    text = stringResource(R.string.keyfinder_permission_button),
                    style = Tq.Type.BodyStrong,
                )
            }
            Spacer(Modifier.height(Tq.Sp.s2))
        }
    }
}

// ─── Note picker sheet ────────────────────────────────────────────────────────

/**
 * Bottom sheet listing all 12 pitch classes in a 4×3 grid. Tapping a cell adds the note and
 * leaves the sheet open — cells whose pitch class is already in [presentPitchClasses] are shown
 * disabled, so this doubles as an at-a-glance record of what's been added so far. The sheet only
 * closes via [onDismiss] (swipe-down / tap-outside), letting the user add several notes in one
 * sitting instead of reopening the sheet after each one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotePickerSheet(
    presentPitchClasses: Set<Int>,
    onAddNote: (pitchClass: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Tq.Color.BgElev1,
        contentColor = Tq.Color.FgPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Tq.Sp.s5, vertical = Tq.Sp.s4),
        ) {
            Text(
                text = stringResource(R.string.keyfinder_picker_title),
                style = Tq.Type.Kicker,
                color = Tq.Color.FgTertiary,
            )
            Spacer(Modifier.height(Tq.Sp.s1))
            Text(
                text = stringResource(R.string.keyfinder_picker_subtitle),
                style = Tq.Type.Caption,
                color = Tq.Color.FgTertiary,
            )
            Spacer(Modifier.height(Tq.Sp.s4))
            // 4×3 grid of pitch class buttons
            listOf(0..3, 4..7, 8..11).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2),
                ) {
                    row.forEach { pc ->
                        val isPresent = pc in presentPitchClasses
                        PickerNoteButton(
                            noteName = ScaleSpeller.ROOT_DISPLAY_NAMES[pc],
                            isPresent = isPresent,
                            onClick = { if (!isPresent) onAddNote(pc) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(Tq.Sp.s2))
            }
            Spacer(Modifier.height(Tq.Sp.s4))
        }
    }
}

@Composable
private fun PickerNoteButton(
    noteName: String,
    isPresent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(Tq.Radius.Sm))
            .background(if (isPresent) Tq.Color.BgElev3 else Tq.Color.BgElev2)
            .clickable(enabled = !isPresent, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = noteName,
            style = Tq.Type.NumericS,
            color = if (isPresent) Tq.Color.FgQuaternary else Tq.Color.FgPrimary,
        )
    }
}

// ─── Scale detail sheet ───────────────────────────────────────────────────────

/**
 * Bottom sheet showing the full detail of a single [ScaleMatch]: primary label, subtitle,
 * percent + badges, and the 7 conventionally-spelled scale notes with degree labels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScaleDetailSheet(
    match: ScaleMatch,
    onDismiss: () -> Unit,
) {
    val labelData = remember(match) { scaleLabelData(match) }
    val primaryLabel = resolveScaleString(labelData.primaryLabelKey, labelData.spelledRoot)
    val subtitle = resolveScaleString(labelData.subtitleKey, labelData.spelledRoot)
    val noteNames = remember(match) {
        ScaleSpeller.scaleNoteNames(match.candidate.rootPitchClass, match.candidate.type)
    }
    val degreeLabels = remember(match) {
        match.candidate.type.intervalsFromRoot.mapIndexed { i, interval ->
            scaleDegreeLabel(i, interval)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Tq.Color.BgElev1,
        contentColor = Tq.Color.FgPrimary,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Tq.Sp.s5),
        ) {
            // Label header
            Text(primaryLabel, style = Tq.Type.H2, color = Tq.Color.FgPrimary)
            Spacer(Modifier.height(Tq.Sp.s1))
            Text(subtitle, style = Tq.Type.MonoMicro, color = Tq.Color.FgTertiary)
            Spacer(Modifier.height(Tq.Sp.s3))
            // Percent + badges row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tq.Sp.s2),
            ) {
                val percentColor = if (match.rank == 1) Tq.Color.SignalMint else Tq.Color.FgPrimary
                Text("${match.percent}%", style = Tq.Type.H2, color = percentColor)
                if (match.isRootMatch) ScaleBadge(stringResource(R.string.keyfinder_badge_tonic), isMint = true)
                if (match.isFull) ScaleBadge(stringResource(R.string.keyfinder_badge_full), isMint = false)
            }
            Spacer(Modifier.height(Tq.Sp.s4))
            HorizontalDivider(color = Tq.Color.LineFaint, thickness = 1.dp)
            Spacer(Modifier.height(Tq.Sp.s4))
            // Notes list
            Text(
                text = stringResource(R.string.keyfinder_detail_notes_label),
                style = Tq.Type.Kicker,
                color = Tq.Color.FgTertiary,
            )
            Spacer(Modifier.height(Tq.Sp.s2))
            Column(verticalArrangement = Arrangement.spacedBy(Tq.Sp.s2)) {
                noteNames.forEachIndexed { i, noteName ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Degree label in a fixed-width slot
                        Text(
                            text = degreeLabels[i],
                            style = Tq.Type.MonoMicro,
                            color = Tq.Color.FgTertiary,
                            modifier = Modifier.width(Tq.Sp.s8),  // 32dp
                        )
                        Text(
                            text = noteName,
                            style = Tq.Type.Body,
                            color = Tq.Color.FgPrimary,
                        )
                    }
                }
            }
            // Extra space below content (above system gesture bar)
            Spacer(Modifier.height(Tq.Sp.s6))
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Resolves a scale-type string-resource key at runtime using [android.content.res.Resources.getIdentifier].
 *
 * Results are cached with [remember] so the reflection lookup only runs once per unique
 * (key, arg) pair. Falls back to "$arg $key" if the resource is not found.
 */
@Composable
private fun resolveScaleString(key: String, arg: String): String {
    val context = LocalContext.current
    return remember(key, arg) {
        val resId = context.resources.getIdentifier(key, "string", context.packageName)
        if (resId != 0) context.getString(resId, arg) else "$arg $key"
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(name = "KeyFinder — idle dark", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun PreviewIdle() {
    ToniqoTheme(useDarkTheme = true) {
        KeyFinderScreen(
            viewModel = PreviewViewModel(KeyFinderUiState()),
        )
    }
}

@Preview(name = "KeyFinder — results dark", showBackground = true, backgroundColor = 0xFF1A1F22)
@Composable
private fun PreviewResults() {
    val results = listOf(
        ScaleMatch(ScaleCandidate(0, ScaleType.IONIAN), percent = 100, isFull = true, isRootMatch = false, rank = 1),
        ScaleMatch(ScaleCandidate(9, ScaleType.AEOLIAN), percent = 88, isFull = false, isRootMatch = false, rank = 2),
        ScaleMatch(ScaleCandidate(2, ScaleType.DORIAN), percent = 86, isFull = false, isRootMatch = false, rank = 3),
    )
    val state = KeyFinderUiState(
        notes = listOf(
            NoteChip(0, "C", false),
            NoteChip(4, "E", false),
            NoteChip(7, "G", false),
        ),
        results = results,
        matchCount = results.size,
    )
    ToniqoTheme(useDarkTheme = true) {
        KeyFinderScreen(viewModel = PreviewViewModel(state))
    }
}

/** Thin preview-only ViewModel stub. */
private class PreviewViewModel(
    private val state: KeyFinderUiState,
) : KeyFinderScreenViewModel {
    override val uiState = kotlinx.coroutines.flow.MutableStateFlow(state)
    override fun addNoteFromPicker(note: Note) = Unit
    override fun removeNote(pitchClass: Int) = Unit
    override fun toggleRoot(pitchClass: Int) = Unit
    override fun clearAll() = Unit
    override fun startListening() = Unit
    override fun stopListening() = Unit
}
