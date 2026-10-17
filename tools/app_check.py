#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""app_check.py - Android 工程基线确定性校验器（app-agent harness 组件）

对单个 Android 项目根目录做机械检查，作为开发自检与独立评审的确定性底线：
只判定可机械判定的基线规则（Gradle Wrapper、AGP/Gradle 兼容性、Kotlin 与
Compose 编译器配对、SDK/JDK 基线、签名与密钥卫生等），架构合理性与需求
正确性由评审 Agent 按 review-checklist.md 人工判断。

同时兼容 Groovy（build.gradle）与 Kotlin DSL（build.gradle.kts）两种构建
脚本，用正则做轻量解析，不做完整构建脚本解析。

用法：
    python tools/app_check.py <project-dir> [more-dirs ...]
    python tools/app_check.py --json <project-dir>

退出码：
    0 - 无 ERROR（WARN 不影响退出码，需逐条人工确认）
    1 - 存在 ERROR
    2 - 用法/IO 错误（目录不存在、无 app 模块构建文件等）

依赖：Python 3.8+ 标准库，无第三方依赖。
"""

import argparse
import json
import re
import sys
from pathlib import Path

# AGP 次版本 -> 最低 Gradle 版本（官方兼容性矩阵节选）
AGP_MIN_GRADLE = {"8.2": "8.2", "8.3": "8.4", "8.5": "8.7"}

WRAPPER_REL = "gradle/wrapper/gradle-wrapper.properties"

# distributionUrl 中的 Gradle 版本（兼容 services.gradle.org 与腾讯镜像）
GRADLE_URL_RE = re.compile(r"gradle-(\d+(?:\.\d+)*?)-(?:bin|all)\.zip")

# AGP 版本：plugins 块 version 写法 / buildscript classpath 写法
AGP_PATTERNS = [
    re.compile(r"com\.android\.application['\"\s)]*\s+version\s*['\"]([\d.]+)['\"]"),
    re.compile(r"com\.android\.tools\.build:gradle:([\d.]+)"),
]
# Kotlin 版本：org.jetbrains.kotlin.android / kotlin("android") / classpath
KOTLIN_PATTERNS = [
    re.compile(r"org\.jetbrains\.kotlin\.android['\"\s)]*\s+version\s*['\"]([\d.]+)['\"]"),
    re.compile(r"kotlin\(\s*['\"]android['\"]\s*\)\s*version\s*['\"]([\d.]+)['\"]"),
    re.compile(r"kotlin-gradle-plugin:([\d.]+)"),
]

COMPOSE_ENABLED_RE = re.compile(r"(?m)^\s*compose\s*=?\s*true\b")
COMPOSE_PLUGIN_ID = "org.jetbrains.kotlin.plugin.compose"
COMPOSE_PLUGIN_RES = [
    re.compile(r"org\.jetbrains\.kotlin\.plugin\.compose"),
    re.compile(r"""kotlin\(\s*['"]plugin\.compose['"]\s*\)"""),
    re.compile(r"""id\(\s*['"]org\.jetbrains\.kotlin\.plugin\.compose['"]\s*\)"""),
    re.compile(r"""alias\(.*plugin\.compose.*\)"""),
]
COMPOSE_EXT_KEY = "kotlinCompilerExtensionVersion"

NAMESPACE_RE = re.compile(r"(?m)^\s*namespace\s*=?\s*['\"]([^'\"]+)['\"]")
APPLICATION_ID_RE = re.compile(r"(?m)^\s*applicationId\s*=?\s*['\"]([^'\"]+)['\"]")
VERSION_CODE_RE = re.compile(r"versionCode\s*=?\s*\d+")
VERSION_NAME_RE = re.compile(r"versionName\s*=?\s*['\"]")
RELEASE_BLOCK_RE = re.compile(r"(?m)^\s*release\s*\{")

# gradle.properties 中的明文凭据键（大写键名）
SECRET_KEY_RE = re.compile(
    r"^\s*[\w.]*(PASSWORD|KEY_ALIAS|API_KEY|SECRET)[\w.]*\s*=")

README_NAMES = ("README.md", "readme.md", "Readme.md", "README.MD")


def read_text(path):
    """读取文件为无 \\r 的文本；文件不存在时抛出。"""
    return Path(path).read_text(encoding="utf-8-sig").replace(
        "\r\n", "\n").replace("\r", "\n")


def parse_version(s):
    """'8.5.2' -> (8, 5, 2)；忽略非数字段。"""
    return tuple(int(p) for p in str(s).split(".") if p.isdigit())


