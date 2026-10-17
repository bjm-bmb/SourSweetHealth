---
id: LRN-0010
title: 设置写入不得依赖 rememberCoroutineScope
category: 工程规范
status: accepted
source_project: 03_vscode
source_task: APP-002
created: 2026-10-17
fingerprint: 2cc8c4932b892974
imported_from: 03_vscode
imported_on: 2026-10-17
distributed_from: app-agent
harness_id: LRN-0010
---

## 观察

设置项（下拉选项、单选列表等）在点击回调里用 `rememberCoroutineScope().launch { prefs.save(...) }` 持久化，且同一回调中还可能触发运行时权限请求（如通知权限）。

运行时权限请求会让 Activity 暂停甚至重建，composition 随之销毁，`rememberCoroutineScope` 的协程被取消，持久化语句从未执行。表现是：对话框正常关闭、界面无报错、但设置值没有变化，重启后仍是旧值。用户只会说「点了没用」。

## 证据

- APP-002：真机复现——点击「每周一早上 6 点」后对话框关闭，副标题仍显示「每天早上 6 点」，读取应用 DataStore 文件确认 `auto_surge_period` 仍为旧值 `daily`。
- 改为 Application 级 `CoroutineScope(SupervisorJob() + Dispatchers.IO)` 后，daily→weekly→daily 双向切换均生效，DataStore 依次读到 `weekly`、`daily`。

## 建议沉淀

在 `bjm-app-format` 与 `docs/android/app-coding-conventions.md` 增加约定：

1. 任何需要跨 Activity 生命周期存活的写入（设置持久化、闹钟重排、通知调度）不得使用 `rememberCoroutineScope`，应使用 Application 级 scope（在 `Application` 中暴露 `CoroutineScope(SupervisorJob() + Dispatchers.IO)`）。
2. 涉及运行时权限的点击回调，顺序必须是：**先发起持久化（Application scope）→ 再请求权限 → 最后关闭 UI**；不得让权限流程成为写入的前置条件。
3. 单元/真机验证要求：所有可切换的设置项都要验证「切换后立即生效 + 重新进入页面保持 + 读取持久化文件确认」三件事，不能只看界面是否关闭。
4. 可机械判定部分：`tools/app_check.py` 可检查 Compose 文件中 `rememberCoroutineScope` 与 `save`/`set` 类持久化调用在同一 lambda 内共现的情况并给出 WARN。


