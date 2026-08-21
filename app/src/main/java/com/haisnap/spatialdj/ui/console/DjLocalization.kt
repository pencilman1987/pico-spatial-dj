package com.haisnap.spatialdj.ui.console

import com.haisnap.spatialdj.domain.model.DeckId
import com.haisnap.spatialdj.domain.model.PlaybackState

enum class UiLanguage {
    Chinese,
    English;

    fun toggled(): UiLanguage = if (this == Chinese) English else Chinese
}

sealed interface DjStatus {
    data object AudioReady : DjStatus
    data class Loading(val trackTitle: String) : DjStatus
    data class DeckReady(val deckId: DeckId) : DjStatus
    data class Imported(val count: Int) : DjStatus
    data class LoadError(val detail: String) : DjStatus
}

data class DjStrings(
    val systemName: String,
    val importAudio: String,
    val switchLanguage: String,
    val deck: String,
    val active: String,
    val standby: String,
    val loadTrack: String,
    val noMedia: String,
    val file: String,
    val local: String,
    val pitch: String,
    val gain: String,
    val cue: String,
    val rpm: String,
    val play: String,
    val pause: String,
    val stop: String,
    val mixer: String,
    val liveChannel: String,
    val channelCount: String,
    val low: String,
    val high: String,
    val level: String,
    val crossfader: String,
    val cut: String,
    val localAudio: String,
    val empty: String,
    val loading: String,
    val ready: String,
    val playing: String,
    val paused: String,
    val error: String,
)

fun UiLanguage.strings(): DjStrings = when (this) {
    UiLanguage.Chinese -> DjStrings(
        systemName = "空间黑胶系统",
        importAudio = "导入音乐",
        switchLanguage = "EN",
        deck = "唱盘",
        active = "当前",
        standby = "待机",
        loadTrack = "载入曲目",
        noMedia = "暂无音乐",
        file = "文件",
        local = "本地",
        pitch = "速度",
        gain = "增益",
        cue = "回点",
        rpm = "转速",
        play = "播放",
        pause = "暂停",
        stop = "停止",
        mixer = "混音台",
        liveChannel = "当前通道",
        channelCount = "双通道",
        low = "低频",
        high = "高频",
        level = "音量",
        crossfader = "横推",
        cut = "切换",
        localAudio = "本地音乐",
        empty = "空闲",
        loading = "载入中",
        ready = "就绪",
        playing = "播放中",
        paused = "已暂停",
        error = "错误",
    )
    UiLanguage.English -> DjStrings(
        systemName = "SPATIAL VINYL SYSTEM",
        importAudio = "IMPORT AUDIO",
        switchLanguage = "中文",
        deck = "DECK",
        active = "ACTIVE",
        standby = "STANDBY",
        loadTrack = "LOAD TRACK",
        noMedia = "NO MEDIA",
        file = "FILE",
        local = "LOCAL",
        pitch = "PITCH",
        gain = "GAIN",
        cue = "CUE",
        rpm = "RPM",
        play = "PLAY",
        pause = "PAUSE",
        stop = "STOP",
        mixer = "MIXER",
        liveChannel = "LIVE CHANNEL",
        channelCount = "2 CH",
        low = "LOW",
        high = "HIGH",
        level = "LEVEL",
        crossfader = "CROSSFADER",
        cut = "CUT",
        localAudio = "LOCAL AUDIO",
        empty = "EMPTY",
        loading = "LOADING",
        ready = "READY",
        playing = "PLAYING",
        paused = "PAUSED",
        error = "ERROR",
    )
}

fun DjStatus.localized(language: UiLanguage): String = when (language) {
    UiLanguage.Chinese -> when (this) {
        DjStatus.AudioReady -> "音频引擎就绪"
        is DjStatus.Loading -> "正在载入 ${trackTitle}"
        is DjStatus.DeckReady -> "唱盘 $deckId 已就绪"
        is DjStatus.Imported -> "已导入 $count 首曲目"
        is DjStatus.LoadError -> "载入失败：$detail"
    }
    UiLanguage.English -> when (this) {
        DjStatus.AudioReady -> "AUDIO ENGINE READY"
        is DjStatus.Loading -> "LOADING ${trackTitle.uppercase()}"
        is DjStatus.DeckReady -> "DECK $deckId READY"
        is DjStatus.Imported -> "IMPORTED $count TRACK(S)"
        is DjStatus.LoadError -> "LOAD ERROR: $detail"
    }
}

fun PlaybackState.localized(strings: DjStrings): String = when (this) {
    PlaybackState.Empty -> strings.empty
    PlaybackState.Loading -> strings.loading
    PlaybackState.Ready -> strings.ready
    PlaybackState.Playing -> strings.playing
    PlaybackState.Paused -> strings.paused
    PlaybackState.Error -> strings.error
}
