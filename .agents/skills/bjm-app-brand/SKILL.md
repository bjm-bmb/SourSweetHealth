---
name: bjm-app-brand
description: "定义与变更品牌 VI：收集品牌输入（品牌名/气质/logo 描述或图片），按 docs/brand/brand-vi.md 模板推导并重写品牌 VI 手册（主色必须为低饱和浊色），落地为 brand-tokens，再经 bjm-app-ui 同步应用到各 app。用于定义新品牌或变更品牌；不用于单页面样式修改。"
---

# 品牌 VI 定义与应用

品牌 VI 以代码方式沉淀：`docs/brand/brand-vi.md` 是手册的唯一权威，`docs/brand/brand-tokens.kt.template` 是手册的 Kotlin 落地形态。本技能负责「定义品牌」这一件事；把品牌铺到具体 app 的界面由 `bjm-app-ui` 完成。

## 适用与边界

- 仅在定义新品牌或变更既有品牌（改名、换主色、调整气质）时使用；单页面样式、单个组件的修改不走本技能。
- 本技能只修改 `docs/brand/` 下的文件；不直接改任何 app 的代码，应用同步交给 `bjm-app-ui`。
- `brand-vi.md` 的章节结构（六节）不增不减，只重写内容；默认实例（单色黑白灰方案）是格式示范，接入真实品牌时整体替换。

## 品牌输入收集

向品牌方收集三类输入，缺项先问不猜：

1. **品牌名与气质关键词**（3 个以内）；气质决定色板与辅助图形的取舍。
2. **logo**：描述或图片；提炼其几何特征作为辅助图形的延展来源。
3. **主色意向**（如有）：用户给出色相方向，Agent 负责落到具体色值。

## VI 推导规则

- **默认单色（黑白灰）方案**：主色即近黑（`#1A1A1A` 级），视觉靠灰阶、字重与留白建立；品牌有明确彩色资产时主色可为低饱和浊色（浊蓝/浊绿/暖陶一类，如 `#5B7C99`、`#7A8B6F`、`#B08968`）；用户给了高饱和色时，主动降饱和并给出对比说明，经确认后采用。
- 中性色阶承担 90% 界面：近黑正文（`#1A1A1A` 级）、近白背景（`#FAFAF9` 级）；语义浊色仅成功/警告/错误三个。
- light/dark 双套**同等设计**，逐色给出两套 hex，不是简单反色。
- 字阶与 `docs/android/ui-design-guide.md` 的 6 级字阶完全对齐，品牌层面只指定各级使用场景，不修改字号。
- 辅助图形从 logo 延展，以代码交付（SVG 路径或 Compose `Canvas`/`ImageVector` 描述），只作点缀不喧宾夺主。

## 落地流程

1. 按推导结果重写 `docs/brand/brand-vi.md` 全文（六节齐全，含色板 hex 表）。
2. 同步重写 `docs/brand/brand-tokens.kt.template`：`BrandColors`（含 light/dark）/ `BrandTypeScale` / `BrandSpacing` / `BrandCorners` 与手册逐值一致。
3. 将品牌手册交品牌方确认；确认后提示用户对各使用中的 app 走 `bjm-app-ui` 同步（新 app 接入或既有 app 换肤）。
4. 收尾按 `bjm-app-harvest` 回顾是否有可复用的 VI 推导经验沉淀回规范包。

## 最终交付物

重写后的 `docs/brand/brand-vi.md` 与 `brand-tokens.kt.template`、主色降饱和等关键决策的说明与依据、品牌方确认记录、待同步 app 清单。
