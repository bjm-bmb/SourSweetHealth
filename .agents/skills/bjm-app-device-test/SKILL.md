---
name: bjm-app-device-test
description: "真机自动化测试操作规范：等待手机连接并授权 USB 调试，构建并安装调试包，启动应用做冒烟与崩溃检查，存在 androidTest 时执行 connectedDebugAndroidTest UI 断言，最后采集截图与 logcat 作为证据。用于任务卡要求真机验证，或改动涉及界面、导航、权限、启动流程等只能在设备上观察的行为；不用于纯逻辑改动（走 testDebugUnitTest 即可）。"
---

# 真机自动化测试（Windows）

本技能是**真机测试执行的唯一入口**。测试分层与失败判据见 `docs/android/device-test-automation.md`，本技能只规定「怎么跑、看什么、怎么判」。

## 适用与边界

- 任务卡在「真机验证」写明要求时，必须执行；改动涉及界面、导航、权限、启动流程、通知、传感器、文件/网络访问等只能在设备上观察的行为时，默认执行。
- 纯逻辑改动（工具类、纯函数、状态计算）走 `.\gradlew.bat testDebugUnitTest`，不强制真机。
- 真机验证失败即任务未完成；不得以「本机没有设备」为由跳过——无设备时如实返回 `BLOCKED`，由主 Agent 提醒需求方连接手机。
- 测试数据只用 mock；不得连接生产后端、不得上传截图到外部服务、不得在日志中记录凭据。
- **取证前隔离设备上已保存的凭据**：应用若持久化 API Key / Token，截图、录屏或启动取证前必须先清除应用数据（优先 `adb shell pm clear <applicationId>`）或改用独立测试包/测试用户；仅在内存注入 dummy 值。`am force-stop` 不能阻止旧进程回写 SharedPreferences，不得作为隔离手段。无法隔离时返回 `BLOCKED`。
- **不得改动用户日常设备的系统网络配置**（私有 DNS、HTTP 代理、防火墙、飞行模式、Wi-Fi/移动数据开关）来制造离线/超时/失败态：优先使用应用内可注入的 mock/延迟，或独立测试设备/模拟器。确需修改时先征得用户同意，测试后逐项恢复，并用 `ping`/接口请求验证恢复结果；恢复步骤与验证结果写入报告。

## 前置条件

- 手机已用 USB 连接并开启「开发者选项 → USB 调试」；首次连接需在手机弹窗点「允许 USB 调试」。
- 本机具备 Android SDK（`C:\Android\Sdk`，含 `platform-tools\adb.exe`）与 JDK 17（`C:\Android\jdk17\jdk17`）。
- 项目带 `gradlew.bat`。

## 取证前凭据隔离（必须）

截图、录屏或启动取证前，按以下顺序隔离设备上已保存的凭据：

1. 优先清除应用私有数据（会删除本地记录，测试数据必须可重建）：

   ```powershell
   adb shell pm clear <applicationId>
   ```

2. 不能清数据时，先停止应用并移出凭据文件，确认目录为空后再启动：

   ```powershell
   adb shell am force-stop <applicationId>
   adb shell run-as <applicationId> mv shared_prefs/settings.xml /data/local/tmp/settings.xml.bak
   adb shell run-as <applicationId> ls shared_prefs
   ```

3. 需要展示问候、登录态等界面时，只注入 dummy 值或使用本地缓存文本，禁止发起真实网络请求。
4. 取证结束后恢复原数据并校验；恢复步骤与校验结果写入报告。

注意：`am force-stop` 后旧进程仍可能回写 SharedPreferences，必须确认 `shared_prefs` 为空或凭据文件已移出，再启动应用。

## 一键执行

在项目根目录（无需手动敲 adb 命令）：

```powershell
powershell -ExecutionPolicy Bypass -File tools\device_test.ps1 -ProjectDir .
```

常用参数：

| 参数 | 作用 |
| --- | --- |
| `-Serial <序列号>` | 多设备时指定目标设备（`adb devices` 查看） |
| `-WaitSeconds 600` | 等待用户连机的超时时间，默认 300 秒 |
| `-SkipBuild` | 复用已有 APK，不重新构建 |
| `-SkipInstrumented` | 只做冒烟（安装/启动/崩溃检查），不跑 UI 断言 |
| `-ApkPath <路径>` | 指定 APK，默认取 `app\build\outputs\apk\**` 最新产物 |
| `-ArtifactsDir <目录>` | 证据目录，默认 `.agent-artifacts\device-test` |

脚本会**先打印连机提示并轮询等待设备**：检测到 `unauthorized` 会提示在手机上授权；超时则返回 `BLOCKED`。这一步就是「只要提醒我连上手机」的落点——Agent 执行脚本，用户只需插线并授权。

## 脚本执行步骤