def version_lt(a, b):
    """版本 a 是否低于 b（按长度补零后比较）。"""
    ta, tb = parse_version(a), parse_version(b)
    n = max(len(ta), len(tb))
    ta += (0,) * (n - len(ta))
    tb += (0,) * (n - len(tb))
    return ta < tb


def line_of(text, pos):
    """文本偏移 -> 1-based 行号。"""
    return text.count("\n", 0, pos) + 1


def find_version_in(text, rel, patterns):
    """在单个构建文件中按候选正则解析版本号，返回 (版本, 文件, 行号)。"""
    for pat in patterns:
        m = pat.search(text)
        if m:
            return m.group(1), rel, line_of(text, m.start())
    return None, rel, 0


def find_app_build_file(root):
    """返回 app 模块构建文件（kts 优先）；不存在返回 None。"""
    for name in ("build.gradle.kts", "build.gradle"):
        p = root / "app" / name
        if p.is_file():
            return p
    return None


def _gitignore_has(root, entry_re):
    gi = root / ".gitignore"
    if not gi.is_file():
        return False
    return bool(re.search(entry_re, read_text(gi), re.M))


def check(project_dir, compile_sdk=34, target_sdk=34, min_sdk=26, jdk=17,
          min_gradle="8.5"):
    """对单个 Android 项目根目录跑全部检查，返回问题列表。

    返回 [{level, file, line, rule, message}]；file 为项目内相对路径，
    line 为 1-based 行号（文件级问题为 0）。目录不存在时抛 FileNotFoundError。
    """
    root = Path(project_dir)
    if not root.is_dir():
        raise FileNotFoundError(str(root))
    jdk = str(jdk)
    issues = []

    def add(level, rule, file, line, msg):
        issues.append({"level": level, "file": file, "line": line,
                       "rule": rule, "message": msg})

    def err(rule, file, line, msg):
        add("ERROR", rule, file, line, msg)

    def warn(rule, file, line, msg):
        add("WARN", rule, file, line, msg)

    # ---- wrapper（E）：Wrapper 存在且 Gradle 版本 >= min_gradle ----
    gradle_version = None
    wrapper = root / "gradle" / "wrapper" / "gradle-wrapper.properties"
    if not wrapper.is_file():
        err("wrapper", WRAPPER_REL, 0,
            "缺少 Gradle Wrapper 配置 gradle/wrapper/gradle-wrapper.properties")
    else:
        wtext = read_text(wrapper)
        m = GRADLE_URL_RE.search(wtext)
        if not m:
            err("wrapper", WRAPPER_REL, 0,
                "未能从 distributionUrl 解析 Gradle 版本")
        else:
            gradle_version = m.group(1)
            if version_lt(gradle_version, min_gradle):
                err("wrapper", WRAPPER_REL, line_of(wtext, m.start()),
                    f"Gradle {gradle_version} 低于最低要求 {min_gradle}，"
                    f"请升级 wrapper 的 distributionUrl")

    # ---- app-module（E）：app 模块构建文件存在 ----
    app_file = find_app_build_file(root)
    if app_file is None:
        err("app-module", "app/build.gradle(.kts)", 0,
            "未找到 app 模块构建文件（app/build.gradle 或 app/build.gradle.kts）")
        return issues  # 其余检查均依赖 app 模块
    app_rel = "app/" + app_file.name
    app_text = read_text(app_file)

    root_build = None
    for name in ("build.gradle.kts", "build.gradle"):
        p = root / name
        if p.is_file():
            root_build = p
            break
    root_text = read_text(root_build) if root_build else ""
    root_rel = root_build.name if root_build else "build.gradle(.kts)"
    all_build_text = app_text + "\n" + root_text

    # ---- jdk-target（E）：JDK 目标为基线值 ----
    jdk_pats = [
        re.compile(r"VERSION_%s\b" % re.escape(jdk)),
        re.compile(r"JVM_%s\b" % re.escape(jdk)),
        re.compile(r"jvmTarget\s*=?\s*['\"]?%s['\"]?\b" % re.escape(jdk)),
    ]
    if not any(p.search(app_text) for p in jdk_pats):
        m = re.search(r"(?m)^\s*(jvmTarget|sourceCompatibility|targetCompatibility)",
                      app_text)
        ln = line_of(app_text, m.start()) if m else 0
        err("jdk-target", app_rel, ln,
            f"未找到 JDK {jdk} 目标配置（compileOptions VERSION_{jdk} 或 "
            f"kotlin jvmTarget {jdk}）")

    # ---- sdk-baseline（E）：compileSdk/targetSdk/minSdk 基线 ----
    for key, op, base in (("compileSdk", "==", compile_sdk),
                          ("targetSdk", "==", target_sdk),
                          ("minSdk", ">=", min_sdk)):
        m = re.search(r"(?m)^\s*%s\s*=?\s*(\d+)" % key, app_text)
        if not m:
            err("sdk-baseline", app_rel, 0, f"未找到 {key} 声明（基线 {op} {base}）")
            continue
        val = int(m.group(1))
        bad = (val != base) if op == "==" else (val < base)
        if bad:
            err("sdk-baseline", app_rel, line_of(app_text, m.start()),
                f"{key} {val} 不符合基线（要求 {op} {base}）")

    # ---- agp-gradle-compat（E）：AGP 与 Gradle 版本按矩阵兼容 ----
    # 先查 app 模块构建文件，再查根构建文件，记录命中位置
    agp_version, agp_rel, agp_line = find_version_in(app_text, app_rel, AGP_PATTERNS)
    if agp_version is None and root_text:
        agp_version, agp_rel, agp_line = find_version_in(
            root_text, root_rel, AGP_PATTERNS)
    if agp_version is None:
        warn("agp-gradle-compat", root_rel, 0,
             "未解析到 AGP 版本（plugins 块或 buildscript classpath），"
             "无法校验与 Gradle 的兼容性")
    else:
        key = ".".join(agp_version.split(".")[:2])
        required = AGP_MIN_GRADLE.get(key)
        if required is None:
            warn("agp-gradle-compat", root_rel, agp_line,
                 f"AGP {agp_version} 不在内置兼容矩阵中，请人工核对官方矩阵")
        elif gradle_version and version_lt(gradle_version, required):
            err("agp-gradle-compat", root_rel, agp_line,
                f"AGP {agp_version} 要求 Gradle >= {required}，"
                f"当前 wrapper 为 {gradle_version}")

    # ---- compose-compiler（E）：Compose 编译器与 Kotlin 版本配对 ----
    if COMPOSE_ENABLED_RE.search(app_text):
        kotlin_version, _, _ = find_version_in(app_text, app_rel, KOTLIN_PATTERNS)
        if kotlin_version is None and root_text:
            kotlin_version, _, _ = find_version_in(
                root_text, root_rel, KOTLIN_PATTERNS)
        has_ext = COMPOSE_EXT_KEY in app_text
        has_plugin = any(rx.search(all_build_text) for rx in COMPOSE_PLUGIN_RES)
        ext_line = line_of(app_text, app_text.find(COMPOSE_EXT_KEY)) if has_ext else 0
        if kotlin_version is None:
            warn("compose-compiler", app_rel, 0,
                 "已启用 Compose 但未解析到 Kotlin 版本，无法校验编译器配对")
        elif parse_version(kotlin_version) >= (2, 0):
            if not has_plugin:
                err("compose-compiler", app_rel, 0,
                    f"Kotlin {kotlin_version}（>= 2.0）启用 Compose 时必须应用 "
                    f"{COMPOSE_PLUGIN_ID} 插件")
            if has_ext:
                err("compose-compiler", app_rel, ext_line,
                    f"Kotlin {kotlin_version}（>= 2.0）不得再配置 "
                    f"{COMPOSE_EXT_KEY}，请删除并改用 {COMPOSE_PLUGIN_ID} 插件")
        else:
            if not has_ext:
                err("compose-compiler", app_rel, 0,
                    f"Kotlin {kotlin_version}（< 2.0）启用 Compose 时必须配置 "
                    f"composeOptions.{COMPOSE_EXT_KEY}（如 \"1.5.10\"）")

    # ---- app-id-namespace（E）：namespace 与 applicationId 一致 ----
    ns = NAMESPACE_RE.search(app_text)
    aid = APPLICATION_ID_RE.search(app_text)
    if not ns:
        err("app-id-namespace", app_rel, 0, "未找到 namespace 声明")
    if not aid:
        err("app-id-namespace", app_rel, 0, "未找到 applicationId 声明")
    if ns and aid and ns.group(1) != aid.group(1):
        err("app-id-namespace", app_rel, line_of(app_text, aid.start()),
            f"namespace（{ns.group(1)}）与 applicationId（{aid.group(1)}）不一致")

    # ---- versioning（E）：versionCode 与 versionName 显式声明 ----
    if not VERSION_CODE_RE.search(app_text):
        err("versioning", app_rel, 0, "未找到 versionCode 声明")
    if not VERSION_NAME_RE.search(app_text):
        err("versioning", app_rel, 0, "未找到 versionName 声明")

    # ---- local-properties-ignore（W）：本机私有文件必须被忽略 ----
    if (root / ".git").exists() and (root / "local.properties").is_file():
        if not _gitignore_has(root, r"^\s*local\.properties\s*$"):
            warn("local-properties-ignore", ".gitignore", 0,
                 "local.properties 存在且本目录是 git 仓库，但 .gitignore 未忽略 "
                 "local.properties（含本机 sdk.dir，不应入库）")

    # ---- secrets-plaintext（W）：凭据不入库 ----
    gp = root / "gradle.properties"
    if gp.is_file():
        for i, ln in enumerate(read_text(gp).split("\n"), 1):
            if SECRET_KEY_RE.match(ln):
                key = ln.split("=", 1)[0].strip()
                warn("secrets-plaintext", "gradle.properties", i,
                     f"疑似明文凭据键「{key}」：口令/API Key 不应入库，"
                     f"应放 gitignored 的 keystore.properties 或构建期从环境变量注入")
    if (root / "keystore.properties").is_file():
        if not _gitignore_has(root, r"^\s*keystore\.properties\s*$"):
            warn("secrets-plaintext", "keystore.properties", 0,
                 "keystore.properties 存在但未被 .gitignore 忽略，签名凭据有入库风险")

    # ---- release-signing（W）：release 构建类型应有签名配置 ----
    m = RELEASE_BLOCK_RE.search(app_text)
    if m and "signingConfig" not in app_text:
        warn("release-signing", app_rel, line_of(app_text, m.start()),
             "存在 release 构建类型但未引用 signingConfigs，release 包将未签名；"
             "签名策略见 docs/android/signing-and-release.md")

    # ---- apk-naming（W）：建议统一 APK 命名 ----
    if "outputFileName" not in app_text:
        warn("apk-naming", app_rel, 0,
             "未配置 APK outputFileName，建议命名为 <app>-<versionName>.apk "
             "（产物目录 app/build/outputs/apk/<buildType>/）")

    # ---- readme（W）：根目录应有 README ----
    if not any((root / n).is_file() for n in README_NAMES):
        warn("readme", "README.md", 0, "根目录缺少 README.md")

    return issues


