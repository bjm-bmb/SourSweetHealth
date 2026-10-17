# Android Gradle 基线

本文件是版本基线与兼容性矩阵的**唯一权威**；`tools/app_check.py` 按本文件机械判定（`wrapper`、`sdk-baseline`、`jdk-target`、`agp-gradle-compat`、`compose-compiler` 规则）。基线来自对 5 个真实仓库的调研：mood-diary、SourSweetHealth、IdeaNote（Compose/Material3）、SonarEar（View/XML+ViewBinding）、vibe-life（Compose）。

## 基线版本

| 项 | 基线 | 说明 |
| --- | --- | --- |
| compileSdk / targetSdk | 34 | |
| minSdk | 26 | 不得低于 26 |
| JDK | 17 | compileOptions 与 kotlin jvmTarget 均为 17；本机 JDK17 位于 `C:\Android\jdk17\jdk17` |
| Gradle | ≥ 8.5 | wrapper 分发版（mood-diary 用 8.7 + 腾讯镜像） |
| AGP | 8.2.2 / 8.3.2 / 8.5.2 | 与 Gradle 版本按下表配对 |
| Kotlin | 1.9.22 或 2.0.x | Compose 配置路径随之二选一 |
| JUnit | 4.13.2 | `testDebugUnitTest`；真机断言按任务卡引入 androidTest（见 `device-test-automation.md`） |

## AGP ↔ Gradle 兼容矩阵

| AGP | 最低 Gradle |
| --- | --- |
| 8.2.x | 8.2 |
| 8.3.x | 8.4 |
| 8.5.x | 8.7 |

升级 AGP 时先把 wrapper 升到矩阵要求以上，再改 AGP 版本；顺序反过来会在 sync 阶段直接失败。

## Compose 编译器：Kotlin 1.9 vs 2.0 两条路径

两条路径互斥，`app_check.py` 的 `compose-compiler` 规则会把错配判为 ERROR。

### Kotlin 1.9.x（如 1.9.22）

用 `composeOptions.kotlinCompilerExtensionVersion` 显式配对编译器扩展版本（1.5.8 / 1.5.10）：

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.10" }
}
```

### Kotlin 2.x（如 2.0.0 / 2.0.20）

必须使用官方 Compose 编译器插件，并删除 `kotlinCompilerExtensionVersion`：

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

android {
    buildFeatures { compose = true }
    // 不得再出现 composeOptions.kotlinCompilerExtensionVersion
}
```

未启用 Compose 的项目（如 View/XML 的 SonarEar）两条路径都不适用，校验器自动跳过。

## 镜像策略（可选加速）

- Gradle 发行包：wrapper 的 `distributionUrl` 可换腾讯镜像 `https\://mirrors.cloud.tencent.com/gradle/gradle-<ver>-bin.zip`（mood-diary 即用此）。
- 依赖：根构建脚本 `repositories` 中把阿里云镜像放在 google()/mavenCentral() 之前：

  ```
  maven { url 'https://maven.aliyun.com/repository/google' }
  maven { url 'https://maven.aliyun.com/repository/central' }
  ```

- 镜像只影响下载速度，不改变版本约束。

## 版本递增规则

- 升降级 SDK / JDK / AGP / Gradle / Kotlin 必须随任务卡显式进行，并在任务卡「工程配置变更」写明；不允许为通过检查而降低校验器基线参数（CLI 覆盖参数只做临时诊断）。
- Kotlin 升级时同步升级 KSP（版本号前缀必须等于 Kotlin 版本，如 1.9.22 → `1.9.22-1.0.17`）。
- 变更基线后同步四处：本文件矩阵 → `tools/app_check.py` 默认值/矩阵 → `tools/tests/` 回归测试 → 各使用中的项目重跑 `install.ps1`。

## app_check 用法

```
python tools/app_check.py <project-dir>     # 人类可读输出
python tools/app_check.py --json .          # JSON 问题列表
```

退出码：0 无 ERROR；1 有 ERROR；2 目录无效或无 app 模块构建文件。
基线阈值可按项目临时覆盖：`--compile-sdk` / `--target-sdk` / `--min-sdk` / `--jdk` / `--min-gradle`（只做诊断，不替代版本递增流程）。
