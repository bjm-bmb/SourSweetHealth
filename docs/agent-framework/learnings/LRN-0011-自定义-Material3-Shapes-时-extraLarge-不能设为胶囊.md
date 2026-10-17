---
id: LRN-0011
title: 自定义 Material3 Shapes 时 extraLarge 不能设为胶囊
category: 工程规范
status: accepted
source_project: 03_vscode
source_task: APP-002
created: 2026-10-17
fingerprint: afc1314c7041c859
imported_from: 03_vscode
imported_on: 2026-10-17
distributed_from: app-agent
harness_id: LRN-0011
---

## 观察

自定义 Material3 `Shapes` 时把 `extraLarge` 定义为「全圆角胶囊」（如 `RoundedCornerShape(percent = 50)`），本意是给悬浮芯片使用。但 M3 的 `AlertDialog`、`Dialog`、`SearchBar` 等组件默认取 `shapes.extraLarge` 作为容器形状，于是这些组件被渲染成大椭圆，标题与正文被圆角裁切，文字溢出容器。

该类缺陷在编译与常规截图检查中都不报错，只有在真机打开对应对话框时才可见。

## 证据

- APP-002：真机打开「自动涌现」对话框，渲染为一个横跨屏幕的大椭圆，标题「自动涌现」左半被裁掉，选项行贴着圆形边缘。
- 该形状定义来自一次未经确认的换肤改动；回退到原有 Shapes 后同一对话框恢复正常圆角矩形（真机截图对比）。

## 建议沉淀

在 `docs/android/ui-design-guide.md` 与 `bjm-app-format` 增加规则：

1. `extraLarge` 只用于「大尺寸卡片/底部面板」等容器，禁止设为百分比圆角或胶囊；胶囊形状单独定义扩展形状（如 `PillShape`）并按需显式传入。
2. 覆盖 `Shapes` 后，必须逐个真机检查使用 M3 默认形状的组件：`AlertDialog`、`Dialog`、`DropdownMenu`、`SearchBar`、`ModalBottomSheet`、`Card`。
3. 设计系统 token 检查清单中增加一条：`extraLarge` 的圆角值必须有绝对上限（例如 ≤ 28dp）。
4. 可机械判定：`tools/app_check.py` 检测 `Shapes(` 定义里 `extraLarge` 使用 `percent = 50` 或 `CircleShape` 时给出 ERROR。


