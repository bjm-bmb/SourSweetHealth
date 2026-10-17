---
id: LRN-0006
title: 真机 UI 取证前隔离已保存的 API Key 与 SharedPreferences
category: 构建手册
status: merged
source_project: mood-diary
source_task: APP-001
created: 2026-10-17
fingerprint: 23c764c7d840f5ae
imported_from: mood-diary
imported_on: 2026-10-17
targets: .agents/skills/bjm-app-device-test/SKILL.md;docs/android/device-test-automation.md
merged_on: 2026-10-17
distributed_from: app-agent
harness_id: LRN-0006
---

## 观察

真机 UI 取证时，如果测试设备上已保存真实 API Key，应用冷启动会按既有逻辑发起问候请求。仅用 `am force-stop` 或替换 `settings.xml` 并不足够：旧进程可能回写 SharedPreferences，形成竞争窗口，使应用短暂恢复真实 Key 并联网。

## 证据

APP-001 开发取证早期出现过一次旧进程回写，导致应用使用设备上已保存的真实 Key 对本轮 mock 记录发起一次问候请求；Key 未被读取、打印、写入仓库或提交。评审复跑应用时同样触发一次问候请求。最终通过删除 `settings.xml`、确认 `shared_prefs` 为空、使用 dummy key + 本地缓存文本完成截图，验证后恢复原数据并校验 md5。

## 建议沉淀

在 `bjm-app-device-test` Skill 与 `docs/android/device-test-automation.md` 增加“取证前隔离凭据”步骤：优先 `pm clear` 或使用独立测试用户/测试包；无法清数据时，先停止进程、移出 `settings.xml`、确认 `shared_prefs` 为空后再启动；禁止带真实 Key 截图或联网；记录取证后的数据恢复与校验方法。可在 `tools/device_test.ps1` 增加可选的“清除应用数据/凭据隔离”参数或提示。
