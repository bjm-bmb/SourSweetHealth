# APP-000：harness 基线自检（工程基线体检）

状态：草稿

> 本任务卡用于验证 app-agent harness 全流程是否可用：用一个最小任务跑通「规划 → 开发 → 构建验证 → 独立评审 → PASS」。它不是真实需求；执行完毕后可删除或归档。执行时按实际情况填写各节。

## 任务分类与需求可追溯

- 任务类型：`feature`
- 用户可见行为是否变化：否（自检场景，非真实需求）
- 需求依据：本任务卡自含的已确认需求快照（下方「实现要点」即冻结内容）
- 本轮适用的已确认需求快照：
  - 在项目中新增一个纯 Kotlin 工具类 `VersionInfo`，返回 `versionName` 与 `versionCode` 的格式化字符串；
  - 为其编写 JUnit4 纯逻辑单测；
  - 不改动任何既有页面与行为。

## 目标

- 用户价值：验证 harness 三角色闭环与本机构建链路可用
- 目标页面 / 功能入口：无 UI（纯逻辑类 + 单测）

## 范围

- 允许修改：`app/src/main/java/<根包>/util/`（新增 VersionInfo.kt）、`app/src/test/java/<根包>/`（新增单测）
- 明确不修改：既有页面、构建脚本、签名配置、依赖清单
- 显式不做清单：UI 页面、网络请求、数据库、第三方依赖新增（任务卡未要求，开发阶段不得自行新增）
- 参考既有代码 / 文档：`docs/android/app-coding-conventions.md`

## 实现要点

- 页面与导航：不涉及
- 数据与状态：`VersionInfo.format(versionName: String, versionCode: Int): String` 返回 `"<versionName> (<versionCode>)"`
- 涉及文件 / 模块：`util/VersionInfo.kt` + 对应测试类
- 依赖变更：不涉及（JUnit 4.13.2 为既有依赖）
- 工程配置变更：不涉及

## 验收条件

1. Given 目标项目已安装本 harness
   When 检查安装结果
   Then `docs/agent-framework/harness-version.txt` 存在且内容与本包 VERSION 一致
2. Given harness 文件就位
   When 运行 `python tools/app_check.py .`
   Then 退出码为 0（WARN 逐条人工确认并记录）
3. Given `app_developer` 完成实现
   When 执行 `.\gradlew.bat testDebugUnitTest assembleDebug`
   Then 单测全部通过且构建成功，`app/build/outputs/apk/debug/` 下产出 APK
4. Given handoff 提交已创建
   When `app_reviewer` 在干净 worktree 评审
   Then 评审报告 13 条逐条有结论（含范围最小性反向检查与亲自构建复核），最终结论为 PASS

## 构建验证

- 验证命令：`.\gradlew.bat testDebugUnitTest assembleDebug`
- 必须通过：`python tools/app_check.py .` 退出码为 0（WARN 逐条人工确认）
- JDK/SDK 环境：JDK 17（`C:\Android\jdk17\jdk17`）、Android SDK（`C:\Android\Sdk`）

## 闭环交付

- 策略：完整闭环（默认）
- 最大修复次数：2（初始评审不计入；达到上限仍失败则停止为 `FAIL`）
- handoff 提交：允许（默认；仅由主 Agent 在任务分支创建，只暂存允许路径内的本任务改动，本地保留，不推送、不合并）
- 本轮 handoff SHA（创建快照后填写）：
- 最终评审结论：PASS / FAIL / BLOCKED / 未执行
- `FAIL` 回传边界：仅修复评审证据支持且仍在本任务允许路径内的问题
- `BLOCKED` 终止条件：harness 文件缺失、git/worktree 不可用、Python 或 JDK/SDK 环境缺失时立即停止

## 安全边界

- 口令、keystore、API Key 不入库：不写入代码、注释、gradle.properties、local.properties、任务卡或报告。
- 不使用生产后端、生产账号或真实用户数据。
- 未明确授权时，不发版、不上架、不推送远端。
