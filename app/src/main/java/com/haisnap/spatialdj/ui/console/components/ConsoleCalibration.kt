package com.haisnap.spatialdj.ui.console.components

import androidx.compose.ui.unit.dp

/**
 * Live-control calibration for the 1672 x 941 top-down console reference.
 *
 * The window is uniformly resized by the manifest, so these offsets remain
 * stable while the whole console scales in Share Space.
 */
internal object ConsoleCalibration {
    const val CompactWidthThreshold = 1300

    val RootHorizontalInset = 34.dp
    val RootHorizontalInsetCompact = 24.dp
    val RootVerticalInset = 26.dp
    val RootVerticalInsetCompact = 20.dp
    val PrimaryRegionGap = 12.dp
    val PrimaryRegionGapCompact = 8.dp

    val DisplayRowHeight = 116.dp
    val DisplayRowHeightCompact = 100.dp
    val DeckScreenWidth = 286.dp
    val DeckScreenWidthCompact = 220.dp
    val CrateScreenWidth = 372.dp
    val CrateScreenWidthCompact = 320.dp
    val DeckScreenInset = 42.dp
    val DeckScreenInsetCompact = 18.dp

    const val PlatterWidthFraction = 0.85f
    const val PlatterHeightFraction = 0.72f
    const val PlatterWidthFractionCompact = 0.78f
    const val PlatterHeightFractionCompact = 0.68f
    val DeckAPlatterOffsetX = 34.dp
    val DeckBPlatterOffsetX = (-34).dp
    val PlatterOffsetY = (-52).dp
    val PlatterOffsetYCompact = (-34).dp

    val ChannelBayWidth = 118.dp
    val ChannelBayWidthCompact = 102.dp
    val ChannelBayHeight = 276.dp
    val ChannelBayHeightCompact = 236.dp
    val ChannelBayOffsetY = 70.dp
    val ChannelBayOffsetYCompact = 48.dp
    val VerticalSliderLength = 154.dp
    val VerticalSliderLengthCompact = 126.dp

    val TransportOffsetY = (-96).dp
    val TransportOffsetYCompact = (-72).dp

    const val MixerHeightFraction = 0.88f
    const val MixerHeightFractionCompact = 0.92f
    val MixerOffsetY = (-18).dp
    val MixerOffsetYCompact = (-10).dp
}
