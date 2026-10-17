# APP-xxx 评审报告

- 评审结论（单选）：PASS / FAIL / BLOCKED
- 任务 ID：
- handoff SHA：
- 评审 Agent：app_reviewer
- 评审时间：

## 需求可追溯

- 任务类型与任务卡一致：是 / 否
- 验收条件逐条核对（逐条列 Given/When/Then 与核对结果）：
- 实现与已确认需求快照一致：是 / 否；不一致项：
- 反向检查（无任务卡范围外的页面/接口/依赖/配置变更）：是 / 否；超范围项：

## 检查清单逐条结论

> 检查项与 `review-checklist.md` 的 15 条一一对应；「脚本」条目的结论以 `tools/app_check.py` 输出为依据，「人工」条目由评审 Agent 判断。

| # | 检查项 | 结论 | 证据（文件/行号/命令） |
| --- | --- | --- | --- |
| 1 | Gradle Wrapper 存在且版本 ≥ 基线 | 通过 / 不通过 |  |
| 2 | app 模块存在，JDK 目标 17 | 通过 / 不通过 |  |
| 3 | SDK 基线 34/34/≥26 | 通过 / 不通过 |  |
| 4 | AGP/Gradle 兼容，Compose 编译器配对 | 通过 / 不通过 |  |
| 5 | namespace == applicationId，版本声明齐全 | 通过 / 不通过 |  |
| 6 | local.properties 被忽略 | 通过 / 不通过 / 不涉及 |  |
| 7 | 无明文凭据入库 | 通过 / 不通过 |  |
| 8 | release 签名策略与 APK 命名 | 通过 / 不通过 / 不涉及 |  |
| 9 | README.md 存在 | 通过 / 不通过 |  |
| 10 | 需求可追溯，验收条件逐条覆盖 | 通过 / 不通过 |  |
| 11 | 范围最小性，无超范围变更 | 通过 / 不通过 |  |
| 12 | 工程规范符合性（纯 Kotlin、UI 路线、包结构、单测约定） | 通过 / 不通过 |  |
| 13 | UI 审美符合设计指南 | 通过 / 不通过 / 不涉及 |  |
| 14 | 构建验证证据真实（亲自执行） | 通过 / 不通过 |  |
| 15 | 真机验证证据（安装/启动/崩溃检查/UI 断言） | 通过 / 不通过 / 不涉及 |  |

## 确定性检查

- 已运行命令：`python tools/app_check.py .`
- 退出状态（0 = 无 ERROR）：
- ERROR 列表（规则 + 文件:行号）：
- WARN 列表及逐条采信结论（采信 / 不采信 + 理由）：

## 构建验证

- git 状态核对（HEAD == handoff SHA、无未跟踪非忽略文件）：是 / 否
- 已运行命令：`.\gradlew.bat testDebugUnitTest assembleDebug`（或任务卡指定命令）
- 退出状态与单测结果（通过/失败/跳过数）：
- APK 产物路径与文件名：

## 真机验证

> 任务卡要求真机验证时必填；未要求时写明「不涉及」及理由。

- 设备型号 / API 级别：
- 已运行命令：`powershell -ExecutionPolicy Bypass -File tools\device_test.ps1 -ProjectDir .`（或任务卡指定命令）
- 退出码与 `result.json` 结论（PASS / FAIL / BLOCKED）：
- 逐步骤状态（构建 / 安装 / 启动 / 进程存活 / UI 测试 / 崩溃检查）：
- UI 断言用例数与结果（无则为「仅冒烟」）：
- 证据路径（`result.json`、`screenshot.png`、`logcat.txt`）：

## FAIL 修复回传

> 仅当结论为 FAIL 时填写。

- 未满足条款（引用上表编号）：
- 最小定位路径（文件 + 位置）：
- 关联证据路径：
- 修复边界（仍在原任务允许路径内的问题）：

## BLOCKED 阻塞说明

> 仅当结论为 BLOCKED 时填写。

- 外部阻塞条件：
- 已验证的事实：
- 最小解除动作：
