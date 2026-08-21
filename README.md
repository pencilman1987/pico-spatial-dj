# PICO Spatial DJ

一个面向 PICO OS 6 Share Space 的空间 DJ 打碟机原型。项目使用 Android、Kotlin、PICO Spatial SDK 与 SpatialUI，将传统双唱盘界面改造成可在空间窗口中运行的交互式控制台。

## 当前能力

- 单个 Volumetric `DefaultWindowContainer`，保持 Share Space 体验
- 双唱盘动态界面与独立曲目状态
- CUE、PLAY、通道音量、Pitch、EQ 与 Crossfader 控件
- 基于 ImageGen 素材的俯视 DJ 硬件底板和唱盘纹理
- 使用 SpatialUI 原生控件提供空间 Hover、声音与触觉反馈
- Repository → UseCase → ViewModel → SpatialUI 分层结构
- 11 项 JVM 单元测试

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

## 说明

当前版本重点验证空间界面、控件布局与 Share Space 交互结构。曲库来自示例 Repository，尚未接入真实媒体库和低延迟双通道音频引擎。
