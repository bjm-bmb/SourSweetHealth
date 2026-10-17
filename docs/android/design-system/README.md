# UI 设计组件库（Compose 模板源码）

本目录是**可编译的 Compose 组件库参考源码**：把 `docs/android/ui-design-guide.md`（审美规范）与 `docs/brand/brand-vi.md`（品牌 VI）落成代码。所有组件只读 token（`MaterialTheme.colorScheme` / `typography` / `shapes` / `Spacing`），**组件内禁止硬编码色值与字号**；无业务逻辑、无第三方依赖，仅 material3 + foundation。

## 组件清单

| 文件 | 组件 | 对应 ui-design-guide |
| --- | --- | --- |
| `Theme.kt.template` | Color/Type/Shape/Dimens + light/dark ColorScheme + `AppTheme` | §二色彩、§三字阶、§四间距、§五形状 |
| `AppButtons.kt.template` | `PrimaryButton`（强调色实心 48dp 高 12dp 圆角）、`SecondaryButton`（描边）、`AppTextButton` | §七 主按钮 |
| `AppCard.kt.template` | `AppCard`（近白底 16dp 圆角零阴影内容卡片） | §七 卡片 |
| `AppListItem.kt.template` | `AppListItem`（≥56dp 高、标题+辅助文字+尾部插槽） | §四 列表项 |
| `AppTopBar.kt.template` | `AppTopBar`（简约顶栏，17sp Medium 标题） | §三 区块标题字级 |
| `AppEmptyState.kt.template` | `AppEmptyState`（一句文案 + 一个主按钮） | §七 空状态 |
| `AppLoading.kt.template` | `AppLoadingBar`（细线进度）+ `AppSkeletonList`（骨架屏） | §六 加载态 |
| `AppTextField.kt.template` | `AppTextField`（12dp 圆角描边输入框） | §五 形状体系 |
| `AppSectionHeader.kt.template` | `AppSectionHeader`（17sp Medium 区块标题 + 可选尾部动作） | §三 字阶 |
| `AppStat.kt.template` | `StatCard`（32sp 大数字 + 标签）、`AppProgressBar`（细线进度 + 百分比） | §三 展示字级、§六 |

## 实例化步骤（由 bjm-app-ui 技能执行）

1. 复制 `Theme.kt.template` 到目标 app 的 `app/src/main/java/<包名>/ui/theme/Theme.kt`，复制各组件模板到 `ui/components/`，**去掉 `.template` 后缀**；
2. 将全部 `{{package}}` 占位替换为目标 app 的根包名；
3. 复制 `docs/brand/brand-tokens.kt.template` 到 `ui/theme/BrandTokens.kt` 并替换包名；把 `Theme.kt` 中的默认色值改为引用 `BrandColors`（品牌色以 `docs/brand/brand-vi.md` 为准，本模板默认值即默认单色实例（主色近黑 `#1A1A1A`））；
4. 界面逐步迁移：新界面一律使用 `App*` 组件，既有界面触及时迁移，不一次性大改；
5. 收口验证：`python tools/app_check.py .` + `.\gradlew.bat testDebugUnitTest assembleDebug`；界面改动按 `bjm-app-device-test` 执行真机验证（`tools\device_test.ps1`）。

## 使用约束

- 组件只暴露文案、回调与插槽，不暴露样式参数（颜色/字号/圆角不可由调用方覆盖）——样式只能改 token，不能改调用点。
- 新增组件先对照 ui-design-guide 的规范条目，加入本目录模板并更新本清单，再在各 app 使用；不在单个 app 里私造组件。
