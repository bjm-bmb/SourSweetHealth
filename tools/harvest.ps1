<#
.SYNOPSIS
  app-agent 经验库工具：记录、回传、落地与校验可复用经验条目。

.DESCRIPTION
  在目标项目中用 -Action new 记录候选经验；在 app-agent 仓库中用 -Action import
  回传候选，-Action promote / reject 标记处理结果，-Action validate 校验一致性。

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools\harvest.ps1 -Action new -Title "Compose 页面统一加 testTag" -Category "工程规范" -SourceTask "APP-003"

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools\harvest.ps1 -Action import -From "C:\path\to\project\docs\agent-framework\learnings"

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools\harvest.ps1 -Action promote -Id LRN-0001 -Targets "bjm-app-format;docs/android/app-coding-conventions.md"
#>
[CmdletBinding()]
param(
    [ValidateSet('new', 'list', 'import', 'promote', 'reject', 'validate', 'reindex')]
    [string]$Action = 'list',
    [string]$HarnessDir = '',
    [string]$ProjectDir = '',
    [string]$From = '',
    [string]$Id = '',
    [string]$Title = '',
    [string]$Category = '工程规范',
    [string]$SourceTask = '',
    [string]$Targets = '',
    [string]$Reason = '',
    [string]$Status = ''
)

$ErrorActionPreference = 'Stop'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$validStatus = @('proposed', 'accepted', 'merged', 'rejected')
$requiredFields = @('id', 'title', 'category', 'status', 'source_project', 'created')

function Resolve-HarnessDir {
    if ($HarnessDir) {
        if (-not (Test-Path -LiteralPath $HarnessDir)) { throw "HarnessDir 不存在: $HarnessDir" }
        return (Resolve-Path -LiteralPath $HarnessDir).Path
    }
    return (Split-Path -Parent $PSScriptRoot)
}

function Get-LearningsDir {
    $root = Resolve-HarnessDir
    if ($ProjectDir) {
        if (-not (Test-Path -LiteralPath $ProjectDir)) { throw "ProjectDir 不存在: $ProjectDir" }
        $root = (Resolve-Path -LiteralPath $ProjectDir).Path
    }
    return (Join-Path $root 'docs\agent-framework\learnings')
}

function Read-Text([string]$path) {
    return [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
}

function Write-Text([string]$path, [string]$text) {
    [System.IO.File]::WriteAllText($path, $text, $utf8NoBom)
}

function Split-Entry([string]$text) {
    $normalized = $text -replace "`r`n", "`n"
    $m = [regex]::Match($normalized, '^---\n([\s\S]*?)\n---\n?')
    if (-not $m.Success) { return $null }
    $fields = [ordered]@{}
    foreach ($line in ($m.Groups[1].Value -split "`n")) {
        if ($line -match '^\s*([A-Za-z_]+):\s*(.*)$') {
            $fields[$Matches[1]] = $Matches[2].Trim()
        }
    }
    $body = $normalized.Substring($m.Length)
    return [pscustomobject]@{ Fields = $fields; Body = $body }
}

function Join-Entry($fields, [string]$body) {
    $sb = New-Object System.Text.StringBuilder
    [void]$sb.Append("---`n")
    foreach ($key in $fields.Keys) {
        [void]$sb.Append("${key}: $($fields[$key])`n")
    }
    [void]$sb.Append("---`n`n")
    [void]$sb.Append($body.TrimStart("`n"))
    return $sb.ToString()
}

function Get-Fingerprint([string]$title, [string]$body) {
    $normalized = (($title + "`n" + $body) -replace '\s+', ' ').Trim()
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $hash = $sha.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($normalized))
    } finally {
        $sha.Dispose()
    }
    return (([System.BitConverter]::ToString($hash) -replace '-', '').Substring(0, 16)).ToLower()
}

function Get-Slug([string]$title) {
    $slug = [regex]::Replace($title.Trim(), '[\\/:*?"<>|\s]+', '-').Trim('-')
    if ($slug.Length -gt 40) { $slug = $slug.Substring(0, 40).Trim('-') }
    if (-not $slug) { $slug = 'entry' }
    return $slug
}

