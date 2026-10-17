---
id: LRN-0005
title: git worktree 下校验器误报 local.properties 未忽略
category: 校验器增强
status: merged
source_project: mood-diary
source_task: APP-001
created: 2026-10-17
fingerprint: f1e079722255d1f1
imported_from: mood-diary
imported_on: 2026-10-17
targets: tools/app_check.py;tools/tests/test_app_check.py
merged_on: 2026-10-17
distributed_from: app-agent
harness_id: LRN-0005
---

## 观察

在 git worktree 中执行 App 工程校验时，`local.properties` 实际上已被 `.gitignore` 以 `/local.properties` 形式忽略，但校验器仍报告“未忽略”。原因是校验器用“目录中存在 `.git` 目录”判断是否为 git 仓库，而 worktree 的 `.git` 是文件；同时其正则不兼容带前导 `/` 的忽略规则。

## 证据

APP-001 开发与评审均在独立 worktree 中执行 `uv run --no-project python tools/app_check.py .`，两次都返回 `0 ERROR, 2 WARN`，其中 `local-properties-ignore` 为误报；执行 `git check-ignore -v local.properties` 返回 `.gitignore:8:/local.properties`，证明实际已忽略。

## 建议沉淀

修改 `tools/app_check.py` 的 `local-properties-ignore` 规则：优先使用 `git check-ignore` 或 `git rev-parse --git-dir` 判定，正则兼容前导 `/`；在 `tools/tests/` 增加回归用例，覆盖普通仓库、worktree、已忽略、未忽略四种场景。同步在 `bjm-app-format` 或 `review-checklist.md` 说明 worktree 场景下该 WARN 的采信方式。
