---
id: LRN-0009
title: Compose 聊天列表首屏定位用 reverseLayout 而非渲染后滚动
category: 工程规范
status: accepted
source_project: 03_vscode
source_task: APP-002
created: 2026-10-17
fingerprint: 9f707f04467c0073
imported_from: 03_vscode
imported_on: 2026-10-17
distributed_from: app-agent
harness_id: LRN-0009
---

## 观察

聊天/时间线界面进入时需要停在最新一条。常见写法是先渲染列表顶部，再用 `LaunchedEffect` + `scrollToItem(lastIndex, Int.MAX_VALUE)` 滚到底。该写法有两个无法靠调参消除的缺陷：

1. 首帧已经渲染了顶部内容，滚动动作在用户眼里就是「从第一条刷到最新」；
2. 定位时机早于 item 测量完成时，巨大 offset 会被过早 clamp，列表停在半路（最后一条被底部输入区裁掉）。

为隐藏滚动过程而加 alpha 遮罩或用 `canScrollForward` 探测「是否到底」，都只能掩盖症状：前者把滚动变成白屏等待，后者在列表尚未测量时立即解除遮罩，滚动过程依旧可见。

## 证据

- APP-002：同一问题先后用「alpha 遮罩 + 一次性 scrollToItem」「双次/三帧重试」「`canScrollForward` 探测」三种补丁，均被真机连拍证伪（300ms 间隔截图显示先顶部后底部，或最后一条被裁）。
- 改为 `LazyColumn(reverseLayout = true)` 后，冷启动 600ms 首帧即位于底部，最后一条完整可见，无滚动过程。

## 建议沉淀

在 `bjm-app-format` 与 `docs/android/app-coding-conventions.md` 增加 Compose 列表约定：

1. 聊天、日志、时间线等「新内容追加在底部」的列表，一律使用 `reverseLayout = true`，让 index 0 对应视觉底部；首帧天然在最新位置，不需要任何滚动或遮罩。
2. 配套要求：items 以倒序映射渲染（`items[lastIndex - index]`）；`key` 使用原始正向索引保证新增项时既有 key 稳定；自动跟随改为 `scrollToItem(0)`；输入框聚焦跟随同理；自绘滚动条比例需按布局方向反转。
3. 明确禁止用「渲染后 `scrollToItem(Int.MAX_VALUE)` + 遮罩/白屏」实现首屏定位。
4. 新增列表页时可加入 UI 断言：首次组合后 `firstVisibleItemIndex == 0` 且无可见滚动。