function Get-Entries([string]$dir) {
    $list = New-Object System.Collections.ArrayList
    if (-not (Test-Path -LiteralPath $dir)) { return $list }
    foreach ($file in (Get-ChildItem -LiteralPath $dir -Filter 'LRN-*.md' -File | Sort-Object Name)) {
        $parsed = Split-Entry (Read-Text $file.FullName)
        if (-not $parsed) { continue }
        [void]$list.Add([pscustomobject]@{ File = $file; Fields = $parsed.Fields; Body = $parsed.Body })
    }
    return $list
}

function Get-NextId([string]$dir) {
    $max = 0
    foreach ($file in (Get-ChildItem -LiteralPath $dir -Filter 'LRN-*.md' -File -ErrorAction SilentlyContinue)) {
        if ($file.Name -match '^LRN-(\d{4})') {
            $n = [int]$Matches[1]
            if ($n -gt $max) { $max = $n }
        }
    }
    return ('LRN-{0:D4}' -f ($max + 1))
}

function Update-Index([string]$dir) {
    $entries = Get-Entries $dir
    $lines = New-Object System.Collections.ArrayList
    [void]$lines.Add('# 经验索引')
    [void]$lines.Add('')
    [void]$lines.Add('> 本文件由 `tools/harvest.ps1 -Action reindex` 自动生成，请勿手工编辑。')
    [void]$lines.Add('')
    [void]$lines.Add('| ID | 标题 | 类别 | 状态 | 来源项目 | 创建日期 | 落地位置 |')
    [void]$lines.Add('| --- | --- | --- | --- | --- | --- | --- |')
    foreach ($entry in $entries) {
        $f = $entry.Fields
        $targets = $f['targets']
        if (-not $targets) { $targets = '-' }
        $cells = @($f['id'], $f['title'], $f['category'], $f['status'], $f['source_project'], $f['created'], $targets)
        $cells = $cells | ForEach-Object { if ($_) { ([string]$_) -replace '\|', '\|' } else { '-' } }
        [void]$lines.Add('| ' + ($cells -join ' | ') + ' |')
    }
    Write-Text (Join-Path $dir 'INDEX.md') (($lines -join "`n") + "`n")
}

function Set-Field([string]$text, [string]$key, [string]$value) {
    $pattern = '(?m)^' + [regex]::Escape($key) + ':.*$'
    if ([regex]::IsMatch($text, $pattern)) {
        return [regex]::Replace($text, $pattern, { param($m) "${key}: ${value}" })
    }
    $normalized = $text -replace "`r`n", "`n"
    $lines = $normalized -split "`n"
    $closeIndex = -1
    $seen = 0
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i].Trim() -eq '---') {
            $seen++
            if ($seen -eq 2) { $closeIndex = $i; break }
        }
    }
    if ($closeIndex -lt 0) { throw '条目缺少 frontmatter 结束标记（---）' }
    $before = @()
    $after = @()
    if ($closeIndex -gt 0) { $before = $lines[0..($closeIndex - 1)] }
    if ($closeIndex -lt ($lines.Count - 1)) { $after = $lines[($closeIndex + 1)..($lines.Count - 1)] }
    $result = @($before) + @("${key}: ${value}") + @('---') + @($after)
    return ($result -join "`n")
}

function Find-EntryFile([string]$dir, [string]$entryId) {
    $match = Get-ChildItem -LiteralPath $dir -Filter 'LRN-*.md' -File | Where-Object { $_.Name -like "$entryId-*" } | Select-Object -First 1
    if ($match) { return $match }
    foreach ($file in (Get-ChildItem -LiteralPath $dir -Filter 'LRN-*.md' -File)) {
        $parsed = Split-Entry (Read-Text $file.FullName)
        if ($parsed -and $parsed.Fields['id'] -eq $entryId) { return $file }
    }
    return $null
}

$dir = Get-LearningsDir

