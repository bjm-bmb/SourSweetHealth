#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""scope_check.py - 任务范围依从性校验器（app-agent harness 组件）

把 handoff 提交实际改动的文件，与任务卡「范围 / 允许修改」声明的路径做机械
比对。应用场景：开发自检、评审前置检查、交付收口。

背景：任务卡允许路径之外的文件一旦被改动，即使构建与单测全绿，也会把既有
功能改坏（真实事故：一个「推送去重」任务顺手改了设置页与列表加载逻辑）。

用法：
    # 用任务卡声明 + git 提交范围
    python tools/scope_check.py --task docs/agent-framework/tasks/APP-001-x.md \
        --since <base-sha> [--project-dir .]

    # 直接给允许路径（可重复），或给改动文件清单
    python tools/scope_check.py --allow app/src/main/java/.../Foo.kt \
        --allow app/src/test/ --paths changed.txt

退出码：
    0 - 无越界文件
    1 - 存在越界文件
    2 - 用法/解析错误（未解析到允许路径、git 失败等）

依赖：Python 3.8+ 标准库。
"""

import argparse
import re
import subprocess
import sys
from pathlib import Path

# 「允许修改」段落里用反引号标注的路径
BACKTICK_PATH_RE = re.compile(r"`([^`\n]+)`")
SECTION_RE = re.compile(r"^##\s*范围\s*$", re.M)
STOP_RE = re.compile(r"^##\s", re.M)

# 反引号里可能是路径，也可能是命令/标识符；只接受看起来像项目内路径的值。
PATH_HINT_RE = re.compile(r"^[\w./\-*{}]+$")
PATH_EXT_RE = re.compile(r"\.(kt|java|kts|gradle|xml|md|json|pro|properties|toml)$")


def read_text(path):
    return Path(path).read_text(encoding="utf-8-sig", errors="replace")


def parse_allowed_from_task(task_path):
    """从任务卡「## 范围」小节的「允许修改」条目提取反引号路径。

    返回 (allowed, reason)。allowed 为空时 reason 说明原因。
    """
    text = read_text(task_path)
    sec = SECTION_RE.search(text)
    if not sec:
        return [], "任务卡缺少「## 范围」小节"
    tail = text[sec.end():]
    stop = STOP_RE.search(tail)
    body = tail[:stop.start()] if stop else tail

    allowed = []
    in_allow = False
    for raw in body.splitlines():
        stripped = raw.strip()
        if not stripped.startswith("-"):
            continue
        indent = len(raw) - len(raw.lstrip())
        if indent == 0:
            # 顶层条目切换上下文；「明确不修改」「显式不做清单」等
            # 同级的其它条目会关闭收集。
            in_allow = "允许修改" in stripped
            if not in_allow:
                continue
        elif not in_allow:
            continue
        for value in BACKTICK_PATH_RE.findall(stripped):
            value = value.strip().strip("/")
            if not value or not PATH_HINT_RE.match(value):
                continue
            if "/" not in value and not PATH_EXT_RE.search(value):
                continue
            allowed.append(value)
    if not allowed:
        return [], "「允许修改」中未解析到任何反引号路径；请改用 --allow 显式声明"
    return allowed, ""


def normalize(path):
    return path.replace("\\", "/").lstrip("./")


def path_allowed(changed, allowed_entries):
    changed = normalize(changed)
    for entry in allowed_entries:
        entry = normalize(entry)
        if changed == entry:
            return True
        if changed.startswith(entry.rstrip("/") + "/"):
            return True
        if entry.endswith("*") and changed.startswith(entry[:-1]):
            return True
    return False


def changed_files_from_git(project_dir, since):
    try:
        out = subprocess.run(
            ["git", "-C", project_dir, "diff", "--name-only", since, "HEAD"],
            capture_output=True, text=True, check=True,
        ).stdout
    except (OSError, subprocess.CalledProcessError) as e:
        raise RuntimeError(f"git diff 失败：{e}")
    return [l.strip() for l in out.splitlines() if l.strip()]


def main(argv=None):
    parser = argparse.ArgumentParser(
        description="校验改动文件是否落在任务卡允许路径内")
    parser.add_argument("--task", help="任务卡路径（从中解析「允许修改」）")
    parser.add_argument("--allow", action="append", default=[],
                        help="允许路径或目录前缀，可重复")
    parser.add_argument("--since", help="git 基线，如提交 SHA；与 --paths 二选一")
    parser.add_argument("--paths", help="改动文件清单（每行一个），用于离线校验")
    parser.add_argument("--project-dir", default=".",
                        help="项目根目录（默认当前目录）")
    parser.add_argument("--include-task-card", action="store_true", default=True,
                        help="自动允许任务卡自身（默认开启）")
    args = parser.parse_args(argv)

    allowed = [normalize(p) for p in args.allow]
    if args.task:
        parsed, reason = parse_allowed_from_task(args.task)
        if not parsed and not allowed:
            print(f"scope_check: {reason}", file=sys.stderr)
            return 2
        allowed.extend(normalize(p) for p in parsed)
        if args.include_task_card:
            allowed.append(normalize(args.task))

    if not allowed:
        print("scope_check: 需要 --task 或 --allow 之一", file=sys.stderr)
        return 2

    if args.paths:
        changed = [normalize(l.strip())
                   for l in read_text(args.paths).splitlines() if l.strip()]
    elif args.since:
        try:
            changed = [normalize(p)
                       for p in changed_files_from_git(args.project_dir, args.since)]
        except RuntimeError as e:
            print(f"scope_check: {e}", file=sys.stderr)
            return 2
    else:
        print("scope_check: 需要 --since 或 --paths 之一", file=sys.stderr)
        return 2

    violations = [p for p in changed if not path_allowed(p, allowed)]
    print(f"scope_check: 允许 {len(allowed)} 条规则，改动 {len(changed)} 个文件")
    for entry in allowed:
        print(f"  allow: {entry}")
    if violations:
        print(f"  -> 越界 {len(violations)} 个文件：")
        for p in violations:
            print(f"     [VIOLATION] {p}")
        print("  -> FAIL：请把改动收敛到任务卡允许路径，或先更新任务卡并重新确认")
        return 1
    print("  -> PASS：全部改动位于允许路径内")
    return 0


if __name__ == "__main__":
    sys.exit(main())
