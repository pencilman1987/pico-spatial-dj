package com.haisnap.spatialdj.ui.console.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.haisnap.spatialdj.R
import com.haisnap.spatialdj.domain.model.DeckId
import com.haisnap.spatialdj.domain.model.DeckState
import com.haisnap.spatialdj.domain.model.PlaybackState
import com.haisnap.spatialdj.domain.model.Track
import com.haisnap.spatialdj.ui.console.DjConsoleEvent
import com.haisnap.spatialdj.ui.console.DjConsoleUiState
import com.haisnap.spatialdj.ui.console.DjStrings
import com.haisnap.spatialdj.ui.console.UiLanguage
import com.haisnap.spatialdj.ui.console.localized
import com.haisnap.spatialdj.ui.console.strings
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Slider
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialDragGesture
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import kotlin.math.roundToInt

@Composable
fun DeviceHeader(
    state: DjConsoleUiState,
    onImportAudio: () -> Unit,
    onToggleLanguage: () -> Unit,
) {
    val strings = state.language.strings()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (isMasterActive(state)) PicoTheme.colorScheme.error else PicoTheme.colorScheme.passable),
            )
            Text("HAISNAP  /  ${strings.systemName}", style = PicoTheme.typography.titleMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.status.localized(state.language),
                modifier = Modifier.width(260.dp),
                style = PicoTheme.typography.labelSmall,
                color = if (isMasterActive(state)) PicoTheme.colorScheme.error else PicoTheme.colorScheme.passable,
                maxLines = 1,
            )
            Button(onClick = onImportAudio) { Text(strings.importAudio) }
            Button(onClick = onToggleLanguage) { Text(strings.switchLanguage) }
        }
    }
}

