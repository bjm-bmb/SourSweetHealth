#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""app_check.py 回归测试。

改动 app_check.py 前后都必须运行本测试，防止重新引入误报或漏检：
    python tools/tests/test_app_check.py

fixtures 约定（每个是一个迷你 Android 项目目录，只含必要的 gradle 文件）：
    valid/               - Kotlin DSL 合规基线，期望 0 问题
    groovy-valid/        - Groovy DSL 合规变体（ViewBinding，不开 Compose），期望 0 问题
    kotlin2-compose-bad/ - Kotlin 2.0 用了 kotlinCompilerExtensionVersion 且缺少
                           compose 插件，期望 compose-compiler ERROR
    jdk-bad/             - jvmTarget 11，期望 jdk-target ERROR
    sdk-bad/             - minSdk 21，期望 sdk-baseline ERROR
    secrets-warn/        - gradle.properties 明文口令，期望 secrets-plaintext WARN 且 0 ERROR
"""

import sys
import unittest
from pathlib import Path
import shutil
import tempfile

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import app_check as ac  # noqa: E402

FIX = Path(__file__).resolve().parent / "fixtures"


def run_check(name, **kwargs):
    return ac.check(str(FIX / name), **kwargs)


def rule_set(issues, level=None):
    return {i["rule"] for i in issues if level is None or i["level"] == level}


def find(issues, rule, level=None):
    return [i for i in issues if i["rule"] == rule and (level is None or i["level"] == level)]


class BaselineTest(unittest.TestCase):
    """合规样例必须零问题——checker 的任何误报都会在这里暴露。"""

    def test_valid_has_no_issues(self):
        issues = run_check("valid")
        self.assertEqual(issues, [], f"合规样例不应有问题：{issues}")

    def test_groovy_valid_has_no_issues(self):
        issues = run_check("groovy-valid")
        self.assertEqual(issues, [], f"Groovy 合规样例不应有问题：{issues}")


class ComposeCompilerTest(unittest.TestCase):
    """Kotlin 2.0 错配旧 Compose 编译器配置必须判 ERROR。"""

    def setUp(self):
        self.issues = run_check("kotlin2-compose-bad")

    def test_compose_compiler_error(self):
        hits = find(self.issues, "compose-compiler", "ERROR")
        self.assertTrue(hits, "Kotlin 2.0 错配 Compose 编译器未被判 ERROR")

    def test_both_violations_reported(self):
        msgs = [i["message"] for i in find(self.issues, "compose-compiler", "ERROR")]
        self.assertTrue(any("plugin.compose" in m for m in msgs), "缺少 compose 插件未被抓")
        self.assertTrue(any("kotlinCompilerExtensionVersion" in m for m in msgs),
                        "残留 kotlinCompilerExtensionVersion 未被抓")

    def test_only_compose_compiler_errors(self):
        self.assertEqual(rule_set(self.issues, "ERROR"), {"compose-compiler"},
                         f"不应有其它 ERROR：{self.issues}")

    def test_kotlin19_path_not_flagged(self):
        """Kotlin 1.9 + kotlinCompilerExtensionVersion 是合法路径（valid 样例）。"""
        self.assertEqual(find(run_check("valid"), "compose-compiler"), [])


class JdkTargetTest(unittest.TestCase):

    def test_jvm_target_11_is_error(self):
        issues = run_check("jdk-bad")
        hits = find(issues, "jdk-target", "ERROR")
        self.assertTrue(hits, "jvmTarget 11 未被判 ERROR")
        self.assertEqual(rule_set(issues, "ERROR"), {"jdk-target"})

    def test_jdk_override(self):
        """以 --jdk 11 口径检查 jdk-bad 样例时不应再报 jdk-target。"""
        issues = run_check("jdk-bad", jdk=11)
        self.assertEqual(find(issues, "jdk-target"), [])


class SdkBaselineTest(unittest.TestCase):

    def test_min_sdk_21_is_error(self):
        issues = run_check("sdk-bad")
        hits = find(issues, "sdk-baseline", "ERROR")
        self.assertTrue(hits, "minSdk 21 未被判 ERROR")
        self.assertTrue(any("minSdk" in i["message"] for i in hits))
        self.assertEqual(rule_set(issues, "ERROR"), {"sdk-baseline"})


class SecretsTest(unittest.TestCase):

    def test_plaintext_secrets_warn_only(self):
        issues = run_check("secrets-warn")
        hits = find(issues, "secrets-plaintext", "WARN")
        self.assertEqual(len(hits), 2, f"明文口令与 KEY_ALIAS 应各报一条 WARN：{issues}")
        self.assertEqual(rule_set(issues, "ERROR"), set(), "明文密钥是 WARN，不应升级 ERROR")

    def test_secrets_warn_exit_0(self):
        self.assertEqual(ac.main([str(FIX / "secrets-warn")]), 0)


class GitignoreTest(unittest.TestCase):
    """local.properties / keystore.properties 的根锚定忽略写法（含 worktree）。"""

    def _make_project(self, tmp, gitignore_lines, with_keystore=False):
        root = Path(tmp) / "proj"
        shutil.copytree(FIX / "valid", root)
        # worktree 下 .git 是文件而不是目录，checker 必须仍识别为 git 仓库
        (root / ".git").write_text("gitdir: /tmp/worktree\n", encoding="utf-8")
        (root / "local.properties").write_text("sdk.dir=/sdk\n", encoding="utf-8")
        if with_keystore:
            (root / "keystore.properties").write_text(
                "storePassword=dummy\n", encoding="utf-8")
        (root / ".gitignore").write_text(
            "\n".join(gitignore_lines) + "\n", encoding="utf-8")
        return root

    def test_plain_local_properties_ignored(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = self._make_project(tmp, ["local.properties"])
            issues = ac.check(str(root))
            self.assertEqual(find(issues, "local-properties-ignore"), [])

    def test_root_anchored_local_properties_ignored(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = self._make_project(tmp, ["/local.properties"])
            issues = ac.check(str(root))
            self.assertEqual(
                find(issues, "local-properties-ignore"), [],
                f"worktree + /local.properties 不应误报：{issues}")

    def test_unignored_local_properties_warned(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = self._make_project(tmp, [".gradle/"])
            issues = ac.check(str(root))
            self.assertEqual(
                len(find(issues, "local-properties-ignore", "WARN")), 1)

    def test_root_anchored_keystore_ignored(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = self._make_project(
                tmp, ["/keystore.properties"], with_keystore=True)
            issues = ac.check(str(root))
            hits = [i for i in find(issues, "secrets-plaintext")
                    if i["file"] == "keystore.properties"]
            self.assertEqual(hits, [])

    def test_unignored_keystore_warned(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = self._make_project(
                tmp, [".gradle/"], with_keystore=True)
            issues = ac.check(str(root))
            hits = [i for i in find(issues, "secrets-plaintext", "WARN")
                    if i["file"] == "keystore.properties"]
            self.assertEqual(len(hits), 1)


class DesignSystemTemplateTest(unittest.TestCase):
    """设计系统模板与 app 实例必须逐字一致（替换 {{package}} 后）。"""

    TEMPLATE = (
        "package {{package}}.ui.components\n\n"
        "/** 示例组件。 */\n"
        "fun appFoo(): Int = 1\n"
    )

    def _make_project(self, tmp, instance_text):
        root = Path(tmp) / "proj"
        shutil.copytree(FIX / "valid", root)
        tpl_dir = root / "docs" / "android" / "design-system"
        tpl_dir.mkdir(parents=True)
        (tpl_dir / "AppFoo.kt.template").write_text(
            self.TEMPLATE, encoding="utf-8")
        comp_dir = (root / "app" / "src" / "main" / "java" /
                    "com" / "example" / "valid" / "ui" / "components")
        comp_dir.mkdir(parents=True)
        (comp_dir / "AppFoo.kt").write_text(instance_text, encoding="utf-8")
        return root

    def test_drift_is_warned(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = self._make_project(
                tmp,
                "package com.example.valid.ui.components\n\n"
                "/** 示例组件。 */\n"
                "fun appFoo(): Int = 2\n")
            issues = ac.check(str(root))
            hits = find(issues, "design-system-templates", "WARN")
            self.assertEqual(len(hits), 1,
                             f"模板漂移应报一条 WARN：{issues}")
            self.assertIn("AppFoo", hits[0]["message"])

    def test_identical_template_no_warn(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = self._make_project(
                tmp, self.TEMPLATE.replace("{{package}}", "com.example.valid"))
            issues = ac.check(str(root))
            self.assertEqual(find(issues, "design-system-templates"), [],
                             f"逐字一致时不应报警：{issues}")


class CliTest(unittest.TestCase):
    """命令行入口行为：0=无 ERROR，1=有 ERROR，2=用法/IO 错误。"""

    def test_missing_dir_exit_2(self):
        self.assertEqual(ac.main([str(FIX / "no-such-project")]), 2)

    def test_no_app_module_exit_2(self):
        """fixtures 根目录本身没有 app 模块构建文件。"""
        self.assertEqual(ac.main([str(FIX)]), 2)

    def test_valid_exit_0(self):
        self.assertEqual(ac.main([str(FIX / "valid")]), 0)

    def test_jdk_bad_exit_1(self):
        self.assertEqual(ac.main([str(FIX / "jdk-bad")]), 1)

    def test_json_exit_code(self):
        self.assertEqual(ac.main(["--json", str(FIX / "valid")]), 0)
        self.assertEqual(ac.main(["--json", str(FIX / "sdk-bad")]), 1)


if __name__ == "__main__":
    unittest.main(verbosity=2)
