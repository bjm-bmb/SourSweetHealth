# 酸甜知己 · SourSweetHealth

![版本](https://img.shields.io/badge/版本-1.0.0-blue) ![协议](https://img.shields.io/badge/协议-Apache%202.0-green) ![平台](https://img.shields.io/badge/平台-Android-brightgreen)

**「酸」是尿酸，「甜」是血糖，「知己」是懂你健康的贴心伴侣。**

一款专为关注血糖和尿酸健康的朋友设计的 Android 应用，界面简洁、操作方便，让记录数据和了解健康趋势变得轻松愉快。

---

## 功能特色

- 📊 **血糖与尿酸随手记** — 支持多时段（空腹、餐后1小时、餐后2小时等）记录
- 📈 **趋势图表可视化** — 按 7天 / 30天 / 3个月 / 自定义 / 全部历史查看，支持手势滑动、点击查看详情、长按编辑
- 🤖 **AI 健康分析** — 支持任何兼容 OpenAI 接口的大模型（SiliconFlow、DeepSeek、通义千问、OpenAI 等），像朋友聊天一样分析健康趋势
- 💡 **偏高快速建议** — 数值偏高时一键获取 AI 医生建议
- 👨‍👩‍👧‍👦 **多用户管理** — 支持家庭成员独立档案管理

## 截图

> *(可在此处添加截图)*

## 技术栈

| 组件 | 版本 |
|------|------|
| Android SDK | 34 (minSdk 26) |
| Kotlin | 1.9.22 |
| Jetpack Compose BOM | 2024.02.02 |
| Room | 2.6.1 |
| DataStore | 1.0.0 |
| OkHttp | 4.12.0 |
| Navigation Compose | 2.7.6 |
| AGP | 8.2.0 |

## 快速开始

### 环境要求

- Android Studio Hedgehog 或更高版本
- JDK 17
- Android SDK 34

### 构建步骤

```bash
git clone https://github.com/your-username/SourSweetHealth.git
cd SourSweetHealth
./gradlew assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/SourSweetHealth.apk`

### 配置 AI 分析功能（可选）

1. 在应用主界面左上角点击设置图标
2. 进入「大模型设置」页面
3. 填入任意兼容 OpenAI 接口的 API 地址、Key 和模型名称

推荐免费使用 [SiliconFlow](https://cloud.siliconflow.cn) 提供的大模型服务。

## 项目结构

```
app/src/main/java/com/soursweethealth/
├── data/
│   ├── Entities.kt          # Room 数据实体（User、HealthRecord）
│   ├── Daos.kt              # 数据库访问接口
│   ├── AppDatabase.kt       # Room 数据库
│   ├── HealthUtils.kt       # 健康阈值、等级判断工具
│   ├── LlmService.kt        # OpenAI 兼容 SSE 流式调用
│   └── SettingsManager.kt   # DataStore 配置管理
├── ui/
│   ├── MainViewModel.kt     # 业务逻辑与状态管理
│   ├── theme/               # Material 3 主题配色
│   └── screens/
│       ├── HomeScreen.kt        # 主页（最新记录 + 趋势）
│       ├── AddRecordScreen.kt   # 添加记录
│       ├── TrendSection.kt      # 趋势图表组件
│       ├── HealthAnalysisScreen.kt  # AI 健康分析
│       └── LlmSettingsScreen.kt     # 大模型配置
├── SourSweetApp.kt          # Application 类（全局异常捕获）
└── CrashReportActivity.kt   # 崩溃日志展示
```

## 参与贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/your-feature`)
3. 提交更改 (`git commit -m 'Add some feature'`)
4. 推送分支 (`git push origin feature/your-feature`)
5. 发起 Pull Request

## 开源协议

本项目基于 [Apache License 2.0](LICENSE) 开源协议。

## 作者

**bjm** — 设计、开发与维护

```
Copyright 2026 SourSweetHealth Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
