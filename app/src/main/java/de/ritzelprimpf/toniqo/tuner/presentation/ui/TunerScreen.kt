package de.ritzelprimpf.toniqo.tuner.presentation.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.tuner.domain.model.TuningStatus
import de.ritzelprimpf.toniqo.tuner.presentation.mapping.toSignalColor
import de.ritzelprimpf.toniqo.tuner.presentation.ui.components.DetectedNoteHero
import de.ritzelprimpf.toniqo.tuner.presentation.ui.components.HzReadoutPair
import de.ritzelprimpf.toniqo.tuner.presentation.ui.components.NeedleGauge
import de.ritzelprimpf.toniqo.tuner.presentation.ui.components.PermissionDeniedCard
import de.ritzelprimpf.toniqo.tuner.presentation.ui.components.PresetChip
import de.ritzelprimpf.toniqo.tuner.presentation.ui.components.PresetPickerSheet
import de.ritzelprimpf.toniqo.tuner.presentation.ui.components.ReadoutWell
import de.ritzelprimpf.toniqo.tuner.presentation.ui.components.ReferencePitchKicker
import de.ritzelprimpf.toniqo.tuner.presentation.ui.components.StatusLine
import de.ritzelprimpf.toniqo.tuner.presentation.ui.components.StringSelectorRow
import de.ritzelprimpf.toniqo.tuner.presentation.ui.components.SuccessRing
import de.ritzelprimpf.toniqo.tuner.presentation.ui.components.TunerSettingsSheet
import de.ritzelprimpf.toniqo.tuner.presentation.util.allTunedHaptic
import de.ritzelprimpf.toniqo.tuner.presentation.util.findActivity
import de.ritzelprimpf.toniqo.tuner.presentation.util.handleGrantAccess
import de.ritzelprimpf.toniqo.tuner.presentation.util.stringAdvancedHaptic
import de.ritzelprimpf.toniqo.tuner.presentation.util.tunedStringHaptic
import de.ritzelprimpf.toniqo.tuner.presentation.viewmodel.TunerEvent
import de.ritzelprimpf.toniqo.tuner.presentation.viewmodel.TunerScreenViewModel
import de.ritzelprimpf.toniqo.tuner.presentation.viewmodel.TunerViewModel
import de.ritzelprimpf.toniqo.ui.components.ScreenHeader
import de.ritzelprimpf.toniqo.ui.components.ToniqoCard
import de.ritzelprimpf.toniqo.ui.theme.Tq
import kotlinx.coroutines.delay

/**
 * Full Guitar Tuner screen — wires the audio pipeline (Phases 5.1–5.3) into UI.
 *
 * Accepts [TunerScreenViewModel] as an interface rather than [TunerViewModel] directly so that
 * Compose UI tests can inject a fake via `createComposeRule().setContent { TunerScreen(viewModel = fake) }`
 * without requiring Hilt. In production, the Hilt default is used transparently.
 *
 * Event collection uses [LaunchedEffect] keyed on `(viewModel.events, lifecycleOwner)` with an
 * inner `repeatOnLifecycle(STARTED)`. This prevents duplicate haptics on rotation and avoids
 * replaying buffered events after resume. See DECISIONS.md entry 8 for the full rationale.
 */
