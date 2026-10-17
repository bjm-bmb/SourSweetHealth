#Requires -Version 5.1
<#
.SYNOPSIS
  真机自动化测试：等待设备 -> 构建 -> 安装 -> 启动 -> UI/冒烟验证 -> 截图与日志取证。

.DESCRIPTION
  在 Android 项目根目录执行。脚本会先等待手机就绪（提示用户连接并授权 USB 调试），
  然后安装调试包、启动主界面、检查进程存活与崩溃日志；若存在 app\src\androidTest
  则继续执行 connectedDebugAndroidTest 做 UI 断言。证据写入 -ArtifactsDir。

  退出码：0 = PASS，1 = FAIL，2 = BLOCKED（环境缺失或设备未连接）。

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools\device_test.ps1 -ProjectDir .

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools\device_test.ps1 -ProjectDir . -Serial R58M12345AB -WaitSeconds 600
#>
[CmdletBinding()]
param(
    [string]$ProjectDir = '.',
    [string]$Serial = '',
    [string]$ApkPath = '',
    [string]$ArtifactsDir = '',
    [switch]$SkipBuild,
    [switch]$SkipInstrumented,
    [int]$WaitSeconds = 300,
    [int]$LaunchWaitSeconds = 5
)

$ErrorActionPreference = 'Continue'

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$steps = New-Object System.Collections.ArrayList
$script:artifactFiles = New-Object System.Collections.ArrayList
$script:verdict = 'PASS'

function Write-Step([string]$name) {
    Write-Host ''
    Write-Host "==> $name"
}

function Add-Result([string]$name, [string]$status, [string]$detail) {
    [void]$steps.Add([pscustomobject]@{ step = $name; status = $status; detail = $detail })
    $color = 'Gray'
    if ($status -eq 'FAIL') { $color = 'Red' }
    elseif ($status -eq 'BLOCKED') { $color = 'Yellow' }
    elseif ($status -eq 'PASS') { $color = 'Green' }
    Write-Host ("    [{0}] {1}{2}" -f $status, $name, $(if ($detail) { " - $detail" } else { '' })) -ForegroundColor $color
}

function Save-Text([string]$fileName, [string]$text) {
    $path = Join-Path $script:artifacts $fileName
    [System.IO.File]::WriteAllText($path, $text, $utf8NoBom)
    [void]$script:artifactFiles.Add($path)
    return $path
}

function Complete-Run([string]$verdict, [string]$reason, [int]$exitCode) {
    $script:verdict = $verdict
    $summary = [ordered]@{
        verdict     = $verdict
        reason      = $reason
        timestamp   = (Get-Date).ToString('o')
        project     = $script:projectRoot
        device      = $script:deviceSerial
        model       = $script:deviceModel
        apiLevel    = $script:deviceApi
        package     = $script:packageName
        apk         = $script:apk
        artifacts   = @($script:artifactFiles)
        steps       = @($steps)
    }
    $jsonPath = Join-Path $script:artifacts 'result.json'
    ($summary | ConvertTo-Json -Depth 6) | Out-File -LiteralPath $jsonPath -Encoding utf8
    Write-Host ''
    if ($verdict -eq 'PASS') {
        Write-Host "RESULT: PASS - $reason" -ForegroundColor Green
    } elseif ($verdict -eq 'FAIL') {
        Write-Host "RESULT: FAIL - $reason" -ForegroundColor Red
    } else {
        Write-Host "RESULT: BLOCKED - $reason" -ForegroundColor Yellow
    }
    Write-Host "证据目录: $script:artifacts"
    exit $exitCode
}

# ---------------------------------------------------------------- 环境解析
if (-not (Test-Path -LiteralPath $ProjectDir)) {
    Write-Host "项目目录不存在: $ProjectDir" -ForegroundColor Red
    exit 2
}
$script:projectRoot = (Resolve-Path -LiteralPath $ProjectDir).Path

if (-not $ArtifactsDir) { $ArtifactsDir = Join-Path $script:projectRoot '.agent-artifacts\device-test' }
$script:artifacts = $ArtifactsDir
New-Item -ItemType Directory -Force -Path $script:artifacts | Out-Null
$script:artifacts = (Resolve-Path -LiteralPath $script:artifacts).Path

$script:deviceSerial = ''
$script:deviceModel = ''
$script:deviceApi = ''
$script:packageName = ''
$script:apk = ''

