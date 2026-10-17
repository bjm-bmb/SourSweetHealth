---
name: bjm-app-delivery
description: "执行 Android App 任务的规划、开发、构建验证、独立评审（app_check + 构建/单测复核）及至多两轮证据驱动修复复评。用于要求完整交付或失败自动修复的 App 构建任务；不用于只读分析、单纯问答或文档维护。"
---

# App 闭环交付

将一个已确认的 App 需求交付到可审查状态：规划、实现、构建验证、独立评审，并在评审 `FAIL` 时有边界地修复和复评。

这是一套调度流程，不替代 `bjm-app-format` 的工程规范与 `bjm-app-build` 的构建操作手册。开始前必须确认任务卡已冻结本轮适用的已确认需求快照（状态为「已确认」）；只有需求摘要或口头描述、尚无已确认任务卡时，先按 `bjm-app-intake` 起草任务卡并经需求方确认，不得直接开始开发。

## 任务分类

规划前，主 Agent 与 `app_planner` 必须将任务归为一种类型，并在任务卡写明判定依据。类型定义与各类的最低评审证据，以 `docs/agent-framework/README.md` 的「任务类型」表为唯一权威。

- 判断原则：用户能观察到的行为变化是 `feature` 或 `bugfix`；观察不到的结构调整才是 `refactor`，其验收以回归证据为主。
- 工程规范（`bjm-app-format` 与 `docs/android/`）仍是实现的唯一权威，任务卡不复述规范原文，只写本任务特有的需求与配置变更。
- 未明确的业务规则、数据来源、状态、权限或兼容性不能自行补全，必须标记 `BLOCKED`。

## 启动与边界

- 仅在用户要求实现、修复、闭环交付或「失败继续修复」时使用；只读分析、代码问答不进入本流程。
- 任务卡须经需求方确认（状态为「已确认」）后方可进入规划与开发，明确允许路径、验收条件、构建验证命令与评审边界。
- 默认最多两次「`FAIL` → 修复 → 独立复评」。`BLOCKED` 立即停止且不计入修复次数；第三次评审仍为 `FAIL` 时停止并报告 `FAIL`。
- 不推送、不发布、不上架、不访问生产环境、不扩大任务范围。涉及新增第三方依赖、权限或结构调整的歧义，先向用户确认。唯一例外：评审 `PASS` 且人工审核通过后的合并回主线与 worktree 清理，且须经用户同意（见交付闭环第 5 步）。
- 基线版本（compileSdk/targetSdk/minSdk/JDK/AGP/Gradle/Kotlin）不得为完成任务而降级或绕过校验器；确需变更时按 `docs/android/gradle-baseline.md` 的版本递增规则随任务卡显式进行。

## 交付闭环

1. 让 `app_planner` 只读确认任务类型、范围与实现要点，并产出实施计划。计划为 `BLOCKED` 时先解决阻塞项，不能猜测。
2. 让 `app_developer` 在任务 worktree 按任务卡范围实现，并按 `bjm-app-build` 完成构建自验：先运行 `python tools/app_check.py .` 修正全部 ERROR，再执行任务卡指定的构建验证命令（默认 `.\gradlew.bat testDebugUnitTest assembleDebug`）并记录真实退出状态；任务卡要求真机验证时执行 `tools\device_test.ps1`（等待用户连机 → 安装启动 → UI 断言 → 截图/日志取证），退出码 0 方为通过；最后按 `review-checklist.md` 逐条自检。**构建与真机验证完成前不得声称完成。** developer 不创建提交；发现需求疑点或需要扩范围时回报主 Agent。
3. 主 Agent 审核开发报告后，在任务分支创建一个**仅本地**的 handoff 提交，记录 SHA；创建前确认任务卡及本流程所需的 Agent 配置已在可创建 worktree 的 Git 基线内，只暂存任务卡允许路径内且由本任务产生的文件，绝不使用广泛暂存命令。无法区分用户既有改动时停止并请求处理。该动作只用于干净评审 worktree 的可追溯交接，绝不推送或合并。
4. 让 `app_reviewer` 在另一份干净 worktree 中检出该 SHA，确认工作区没有未跟踪的非忽略文件，再依据任务卡冻结的需求快照执行：先核对 git 状态，运行 `python tools/app_check.py .` 作为确定性底线（ERROR 直接判不合规，WARN 逐条人工确认），再亲自执行任务卡指定的构建与单测命令复核构建证据（不采信转述），最后按 `review-checklist.md` 逐条人工评审。
5. 评审为 `PASS` 时，主 Agent 汇总开发报告和评审证据，等待人工审核；不自动合并。人工审核通过后，主 Agent 必须提示用户将 handoff 分支合并回主追踪线（交付物以此进入正常版本追踪），并清理本任务的 worktree 与已合并分支；经用户同意后执行。不提示、不合并而直接结束任务的，交付物只留在 handoff 分支与 worktree 中，视为未完成的收尾。
6. 评审为 `FAIL` 时，主 Agent 仅把报告中的可复现证据、handoff SHA 和本轮编号交回 `app_developer`。developer 只修复该证据支持、且仍在原任务范围内的问题；随后由主 Agent 创建新的本地 handoff 提交并重新执行第 4 步。
7. 评审为 `BLOCKED`，或达到修复上限仍失败时，停止循环。主 Agent 报告真实状态与证据，说明已完成轮次和需要用户决定的事项；不得用删节、弱化口径或跳过构建验证把结果改成通过。
8. 闭环收尾（`PASS` / `FAIL` / `BLOCKED` 均包含）报告交付物后，主 Agent 按 `bjm-app-harvest` 回顾本轮积累的可复用能力：有候选时主动询问用户是否沉淀，用户同意后在目标项目用 `tools\harvest.ps1 -Action new` 落盘为 `docs/agent-framework/learnings/LRN-xxxx-*.md`（状态 `proposed`），再由规范包仓库 `-Action import` 回传并 `-Action promote` 标记落地；无候选时不打扰用户。

## 最终交付物

- `PASS`：任务卡、最终 handoff SHA、任务类型与判定依据、修改与新增文件清单、`python tools/app_check.py .` 与构建/单测命令及真实退出状态、APK 产物路径、评审结论与逐条证据、已知限制。
- `FAIL`：上述证据加上每轮修复摘要与仍未满足的验收条件。
- `BLOCKED`：阻塞的需求/权限/环境条件、已验证的事实、最小解除动作。
