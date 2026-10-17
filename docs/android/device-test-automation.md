# 真机自动化测试规范

本文件是 App 真机测试的**唯一权威**：测试分层、设备约定、执行命令、证据要求与失败判据。执行细节见 `bjm-app-device-test` Skill 与 `tools/device_test.ps1`。

## 一、测试分层

| 层 | 内容 | 是否需要设备 | 命令 | 何时必须执行 |
| --- | --- | --- | --- | --- |
| L1 纯逻辑单测 | JUnit 4，不依赖 Android 框架 | 否 | `.\gradlew.bat testDebugUnitTest` | 每次任务（默认验证集） |
| L2 真机冒烟 | 安装、启动、进程存活、无崩溃/ANR、截图 | 是 | `tools\device_test.ps1` | 任务卡要求真机验证，或改动涉及界面/导航/权限/启动流程 |
| L3 真机 UI 断言 | `connectedDebugAndroidTest`（Compose UI Test / Espresso / UiAutomator） | 是 | 同上（脚本自动执行） | 任务卡验收条件含页面级断言，或 bugfix 需复现 UI 行为 |

判定顺序：L1 通过 → L2 通过 → L3 通过。任一层失败即任务未完成，不以低层通过替代高层失败。

## 二、设备约定

- 一台 USB 连接的真机，开启「开发者选项 → USB 调试」，首次连接在手机弹窗授权。
- 多设备时用 `-Serial <序列号>` 指定；脚本会把 `ANDROID_SERIAL` 设为该设备，避免 `connectedDebugAndroidTest` 打到错误设备。
- 设备 API 级别不得低于项目 `minSdk`；报告需记录设备型号与 API 级别，便于复现。
- 模拟器可用于本机自测，但任务交付的证据必须来自真机（除非任务卡明确允许模拟器）。

## 三、执行命令

```powershell
# 标准执行（等待手机 -> 构建 -> 安装 -> 启动 -> 冒烟/UI 断言 -> 取证）
powershell -ExecutionPolicy Bypass -File tools\device_test.ps1 -ProjectDir .

# 多设备指定目标机
powershell -ExecutionPolicy Bypass -File tools\device_test.ps1 -ProjectDir . -Serial <serial>

# 只复跑测试，不重新构建
powershell -ExecutionPolicy Bypass -File tools\device_test.ps1 -ProjectDir . -SkipBuild

# 只做冒烟，不跑 androidTest
powershell -ExecutionPolicy Bypass -File tools\device_test.ps1 -ProjectDir . -SkipInstrumented
```

退出码：`0` = PASS，`1` = FAIL，`2` = BLOCKED。

## 四、证据要求

证据目录默认 `.agent-artifacts\device-test`（已 gitignore，不进入交付提交），必须包含：

| 文件 | 内容 |
| --- | --- |
| `result.json` | 结论、原因、设备型号/API、包名、APK、逐步骤状态、证据清单 |
| `build.log` | `assembleDebug` 完整输出 |
| `install.log` | `adb install` 输出 |
| `launch.log` | `am start -W` / `monkey` 输出 |
| `connectedDebugAndroidTest.log` | UI 断言测试输出（存在 androidTest 时） |
| `logcat.txt` | 启动后全量日志（含崩溃/ANR 判定依据） |
| `screenshot.png` | 启动后的界面截图 |

开发报告与评审报告都必须引用 `result.json` 的结论与证据路径，不得只写「已验证」。

## 五、失败判据

判定为 FAIL 的情形：

- `assembleDebug` 非 0 退出；
- `adb install` 未返回 `Success`；
- 启动命令报 `Error type` / `No activities found`；
- 启动后 `pidof <applicationId>` 为空；
- `connectedDebugAndroidTest` 非 0 退出（用例失败或 instrumentation 崩溃）；
- logcat 出现本应用 `FATAL EXCEPTION` 或 `ANR in <applicationId>`。

判定为 BLOCKED 的情形（不计入修复轮次）：

- 无 `adb`、无 `gradlew.bat`；
- 等待超时仍无设备，或设备长期 `unauthorized` / `offline`；
- 无法隔离设备上已保存的凭据（见 7.1）；
- JDK/SDK 环境缺失导致无法构建。

