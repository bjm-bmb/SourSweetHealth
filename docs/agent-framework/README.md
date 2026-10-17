# App Agent 工作流

这套框架用于在本仓库中以受控方式完成 Android App 的构建任务：主 Agent 管理需求，规划 Agent 只读分析，开发 Agent 在独立 worktree 中实现并自验构建，独立评审 Agent 在干净 worktree 中复检（确定性校验 + 亲自构建复核）。仓库保存代码、任务卡和 Agent 执行规则。

工程规范以 `bjm-app-format` Skill 与 `docs/android/`（`gradle-baseline.md`、`app-coding-conventions.md`、`signing-and-release.md`）为唯一权威，任何既有项目配置、口头描述或第三方模板都不能覆盖它；其中「最小充分原则」是开发的第一优先级——只做任务卡冻结范围内的页面、接口、依赖与配置变更。三个 Agent 开始工作前都必须完整读取对应角色规范（`docs/agent-framework/roles/`），并在任务卡中指出本次实际适用的规则。评审检查以 `docs/agent-framework/review-checklist.md` 为唯一权威，其中可机械判定的条目由 `tools/app_check.py` 兜底。

## 默认闭环交付

除非任务卡明确只要求规划、分析或评审，App 任务默认采用 `bjm-app-delivery` 闭环：

1. `app_planner` 只读规划；阻塞项先由主 Agent 澄清。任务涉及工程基线或签名变更时，计划中对照 `docs/android/` 规范写明依据与 `app_check.py` 预期结果。
2. `app_developer` 按冻结的任务卡在任务 worktree 中实现，并完成构建自验（先运行 `python tools/app_check.py .` 修正全部 ERROR；再执行任务卡指定的构建验证命令，默认 `.\gradlew.bat testDebugUnitTest assembleDebug`，记录真实退出状态；任务卡要求真机验证时执行 `tools\device_test.ps1`——等待用户连机、安装启动、UI 断言、截图与日志取证，退出码 0 方为通过；再按 `review-checklist.md` 逐条自检），但不创建提交。**构建与真机验证完成前不得声称完成。**
3. 主 Agent 根据开发报告在任务分支创建仅本地的 handoff 提交，记录 SHA；创建开发 worktree 前确认任务卡与 Agent 配置已在可见的 Git 基线内，创建 handoff 提交时只暂存任务允许路径内的本任务改动，且包含任务卡的最终快照；无法区分既有用户改动时停止。该提交不推送、不合并。
4. `app_reviewer` 在另一份干净 worktree 中依据任务卡的固定需求快照验证该 SHA；HEAD 必须匹配 SHA，且不能有非忽略的未跟踪文件。评审先核对 git 状态，运行 `python tools/app_check.py .` 作为确定性底线（ERROR 直接判不合规，WARN 逐条人工确认），再亲自执行构建与单测命令复核，任务卡要求真机验证时亲自执行 `tools\device_test.ps1` 并核对 `result.json`、截图与 logcat，最后逐条人工评审。`PASS` 时汇总报告并等待人工审核；`FAIL` 时由主 Agent 将证据回传开发 Agent 修复，再创建新快照并复评。
5. 初始评审后最多允许两轮修复复评。`BLOCKED` 立即停止且不计入轮次；第三次评审仍为 `FAIL` 时停止为 `FAIL`，由主 Agent 请求下一步决定。
6. 闭环收尾时按 `bjm-app-harvest` 回顾本轮可复用能力：有候选时主动询问用户是否沉淀回 app-agent 规范包，无候选时不打扰用户。

handoff 提交只解决独立验证和可追溯交接，不等同于用户审核后的正式提交。人工审核通过后，主 Agent 必须提示用户把 handoff 分支合并回主追踪线（交付物以此进入正常版本追踪），并清理本任务的 worktree 与已合并分支；经用户同意后执行。`.agent-artifacts/` 是 developer/reviewer 的隔离 worktree，不是产出目录，保持 gitignore；交付物只以合并回主线的提交为准，不得以 worktree 工作区内容充当交付。

## 任务类型

不要求每个改动走同一种深度。先由规划 Agent 分类，再按下面的最低标准处理；任务卡必须写清类型与判定依据。