@Composable
fun TunerScreen(
    viewModel: TunerScreenViewModel = hiltViewModel<TunerViewModel>(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val activity = context.findActivity() as? Activity

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { viewModel.onPermissionRequested() },
    )

    // Short mint ring flash on the readout well, driven by TunerEvent.StringAdvanced below —
    // reuses SuccessRing's existing fade animation with a much shorter hold than the
    // ALL_STRINGS_TUNED celebration, since this just needs to catch the eye, not celebrate.
    var advanceFlashVisible by remember { mutableStateOf(false) }

    // Keys ensure the effect re-launches only on actual identity changes.
    // repeatOnLifecycle(STARTED) pauses collection while backgrounded and resumes on foreground
    // without re-running the outer LaunchedEffect, preventing duplicate haptics on rotation.
    LaunchedEffect(viewModel.events, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is TunerEvent.StringTuned -> haptic.tunedStringHaptic()
                    is TunerEvent.StringAdvanced -> {
                        haptic.stringAdvancedHaptic()
                        advanceFlashVisible = true
                        delay(ADVANCE_FLASH_HOLD_MS)
                        advanceFlashVisible = false
                    }
                    TunerEvent.AllStringsTuned -> haptic.allTunedHaptic()
                    TunerEvent.EnteredChromaticMode -> Unit
                }
            }
        }
    }

    // Restart the pipeline when returning to the foreground — covers the "permanently denied,
    // went to app settings, granted manually" path where onResult never fires.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var presetSheetOpen by rememberSaveable { mutableStateOf(false) }
    var settingsSheetOpen by rememberSaveable { mutableStateOf(false) }
    var modeMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(horizontal = Tq.Sp.s5).padding(top = Tq.Sp.s5)) {
        ScreenHeader(
            title = uiState.selectedPreset?.displayName ?: "—",
            kicker = { ReferencePitchKicker(referencePitchHz = uiState.referencePitchHz) },
            trailingAction = {
                // Settings icon button (uses 'settings' glyph per DECISIONS.md 2026-05-20)
                IconButton(
                    onClick = { settingsSheetOpen = true },
                    modifier = Modifier.size(Tq.Sp.s10).align(Alignment.TopEnd),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.tuner_cd_settings),
                        tint = Tq.Color.FgSecondary,
                        modifier = Modifier.size(Tq.Sp.s5),
                    )
                }
            },
        )
        Spacer(Modifier.height(Tq.Sp.s6))
        PresetChip(
            preset = uiState.selectedPreset,
            mode = uiState.mode,
            expanded = modeMenuExpanded,
            onLabelClick = { presetSheetOpen = true },
            onChevronClick = { modeMenuExpanded = true },
            onDismissMenu = { modeMenuExpanded = false },
            onExitChromaticMode = {
                viewModel.onExitChromaticMode()
                modeMenuExpanded = false
            },
            onSelectChromaticMode = {
                viewModel.onEnterChromaticMode()
                modeMenuExpanded = false
            },
        )
        Spacer(Modifier.height(Tq.Sp.s4))

        when (uiState.status) {
            TuningStatus.PERMISSION_DENIED -> PermissionDeniedCard(
                onGrantAccess = {
                    handleGrantAccess(
                        activity = activity,
                        permissionLauncher = permissionLauncher,
                        hasRequestedBefore = uiState.hasRequestedAudioPermission,
                    )
                },
            )
            TuningStatus.CAPTURE_FAILED -> CaptureFailedCard()
            else -> Box {
                ReadoutWell {
                    DetectedNoteHero(
                        note = uiState.detectedNote,
                        semanticColor = uiState.status.toSignalColor(Tq.Palette),
                    )
                    StatusLine(
                        status = uiState.status,
                        centsOffTarget = uiState.centsOffTarget,
                    )
                    NeedleGauge(
                        cents = uiState.centsOffTarget,
                        semanticColor = uiState.status.toSignalColor(Tq.Palette),
                    )
                    HzReadoutPair(
                        detectedHz = uiState.detectedFrequencyHz,
                        targetHz = uiState.targetFrequencyHz,
                    )
                }
                SuccessRing(
                    visible = uiState.status == TuningStatus.ALL_STRINGS_TUNED || advanceFlashVisible,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }

        Spacer(Modifier.height(Tq.Sp.s4))
        StringSelectorRow(
            preset = uiState.selectedPreset,
            currentStringIndex = uiState.currentStringIndex,
            tunedStringIndices = uiState.tunedStringIndices,
            activeSemanticColor = uiState.status.toSignalColor(Tq.Palette),
            mode = uiState.mode,
            onStringTap = viewModel::onStringSelected,
        )
    }

    if (presetSheetOpen) {
        PresetPickerSheet(
            grouped = uiState.availablePresets,
            selectedPresetId = uiState.selectedPreset?.id,
            onDismiss = { presetSheetOpen = false },
            onSelect = { id ->
                viewModel.onPresetSelected(id)
                presetSheetOpen = false
            },
        )
    }
    if (settingsSheetOpen) {
        TunerSettingsSheet(
            referencePitchHz = uiState.referencePitchHz,
            autoAdvanceEnabled = uiState.autoAdvanceEnabled,
            onReferencePitchChanged = viewModel::onReferencePitchChanged,
            onAutoAdvanceChanged = viewModel::onAutoAdvanceChanged,
            onDismiss = { settingsSheetOpen = false },
        )
    }
}

@Composable
private fun CaptureFailedCard() {
    ToniqoCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.tuner_capture_failed),
            style = Tq.Type.Body,
            color = Tq.Color.FgSecondary,
        )
    }
}

/**
 * How long the [SuccessRing] stays visible for a [TunerEvent.StringAdvanced] flash, before
 * SuccessRing's own 320ms fade-out begins. Short on purpose — this is a "notice me" flash, not
 * the ALL_STRINGS_TUNED celebration (which holds for 1200ms; see TunerViewModel).
 */
private const val ADVANCE_FLASH_HOLD_MS = 350L