switch ($Action) {
    'new' {
        if (-not $Title) { throw '请用 -Title 指定经验标题' }
        if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
        $templatePath = Join-Path $dir '_template.md'
        if (Test-Path -LiteralPath $templatePath) {
            $text = Read-Text $templatePath
        } else {
            $text = "---`nid: LRN-0000`ntitle: x`ncategory: x`nstatus: proposed`nsource_project: x`ncreated: 2000-01-01`n---`n`n## 观察`n`n## 证据`n`n## 建议沉淀`n"
        }
        $newId = Get-NextId $dir
        $projectName = ''
        if ($ProjectDir) { $projectName = Split-Path -Leaf ((Resolve-Path -LiteralPath $ProjectDir).Path) }
        if (-not $projectName) { $projectName = Split-Path -Leaf (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)) }
        $text = Set-Field $text 'id' $newId
        $text = Set-Field $text 'title' $Title
        $text = Set-Field $text 'category' $Category
        $text = Set-Field $text 'status' 'proposed'
        $text = Set-Field $text 'source_project' $projectName
        $text = Set-Field $text 'source_task' $SourceTask
        $text = Set-Field $text 'created' (Get-Date -Format 'yyyy-MM-dd')
        $fileName = "$newId-$(Get-Slug $Title).md"
        Write-Text (Join-Path $dir $fileName) $text
        Update-Index $dir
        Write-Host "[ok] 已创建候选经验: $fileName" -ForegroundColor Green
        Write-Host "     目录: $dir"
        Write-Host '     请补全「观察 / 证据 / 建议沉淀」后，在 app-agent 仓库执行 -Action import 回传。'
    }
    'list' {
        $entries = @(Get-Entries $dir)
        if ($Status) { $entries = @($entries | Where-Object { $_.Fields['status'] -eq $Status }) }
        if ($entries.Count -eq 0) {
            Write-Host '暂无经验条目。'
        } else {
            $entries | ForEach-Object {
                [pscustomobject]@{
                    ID       = $_.Fields['id']
                    标题     = $_.Fields['title']
                    类别     = $_.Fields['category']
                    状态     = $_.Fields['status']
                    来源项目 = $_.Fields['source_project']
                    创建日期 = $_.Fields['created']
                }
            } | Format-Table -AutoSize
        }
    }
    'import' {
        if (-not $From) { throw '请用 -From 指定来源 learnings 目录' }
        if (-not (Test-Path -LiteralPath $From)) { throw "来源目录不存在: $From" }
        if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
        $existing = Get-Entries $dir
        $existingFingerprints = @{}
        $existingIds = @{}
        foreach ($entry in $existing) {
            $fp = $entry.Fields['fingerprint']
            if (-not $fp) { $fp = Get-Fingerprint $entry.Fields['title'] $entry.Body }
            $existingFingerprints[$fp] = $entry.Fields['id']
            $existingIds[$entry.Fields['id']] = $true
        }
        $imported = 0
        $skipped = 0
        foreach ($file in (Get-ChildItem -LiteralPath $From -Filter 'LRN-*.md' -File | Sort-Object Name)) {
            $parsed = Split-Entry (Read-Text $file.FullName)
            if (-not $parsed) { Write-Host "[skip] 无法解析: $($file.Name)"; $skipped++; continue }
            if ($parsed.Fields['status'] -ne 'proposed') { Write-Host "[skip] 非候选条目（$($parsed.Fields['status'])）: $($file.Name)"; $skipped++; continue }
            $fingerprint = Get-Fingerprint $parsed.Fields['title'] $parsed.Body
            if ($existingFingerprints.ContainsKey($fingerprint)) {
                Write-Host "[skip] 重复经验（已存在 $($existingFingerprints[$fingerprint])）: $($file.Name)"
                $skipped++
                continue
            }
            $newId = $parsed.Fields['id']
            if (-not $newId -or $existingIds.ContainsKey($newId)) { $newId = Get-NextId $dir }
            while ($existingIds.ContainsKey($newId)) { $newId = Get-NextId $dir }
            $text = Join-Entry $parsed.Fields $parsed.Body
            $text = Set-Field $text 'id' $newId
            $text = Set-Field $text 'status' 'accepted'
            $text = Set-Field $text 'fingerprint' $fingerprint
            $text = Set-Field $text 'imported_from' $parsed.Fields['source_project']
            $text = Set-Field $text 'imported_on' (Get-Date -Format 'yyyy-MM-dd')
            $fileName = "$newId-$(Get-Slug $parsed.Fields['title']).md"
            Write-Text (Join-Path $dir $fileName) $text
            $existingIds[$newId] = $true
            $existingFingerprints[$fingerprint] = $newId
            Write-Host "[ok] 导入 $($file.Name) -> $fileName" -ForegroundColor Green
            $imported++
        }
        Update-Index $dir
        Write-Host "导入完成: $imported 条，跳过 $skipped 条。"
    }
    'promote' {
        if (-not $Id) { throw '请用 -Id 指定经验编号' }
        if (-not $Targets) { throw '请用 -Targets 指定落地文件（分号分隔）' }
        $file = Find-EntryFile $dir $Id
        if (-not $file) { throw "未找到经验条目: $Id" }
        $text = Read-Text $file.FullName
        $text = Set-Field $text 'status' 'merged'
        $text = Set-Field $text 'targets' $Targets
        $text = Set-Field $text 'merged_on' (Get-Date -Format 'yyyy-MM-dd')
        Write-Text $file.FullName $text
        Update-Index $dir
        Write-Host "[ok] $Id 已标记为 merged：$Targets" -ForegroundColor Green
    }
    'reject' {
        if (-not $Id) { throw '请用 -Id 指定经验编号' }
        if (-not $Reason) { throw '请用 -Reason 说明不沉淀的理由' }
        $file = Find-EntryFile $dir $Id
        if (-not $file) { throw "未找到经验条目: $Id" }
        $text = Read-Text $file.FullName
        $text = Set-Field $text 'status' 'rejected'
        $text = Set-Field $text 'reject_reason' $Reason
        $text = Set-Field $text 'rejected_on' (Get-Date -Format 'yyyy-MM-dd')
        Write-Text $file.FullName $text
        Update-Index $dir
        Write-Host "[ok] $Id 已标记为 rejected：$Reason" -ForegroundColor Yellow
    }
    'validate' {
        $entries = @(Get-Entries $dir)
        $errors = New-Object System.Collections.ArrayList
        $seenIds = @{}
        foreach ($entry in $entries) {
            $name = $entry.File.Name
            foreach ($field in $requiredFields) {
                if (-not $entry.Fields[$field]) { [void]$errors.Add("${name}: 缺少必填字段 $field") }
            }
            $entryStatus = $entry.Fields['status']
            if ($entryStatus -and ($validStatus -notcontains $entryStatus)) {
                [void]$errors.Add("${name}: 非法状态 $entryStatus")
            }
            if ($entryStatus -eq 'merged' -and -not $entry.Fields['targets']) {
                [void]$errors.Add("${name}: merged 状态必须填写 targets")
            }
            if ($entryStatus -eq 'rejected' -and -not $entry.Fields['reject_reason']) {
                [void]$errors.Add("${name}: rejected 状态必须填写 reject_reason")
            }
            $entryId = $entry.Fields['id']
            if ($entryId -and $entryId -notmatch '^LRN-\d{4}$') { [void]$errors.Add("${name}: id 格式应为 LRN-0000") }
            if ($entryId) {
                if ($seenIds.ContainsKey($entryId)) { [void]$errors.Add("${name}: id 重复 $entryId") } else { $seenIds[$entryId] = $true }
                if ($name -notlike "$entryId-*") { [void]$errors.Add("${name}: 文件名前缀与 id（$entryId）不一致") }
            }
        }
        $indexPath = Join-Path $dir 'INDEX.md'
        if (-not (Test-Path -LiteralPath $indexPath)) {
            [void]$errors.Add('缺少 INDEX.md（执行 -Action reindex 生成）')
        } else {
            $indexText = Read-Text $indexPath
            foreach ($entry in $entries) {
                if ($indexText -notmatch [regex]::Escape($entry.Fields['id'])) {
                    [void]$errors.Add("INDEX.md 缺少条目 $($entry.Fields['id'])（执行 -Action reindex）")
                }
            }
        }
        if ($errors.Count -eq 0) {
            Write-Host ("[ok] 校验通过：{0} 条经验，字段/状态/INDEX 一致。" -f $entries.Count) -ForegroundColor Green
        } else {
            foreach ($e in $errors) { Write-Host "[error] $e" -ForegroundColor Red }
            Write-Host ("校验失败：{0} 个问题。" -f $errors.Count) -ForegroundColor Red
            exit 1
        }
    }
    'reindex' {
        if (-not (Test-Path -LiteralPath $dir)) { throw "learnings 目录不存在: $dir" }
        Update-Index $dir
        Write-Host "[ok] 已重建 INDEX.md: $(Join-Path $dir 'INDEX.md')" -ForegroundColor Green
    }
}
