---
id: LRN-0012
title: UI 改动必须真机逐项点击并截图核对
category: 流程改进
status: accepted
source_project: 03_vscode
source_task: APP-002
created: 2026-10-17
fingerprint: 1d48140ebbd17767
imported_from: 03_vscode
imported_on: 2026-10-17
distributed_from: app-agent
harness_id: LRN-0012
---

## 观察

任务卡与评审清单都要求真机验证，但既有真机脚本 `device_test.ps1` 只做「安装 → 启动 → 进程存活 → 无崩溃」的冒烟检查，不验证界面是否正确。当改动涉及主题、形状、列表布局、设置交互时，冒烟通过 + 单元测试全绿仍然完全无法发现：

- 对话框形状错误、文字被裁切；
- 配色与既有风格不一致；
- 设置项点击后没有真正保存；
- 列表首屏定位错误（内容从顶部开始滚动、最后一条被底部栏裁掉）。

这些都需要「按用户路径实际点进去，并看截图」才能发现；本轮全部是由需求方在真机上手点后发现的。

## 证据

- APP-002：`device_test.ps1` 退出码 0、`app_check` 0 ERROR/0 WARN、18 个单测全绿，但真机上对话框呈胶囊形并裁字、设置项点击不保存、首页首屏滚动异常。
- 修复后改用 `adb shell input tap` + `screencap` + `uiautomator dump` + 300ms 连拍逐帧核对，并读取应用 DataStore 文件确认持久化，才拿到可信证据。

## 建议沉淀

在 `bjm-app-device-test`（SKILL 与 `docs/android/device-test-automation.md`）与 `review-checklist.md` 中增加「交互级真机复核」：

1. 凡改动涉及主题/配色/Shapes/Typography/列表布局/设置项/对话框/导航，任务卡必须列明**要点击的页面路径与关键点击点**，而非只写「仅冒烟」。
2. 执行方式固定为：`input tap` 进入页面 → `uiautomator dump` 取真实坐标 → 点击目标控件 → `screencap` 截图 → 对持久化类操作读取应用私有 DataStore/SharedPreferences 文件确认落盘。
3. 首屏类改动需做 300ms 间隔连拍，逐帧确认「第一次可见帧」就是目标状态，而不是先出现中间状态再跳变。
4. 评审 Agent 必须亲自执行上述点击与截图，不接受「构建通过 + 冒烟通过」作为 UI 改动的验收证据。
5. 可在 `tools/device_test.ps1` 增加可选的 `-TapPath "设置;自动涌现"` 参数与截图归档，降低逐次手写的成本。


