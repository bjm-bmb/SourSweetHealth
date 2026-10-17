<#
.SYNOPSIS
  app-agent 规范包自动更新：从 GitHub 仓库拉取最新版本并在需要时静默升级当前项目。

.DESCRIPTION
  由 install.ps1 复制到每个已安装 app-agent 的项目（tools\harness_update.ps1）。
  打开仓库时通过 .vscode\tasks.json 的 runOn=folderOpen 任务自动执行，也可手工运行：

    powershell -ExecutionPolicy Bypass -File tools\harness_update.ps1
    powershell -ExecutionPolicy Bypass -File tools\harness_update.ps1 -Check
    powershell -ExecutionPolicy Bypass -File tools\harness_update.ps1 -LocalSource "C:\path\to\bjm-app-dev-agent"

  行为：
    1) 读取本项目 docs\agent-framework\harness-version.txt；
    2) 用 git 拉取规范包仓库到 .agent-artifacts\harness-cache（已有缓存则 fetch），读取其中 app-agent\VERSION；
       git 走本机已保存的凭据，因此私有仓库同样可用；无凭据或离线时跳过（退出码 2）；
    3) 远端更新时用缓存里的 app-agent\install\install.ps1 升级本项目（幂等，项目自有品牌/设计系统/任务卡/经验条目不被覆盖）；
    4) 结果写入 .agent-artifacts\harness-update.log（该目录已被安装脚本加入 .gitignore）。

  退出码：0 = 已是最新或升级成功；1 = 升级失败；2 = 无法检查（离线/无凭据/缺少工具）。

.PARAMETER TargetDir
  目标项目根目录。默认是本脚本所在目录的上一级（即 tools\ 所属项目）。

.PARAMETER LocalSource
  用本地规范包克隆替代 git 拉取（离线验证与调试用），指向仓库根目录或 app-agent 目录。

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools\harness_update.ps1 -Check
#>
[CmdletBinding()]
param(
    [string]$TargetDir = '',
    [string]$Repo = 'bjm-bmb/bjm-app-dev-agent',
    [string]$Branch = 'main',
    [switch]$Check,
    [switch]$Force,
    [switch]$Quiet,
    [string]$LocalSource = '',
    [switch]$SkipDocs,
    [switch]$ForceBrandDocs,
    [switch]$ForceDesignSystem
)

$ErrorActionPreference = 'Stop'

function Write-Info([string]$message) {
    if (-not $Quiet) { Write-Host $message }
}

function Write-Log([string]$path, [string[]]$lines) {
    if (-not $path) { return }
    try {
        $dir = Split-Path -Parent $path
        if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
        $stamp = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
        Add-Content -LiteralPath $path -Value (@("[$stamp]") + ($lines | ForEach-Object { "  $_" })) -Encoding UTF8
    } catch {
        # logging must never break an unattended run
    }
}

function Read-VersionFile([string]$path) {
    if (-not (Test-Path -LiteralPath $path)) { return [version]'0.0.0' }
    $text = ((Get-Content -LiteralPath $path -Raw) -as [string])
    if (-not $text) { return [version]'0.0.0' }
    $parsed = $null
    if ([version]::TryParse($text.Trim(), [ref]$parsed)) { return $parsed }
    return [version]'0.0.0'
}

function Invoke-GitQuiet([string[]]$gitArgs) {
    # credentials come from the local git credential helper; never prompt in an unattended run
    $previousPrompt = $env:GIT_TERMINAL_PROMPT
    $previousGcm = $env:GCM_INTERACTIVE
    try {
        $env:GIT_TERMINAL_PROMPT = '0'
        $env:GCM_INTERACTIVE = 'never'
        $output = & git @gitArgs 2>&1
        return [pscustomobject]@{ ExitCode = $LASTEXITCODE; Output = @($output | ForEach-Object { [string]$_ }) }
    } finally {
        $env:GIT_TERMINAL_PROMPT = $previousPrompt
        $env:GCM_INTERACTIVE = $previousGcm
    }
}

if (-not $TargetDir) { $TargetDir = Split-Path -Parent $PSScriptRoot }
if (-not (Test-Path -LiteralPath $TargetDir)) {
    Write-Host "[error] 目标项目不存在: $TargetDir"
    exit 1
}
$target = (Resolve-Path -LiteralPath $TargetDir).Path
$logFile = Join-Path $target '.agent-artifacts\harness-update.log'
$localVersion = Read-VersionFile (Join-Path $target 'docs\agent-framework\harness-version.txt')