| 类型 | 处理 | 必需的评审证据 |
| --- | --- | --- |
| `feature`（新功能/新页面） | 按计划实现页面、数据与状态；依赖与配置变更显式列出 | 验收条件逐条核对、构建验证通过、范围最小性反向检查 |
| `bugfix`（既有行为与预期不符） | 定位根因，最小修复，补回归单测 | 复现路径与根因、修复前后行为对照、单测通过 |
| `refactor`（不改变可观察行为的结构调整） | 只调结构；行为不变需论证 | 回归证据：单测与构建全部通过，无行为差异 |

判断口诀：**用户能观察到的行为变化就是 feature/bugfix，观察不到的结构调整才是 refactor。**

## 宿主与角色注册

三个角色（`app_planner`、`app_developer`、`app_reviewer`）的完整规范保存在 `docs/agent-framework/roles/`，是唯一事实来源；宿主只有薄注册文件，并在启动时要求完整读取角色规范：

- VS Code Copilot：`.github/agents/*.agent.md`（`tools` 限制：planner 只读，developer 可编辑执行，reviewer 只读+执行用于构建复核）。

修改角色行为只改 `roles/` 下的文件；角色注册与 roles 文件必须随任务卡一起进入 Git 基线，隔离 worktree 才能读到。

## 使用方式

1. 需求方只需提供简要的概要需求（一句话、一段话或几个要点）。按 `bjm-app-intake` 流程：Agent 只读分析后自动起草任务卡并存为 `docs/agent-framework/tasks/APP-xxx-短名称.md`（编号自动取现有最大值 +1），向需求方展示全文与待确认项清单，确认后状态改为「已确认」并冻结为本轮需求快照。熟手也可从 `task-template.md` 手工复制填卡，两条路径地位相同。
2. 在 VS Code Copilot 的项目对话中提供任务卡路径，并要求按 `app_planner`、`app_developer`、`app_reviewer` 的顺序执行。
3. 规划阶段若存在需求、数据来源、权限或工程配置不明确的内容，先由主 Agent 向需求方确认；不得猜测实现。
4. 开发 Agent 只在独立 worktree 修改文件。主 Agent 创建仅本地的 handoff 提交后，评审 Agent 在另一份干净 worktree 中验证该提交：先核对 git 状态、跑 `python tools/app_check.py .` 作确定性底线，再亲自执行构建与单测命令，最后按 `review-checklist.md` 逐条人工评审。
5. 评审 `FAIL` 默认回传开发 Agent 修复并复评，最多两轮；评审 `BLOCKED` 或达到上限则停止。人工审核实现与评审证据后，才允许合并。
6. 品牌与组件能力：定义或变更品牌 VI 用 `bjm-app-brand`（重写 `docs/brand/brand-vi.md` 与 brand-tokens），把品牌与组件库实例化到具体 app 用 `bjm-app-ui`（复制 `docs/android/design-system/` 模板并迁移界面）；两者独立于任务闭环，可在任意时刻发起。
7. Agent 不自动发版、不上架、不推送远端、不访问生产环境。

## 经验库与沉淀

任务收尾的可复用能力不是口头约定，而是落盘资产：

- 目标项目：`docs/agent-framework/learnings/LRN-xxxx-*.md`，状态 `proposed`，由 `tools\harvest.ps1 -Action new` 创建。
- 规范包仓库：同目录，状态 `accepted` → 修改规范后 `merged`（或 `rejected` 留理由）。
- 回传：`tools\harvest.ps1 -Action import -From "<项目>\docs\agent-framework\learnings"`，按内容指纹去重、ID 冲突自动重编号。
- 索引：`INDEX.md` 由 `-Action reindex` 生成，`-Action validate` 校验字段与索引一致性。

条目格式与状态定义见 `learnings/README.md`。

## 当前阶段的限制

- 确定性检查由 `tools/app_check.py` 承担（Python 3.8+，仅标准库，随安装脚本部署到目标项目）：Wrapper/AGP/Gradle/Kotlin/Compose 基线、SDK/JDK 基线、namespace 与 applicationId、版本声明、密钥与签名卫生。脚本未覆盖的语义评审（架构合理性、需求覆盖度、UI 正确性）仍为人工。改动校验器前后必须运行回归测试：`python -m unittest discover -s tools/tests`。
- 真机测试由 `tools/device_test.ps1` 承担：等待设备 → 构建 → 安装 → 启动 → 冒烟 → `connectedDebugAndroidTest`（有 androidTest 时）→ 截图与 logcat 取证；设备可用性依赖人工连接（脚本会提示并等待），无设备时返回 `BLOCKED`。截图仅作人工核对，不做像素级比对。
- 构建与真机验证依赖本机 Android SDK（`C:\Android\Sdk`）、JDK 17（`C:\Android\jdk17\jdk17`）与一台 USB 调试真机；环境不可用时评审返回 `BLOCKED`，不以静态检查替代。
- 尚未建立 CI 集成与多机型/性能/弱网测试矩阵。

