# PICO Spatial DJ

一个面向 PICO OS 6 Share Space 的空间 DJ 打碟机原型。项目使用 Android、Kotlin、PICO Spatial SDK 与 SpatialUI，将传统双唱盘界面改造成可在空间窗口中运行的交互式控制台。

## 当前能力

- 单个 Volumetric `DefaultWindowContainer`，保持 Share Space 体验
- 双唱盘动态界面与独立曲目状态
- 48 kHz 双通道 PCM 实时混音，可同时播放两路声音
- 4 首内置实时合成演示曲，无需联网或额外下载
- CUE、PLAY/PAUSE、STOP、进度、通道音量、Pitch、LOW/HIGH EQ 与 Crossfader
- 唱盘空间拖动 Scratch，暂停状态也提供短促刮盘预听
- 通过系统文件选择器导入本地 MP3、WAV、OGG 或 M4A，并持久保留访问权限
- 顶部按钮可在完整中文界面与英语界面之间即时切换，不会重置当前播放状态
- 基于 ImageGen 素材的俯视 DJ 硬件底板和唱盘纹理
- 使用 SpatialUI 原生控件提供空间 Hover、声音与触觉反馈
- Repository → UseCase → ViewModel → SpatialUI 分层结构
- 14 项 JVM 单元测试

## 技术栈

- PICO Spatial SDK BOM 0.13.3
- Kotlin 2.1.20
- Android Gradle Plugin 8.13.2
- Android API 35
- arm64-v8a

## 构建

请先按照 PICO Spatial SDK 文档配置 Android SDK、PICO SDK 与依赖仓库，然后执行：

```bash
./gradlew testDebugUnitTest assembleDebug
```

生成的调试 APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 运行结构

- 应用包名：`com.haisnap.spatialdj`
- 启动 Activity：`com.haisnap.spatialdj.platform.LaunchActivity`
- 默认空间窗口：`1600 x 960 x 640 dp`
- 窗口模式：Volumetric / Uniform Resizable / Fixed World Scale

界面覆盖层的位置统一由 `ConsoleCalibration.kt` 管理。更换 DJ 底板素材时，应只调整其中的命名参数，避免在 Composable 中分散硬编码坐标。

## 使用方法

1. 点击右上角 `EN` / `中文` 按钮可切换界面语言。
2. 点击中部曲库中的曲目，将前两首依次装载到 Deck A 和 Deck B；再次选择会装载到当前活动 Deck。
3. 等待顶部状态显示唱盘已就绪后点击播放。
4. 回点会暂停并回到曲首，停止同样复位；拖动唱盘可快速定位并进行 Scratch 预听。
5. 点击右上角导入音乐按钮选择设备中的音频。本地曲目会出现在中部曲库最近四项中。

## 音频说明

本地音频由 Android 系统解码器转成 PCM 后进入双 Deck 混音器，因此实际格式兼容性取决于 PICO OS 自带解码器。为控制空间设备内存占用，单个导入文件最多解码前 8 分钟。系统文件选择器授予的 URI 权限会持久保存，不需要申请整库读取权限。
