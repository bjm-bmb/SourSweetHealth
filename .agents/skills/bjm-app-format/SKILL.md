---
name: bjm-app-format
description: "Android App 工程规范的唯一权威：纯 Kotlin、基线版本（compileSdk 34 / targetSdk 34 / minSdk 26 / JDK 17）、Kotlin 与 Compose 编译器配对、签名与密钥边界、JUnit4 单测约定。用于开发、修改、评审任何 Android 代码或判断工程配置是否合规；不用于需求受理与流程调度。"
---

# Android App 工程规范

本 Skill 是 App 工程规范的**唯一权威**，规则原文在 `docs/android/`：`gradle-baseline.md`（版本基线与兼容性矩阵）、`app-coding-conventions.md`（编码约定）、`signing-and-release.md`（签名与发布）、`ui-design-guide.md`（UI 设计美学规范：简约且高级）。任何既有项目配置、口头描述或第三方模板都不能覆盖它；确定性底线由 `tools/app_check.py` 机械判定。

## 铁律

1. 纯 Kotlin。不新增 Java 源文件；既有 Java 代码在触及时评估迁移，任务卡未要求时不扩大迁移范围。
2. 单一 `:app` 模块 + Gradle Wrapper。不擅自拆分模块；确需拆分时先经任务卡明确。
3. 基线版本不动：compileSdk 34 / targetSdk 34 / minSdk 26 / JDK 17（compileOptions 与 kotlin jvmTarget 均为 17）。版本升降级只能按 `docs/android/gradle-baseline.md` 的递增规则随任务卡显式进行。
4. AGP / Gradle / Kotlin 三者版本必须兼容（矩阵见 `gradle-baseline.md`）；Kotlin ≥ 2.0 的项目启用 Compose 必须用 `org.jetbrains.kotlin.plugin.compose` 插件，不得再配置 `kotlinCompilerExtensionVersion`；Kotlin 1.9.x 则必须配置它。
5. 签名与密钥边界：签名凭据放 gitignored 的 `keystore.properties`；口令、keystore、API Key 一律不入库（不写进 gradle.properties / local.properties / 代码 / 注释），运行时密钥构建期从环境变量注入。详见 `docs/android/signing-and-release.md`。
6. 测试约定：JUnit 4 纯逻辑单测，验证命令 `.\gradlew.bat testDebugUnitTest`；仅当任务卡要求真机验证时引入 androidTest（依赖与断言约定见 `docs/android/device-test-automation.md`），不得为了跑通而删除断言或跳过用例。
7. namespace 与 applicationId 一致；versionCode 与 versionName 必须显式声明；APK 命名 `<app>-<versionName>.apk`。
8. UI 审美遵循 `docs/android/ui-design-guide.md`（简约且高级）：单一低饱和强调色、6 级字阶、4dp 网格间距、统一圆角体系、层级靠留白而非装饰；界面代码禁止硬编码颜色/字号/圆角，一律走 theme token。组件优先使用 `docs/android/design-system/` 实例化的 `App*` 组件，不重复造轮子；品牌色以 `docs/brand/brand-vi.md` 为准。

## UI 技术路线

- 新项目与新增页面优先 Jetpack Compose + Material3；既有 View/XML 项目在该项目内保持一致（不混建两套 UI 体系），ViewBinding 替代 findViewById。
- Compose 编译器配置按 Kotlin 版本二选一，代码片段见 `docs/android/gradle-baseline.md`。

## 与校验器的分工

- `python tools/app_check.py .` 机械判定上述铁律中可判定的部分：`wrapper`、`app-module`、`jdk-target`、`sdk-baseline`、`agp-gradle-compat`、`compose-compiler`、`app-id-namespace`、`versioning` 为 ERROR；`local-properties-ignore`、`secrets-plaintext`、`release-signing`、`apk-naming`、`readme` 为 WARN。
- ERROR 必须修正后才能交付；WARN 逐条人工确认并在报告中说明采信结论。
- 脚本未覆盖的语义质量（架构合理性、状态管理正确性、需求覆盖度）由评审 Agent 按 `review-checklist.md` 判断。

## 最终交付物

符合上述铁律与 `docs/android/` 规范原文的工程配置与代码；`python tools/app_check.py .` 退出码 0；WARN 的逐条采信结论。