## 推荐的对话提示

只有概要需求（尚无任务卡）时，从需求受理开始：

```text
使用 $bjm-app-intake 受理以下需求：<一句话或一段话描述需求>。
由 app_planner 只读分析并起草任务卡（含任务类型判定、实现要点推断、Given/When/Then 验收条件、显式不做清单、是否需要真机验证），
自动编号写入 docs/agent-framework/tasks/APP-xxx-短名称.md；
把任务卡全文与待确认项清单交我确认，我确认后再冻结并进入 $bjm-app-delivery 闭环。
未经我确认不得开始开发。
```

任务卡已确认时，直接进入闭环交付：

```text
使用 $bjm-app-delivery 按 docs/agent-framework/tasks/APP-xxx-短名称.md 执行。
先由 app_planner 只读分析并报告阻塞项，产出含显式不做清单的实施计划；确认后让 app_developer 在独立 worktree 实现
（实现以最小充分为第一优先级；自检先运行 python tools/app_check.py . 修正全部 ERROR，
再执行 .\gradlew.bat testDebugUnitTest assembleDebug 记录真实退出状态；
任务卡要求真机验证时执行 powershell -ExecutionPolicy Bypass -File tools\device_test.ps1 -ProjectDir .，
按提示连接手机并授权 USB 调试，等待脚本完成安装、启动、UI 断言与取证（退出码 0 方为通过）；
然后按 review-checklist.md 逐条核对；构建与真机验证完成前不得声称完成）；
由主 Agent 创建仅本地 handoff 提交后让 app_reviewer 在干净 worktree 复评
（先核对 git 状态，跑 tools/app_check.py 作确定性底线，亲自执行构建与单测命令复核，
任务卡要求真机验证时再亲自跑一次 tools\device_test.ps1 并核对截图与日志，最后逐条人工评审）。
评审 FAIL 时在原任务范围内修复并复评，最多两轮；BLOCKED 立即停止。
禁止发版、上架、访问生产环境或使用生产凭据。
```

模板说明：

- `bjm-app-intake` Skill：需求受理——需求方只提概要需求，Agent 自动起草任务卡并经确认后冻结。
- `bjm-app-harvest` Skill：能力沉淀——闭环收尾回顾可复用能力，写入经验库条目并回传规范包。
- `learnings/README.md`：经验库格式、状态流转与回传流程；`learnings/INDEX.md` 为自动生成的索引。
- `bjm-app-format` Skill：工程规范唯一权威（铁律），规则原文在 `docs/android/`。
- `bjm-app-build` Skill：Windows 构建与验证操作手册。
- `bjm-app-device-test` Skill：真机自动化测试操作规范（等待连机、安装启动、UI 断言、截图与日志取证）。
- `task-template.md`：App 任务卡，冻结需求依据、实现要点与评审边界（由 `bjm-app-intake` 起草或熟手手工填写）。
- `review-report-template.md`：独立评审的固定报告格式（含检查清单逐条结论表）。
- `review-checklist.md`：评审检查清单（developer 自检 / reviewer 评审 / 报告模板共用，含范围最小性）。
- `../android/gradle-baseline.md`：版本基线与 AGP/Gradle/Kotlin/Compose 兼容矩阵。
- `../android/app-coding-conventions.md`：编码约定（纯 Kotlin、包结构、单测、混淆取舍）。
- `../android/signing-and-release.md`：签名三种模式与密钥安全红线。
- `../android/device-test-automation.md`：真机测试分层、设备约定、证据要求与失败判据。
- `../../tools/app_check.py`：工程基线确定性校验器（ERROR 退出码 1，WARN 需人工确认；阈值可用 CLI 参数覆盖）。
- `../../tools/device_test.ps1`：真机测试执行脚本（退出码 0/1/2 对应 PASS/FAIL/BLOCKED，证据写入 `.agent-artifacts/device-test`）。
- `../../tools/harvest.ps1`：经验库工具（new / list / import / promote / reject / validate / reindex）。
- `../../tools/tests/`：校验器回归测试（`python -m unittest discover -s tools/tests`，改动校验器前后必须运行）。
