# Android App 评审检查清单

> 本清单是 App 任务评审的唯一权威：developer 完成后按本清单自检，reviewer 按本清单逐条评审并在评审报告记录结论。工程规范原文见 `bjm-app-format` Skill 与 `docs/android/`；本清单只规定「检查什么、怎么判」。
>
> 标注「脚本」的条目由 `python tools/app_check.py .` 机械判定：脚本 ERROR 即不符合；标注「人工」的条目由评审 Agent 依据规则原文判断。脚本 PASS 只代表确定性底线成立，不代表整体通过。

## 一、工程基线（脚本）

1. （脚本）Gradle Wrapper 存在且版本 ≥ 基线（`wrapper`）。
2. （脚本）app 模块构建文件存在，JDK 目标为 17（`app-module`、`jdk-target`）。
3. （脚本）SDK 基线：compileSdk 34 / targetSdk 34 / minSdk ≥ 26（`sdk-baseline`）。
4. （脚本）AGP 与 Gradle 版本按矩阵兼容；Compose 编译器与 Kotlin 版本配对（`agp-gradle-compat`、`compose-compiler`）。
5. （脚本）namespace 与 applicationId 一致；versionCode / versionName 显式声明（`app-id-namespace`、`versioning`）。

## 二、安全与卫生（脚本+人工）

6. （脚本+人工）`local.properties` 被 .gitignore 忽略（`local-properties-ignore` WARN 逐条确认）。
7. （脚本+人工）无明文凭据入库：gradle.properties 无口令 / KEY_ALIAS / API_KEY 键，keystore.properties 被忽略（`secrets-plaintext`）。
8. （脚本+人工）release 签名策略与任务卡一致；APK 命名 `<app>-<versionName>.apk`（`release-signing`、`apk-naming` WARN 逐条确认）。
9. （脚本+人工）根目录 README.md 存在（`readme` WARN）。

## 三、需求与实现（人工）

10. （人工）需求可追溯：任务类型与任务卡一致；任务卡冻结的验收条件逐条核对，每条都有实现与证据。
11. （人工）范围最小性反向检查：无任务卡范围外的页面、接口、第三方依赖与配置变更；超出项一律回退，确属必要先扩任务卡。
12. （人工）工程规范符合性：纯 Kotlin；UI 技术路线与项目一致（Compose/Material3 或 View/XML+ViewBinding）；包结构、KSP/kapt 与 JUnit4 单测约定符合 `docs/android/app-coding-conventions.md`。
13. （人工）UI 审美符合 `docs/android/ui-design-guide.md`：按该文「审美验收清单」10 条逐条核对（单一低饱和强调色、6 级字阶、4dp 网格、统一圆角、无硬编码样式、深色模式可用等）；新界面优先复用 `docs/android/design-system/` 实例化的 `App*` 组件，品牌色与 `docs/brand/brand-vi.md` 一致。
14. （人工）构建验证证据：评审 Agent 亲自执行 `python tools/app_check.py .` 与任务卡指定的构建/单测命令（默认 `.\gradlew.bat testDebugUnitTest assembleDebug`），退出状态真实记录，不采信开发报告转述。
15. （人工）真机验证证据（任务卡要求时必检）：评审 Agent 亲自执行 `tools\device_test.ps1`，核对 `result.json` 的结论与逐步骤状态、`screenshot.png` 与 `logcat.txt`；退出码非 0 即不符合。设备不可用返回 `BLOCKED`，不得以静态检查或单测替代真机结论。
16. （脚本+人工）设计与系统栏一致性：`docs/android/design-system/*.kt.template` 与 app 实例逐字一致（`design-system-templates` WARN 逐条确认）；浅/深色下状态栏与导航栏图标清晰可见（`ui-design-guide.md` 审美清单第 11 条）。
17. （人工）测试依赖与验收一致：验收要求运行单测时，核实 `testImplementation` 实际存在；缺失时任务卡须写明新增测试依赖及允许修改的构建文件范围，可用 `.\gradlew.bat :app:dependencies --configuration debugUnitTestCompileClasspath` 复核。

18. （脚本）范围依从性：`python tools/scope_check.py --task <任务卡> --since <基线 SHA>` 退出码为 0；任何落在「允许修改」之外的改动直接判不合规，不得以「顺手修复」解释。
19. （脚本）Compose 界面回归底线：`shapes-capsule` 为 ERROR（`Shapes.extraLarge` 被设为胶囊会让 M3 对话框变椭圆并裁字）；`settings-write-scope`、`chat-list-reverse` 为 WARN，须逐条确认并说明理由。
20. （人工）交互级真机复核：涉及界面、主题、Shapes、Typography、列表布局、设置项、对话框、导航的改动，必须核对 `-UiSteps` 的 `step-*.png`：首屏第一次可见帧即目标状态、首/末条不被浮动按钮或输入栏遮挡、设置项切换后即时生效并持久化。仅有冒烟 PASS 与单测全绿不构成界面改动的验收证据。
21. （人工）未改动区域回归：逐项对照任务卡「明确不修改」，用 `git diff --name-only` 确认对应文件未被触碰；并确认既有页面的视觉与交互与改动前一致。

## 与校验器的分工

- 脚本 ERROR：直接判为不合规，写入评审报告「确定性检查」。
- 脚本 WARN：逐条人工确认，在报告中说明采信与否及理由。
- 脚本未覆盖的语义质量（架构合理性、状态管理正确性、需求覆盖度）由评审 Agent 判断，属需求可追溯范畴，规则见 `docs/agent-framework/roles/app-reviewer.md`。
