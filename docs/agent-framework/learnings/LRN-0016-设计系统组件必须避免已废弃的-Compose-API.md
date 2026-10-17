---
id: LRN-0016
title: 设计系统组件必须避免已废弃的 Compose API
category: 工程规范
status: merged
source_project: SourSweetHealth
source_task: ADHOC-STANDARDIZE
created: 2026-10-17
fingerprint: e270c1ee4f19dfb9
imported_from: SourSweetHealth
imported_on: 2026-10-17
targets: docs/android/design-system/AppStat.kt.template;docs/android/design-system/README.md
merged_on: 2026-10-17
distributed_from: app-agent
harness_id: LRN-0016
---

## 观察

设计系统组件模板使用了 Material3 中已废弃的 `LinearProgressIndicator(Float, ...)` 重载。模板是单一事实来源，缺陷会随实例化复制到所有项目，每个项目在编译期都产生 deprecation 警告，长期会掩盖真实警告。

## 证据

某项目规范化改造的编译输出：`AppStat.kt:57:13 'LinearProgressIndicator(Float, ...)' is deprecated. Use the overload that takes progress as a lambda`；功能正常，但模板侧缺陷已扩散。

## 建议沉淀

把 `docs/android/design-system/AppStat.kt.template` 的进度条改为 `progress = { ... }` 的 Lambda 重载；在 `docs/android/design-system/README.md` 增加「模板必须零编译警告」的约定，避免同类问题再次进入模板。