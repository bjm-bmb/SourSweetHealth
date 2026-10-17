# 经验索引

> 本文件由 `tools/harvest.ps1 -Action reindex` 自动生成，请勿手工编辑。

| ID | 标题 | 类别 | 状态 | 来源项目 | 创建日期 | 落地位置 |
| --- | --- | --- | --- | --- | --- | --- |
| LRN-0001 | 设计系统模板与实例逐字一致性检查 | 校验器增强 | merged | vibe-life | 2026-10-17 | tools/app_check.py;tools/tests/test_app_check.py;.agents/skills/bjm-app-ui/SKILL.md;docs/agent-framework/review-checklist.md |
| LRN-0002 | Compose 系统栏图标随明暗主题适配 | 工程规范 | merged | vibe-life | 2026-10-17 | docs/android/ui-design-guide.md;docs/agent-framework/review-checklist.md |
| LRN-0003 | 真机离线验证不得改动日常设备网络配置 | 流程改进 | merged | vibe-life | 2026-10-17 | .agents/skills/bjm-app-device-test/SKILL.md;docs/agent-framework/roles/app-reviewer.md |
| LRN-0004 | 规范包安装不得覆盖项目已定制的品牌与设计系统 | 流程改进 | merged | vibe-life | 2026-10-17 | install/install.ps1;README.md |
| LRN-0005 | git worktree 下校验器误报 local.properties 未忽略 | 校验器增强 | merged | mood-diary | 2026-10-17 | tools/app_check.py;tools/tests/test_app_check.py |
| LRN-0006 | 真机 UI 取证前隔离已保存的 API Key 与 SharedPreferences | 构建手册 | merged | mood-diary | 2026-10-17 | .agents/skills/bjm-app-device-test/SKILL.md;docs/android/device-test-automation.md |
| LRN-0007 | 任务卡冻结前验证测试依赖真实存在 | 流程改进 | merged | mood-diary | 2026-10-17 | .agents/skills/bjm-app-intake/SKILL.md;.agents/skills/bjm-app-delivery/SKILL.md;docs/agent-framework/task-template.md;docs/agent-framework/review-checklist.md |
| LRN-0008 | 不得把来源不明的未提交改动当作 Git 基线提交 | 流程改进 | accepted | 03_vscode | 2026-10-17 | - |
| LRN-0009 | Compose 聊天列表首屏定位用 reverseLayout 而非渲染后滚动 | 工程规范 | accepted | 03_vscode | 2026-10-17 | - |
| LRN-0010 | 设置写入不得依赖 rememberCoroutineScope | 工程规范 | accepted | 03_vscode | 2026-10-17 | - |
| LRN-0011 | 自定义 Material3 Shapes 时 extraLarge 不能设为胶囊 | 工程规范 | accepted | 03_vscode | 2026-10-17 | - |
| LRN-0012 | UI 改动必须真机逐项点击并截图核对 | 流程改进 | accepted | 03_vscode | 2026-10-17 | - |
| LRN-0013 | 任务范围依从性必须机检且禁止扩范围修复 | 流程改进 | accepted | 03_vscode | 2026-10-17 | - |
| LRN-0014 | 界面验收必须有交互级真机断言而非冒烟 | 校验器增强 | accepted | 03_vscode | 2026-10-17 | - |
| LRN-0015 | 设计系统输入框组件需覆盖只读、键盘与错误态 | 工程规范 | merged | SourSweetHealth | 2026-10-17 | docs/android/design-system/AppTextField.kt.template;docs/android/design-system/README.md |
| LRN-0016 | 设计系统组件必须避免已废弃的 Compose API | 工程规范 | merged | SourSweetHealth | 2026-10-17 | docs/android/design-system/AppStat.kt.template;docs/android/design-system/README.md |
