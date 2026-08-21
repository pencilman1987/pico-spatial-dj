package com.haisnap.spatialdj.ui.console

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haisnap.spatialdj.R
import com.haisnap.spatialdj.ui.console.components.CrateStrip
import com.haisnap.spatialdj.ui.console.components.ConsoleCalibration
import com.haisnap.spatialdj.ui.console.components.DeckPanel
import com.haisnap.spatialdj.ui.console.components.DeviceHeader
import com.haisnap.spatialdj.ui.console.components.MixerPanel
import com.pico.spatial.ui.design.PicoTheme

@Composable
fun DjConsoleScreen() {
    val context = LocalContext.current
    val factory = remember(context) { DjConsoleViewModel.Factory(context.applicationContext) }
    val viewModel: DjConsoleViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        viewModel.onEvent(DjConsoleEvent.ImportTracks(uris.map { it.toString() }))
    }
    DjConsoleContent(
        state = state,
        onEvent = viewModel::onEvent,
        onImportAudio = { audioPicker.launch(arrayOf("audio/mpeg", "audio/wav", "audio/ogg", "audio/mp4", "audio/*")) },
    )
}

@Composable
internal fun DjConsoleContent(
    state: DjConsoleUiState,
    onEvent: (DjConsoleEvent) -> Unit,
    onImportAudio: () -> Unit = {},
) {
    // design-style: opaque-root — the full-frame asset represents a solid physical console body.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PicoTheme.colorScheme.fillPrimary),
    ) {
        Image(
            painter = painterResource(R.drawable.dj_console_topdown_base_v2),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            alpha = 1f,
        )
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxWidth < ConsoleCalibration.CompactWidthThreshold.dp
            val edge = if (compact) ConsoleCalibration.RootHorizontalInsetCompact else ConsoleCalibration.RootHorizontalInset
            val verticalEdge = if (compact) ConsoleCalibration.RootVerticalInsetCompact else ConsoleCalibration.RootVerticalInset
            val gap = if (compact) ConsoleCalibration.PrimaryRegionGapCompact else ConsoleCalibration.PrimaryRegionGap
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = edge, vertical = verticalEdge),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                DeviceHeader(
                    state = state,
                    onImportAudio = onImportAudio,
                    onToggleLanguage = { onEvent(DjConsoleEvent.ToggleLanguage) },
                )
                CrateStrip(
                    state = state,
                    onEvent = onEvent,
                    modifier = Modifier.height(if (compact) ConsoleCalibration.DisplayRowHeightCompact else ConsoleCalibration.DisplayRowHeight),
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DeckPanel(
                        deck = state.deckA,
                        activeDeck = state.activeDeck,
                        language = state.language,
                        onEvent = onEvent,
                        modifier = Modifier.weight(1f),
                        compact = compact,
                    )
                    MixerPanel(
                        state = state,
                        onEvent = onEvent,
                        modifier = Modifier
                            .weight(if (compact) 0.54f else 0.62f)
                            .fillMaxHeight(if (compact) ConsoleCalibration.MixerHeightFractionCompact else ConsoleCalibration.MixerHeightFraction)
                            .offset(y = if (compact) ConsoleCalibration.MixerOffsetYCompact else ConsoleCalibration.MixerOffsetY),
                        compact = compact,
                    )
                    DeckPanel(
                        deck = state.deckB,
                        activeDeck = state.activeDeck,
                        language = state.language,
                        onEvent = onEvent,
                        modifier = Modifier.weight(1f),
                        compact = compact,
                    )
                }
            }
        }
    }
}