Write-Host "项目   : $script:projectRoot"
Write-Host "证据   : $script:artifacts"

# JAVA_HOME / ANDROID_HOME 默认值（与 docs/android 基线一致）
if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath 'C:\Android\jdk17\jdk17')) {
    $env:JAVA_HOME = 'C:\Android\jdk17\jdk17'
}
if (-not $env:ANDROID_HOME -and (Test-Path -LiteralPath 'C:\Android\Sdk')) {
    $env:ANDROID_HOME = 'C:\Android\Sdk'
}

$adb = $null
$adbCmd = Get-Command adb -ErrorAction SilentlyContinue
if ($adbCmd) {
    $adb = $adbCmd.Source
} else {
    $candidates = New-Object System.Collections.ArrayList
    if ($env:ANDROID_HOME) { [void]$candidates.Add((Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe')) }
    if ($env:ANDROID_SDK_ROOT) { [void]$candidates.Add((Join-Path $env:ANDROID_SDK_ROOT 'platform-tools\adb.exe')) }
    [void]$candidates.Add('C:\Android\Sdk\platform-tools\adb.exe')
    foreach ($c in $candidates) {
        if (Test-Path -LiteralPath $c) { $adb = $c; break }
    }
}
if (-not $adb) {
    Add-Result 'adb 可用' 'BLOCKED' '未找到 adb（ANDROID_HOME\platform-tools 或 PATH）'
    Complete-Run 'BLOCKED' 'adb 不可用，无法执行真机测试' 2
}
Add-Result 'adb 可用' 'PASS' $adb

$gradlew = Join-Path $script:projectRoot 'gradlew.bat'
if (-not (Test-Path -LiteralPath $gradlew)) {
    Add-Result 'Gradle Wrapper' 'BLOCKED' '缺少 gradlew.bat'
    Complete-Run 'BLOCKED' '缺少 gradlew.bat' 2
}

# ---------------------------------------------------------------- 等待设备
function Get-ConnectedDevices {
    $output = & $adb devices 2>&1 | Out-String
    $list = New-Object System.Collections.ArrayList
    foreach ($line in ($output -split "`r?`n")) {
        $trimmed = $line.Trim()
        if (-not $trimmed) { continue }
        if ($trimmed -like 'List of devices*') { continue }
        if ($trimmed -match '^(\S+)\s+(.+)$') {
            [void]$list.Add([pscustomobject]@{ Serial = $Matches[1]; State = $Matches[2].Trim() })
        }
    }
    return $list
}

Write-Step '等待真机连接'
Write-Host '    请连接手机并开启「USB 调试」；手机上弹出授权提示时请点「允许」。'
$deadline = (Get-Date).AddSeconds($WaitSeconds)
$lastNotice = $null
$selected = $null
while ($true) {
    $devices = Get-ConnectedDevices
    if ($Serial) {
        $devices = @($devices | Where-Object { $_.Serial -eq $Serial })
    }
    $ready = @($devices | Where-Object { $_.State -eq 'device' })
    if ($ready.Count -gt 0) {
        $selected = $ready[0].Serial
        break
    }
    if ((Get-Date) -ge $deadline) { break }
    $unauthorized = @($devices | Where-Object { $_.State -like '*unauthorized*' })
    if ($unauthorized.Count -gt 0 -and $lastNotice -ne 'unauthorized') {
        Write-Host '    检测到未授权设备：请在手机弹窗中点「允许 USB 调试」。' -ForegroundColor Yellow
        $lastNotice = 'unauthorized'
    } elseif ($devices.Count -eq 0 -and $lastNotice -ne 'none') {
        Write-Host '    尚未检测到设备…' -ForegroundColor Yellow
        $lastNotice = 'none'
    }
    $remaining = [int](($deadline - (Get-Date)).TotalSeconds)
    Write-Host ("    等待中，剩余 {0} 秒" -f $remaining)
    Start-Sleep -Seconds 10
}

if (-not $selected) {
    Add-Result '设备就绪' 'BLOCKED' "等待 ${WaitSeconds}s 仍未检测到可用设备"
    Complete-Run 'BLOCKED' '设备未连接（请连接手机并授权 USB 调试后重试）' 2
}
$script:deviceSerial = $selected
Add-Result '设备就绪' 'PASS' $selected

$script:deviceModel = (& $adb -s $selected shell getprop ro.product.model 2>&1 | Out-String).Trim()
$script:deviceApi = (& $adb -s $selected shell getprop ro.build.version.sdk 2>&1 | Out-String).Trim()
Write-Host ("    设备型号: {0} (API {1})" -f $script:deviceModel, $script:deviceApi)
$env:ANDROID_SERIAL = $selected

# ---------------------------------------------------------------- 构建
Write-Step '构建调试包'
if ($SkipBuild) {
    Add-Result '构建' 'SKIP' '按 -SkipBuild 跳过'
} else {
    Push-Location -LiteralPath $script:projectRoot
    $buildOutput = (& $gradlew assembleDebug 2>&1 | Out-String)
    $buildExit = $LASTEXITCODE
    Pop-Location
    Save-Text 'build.log' $buildOutput | Out-Null
    if ($buildExit -ne 0) {
        Add-Result '构建' 'FAIL' "assembleDebug 退出码 $buildExit"
        Complete-Run 'FAIL' '构建失败，详见 build.log' 1
    }
    Add-Result '构建' 'PASS' 'assembleDebug 退出码 0'
}

# ---------------------------------------------------------------- 定位 APK
Write-Step '定位 APK'
if ($ApkPath) {
    if (-not (Test-Path -LiteralPath $ApkPath)) {
        Add-Result 'APK' 'FAIL' "指定 APK 不存在: $ApkPath"
        Complete-Run 'FAIL' 'APK 不存在' 1
    }
    $script:apk = (Resolve-Path -LiteralPath $ApkPath).Path
} else {
    $apkRoot = Join-Path $script:projectRoot 'app\build\outputs\apk'
    $found = @(Get-ChildItem -Path $apkRoot -Filter *.apk -Recurse -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending)
    if ($found.Count -eq 0) {
        Add-Result 'APK' 'FAIL' "未找到 APK: $apkRoot"
        Complete-Run 'FAIL' '未找到构建产物，请先执行 assembleDebug' 1
    }
    $script:apk = $found[0].FullName
}
Add-Result 'APK' 'PASS' $script:apk

# ---------------------------------------------------------------- 解析包名
Write-Step '解析 applicationId'
foreach ($candidate in @('app\build.gradle.kts', 'app\build.gradle')) {
    $buildFile = Join-Path $script:projectRoot $candidate
    if (-not (Test-Path -LiteralPath $buildFile)) { continue }
    $buildText = [System.IO.File]::ReadAllText($buildFile, [System.Text.Encoding]::UTF8)
    $patterns = @(
        'applicationId\s*=?\s*"([^"]+)"',
        "applicationId\s*=?\s*'([^']+)'",
        'namespace\s*=?\s*"([^"]+)"',
        "namespace\s*=?\s*'([^']+)'"
    )
    foreach ($p in $patterns) {
        $m = [regex]::Match($buildText, $p)
        if ($m.Success) { $script:packageName = $m.Groups[1].Value; break }
    }
    if ($script:packageName) { break }
}
if (-not $script:packageName) {
    Add-Result 'applicationId' 'FAIL' '未能从 app/build.gradle(.kts) 解析 applicationId'
    Complete-Run 'FAIL' '无法确定目标包名' 1
}
Add-Result 'applicationId' 'PASS' $script:packageName

# ---------------------------------------------------------------- 安装
Write-Step '安装到真机'
$installOutput = (& $adb -s $selected install -r -t $script:apk 2>&1 | Out-String)
Save-Text 'install.log' $installOutput | Out-Null
if ($installOutput -notmatch 'Success') {
    Add-Result '安装' 'FAIL' ($installOutput.Trim() -replace "`r?`n", ' | ')
    Complete-Run 'FAIL' 'APK 安装失败' 1
}
Add-Result '安装' 'PASS' 'install Success'

# ---------------------------------------------------------------- 启动
Write-Step '启动主界面'
& $adb -s $selected logcat -c | Out-Null
$resolveOutput = (& $adb -s $selected shell cmd package resolve-activity --brief $script:packageName 2>&1 | Out-String)
$component = ($resolveOutput -split "`r?`n" | Where-Object { $_ -match '/' } | Select-Object -Last 1)
$component = $component.Trim()

$launchOutput = ''
if ($component) {
    $launchOutput = (& $adb -s $selected shell am start -W -n $component 2>&1 | Out-String)
} else {
    $launchOutput = (& $adb -s $selected shell monkey -p $script:packageName -c android.intent.category.LAUNCHER 1 2>&1 | Out-String)
}
Save-Text 'launch.log' $launchOutput | Out-Null
if ($launchOutput -match 'Error type|does not exist|No activities found') {
    Add-Result '启动' 'FAIL' ($launchOutput.Trim() -replace "`r?`n", ' | ')
    Complete-Run 'FAIL' '主界面启动失败' 1
}
Add-Result '启动' 'PASS' $(if ($component) { $component } else { 'monkey LAUNCHER' })

Start-Sleep -Seconds $LaunchWaitSeconds

# ---------------------------------------------------------------- 进程存活
Write-Step '进程存活检查'
$pidOutput = (& $adb -s $selected shell pidof $script:packageName 2>&1 | Out-String).Trim()
if (-not $pidOutput) {
    Add-Result '进程存活' 'FAIL' '启动后未找到进程（可能已崩溃）'
    Complete-Run 'FAIL' '应用进程不存在' 1
}
Add-Result '进程存活' 'PASS' "pid=$pidOutput"

# ---------------------------------------------------------------- UI 断言
Write-Step '真机 UI 自动化测试'
$androidTestDir = Join-Path $script:projectRoot 'app\src\androidTest'
$instrumentedFiles = 0
if (Test-Path -LiteralPath $androidTestDir) {
    $instrumentedFiles = @(Get-ChildItem -Path $androidTestDir -Recurse -File -ErrorAction SilentlyContinue).Count
}
if ($SkipInstrumented) {
    Add-Result 'UI 测试' 'SKIP' '按 -SkipInstrumented 跳过'
} elseif ($instrumentedFiles -eq 0) {
    Add-Result 'UI 测试' 'SKIP' 'app/src/androidTest 无测试代码，仅执行冒烟验证'
} else {
    Push-Location -LiteralPath $script:projectRoot
    $testOutput = (& $gradlew connectedDebugAndroidTest 2>&1 | Out-String)
    $testExit = $LASTEXITCODE
    Pop-Location
    Save-Text 'connectedDebugAndroidTest.log' $testOutput | Out-Null
    if ($testExit -ne 0) {
        Add-Result 'UI 测试' 'FAIL' "connectedDebugAndroidTest 退出码 $testExit"
        Complete-Run 'FAIL' '真机 UI 测试失败，详见 connectedDebugAndroidTest.log' 1
    }
    Add-Result 'UI 测试' 'PASS' 'connectedDebugAndroidTest 退出码 0'
}

# ---------------------------------------------------------------- 崩溃检查
Write-Step '崩溃与 ANR 检查'
$logcatOutput = (& $adb -s $selected logcat -d -t 20000 -v threadtime 2>&1 | Out-String)
Save-Text 'logcat.txt' $logcatOutput | Out-Null
$crashPattern = 'FATAL EXCEPTION[\s\S]{0,600}?Process:\s*' + [regex]::Escape($script:packageName)
if ($logcatOutput -match $crashPattern) {
    Add-Result '崩溃检查' 'FAIL' 'logcat 中发现本应用 FATAL EXCEPTION'
    Complete-Run 'FAIL' '应用发生崩溃，详见 logcat.txt' 1
}
$anrPattern = 'ANR in ' + [regex]::Escape($script:packageName)
if ($logcatOutput -match $anrPattern) {
    Add-Result '崩溃检查' 'FAIL' 'logcat 中发现本应用 ANR'
    Complete-Run 'FAIL' '应用无响应（ANR），详见 logcat.txt' 1
}
Add-Result '崩溃检查' 'PASS' '未发现 FATAL EXCEPTION / ANR'

# ---------------------------------------------------------------- 取证
Write-Step '采集截图'
$remoteShot = '/sdcard/bjm_device_test.png'
& $adb -s $selected shell screencap -p $remoteShot | Out-Null
$localShot = Join-Path $script:artifacts 'screenshot.png'
$pullOutput = (& $adb -s $selected pull $remoteShot $localShot 2>&1 | Out-String)
& $adb -s $selected shell rm -f $remoteShot | Out-Null
if (Test-Path -LiteralPath $localShot) {
    [void]$script:artifactFiles.Add($localShot)
    Add-Result '截图' 'PASS' $localShot
} else {
    Save-Text 'screencap.log' $pullOutput | Out-Null
    Add-Result '截图' 'WARN' '截图采集失败（不影响其他结论）'
}

Complete-Run 'PASS' '真机验证通过（安装、启动、进程存活、无崩溃）' 0