1. 环境解析：定位 `adb`、`gradlew.bat`，缺失即 `BLOCKED`。
2. 等待设备：轮询 `adb devices` 直到出现状态为 `device` 的目标机。
3. 构建：`.\gradlew.bat assembleDebug`，日志落 `build.log`。
4. 安装：`adb install -r -t <apk>`。
5. 启动：解析 launcher 组件并 `am start -W`（无法解析时回退 `monkey`）。
6. 存活检查：`pidof <applicationId>` 必须非空。
7. UI 断言：`app\src\androidTest` 有测试代码时执行 `.\gradlew.bat connectedDebugAndroidTest`。
8. 崩溃检查：logcat 中出现本应用 `FATAL EXCEPTION` 或 `ANR in <applicationId>` 即失败。
9. 取证：截图 `screenshot.png`、`logcat.txt`、各步骤日志与 `result.json`。

## 结论映射

| 退出码 | 结论 | 含义与后续动作 |
| --- | --- | --- |
| 0 | `PASS` | 安装、启动、进程存活、无崩溃、UI 测试（如有）全部通过 |
| 1 | `FAIL` | 构建失败、安装失败、启动失败、进程不存在、崩溃/ANR、UI 测试失败；证据在证据目录，回传开发修复 |
| 2 | `BLOCKED` | 无 adb、无 gradlew、设备未连接或未授权、等待超时；不计入修复轮次，由主 Agent 提示用户连机 |

`PASS` 只代表真机层面可运行；语义正确性与需求覆盖仍按 `review-checklist.md` 人工判定。

## UI 断言（androidTest）约定

任务卡要求页面级断言时，在 `app/src/androidTest` 增加测试，并满足：

- `defaultConfig` 声明 `testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"`。
- Compose 项目使用 `androidx.compose.ui:ui-test-junit4` 与 `debugImplementation` 的 `ui-test-manifest`；View/XML 项目使用 `androidx.test.espresso:espresso-core` 或 `androidx.test.uiautomator:uiautomator`。
- 版本与项目现有 Compose/Kotlin 基线一致，新增依赖须在任务卡「依赖变更」显式列出。
- 断言必须可重复：不依赖网络、时间、真实账号；进入页面用 `createAndroidComposeRule`/`ActivityScenario`，元素定位优先 `testTag`。

## 与闭环的衔接

- `app_developer`：构建自验阶段执行本脚本，报告真实退出码、设备型号/API 级别与证据目录；未通过不得声称完成。
- `app_reviewer`：评审阶段独立再跑一次（不采信开发报告），核对 `result.json` 与截图；设备不可用时返回 `BLOCKED`，不以静态检查替代。
- 截图与日志属于临时产物，写入 `.agent-artifacts/`（已 gitignore），不进入交付提交。

## 常见故障处置

| 现象 | 处置 |
| --- | --- |
| `unauthorized` | 手机上点「允许 USB 调试」；必要时「撤销 USB 调试授权」后重插 |
| `offline` | 重插数据线或 `adb kill-server; adb start-server` |
| 列表为空 | 换数据线/接口，确认 USB 模式为「文件传输」，安装厂商 USB 驱动 |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 先 `adb uninstall <applicationId>` 再重装（会清除该应用本地数据，需用户确认） |
| `INSTALL_FAILED_INSUFFICIENT_STORAGE` | 清理设备空间后重试 |
| 启动后进程消失 | 看 `logcat.txt` 首个 `FATAL EXCEPTION` 堆栈定位崩溃点 |
| 截图/启动后仍使用旧 Key | 不要只做 `am force-stop`；按「取证前凭据隔离」清除应用数据或移出 `shared_prefs` 并确认目录为空 |
| `connectedDebugAndroidTest` 无设备 | 确认 `ANDROID_SERIAL` 已指向目标机（脚本自动设置） |

## 交互级复核（-UiSteps，界面改动必用）

冒烟只证明应用能启动，不证明界面正确。涉及主题、Shapes、Typography、列表布局、设置项、对话框、导航的改动，必须提供 `-UiSteps` 走完必点路径。

```powershell
powershell -ExecutionPolicy Bypass -File tools\device_test.ps1 -ProjectDir . -Serial <serial> `
  -UiSteps "tap:设置;assert:自动涌现;tap:自动涌现;assert:每天早上 6 点;shot:surge-dialog"
```

步骤语法（分号分隔）：`tap:控件文本`（按 text / content-desc 精确匹配后点中心）、`assert:文本`（当前界面必须包含）、`wait:毫秒`、`shot:名称`。

约定：
- 每步自动截图到 `<ArtifactsDir>\step-<序号>-<名称>.png`，作为评审证据。
- 断言失败立即 `FAIL`（退出码 1）并保留失败截图。
- 首屏类改动额外用 300ms 间隔连拍，确认第一次可见帧就是目标状态。
- 设置项类改动：切换后重新进入页面确认保持，并读取应用私有 DataStore / SharedPreferences 文件确认已落盘。