# resolve the source package: local clone when -LocalSource is given, otherwise the git cache
$sourceRoot = ''
if ($LocalSource) {
    if (-not (Test-Path -LiteralPath $LocalSource)) { Write-Host "[error] LocalSource 不存在: $LocalSource"; exit 1 }
    $candidate = (Resolve-Path -LiteralPath $LocalSource).Path
    if (Test-Path -LiteralPath (Join-Path $candidate 'app-agent\install\install.ps1')) {
        $sourceRoot = Join-Path $candidate 'app-agent'
    } elseif (Test-Path -LiteralPath (Join-Path $candidate 'install\install.ps1')) {
        $sourceRoot = $candidate
    } else {
        Write-Host "[error] LocalSource 不是规范包目录（缺少 install\install.ps1）: $candidate"
        exit 1
    }
} else {
    if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
        Write-Host '[skip] 未找到 git，无法检查规范包更新'
        Write-Log $logFile @('跳过：未找到 git', "当前版本：$localVersion")
        exit 2
    }
    $cache = Join-Path $target '.agent-artifacts\harness-cache'
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $cache) | Out-Null
    Write-Info "检查更新: $Repo@$Branch"
    if (Test-Path -LiteralPath (Join-Path $cache '.git')) {
        # the harness is distributed with LF endings; a cache checked out with the machine's
        # core.autocrlf=true would install CRLF files and make every project file look modified
        $cacheEol = Invoke-GitQuiet @('-C', $cache, 'config', '--get', 'core.autocrlf')
        if (($cacheEol.Output | Select-Object -First 1) -ne 'false') {
            Remove-Item -LiteralPath $cache -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
    if (Test-Path -LiteralPath (Join-Path $cache '.git')) {
        $fetched = Invoke-GitQuiet @('-C', $cache, 'fetch', '--quiet', '--depth', '1', 'origin', $Branch)
        if ($fetched.ExitCode -eq 0) {
            $reset = Invoke-GitQuiet @('-C', $cache, 'reset', '--hard', '--quiet', 'FETCH_HEAD')
            if ($reset.ExitCode -ne 0) { $fetched = $reset }
        }
        if ($fetched.ExitCode -ne 0) {
            Remove-Item -LiteralPath $cache -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
    if (-not (Test-Path -LiteralPath (Join-Path $cache '.git'))) {
        $repoUrl = "https://github.com/$Repo"
        $cloned = Invoke-GitQuiet @('-c', 'core.autocrlf=false', '-c', 'core.eol=lf', 'clone', '--quiet', '--depth', '1', '--branch', $Branch, $repoUrl, $cache)
        if ($cloned.ExitCode -eq 0) {
            [void](Invoke-GitQuiet @('-C', $cache, 'config', 'core.autocrlf', 'false'))
            [void](Invoke-GitQuiet @('-C', $cache, 'config', 'core.eol', 'lf'))
        } else {
            Write-Host '[skip] 无法拉取规范包仓库（离线或本机无该仓库凭据），保持当前版本'
            Write-Log $logFile (@("跳过：git 拉取失败 $repoUrl") + @($cloned.Output | Select-Object -First 5) + @("当前版本：$localVersion"))
            exit 2
        }
    }
    if (Test-Path -LiteralPath (Join-Path $cache 'app-agent\install\install.ps1')) {
        $sourceRoot = Join-Path $cache 'app-agent'
    } elseif (Test-Path -LiteralPath (Join-Path $cache 'install\install.ps1')) {
        $sourceRoot = $cache
    } else {
        Write-Host '[skip] 拉取到的仓库中没有 app-agent\install\install.ps1'
        Write-Log $logFile @('跳过：拉取内容缺少 app-agent\install\install.ps1')
        exit 2
    }
}

$remoteVersion = Read-VersionFile (Join-Path $sourceRoot 'VERSION')
Write-Info ("版本: 当前 {0} / 远端 {1}" -f $localVersion, $remoteVersion)

if (-not $Force -and $remoteVersion -le $localVersion) {
    Write-Info ("[ok] 已是最新（{0}）" -f $localVersion)
    Write-Log $logFile @("已是最新：$localVersion")
    exit 0
}
if ($Check) {
    Write-Host ("[更新可用] 当前 {0} -> 远端 {1}；去掉 -Check 即可升级。" -f $localVersion, $remoteVersion)
    Write-Log $logFile @("检测到新版本：$localVersion -> $remoteVersion")
    exit 0
}

$installArgs = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $sourceRoot 'install\install.ps1'), '-TargetDir', $target)
if ($SkipDocs) { $installArgs += '-SkipDocs' }
if ($ForceBrandDocs) { $installArgs += '-ForceBrandDocs' }
if ($ForceDesignSystem) { $installArgs += '-ForceDesignSystem' }

Write-Info ("升级 {0} -> {1}" -f $localVersion, $remoteVersion)
$installOutput = & powershell @installArgs 2>&1
$installExit = $LASTEXITCODE
if ($installExit -ne 0) {
    if ($Quiet) {
        Write-Host ('[error] 规范包升级失败（退出码 {0}），详情见 .agent-artifacts\harness-update.log' -f $installExit)
    } else {
        $installOutput | ForEach-Object { Write-Host $_ }
    }
    Write-Log $logFile (@("升级失败（退出码 $installExit）：$localVersion -> $remoteVersion") + @($installOutput | ForEach-Object { [string]$_ }))
    exit 1
}

$summary = @($installOutput | ForEach-Object { [string]$_ } | Where-Object { $_ -match '^\s*\[(warn|note|error)\]|^\s*\[ok\]\s+docs/agent-framework/learnings|harness version stamp' })
Write-Host ("[ok] 规范包已升级 {0} -> {1}" -f $localVersion, $remoteVersion)
$summary | ForEach-Object { Write-Host $_ }
Write-Log $logFile (@("升级成功：$localVersion -> $remoteVersion") + $summary)
exit 0
