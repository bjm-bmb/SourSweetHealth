#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""scope_check.py 回归测试。

    python tools/tests/test_scope_check.py

覆盖：任务卡路径解析、目录前缀匹配、越界判定、离线清单模式。
"""

import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
import scope_check as sc  # noqa: E402

TASK_CARD = """# APP-999：测试

## 范围

- 允许修改：
  - `app/src/main/java/com/example/Foo.kt`
  - `app/src/main/java/com/example/data/`
- 明确不修改：`app/src/main/java/com/example/ui/Bar.kt`
- 显式不做清单：
  - 不改 UI

## 实现要点
"""


class ParseTaskCard(unittest.TestCase):
    def _write(self, text):
        f = tempfile.NamedTemporaryFile("w", suffix=".md", delete=False,
                                        encoding="utf-8")
        f.write(text)
        f.close()
        return f.name

    def test_extracts_only_allow_section(self):
        allowed, reason = sc.parse_allowed_from_task(self._write(TASK_CARD))
        self.assertEqual(reason, "")
        self.assertIn("app/src/main/java/com/example/Foo.kt", allowed)
        self.assertIn("app/src/main/java/com/example/data", allowed)
        self.assertNotIn("app/src/main/java/com/example/ui/Bar.kt", allowed)

    def test_missing_section_reports_reason(self):
        allowed, reason = sc.parse_allowed_from_task(self._write("# 无范围\n"))
        self.assertEqual(allowed, [])
        self.assertTrue(reason)


class PathMatching(unittest.TestCase):
    def test_exact_file(self):
        self.assertTrue(sc.path_allowed(
            "app/src/main/java/com/example/Foo.kt",
            ["app/src/main/java/com/example/Foo.kt"]))

    def test_directory_prefix(self):
        self.assertTrue(sc.path_allowed(
            "app/src/main/java/com/example/data/db/Bar.kt",
            ["app/src/main/java/com/example/data"]))
        # 相邻同前缀目录不得误判
        self.assertFalse(sc.path_allowed(
            "app/src/main/java/com/example/database/Bar.kt",
            ["app/src/main/java/com/example/data"]))

    def test_violation(self):
        self.assertFalse(sc.path_allowed(
            "app/src/main/java/com/example/ui/Bar.kt",
            ["app/src/main/java/com/example/Foo.kt"]))

    def test_windows_separators(self):
        self.assertTrue(sc.path_allowed(
            "app\\src\\main\\java\\com\\example\\data\\X.kt",
            ["app/src/main/java/com/example/data"]))


class CliModes(unittest.TestCase):
    def test_paths_mode_pass_and_fail(self):
        with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False,
                                         encoding="utf-8") as f:
            f.write("app/src/main/java/com/example/data/X.kt\n")
            ok = f.name
        with tempfile.NamedTemporaryFile("w", suffix=".txt", delete=False,
                                         encoding="utf-8") as f:
            f.write("app/src/main/java/com/example/ui/Bar.kt\n")
            bad = f.name
        allow = ["--allow", "app/src/main/java/com/example/data"]
        self.assertEqual(sc.main(allow + ["--paths", ok]), 0)
        self.assertEqual(sc.main(allow + ["--paths", bad]), 1)

    def test_missing_inputs(self):
        self.assertEqual(sc.main(["--paths", "x.txt"]), 2)


if __name__ == "__main__":
    unittest.main(verbosity=2)
