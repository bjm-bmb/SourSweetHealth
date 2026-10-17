---
name: bjm-app-build
description: "Windows 本机 Android 构建与验证操作手册：gradlew 调用方式、JAVA_HOME/ANDROID_HOME 环境、国内镜像、KSP/kapt、产物路径与命名、真机测试入口、常见错误处置。用于执行或排查本机构建、单测、打包与真机验证；不用于需求分析与代码评审。"
---

# Android 构建与验证操作手册（Windows）

本手册是本机执行 Android 构建、单测与打包的操作依据。环境约定：本机 Android SDK 在 `C:\Android\Sdk`（即 `local.properties` 的 `sdk.dir`），JDK 17 在 `C:\Android\jdk17\jdk17`；确定性基线检查用 `python tools/app_check.py .`。

## 环境准备

- 仅使用 JDK 17 构建（AGP 8.x 要求）。构建会话内显式指定：

  ```powershell
  $env:JAVA_HOME = "C:\Android\jdk17\jdk17"
  $env:ANDROID_HOME = "C:\Android\Sdk"
  ```

- `local.properties` 的 `sdk.dir=C:\\Android\\Sdk`；该文件为本机私有，必须被 .gitignore 忽略，不得入库。

## 构建命令

- 项目带 `gradlew.bat`（Windows 必用）：`.\gradlew.bat <task>`；PowerShell 下不可省略 `.\`；仅提供 `gradlew` 无 `.bat` 的项目在 Git Bash 中用 `./gradlew`。
- 常用任务：
  - `.\gradlew.bat testDebugUnitTest` —— JUnit4 纯逻辑单测（默认验证集，不需要设备）；
  - `.\gradlew.bat assembleDebug` —— 调试包；
  - `.\gradlew.bat assembleRelease` —— 发布包（需签名配置，见 `docs/android/signing-and-release.md`）；
  - `.\gradlew.bat connectedDebugAndroidTest` —— 真机 UI 断言（需设备，建议由 `tools\device_test.ps1` 统一触发）；
  - 闭环默认验证命令：`.\gradlew.bat testDebugUnitTest assembleDebug`。
- 先跑确定性底线再构建：`python tools/app_check.py .`（ERROR 必须先修正）。
- 产物路径：`app/build/outputs/apk/<buildType>/`；APK 命名约定 `<app>-<versionName>.apk`（outputFileName 配置见签名文档）。

## 真机自动化测试（需要设备）

任务卡「真机验证」要求时，构建自验必须追加真机测试：

```powershell
powershell -ExecutionPolicy Bypass -File tools\device_test.ps1 -ProjectDir .
```

脚本先提示并等待手机连接、授权 USB 调试（等待超时返回 BLOCKED），随后自动完成构建、安装、启动、冒烟、UI 断言，并采集截图、logcat 与 `result.json`；退出码 `0`/`1`/`2` 对应 PASS/FAIL/BLOCKED。分层与失败判据见 `docs/android/device-test-automation.md`，操作细节见 `bjm-app-device-test` Skill。建议统一由脚本触发 `connectedDebugAndroidTest`：它会设置 `ANDROID_SERIAL` 并统一取证，避免多设备时打到错误机器。

## 国内镜像（可选加速）

- Gradle 发行包：wrapper 的 `distributionUrl` 可换腾讯镜像 `https\://mirrors.cloud.tencent.com/gradle/gradle-<ver>-bin.zip`。
- 依赖：根构建脚本 `repositories` 中把阿里云镜像放在 google()/mavenCentral() 之前：

  ```
  maven { url 'https://maven.aliyun.com/repository/google' }
  maven { url 'https://maven.aliyun.com/repository/central' }
  ```

- 镜像只解决下载速度，不改变版本约束；版本兼容性以 `docs/android/gradle-baseline.md` 矩阵为准。

## 注解处理（KSP / kapt）

- 新项目 Room 等注解处理一律用 KSP（版本与 Kotlin 配对，如 Kotlin 1.9.22 对应 `1.9.22-1.0.17`）；既有 kapt 项目保持一致，不在同一任务中混用两套。
- 升级 Kotlin 时同步升级 KSP（版本号前缀必须等于 Kotlin 版本）。

## 常见错误处置

| 症状 | 处置 |
| --- | --- |
| `JAVA_HOME is set to an invalid directory` / 使用了非 17 的 JDK | 按「环境准备」重设 JAVA_HOME 后重开终端再跑 |
| `SDK location not found` | 检查 `local.properties` 的 `sdk.dir` 与 `$env:ANDROID_HOME` |
| `Minimum supported Gradle version is X` | 按 `docs/android/gradle-baseline.md` 矩阵升级 wrapper，不降低 AGP 凑配 |
| Kotlin 2.x 项目报 Compose 编译器不兼容 | 确认已应用 `org.jetbrains.kotlin.plugin.compose` 插件且删除 `kotlinCompilerExtensionVersion`（两条配置路径见 gradle-baseline.md） |
| 依赖下载超时 | 配置国内镜像后重试；仍失败检查代理设置 |
| 构建 daemon 异常 | `.\gradlew.bat --stop` 后重跑；不删 `.gradle` 全量缓存，除非确认损坏 |

## 最终交付物

构建验证命令与真实退出状态（单测通过数、APK 产物路径与文件名）、`python tools/app_check.py .` 退出码、遇到问题时按上表的处置记录。
