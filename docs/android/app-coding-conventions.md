# Android 编码约定

本文件是代码层面约定的唯一权威；版本基线见 `gradle-baseline.md`，签名与密钥边界见 `signing-and-release.md`。约定来自 5 个参考仓库的共性实践。

## 语言与技术栈

- 纯 Kotlin，不新增 Java 源文件；既有 Java 代码在触及时评估迁移，任务卡未要求时不扩大迁移范围。
- UI 二选一且项目内统一，不混建两套体系：
  - Jetpack Compose + Material3（mood-diary / SourSweetHealth / IdeaNote / vibe-life）；
  - View/XML + ViewBinding（SonarEar），用 ViewBinding 替代 findViewById。
- 单 `:app` 模块 + Gradle Wrapper；不擅自拆模块，确需拆分时先经任务卡明确。

## 包结构约定

- 根包 = namespace = applicationId（`app_check.py` 判二者必须一致）。
- 按功能分层分包：`ui/`（页面与组件）、`data/`（数据源与仓库）、`model/`（数据类）、`util/`（纯逻辑工具）；Compose 项目页面内聚为可组合函数，状态用 ViewModel + StateFlow/LiveData 管理。
- Room 等注解处理用 KSP（版本与 Kotlin 配对，如 `1.9.22-1.0.17`）；既有 kapt 项目（SonarEar）保持一致，不在同一任务中混用两套。

## 测试约定

- JUnit 4（4.13.2）纯逻辑单测：不依赖 Android 框架与设备，验证命令 `.\gradlew.bat testDebugUnitTest`。
- 可测性优先：业务规则、状态流转、格式化/计算逻辑下沉到纯 Kotlin 类，UI 层保持薄。
- 新增纯逻辑类必须附带单测；bugfix 必须补回归单测。
- 真机测试（任务卡要求时）：由 `tools\device_test.ps1` 统一执行，做安装、启动、冒烟与 `connectedDebugAndroidTest`；androidTest 依赖、断言写法与失败判据见 `docs/android/device-test-automation.md`。不得为了跑通而删除断言或跳过用例。

## 混淆与发布取舍

- 个人/小工具类 App 默认 `minifyEnabled false`（5 个参考仓库均未开混淆），换取可读崩溃堆栈与更低维护成本。
- 任务卡明确要求加固/瘦身时再开启 R8，并同步补充 keep 规则与回归验证；开启混淆属于「工程配置变更」，必须写进任务卡。

## 其他

- 权限在 AndroidManifest 中最小化声明；新增权限写入任务卡「工程配置变更」。
- 资源与字符串不硬编码用户可见文案到代码里的业务规则中；新增页面同时考虑深色模式（Compose Material3 主题默认支持）。
- 密钥与签名边界见 `signing-and-release.md`；版本基线与 Compose 配置路径见 `gradle-baseline.md`。