## 六、androidTest 依赖基线

任务卡要求 L3 时，按下表添加（版本与项目现有 Compose/Kotlin 基线保持一致，新增依赖须在任务卡「依赖变更」显式列出）：

| 用途 | 依赖 |
| --- | --- |
| Instrumentation Runner | `defaultConfig { testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner" }` |
| 基础测试框架 | `androidTestImplementation "androidx.test.ext:junit:<ver>"`、`androidTestImplementation "androidx.test:runner:<ver>"` |
| Compose UI 断言 | `androidTestImplementation "androidx.compose.ui:ui-test-junit4"`（随 Compose BOM）、`debugImplementation "androidx.compose.ui:ui-test-manifest"` |
| View/XML 断言 | `androidTestImplementation "androidx.test.espresso:espresso-core:<ver>"` |
| 跨应用/系统 UI | `androidTestImplementation "androidx.test.uiautomator:uiautomator:<ver>"` |

约定：

- 测试代码位于 `app/src/androidTest/kotlin`（或 `java`），类名以 `Test` 结尾。
- 元素定位优先 `testTag` / `contentDescription`，不依赖文案与坐标。
- 断言不依赖真实网络与真实账号；时间、随机数、外部服务一律 mock。
- 不因为「跑不过」而删除断言或跳过用例；确属环境限制时在报告中说明并返回 BLOCKED。

## 七、安全红线

- 只用 mock 数据；不连接生产后端、不使用生产账号。
- 截图与日志可能含设备信息，只保存在本地 `.agent-artifacts/`，不得上传第三方服务或提交入库。
- 不记录口令、Token、API Key；发现日志中出现凭据立即清除并报告。
- 不自动发版、不上架、不推送远端。

### 7.1 取证前凭据隔离（必须）

- 应用持久化 API Key / Token 时，截图、录屏或启动取证前必须先隔离：优先 `adb shell pm clear <applicationId>`，或改用独立测试包/测试用户；不能清数据时，先 `am force-stop`，再移出 `shared_prefs` 中的凭据文件并确认目录为空。
- `am force-stop` 本身不足以隔离：旧进程可能回写 SharedPreferences，使应用恢复真实凭据并联网。
- 需要展示登录态、问候等界面时，只注入 dummy 值或使用本地缓存文本，禁止发起真实网络请求。
- 取证后恢复原数据并校验；无法完成隔离时判定为 `BLOCKED`，不得带真实凭据取证。

## 八、当前限制

- 无 CI 集成：测试由 Agent 在本机触发，设备可用性依赖人工连接。
- 未覆盖性能、功耗、弱网、多机型矩阵；这些需要独立测试方案。
- 截图仅作人工核对证据，不做像素级图像比对。

## 交互级复核参数（-UiSteps）

`tools\device_test.ps1` 支持 `-UiSteps "<步骤串>"`，在冒烟与崩溃检查之间执行界面交互断言：

| 步骤 | 含义 |
| --- | --- |
| `tap:控件文本` | 用 uiautomator dump 按 `text` 或 `content-desc` 精确匹配，点击其 bounds 中心 |
| `assert:文本` | 重新 dump，断言当前界面包含该文本 |
| `wait:毫秒` | 等待（动画/网络） |
| `shot:名称` | 截图到 `step-<序号>-<名称>.png` |

示例：

```powershell
powershell -ExecutionPolicy Bypass -File tools\device_test.ps1 -ProjectDir . `
  -UiSteps "tap:设置;assert:自动涌现;tap:自动涌现;assert:每天早上 6 点;shot:surge-dialog"
```

约定：

- 未提供 `-UiSteps` 时结果记为 `SKIP`；涉及界面的任务卡不得以 SKIP 作为验收证据。
- 断言失败立即 FAIL（退出码 1），并保存 `step-*-assert-fail-*.png`。
- 首屏类改动配合 300ms 连拍，确认「第一次可见帧」即目标状态。
- 设置项类改动需读取应用私有存储确认持久化，不能只看界面是否关闭。