@Composable
fun CrateStrip(
    state: DjConsoleUiState,
    onEvent: (DjConsoleEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = state.language.strings()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val compact = maxWidth < ConsoleCalibration.CompactWidthThreshold.dp
        val deckScreenWidth = if (compact) ConsoleCalibration.DeckScreenWidthCompact else ConsoleCalibration.DeckScreenWidth
        val crateScreenWidth = if (compact) ConsoleCalibration.CrateScreenWidthCompact else ConsoleCalibration.CrateScreenWidth
        val deckScreenInset = if (compact) ConsoleCalibration.DeckScreenInsetCompact else ConsoleCalibration.DeckScreenInset
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(end = deckScreenInset),
                contentAlignment = Alignment.CenterEnd,
            ) {
                DeckDisplay(state.deckA, state.activeDeck == DeckId.A, strings, Modifier.width(deckScreenWidth).fillMaxHeight())
            }
            ConsoleDisplay(modifier = Modifier.width(crateScreenWidth).fillMaxHeight(), contentPadding = 7.dp) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.tracks.takeLast(4).chunked(2).forEach { rowTracks ->
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            rowTracks.forEach { track ->
                                TrackButton(
                                    track = track,
                                    strings = strings,
                                    selected = state.selectedTrackId == track.id,
                                    onClick = { onEvent(DjConsoleEvent.LoadTrack(track.id)) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(start = deckScreenInset),
                contentAlignment = Alignment.CenterStart,
            ) {
                DeckDisplay(state.deckB, state.activeDeck == DeckId.B, strings, Modifier.width(deckScreenWidth).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun DeckDisplay(deck: DeckState, selected: Boolean, strings: DjStrings, modifier: Modifier = Modifier) {
    val accent = deckAccent(deck.id)
    ConsoleDisplay(modifier = modifier, contentPadding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${strings.deck} ${deck.id}  ${if (selected) strings.active else strings.standby}",
                    style = PicoTheme.typography.labelSmall,
                    color = accent,
                )
                Text(
                    deck.track?.title?.uppercase() ?: strings.loadTrack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    deck.track?.artist?.uppercase() ?: strings.noMedia,
                    style = PicoTheme.typography.labelSmall,
                    color = PicoTheme.colorScheme.labelTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(deck.track?.bpm?.takeIf { it > 0 }?.toString() ?: strings.file, style = PicoTheme.typography.headlineSmall, color = accent)
                Text(if ((deck.track?.bpm ?: 0) > 0) "BPM" else strings.local, style = PicoTheme.typography.labelSmall, color = PicoTheme.colorScheme.labelTertiary)
            }
        }
    }
}

@Composable
fun DeckPanel(
    deck: DeckState,
    activeDeck: DeckId,
    language: UiLanguage,
    onEvent: (DjConsoleEvent) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val accent = deckAccent(deck.id)
    val strings = language.strings()
    BoxWithConstraints(modifier = modifier.fillMaxHeight()) {
        val platterSize = if (compact) {
            minOf(maxWidth * ConsoleCalibration.PlatterWidthFractionCompact, maxHeight * ConsoleCalibration.PlatterHeightFractionCompact)
        } else {
            minOf(maxWidth * ConsoleCalibration.PlatterWidthFraction, maxHeight * ConsoleCalibration.PlatterHeightFraction)
        }
        val platterOffsetX = if (deck.id == DeckId.A) ConsoleCalibration.DeckAPlatterOffsetX else ConsoleCalibration.DeckBPlatterOffsetX
        val platterOffsetY = if (compact) ConsoleCalibration.PlatterOffsetYCompact else ConsoleCalibration.PlatterOffsetY
        val channelAlignment = if (deck.id == DeckId.A) Alignment.CenterStart else Alignment.CenterEnd
        AnimatedPlatter(
            deck = deck,
            accent = accent,
            strings = strings,
            onScratch = { onEvent(DjConsoleEvent.Scratch(deck.id, it)) },
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = platterOffsetX, y = platterOffsetY)
                .size(platterSize),
        )
        DeckChannelControls(
            deck = deck,
            strings = strings,
            onEvent = onEvent,
            compact = compact,
            modifier = Modifier
                .align(channelAlignment)
                .offset(y = if (compact) ConsoleCalibration.ChannelBayOffsetYCompact else ConsoleCalibration.ChannelBayOffsetY)
                .size(
                    width = if (compact) ConsoleCalibration.ChannelBayWidthCompact else ConsoleCalibration.ChannelBayWidth,
                    height = if (compact) ConsoleCalibration.ChannelBayHeightCompact else ConsoleCalibration.ChannelBayHeight,
                ),
        )
        TransportCluster(
            deck = deck,
            activeDeck = activeDeck,
            strings = strings,
            onEvent = onEvent,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(
                    x = platterOffsetX,
                    y = if (compact) ConsoleCalibration.TransportOffsetYCompact else ConsoleCalibration.TransportOffsetY,
                ),
        )
    }
}

@Composable
private fun DeckChannelControls(
    deck: DeckState,
    strings: DjStrings,
    onEvent: (DjConsoleEvent) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val progressColor = deckAccent(deck.id)
    val progressBackground = PicoTheme.colorScheme.dividerLine
    ConsoleDisplay(modifier = modifier, contentPadding = if (compact) 7.dp else 9.dp) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${strings.deck} ${deck.id}", style = PicoTheme.typography.labelMedium, color = deckAccent(deck.id))
                Text(
                    deck.playbackState.localized(strings),
                    style = PicoTheme.typography.labelSmall,
                    color = PicoTheme.colorScheme.labelTertiary,
                )
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                VerticalChannelStrip(
                    label = strings.pitch,
                    value = deck.tempo,
                    valueRange = 0.5f..1.5f,
                    compact = compact,
                    onValueChange = { onEvent(DjConsoleEvent.SetTempo(deck.id, it)) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                VerticalChannelStrip(
                    label = strings.gain,
                    value = deck.volume,
                    valueRange = 0f..1f,
                    compact = compact,
                    onValueChange = { onEvent(DjConsoleEvent.SetVolume(deck.id, it)) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
            Text(
                deck.track?.let { "${formatDuration(deck.positionSeconds.toInt())} / ${formatDuration(it.durationSeconds)}" } ?: "--:-- / --:--",
                style = PicoTheme.typography.labelSmall,
                color = PicoTheme.colorScheme.labelTertiary,
                maxLines = 1,
            )
            Canvas(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)) {
                drawRect(progressBackground)
                drawRect(progressColor, size = androidx.compose.ui.geometry.Size(size.width * deck.progress, size.height))
            }
        }
    }
}

@Composable
private fun VerticalChannelStrip(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    compact: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = PicoTheme.typography.labelSmall, color = PicoTheme.colorScheme.labelSecondary)
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier
                    .requiredWidth(if (compact) ConsoleCalibration.VerticalSliderLengthCompact else ConsoleCalibration.VerticalSliderLength)
                    .graphicsLayer { rotationZ = -90f },
            )
        }
        Text(formatValue(value, valueRange), style = PicoTheme.typography.labelSmall)
    }
}

