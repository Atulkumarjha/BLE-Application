<#
Install an APK to a connected Android device and capture `adb logcat` to `logs/adb_logcat.log`.
Usage:
  .\tools\install_and_log.ps1 -ApkPath path\to\app-debug.apk

If `-ApkPath` is omitted, the script will look for `build\app\outputs\flutter-apk\app-debug.apk`.
#>

param(
    [string]$ApkPath = "",
    [int]$DeviceIndex = 0
)

Set-StrictMode -Version Latest

$root = Resolve-Path -Path .
$logsDir = Join-Path $root 'logs'
if (-not (Test-Path $logsDir)) { New-Item -ItemType Directory -Path $logsDir | Out-Null }

function Check-Command($cmd) {
    try { Get-Command $cmd -ErrorAction Stop | Out-Null; return $true } catch { return $false }
}

if (-not (Check-Command adb)) { Write-Error "adb not found on PATH. Install platform-tools and ensure adb is available."; exit 2 }

if (-not $ApkPath) {
    $default = Join-Path $root "build\app\outputs\flutter-apk\app-debug.apk"
    if (Test-Path $default) { $ApkPath = $default } else { Write-Error "No APK path provided and default APK not found: $default"; exit 2 }
}

if (-not (Test-Path $ApkPath)) { Write-Error "APK not found at $ApkPath"; exit 2 }

Write-Output "Installing APK: $ApkPath"
& adb install -r $ApkPath

Write-Output "Installed. Starting adb logcat to logs/adb_logcat.log (Ctrl+C to stop)"
& adb logcat -v time > (Join-Path $logsDir 'adb_logcat.log')
