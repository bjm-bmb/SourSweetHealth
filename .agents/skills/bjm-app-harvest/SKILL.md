---
name: bjm-app-harvest
description: "App 构建任务收尾时回顾本轮积累的可复用能力，主动询问用户是否沉淀为 app-agent 规范包的能力（工程规范、构建手册、校验器增强等），并通过经验库条目（LRN-xxxx）落盘、回传、落地与追踪状态。用于闭环交付结束或任务中产生可复用做法时；不用于项目特定业务代码的记录。"
---

# App 能力沉淀

app-agent 规范包是单一事实来源，各项目通过 `install.ps1` 同步。执行 App 构建任务的过程中一旦积累了可复用的好做法，主 Agent 必须主动询问用户是否沉淀回规范包，而不是让它散落在单个项目里。

## 触发时机

- `bjm-app-delivery` 闭环收尾时（`PASS` / `FAIL` / `BLOCKED` 都算）：最终结论报告之后，先做能力回顾再结束。
- 任务执行中用户总结出可复用做法、或主 Agent 意识到某做法值得推广时，可随时进入本流程，不必等到收尾。

## 候选识别

回顾本轮任务的实际做法与产出，列出候选清单。典型候选与沉淀位置：

| 候选类型 | 例子 | 沉淀位置 |
| --- | --- | --- |
| 工程规范新规则 | 基线版本调整、Compose 配置新约束、包结构约定 | `bjm-app-format` Skill + `docs/android/*` + `review-checklist.md`；可机械判定的同步 `tools/app_check.py` 与回归测试 |
| 流程改进 | 受理/评审/修复环节的更好做法 | `bjm-app-intake` / `bjm-app-delivery` Skill、`docs/agent-framework/README.md`、`roles/*.md` |
| 校验器增强 | 新的确定性检查规则、AGP/Gradle 兼容矩阵更新 | `tools/app_check.py` + `tools/tests/`（必须补回归测试） |
| 构建手册增补 | 新的常见错误处置、镜像与代理经验 | `bjm-app-build` Skill + `docs/android/gradle-baseline.md` |
| 宿主注册改进 | 工具限制、注册方式优化 | `.github/agents/*.agent.md` 等薄注册文件 |

以下**不属于**沉淀候选：项目特定业务代码与规则、一次性决策与临时变通、任何凭据或环境私有信息。

没有候选时不要打扰用户，直接结束；有候选时必须主动询问，不得默默跳过，也不得未经同意直接改规范包。

## 沉淀流程

1. **列候选**：向用户展示候选清单，每项写明：能力描述、本轮证据（哪个任务/哪份产出验证过）、建议沉淀位置。
2. **用户选择**：用户可全部沉淀、挑选部分或放弃；放弃不追问。
3. **项目内落盘**：对用户选定的项，在目标项目执行

   ```powershell
   powershell -ExecutionPolicy Bypass -File tools\harvest.ps1 -Action new -Title "<能力名>" -Category "<类别>" -SourceTask "<APP-xxx>"
   ```

   生成 `docs/agent-framework/learnings/LRN-xxxx-<slug>.md`（状态 `proposed`），并补全「观察 / 证据 / 建议沉淀」——去项目化，不写业务名词、包名与私有路径。条目格式与状态定义见 `docs/agent-framework/learnings/README.md`。
4. **回传规范包**：在 app-agent 仓库执行

   ```powershell
   powershell -ExecutionPolicy Bypass -File tools\harvest.ps1 -Action import -From "<项目>\docs\agent-framework\learnings"
   ```

   只导入 `proposed` 条目；按内容指纹去重、ID 冲突自动重编号，导入后状态为 `accepted`。
5. **落地到规范**：按条目「建议沉淀」修改对应文件。工程规范变更必须同步 `bjm-app-format`、`docs/android/*`、`review-checklist.md`；可机械判定的同步 `tools/app_check.py` 与 `tools/tests/` 回归测试；`install.ps1` 技能清单、README 与 `VERSION` 按实际改动同步。
6. **标记完成**：

   ```powershell
   powershell -ExecutionPolicy Bypass -File tools\harvest.ps1 -Action promote -Id LRN-0001 -Targets "bjm-app-format;docs/android/xxx.md"
   ```

   不采纳时用 `-Action reject -Id LRN-0001 -Reason "<理由>"` 留痕，避免同一提议反复出现。
7. **校验与分发**：`-Action validate` 必须通过（字段、状态、ID 唯一性、INDEX 一致）；随后提示用户对各使用中的项目重跑 `install.ps1` 同步。规范包仓库的提交与推送按用户指示执行。

## 边界

- 只改 app-agent 规范包；目标项目里已同步的副本由下次 `install.ps1` 覆盖，不在目标项目里单独修补规范文件。
- 沉淀的是「能力」，不是任务产出：任务卡、实现代码、评审报告属于项目自身，不进规范包。
- 项目内生成的 `proposed` 条目属于该项目，回传后由规范包统一管理；`INDEX.md` 由 `harvest.ps1` 生成，禁止手工编辑。
- 校验器改动必须先跑 `python -m unittest discover -s tools/tests` 回归测试再标记 `merged`。