@Composable
private fun AnimatedPlatter(
    deck: DeckState,
    accent: Color,
    strings: DjStrings,
    onScratch: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val transition = rememberInfiniteTransition(label = "deck-${deck.id}-rotation")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2600, easing = LinearEasing)),
        label = "platter-rotation",
    )
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .spatialHoverEffect()
            .pointerInput(deck.id) {
                detectSpatialDragGesture(context, targetedToEntity = TargetEntity.any()) { drag ->
                    onScratch(drag.dragAmount.x * 0.006f)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.vinyl_platter_neutral),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .graphicsLayer { rotationZ = if (deck.playbackState == PlaybackState.Playing) rotation else 0f },
        )
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = accent.copy(alpha = 0.32f),
                radius = size.minDimension * 0.48f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12.dp.toPx()),
            )
            drawCircle(color = accent, radius = size.minDimension * 0.48f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
        }
        Column(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(PicoTheme.colorScheme.fillPrimary.copy(alpha = 0.94f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                if (deck.playbackState == PlaybackState.Playing) "33⅓" else strings.cue,
                style = PicoTheme.typography.titleMedium,
                color = accent,
            )
            Text(strings.rpm, style = PicoTheme.typography.labelSmall, color = PicoTheme.colorScheme.labelTertiary)
        }
    }
}

@Composable
private fun TransportCluster(
    deck: DeckState,
    activeDeck: DeckId,
    strings: DjStrings,
    onEvent: (DjConsoleEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = deckAccent(deck.id)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PicoTheme.colorScheme.fillPrimary.copy(alpha = 0.9f))
            .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = { onEvent(DjConsoleEvent.Cue(deck.id)) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (activeDeck == deck.id) accent else PicoTheme.colorScheme.fillTertiary,
                contentColor = if (activeDeck == deck.id) PicoTheme.colorScheme.fillPrimary else PicoTheme.colorScheme.labelPrimary,
            ),
        ) { Text(strings.cue) }
        Button(
            onClick = { onEvent(DjConsoleEvent.TogglePlayback(deck.id)) },
            enabled = deck.track != null,
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = PicoTheme.colorScheme.fillPrimary),
        ) { Text(if (deck.playbackState == PlaybackState.Playing) strings.pause else strings.play) }
        Button(
            onClick = { onEvent(DjConsoleEvent.Stop(deck.id)) },
            enabled = deck.track != null,
        ) { Text(strings.stop) }
    }
}

@Composable
fun MixerPanel(
    state: DjConsoleUiState,
    onEvent: (DjConsoleEvent) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val deck = if (state.activeDeck == DeckId.A) state.deckA else state.deckB
    val strings = state.language.strings()
    HardwarePanel(modifier = modifier, compact = compact) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(strings.mixer, style = PicoTheme.typography.headlineSmall)
                Text(
                    "${strings.deck} ${state.activeDeck}  ${strings.liveChannel}",
                    style = PicoTheme.typography.labelSmall,
                    color = deckAccent(state.activeDeck),
                )
            }
            Text(strings.channelCount, style = PicoTheme.typography.labelMedium, color = PicoTheme.colorScheme.labelTertiary)
        }
        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
        VuBridge(
            deckALevel = state.deckA.level,
            deckBLevel = state.deckB.level,
            modifier = Modifier.fillMaxWidth().height(if (compact) 76.dp else 92.dp),
        )
        Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
        ChannelStrip(strings.low, deck.bass, -1f..1f) { onEvent(DjConsoleEvent.SetBass(state.activeDeck, it)) }
        ChannelStrip(strings.high, deck.treble, -1f..1f) { onEvent(DjConsoleEvent.SetTreble(state.activeDeck, it)) }
        ChannelStrip(strings.level, deck.volume, 0f..1f) { onEvent(DjConsoleEvent.SetVolume(state.activeDeck, it)) }
        Spacer(Modifier.weight(1f))
        Text(strings.crossfader, style = PicoTheme.typography.labelMedium)
        Slider(
            value = state.crossfader,
            onValueChange = { onEvent(DjConsoleEvent.SetCrossfader(it)) },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("A ${(state.crossfadeGains.deckA * 100).toInt()}", style = PicoTheme.typography.labelSmall, color = PicoTheme.colorScheme.interaction)
            Text(strings.cut, style = PicoTheme.typography.labelSmall, color = PicoTheme.colorScheme.labelTertiary)
            Text("${(state.crossfadeGains.deckB * 100).toInt()} B", style = PicoTheme.typography.labelSmall, color = PicoTheme.colorScheme.alert)
        }
    }
}

