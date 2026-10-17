---
name: bjm-app-ui
description: "把品牌 VI 与设计系统实例化到目标 app：复制 docs/android/design-system/ 组件模板与 docs/brand/brand-tokens 到 ui/theme|components/ 并替换包名，按 brand-vi.md 调整 token，界面逐步迁移到 App* 组件，构建验证收口。用于新建 app 接入设计系统或既有 app 迁移/换肤；不用于品牌定义本身（品牌变更走 bjm-app-brand）。"
---

# 设计系统实例化（品牌 → App）

把 `docs/brand/brand-vi.md` 定义的品牌与 `docs/android/design-system/` 的组件库落到具体 Android app：token 就位、组件就位、界面逐步迁移。品牌定义本身不在本技能范围内——VI 缺失或需变更时先走 `bjm-app-brand`。

## 适用与边界

- 用于新建 app 首次接入设计系统，或既有 app 迁移/换肤；View/XML 项目不套用 Compose 组件库（保持该项目 UI 路线，仅同步品牌色值到 `res/values/`）。
- 开始前确认 `docs/brand/brand-vi.md` 已是当前品牌的定稿（非默认实例占位）；仍是默认单色占位且品牌已有彩色资产时，先提示用户走 `bjm-app-brand`。
- 界面迁移遵循最小充分与任务卡边界：新界面一律用 `App*` 组件，既有界面触及时迁移，不一次性大改。

## 实例化流程

1. **读源**：完整读取 `docs/brand/brand-vi.md`、`docs/brand/brand-tokens.kt.template`、`docs/android/design-system/README.md` 与全部组件模板。
2. **复制就位**：`Theme.kt.template` → `app/src/main/java/<包名>/ui/theme/Theme.kt`；各组件模板去 `.template` 后缀 → `ui/components/`；`brand-tokens.kt.template` → `ui/theme/BrandTokens.kt`。全部 `{{package}}` 占位替换为目标 app 根包名。
3. **对齐品牌**：`Theme.kt` 的色值改读 `BrandColors`（与 brand-vi.md 逐值一致）；发现 token 与手册不一致时以手册为准并回报。
4. **界面迁移**：新界面只使用 `App*` 组件；组件不满足的场景先回报，不在界面代码里硬编码样式绕过。
5. **构建验证收口**：`python tools/app_check.py .` 修正全部 ERROR，再执行 `.\gradlew.bat testDebugUnitTest assembleDebug` 记录真实退出状态；界面相关改动按 `bjm-app-device-test` 执行真机验证；**构建与真机验证完成前不得声称完成**。

## 使用约束

- 组件只读 token（`MaterialTheme` / `Spacing`），调用方只能传文案、回调与插槽；样式调整只能改 token，不能改调用点。
- 需要新组件时，先在 `docs/android/design-system/` 沉淀模板并更新其 README 清单（经 `bjm-app-harvest` 流程），再在各 app 使用；不在单个 app 私造组件。

## 最终交付物

实例化文件清单（theme/components 路径）、token 与 brand-vi.md 的一致性核对结果、界面迁移范围说明、`app_check.py` 与构建/单测命令及真实退出状态、已知限制。