def main(argv=None):
    parser = argparse.ArgumentParser(
        description="Android 工程基线确定性校验器（ERROR 退出码 1，WARN 仅提示）")
    parser.add_argument("dirs", nargs="+", help="待检查的 Android 项目根目录")
    parser.add_argument("--json", action="store_true", help="以 JSON 输出问题列表")
    parser.add_argument("--compile-sdk", type=int, default=34,
                        help="compileSdk 基线（默认 34）")
    parser.add_argument("--target-sdk", type=int, default=34,
                        help="targetSdk 基线（默认 34）")
    parser.add_argument("--min-sdk", type=int, default=26,
                        help="minSdk 最低值（默认 26）")
    parser.add_argument("--jdk", default="17", help="JDK 基线（默认 17）")
    parser.add_argument("--min-gradle", default="8.5",
                        help="最低 Gradle 版本（默认 8.5）")
    args = parser.parse_args(argv)

    all_issues = []
    failed = False
    for d in args.dirs:
        try:
            issues = check(d, compile_sdk=args.compile_sdk,
                           target_sdk=args.target_sdk, min_sdk=args.min_sdk,
                           jdk=args.jdk, min_gradle=args.min_gradle)
        except FileNotFoundError:
            print(f"app_check: 目录不存在：{d}", file=sys.stderr)
            return 2
        except UnicodeDecodeError as e:
            print(f"app_check: 文件不是 UTF-8 编码：{e}", file=sys.stderr)
            return 2
        if any(i["rule"] == "app-module" and i["level"] == "ERROR"
               for i in issues):
            print(f"app_check: {d} 不是可识别的 Android 项目"
                  f"（缺少 app 模块构建文件）", file=sys.stderr)
            return 2
        all_issues.extend(dict(it, dir=d) for it in issues)
        errors = sum(1 for i in issues if i["level"] == "ERROR")
        warns = sum(1 for i in issues if i["level"] == "WARN")
        if errors:
            failed = True
        if not args.json:
            print(f"app_check: {d}")
            for it in issues:
                loc = f"{it['file']}:L{it['line']}" if it["line"] else it["file"]
                print(f"  [{it['level']}] {it['rule']} {loc}: {it['message']}")
            print(f"  -> {errors} ERROR, {warns} WARN"
                  f" -> {'FAIL' if errors else 'PASS'}")
            print()

    if args.json:
        print(json.dumps(all_issues, ensure_ascii=False, indent=2))
    total_err = sum(1 for i in all_issues if i["level"] == "ERROR")
    if not args.json:
        print(f"总计：{len(all_issues)} 个问题（{total_err} ERROR）"
              f" -> {'FAIL' if failed else 'PASS'}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
