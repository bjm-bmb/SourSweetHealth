# 签名与发布

签名与密钥管理的安全边界由本文件定义；`tools/app_check.py` 的 `secrets-plaintext`、`release-signing`、`apk-naming`、`local-properties-ignore` 规则按本文件做机械提示（WARN，评审逐条人工确认）。

## 三种签名模式对比

| 模式 | 实例仓库 | 评价 |
| --- | --- | --- |
| ① 独立 keystore + 凭据 | mood-diary | 可用于正式发布；但把口令明文写进 gradle.properties 是**反面案例**（密钥入库） |
| ② 复用 debug 签名 | IdeaNote | 内测/自测分发方便；debug 密钥公开，不适合上架 |
| ③ 无签名配置仅 debug 分发 | SonarEar | 仅本机调试；release 包未签名无法安装分发 |

## 推荐做法：keystore.properties 模式（vibe-life 范式）

签名凭据放仓库根目录 gitignored 的 `keystore.properties`：

```properties
storeFile=release.keystore
storePassword=（本机保管，勿入库）
keyAlias=（别名）
keyPassword=（口令）
```

`.gitignore` 必须包含：

```
keystore.properties
*.keystore
local.properties
```

构建脚本读取（Groovy 示例，Kotlin DSL 同理）：

```groovy
def keystoreProps = new Properties()
def keystoreFile = rootProject.file('keystore.properties')
if (keystoreFile.exists()) {
    keystoreProps.load(new FileInputStream(keystoreFile))
}

android {
    signingConfigs {
        release {
            storeFile keystoreProps['storeFile'] ? rootProject.file(keystoreProps['storeFile']) : null
            storePassword keystoreProps['storePassword']
            keyAlias keystoreProps['keyAlias']
            keyPassword keystoreProps['keyPassword']
        }
    }
    buildTypes {
        release {
            // keystore.properties 缺失时回落 debug 签名，保证新机器可构建
            signingConfig keystoreFile.exists() ? signingConfigs.release : signingConfigs.debug
        }
    }
}
```

keystore 文件本身放仓库外或本机安全目录，`storeFile` 用相对路径引用；keystore 与凭据永不下库。

## APK 命名约定

`outputFileName` 统一为 `<app>-<versionName>.apk`：

```groovy
applicationVariants.all { variant ->
    variant.outputs.all {
        outputFileName = "<app>-${variant.versionName}.apk"
    }
}
```

产物在 `app/build/outputs/apk/<buildType>/`。评审报告与交付说明引用 APK 时必须给出完整文件名（含 versionName）。

## 密钥安全红线

- 口令、keystore、API Key 一律不入库：不写进 gradle.properties（mood-diary 明文口令为反面案例）、不写进 local.properties（vibe-life 把 DEEPSEEK_API_KEY 明文放 local.properties 属反面案例——local.properties 虽被 gitignore，仍会在本机明文落盘并被误分享）、不写进代码、注释、任务卡或评审报告。
- API Key 等运行时密钥构建期从环境变量注入，例如 `buildConfigField("String", "DEEPSEEK_API_KEY", "\"${System.getenv('DEEPSEEK_API_KEY') ?: ''}\"")`，缺省为空并在运行时优雅降级。
- `app_check.py` 的 `secrets-plaintext` 规则对 gradle.properties 中的明文键与未忽略的 keystore.properties 给出 WARN，评审必须逐条确认，不得默认采信。
