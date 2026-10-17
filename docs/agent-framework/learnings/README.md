# 经验库（Learnings）

本目录是 app-agent 的**经验唯一事实来源**：把具体项目中验证过的可复用做法沉淀为条目，经回传、评审、落地后形成可追溯记录。技能、规范、校验器的修改以本目录的条目为依据。

## 为什么需要它

- `bjm-app-harvest` 只规定「收尾要问用户是否沉淀」，没有落盘记录，经验会散落在各项目并丢失。
- 没有状态追踪就无法回答：哪些经验已落地？哪些还在待办？哪条规范来自哪个项目的哪次任务？
- 每次 `install.ps1` 都会覆盖目标项目的规范文件，因此经验必须回到本仓库修改，再分发。

## 目录结构

```text
docs/agent-framework/learnings/
├── README.md          # 本文件：格式、状态、流程
├── _template.md       # 新条目模板（由 tools/harvest.ps1 -Action new 使用）
├── INDEX.md           # 自动生成的索引（tools/harvest.ps1 -Action reindex）
└── LRN-0001-<slug>.md # 经验条目
```

## 分发到目标项目

`install.ps1` 会把规范包内的**全部条目**（README、`_template.md` 与所有 `LRN-*.md`）同步到目标项目，让项目侧 Agent 能直接读到规范背后的经验依据：

- 分发的条目追加两个字段：`distributed_from: app-agent`、`harness_id: <规范包内的编号>`；`id` 与文件名以目标项目实际分配到的编号为准。
- 重装时按 `harness_id` 找到既有副本并刷新内容（规范包里更新了条目，项目侧会同步）。
- 项目自有条目中与规范包条目**内容完全相同**（按内容指纹判定）的，会被接管为受管副本（补上两个标记字段、状态对齐规范包），避免同一经验在目录里出现两份。
- 其余项目自有条目（无 `distributed_from` 标记）永不被覆盖；规范包条目若与项目条目 ID 冲突，自动改用下一个空闲编号，项目条目保持原样。
- 同步后自动执行 `tools\harvest.ps1 -Action reindex`，`INDEX.md` 始终反映目录实际内容（`-Action validate` 可复核）。

## 条目格式

```markdown
---
id: LRN-0001
title: 一句话说明可复用能力
category: 工程规范 | 流程改进 | 校验器增强 | 构建手册 | 宿主注册
status: proposed | accepted | rejected | merged
source_project: mood-diary
source_task: APP-003
created: 2026-10-17
fingerprint: <16 位内容指纹，import 时自动写入>
targets: bjm-app-format; docs/android/app-coding-conventions.md
---

## 观察
本轮实际发生的事实（去项目化描述，不含业务名词与私有路径）。

## 证据
哪个任务、哪份产出、哪次构建/评审验证过；可引用命令与结果摘要。

## 建议沉淀
具体改哪个文件、加什么规则或检查；可机械判定的需说明同步 app_check.py 与回归测试。
```

必填字段：`id`、`title`、`category`、`status`、`source_project`、`created`。`merged` 状态必须有 `targets`。

## 状态流转

| 状态 | 含义 | 产生方式 |
| --- | --- | --- |
| `proposed` | 项目中记录、待回传的候选 | 目标项目里 `harvest.ps1 -Action new` |
| `accepted` | 已回传并导入规范包，待落地 | 规范包仓库里 `harvest.ps1 -Action import` |
| `merged` | 已修改技能/规范/校验器并验证 | `harvest.ps1 -Action promote` |
| `rejected` | 经评估不沉淀（保留理由，避免重复提议） | `harvest.ps1 -Action reject` |

## 闭环流程

1. **项目内记录**（目标项目）：闭环收尾时按 `bjm-app-harvest` 回顾本轮做法，产出候选条目：

   ```powershell
   powershell -ExecutionPolicy Bypass -File tools\harvest.ps1 -Action new -Title "Compose 页面统一加 testTag" -Category "工程规范" -SourceTask "APP-003"
   ```

   条目写入 `<项目>\docs\agent-framework\learnings\LRN-xxxx-*.md`，状态为 `proposed`。

2. **回传导入**（app-agent 仓库）：

   ```powershell
   powershell -ExecutionPolicy Bypass -File tools\harvest.ps1 -Action import -From "C:\path\to\project\docs\agent-framework\learnings"
   ```

   只导入 `proposed` 条目；按内容指纹去重，ID 冲突自动重新编号，导入后状态为 `accepted`。

3. **落地**：按条目「建议沉淀」修改对应技能/规范/校验器；工程规范变更必须同步 `bjm-app-format`、`docs/android/*`、`review-checklist.md`，可机械判定的同步 `tools/app_check.py` 与 `tools/tests/`。

4. **标记完成**：

   ```powershell
   powershell -ExecutionPolicy Bypass -File tools\harvest.ps1 -Action promote -Id LRN-0001 -Targets "bjm-app-format;docs/android/app-coding-conventions.md"
   ```

5. **分发**：推送到 `main` 并递增 `VERSION`；使用中的项目下次打开时会由 `tools\harness_update.ps1`（`.vscode\tasks.json` 的 `runOn: folderOpen` 任务）自动同步，也可手工执行 `install.ps1`。

## 质量要求

- 去项目化：不写业务名词、包名、账号、私有路径；只保留可推广的做法。
- 有证据：写明验证过的任务与命令结果，禁止凭印象立规则。
- 一次一条：一条经验只描述一个可复用能力，便于单独评审与回滚。
- 不沉淀：项目特定业务规则、一次性变通、凭据与环境私有信息。
- 拒绝要留痕：`rejected` 必填理由，避免同一提议反复出现。

## 常用命令

| 命令 | 作用 |
| --- | --- |
| `-Action new -Title <标题> [-Category <类别>] [-SourceTask <任务>] [-ProjectDir <项目>]` | 新建候选条目 |
| `-Action list [-Status proposed\|accepted\|merged\|rejected]` | 列出条目 |
| `-Action import -From <项目 learnings 目录>` | 回传导入候选 |
| `-Action promote -Id <ID> -Targets <文件列表>` | 标记已落地 |
| `-Action reject -Id <ID> -Reason <理由>` | 标记不沉淀 |
| `-Action validate` | 校验字段、ID 唯一性、INDEX 一致性 |
| `-Action reindex` | 重建 `INDEX.md` |
