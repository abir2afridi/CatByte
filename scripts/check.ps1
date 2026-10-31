<# 
.SYNOPSIS
    CatByte Local Verification Pipeline for Windows PowerShell
.DESCRIPTION
    Run this before every commit: .\scripts\check.ps1
    Equivalent to check.sh for Linux/macOS
#>

param(
    [switch]$SkipTests,
    [switch]$SkipLint
)

$ErrorActionPreference = "Stop"
$PROJECT_ROOT = Split-Path -Parent $MyInvocation.MyCommand.Definition | Join-Path -ChildPath ".."
Set-Location $PROJECT_ROOT

$OVERALL_STATUS = 0

function Run-Step {
    param(
        [string]$Name,
        [string]$Command
    )
    Write-Host "▶ $Name"
    try {
        Invoke-Expression $Command
        Write-Host "✓ $Name passed`n"
        return $true
    } catch {
        Write-Host "✗ $Name FAILED`n"
        $script:OVERALL_STATUS = 1
        return $false
    }
}

Write-Host "========================================"
Write-Host "  CatByte Local Verification Pipeline  "
Write-Host "========================================"

# 1. Format check (spotless/ktlint)
Run-Step "Code Format (spotless)" ".\gradlew :app:spotlessCheck --no-daemon"

# 2. Static analysis (detekt)
if (-not $SkipLint) {
    Run-Step "Static Analysis (detekt)" ".\gradlew :app:detekt --no-daemon"
}

# 3. Android Lint
if (-not $SkipLint) {
    Run-Step "Android Lint" ".\gradlew :app:lintDebug --no-daemon"
}

# 4. Compile Debug (Kotlin + Java)
Run-Step "Compile Debug" ".\gradlew :app:compileDebugKotlin --no-daemon"

# 5. Unit Tests (JVM)
if (-not $SkipTests) {
    Run-Step "Unit Tests" ".\gradlew :app:testDebugUnitTest --no-daemon"
}

# 6. Assemble Debug APK
Run-Step "Assemble Debug APK" ".\gradlew :app:assembleDebug --no-daemon"

# 7. Verify APK exists and reasonable size
$APK_PATH = "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $APK_PATH) {
    $APK_SIZE = (Get-Item $APK_PATH).Length
    $APK_SIZE_MB = [math]::Round($APK_SIZE / 1MB, 2)
    Write-Host "✓ APK generated: $APK_PATH ($APK_SIZE_MB MB)"
    
    if ($APK_SIZE -gt 104857600) {
        Write-Host "⚠ APK size exceeds 100MB budget"
    }
} else {
    Write-Host "✗ APK not found at $APK_PATH"
    $OVERALL_STATUS = 1
}

# 8. Dependency verification
Run-Step "Dependency Insight (Media3)" ".\gradlew :app:dependencyInsight --dependency media3-exoplayer --configuration releaseRuntimeClasspath --no-daemon"

# Summary
Write-Host "========================================"
if ($OVERALL_STATUS -eq 0) {
    Write-Host "  ALL CHECKS PASSED ✓"
} else {
    Write-Host "  SOME CHECKS FAILED ✗"
}
Write-Host "========================================"

exit $OVERALL_STATUS