@Composable
private fun VuBridge(
    deckALevel: Float,
    deckBLevel: Float,
    modifier: Modifier = Modifier,
) {
    val background = PicoTheme.colorScheme.fillPrimary
    val inactive = PicoTheme.colorScheme.dividerLine
    val green = PicoTheme.colorScheme.passable
    val amber = PicoTheme.colorScheme.alert
    val red = PicoTheme.colorScheme.error
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(10.dp),
    ) {
        val bars = 12
        val gap = 3.dp.toPx()
        val barHeight = (size.height - gap * (bars - 1)) / bars
        val channelWidth = (size.width - 12.dp.toPx()) / 2f
        repeat(bars) { index ->
            val y = size.height - (index + 1) * barHeight - index * gap
            val color = when {
                index >= 10 -> red
                index >= 8 -> amber
                else -> green
            }
            val aOn = index < (deckALevel.coerceIn(0f, 1f) * bars).roundToInt()
            val bOn = index < (deckBLevel.coerceIn(0f, 1f) * bars).roundToInt()
            drawRect(if (aOn) color else inactive, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Size(channelWidth, barHeight))
            drawRect(if (bOn) color else inactive, androidx.compose.ui.geometry.Offset(channelWidth + 12.dp.toPx(), y), androidx.compose.ui.geometry.Size(channelWidth, barHeight))
        }
    }
}

@Composable
private fun ChannelStrip(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = PicoTheme.typography.labelSmall, color = PicoTheme.colorScheme.labelSecondary)
            Text(formatValue(value, valueRange), style = PicoTheme.typography.labelSmall)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TrackButton(
    track: Track,
    strings: DjStrings,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) PicoTheme.colorScheme.interaction else PicoTheme.colorScheme.fillPrimary,
            contentColor = if (selected) PicoTheme.colorScheme.fillPrimary else PicoTheme.colorScheme.labelPrimary,
        ),
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start)
            Text(if (track.bpm > 0) "${track.bpm} BPM" else strings.localAudio, style = PicoTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun ConsoleDisplay(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(PicoTheme.colorScheme.fillPrimary.copy(alpha = 0.9f))
            .border(1.dp, PicoTheme.colorScheme.dividerLine, shape)
            .padding(contentPadding),
    ) {
        content()
    }
}

@Composable
private fun HardwarePanel(
    modifier: Modifier = Modifier,
    compact: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(PicoTheme.colorScheme.fillSecondary.copy(alpha = 0.91f))
            .border(1.dp, PicoTheme.colorScheme.dividerLine, shape)
            .padding(if (compact) 12.dp else 16.dp),
        content = content,
    )
}

@Composable
private fun deckAccent(deckId: DeckId): Color =
    if (deckId == DeckId.A) PicoTheme.colorScheme.interaction else PicoTheme.colorScheme.alert

private fun isMasterActive(state: DjConsoleUiState): Boolean =
    state.deckA.playbackState == PlaybackState.Playing || state.deckB.playbackState == PlaybackState.Playing

private fun formatDuration(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

private fun formatValue(value: Float, range: ClosedFloatingPointRange<Float>): String =
    when {
        range.start < 0f -> "%+.0f".format(value * 100)
        range.endInclusive > 1f -> "×%.2f".format(value)
        else -> "%.0f".format(value * 100)
    }
