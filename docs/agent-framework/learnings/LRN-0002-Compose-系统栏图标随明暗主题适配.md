---
id: LRN-0002
title: Compose 系统栏图标随明暗主题适配
category: 工程规范
status: merged
source_project: vibe-life
source_task: APP-001
created: 2026-10-17
fingerprint: 1f23b61049c8c381
imported_from: vibe-life
imported_on: 2026-10-17
targets: docs/android/ui-design-guide.md;docs/agent-framework/review-checklist.md
merged_on: 2026-10-17
distributed_from: app-agent
harness_id: LRN-0002
---

## 观察

Compose 应用如果只在主题 XML 中设置 `windowLightStatusBar`，浅色模式下系统导航栏仍可能绘制白色图标；当导航栏背景是浅色时，三键导航/手势条几乎不可见。应用侧需要随明暗主题主动设置系统栏图标外观：`WindowCompat.getInsetsController(window, window.decorView)` 的 `isAppearanceLightStatusBars` 与 `isAppearanceLightNavigationBars` 取反于深色判定，并在 `SideEffect` 中同步，才能随系统主题切换实时生效。

## 证据

APP-001 首轮评审在真机像素采样：浅色导航栏背景 `#FAFAF8`、图标约 `#F5F5F3`，对比度仅 1.04–1.51:1，验收失败；修复后浅色图标约 `#787878`，对比度 4.22:1，深色 18.22:1，第二轮独立复评 PASS。浅/深色截图与 uiautomator 证据均已留存。

## 建议沉淀

在 `docs/android/ui-design-guide.md` 增加「系统栏图标随明暗主题适配」规则（状态栏与导航栏都要设置，禁止只设其一）；`bjm-app-format` 或 `app-coding-conventions` 补充 Compose 实现约定与 `WindowCompat` 示例；`review-checklist.md` 增加「浅/深色系统栏图标清晰可见」人工检查项；`bjm-app-device-test` 的截图取证明确包含底部系统导航栏区域，必要时放大取证与像素对比。
