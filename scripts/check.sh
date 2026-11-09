#!/usr/bin/env bash
# CatByte Local Verification Pipeline
# Run this before every commit: ./scripts/check.sh
# Works on Linux, macOS, and Windows (via Git Bash or WSL)

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  CatByte Local Verification Pipeline  ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Track overall status
OVERALL_STATUS=0

run_step() {
    local name="$1"
    local cmd="$2"
    echo -e "${BLUE}▶ ${name}${NC}"
    if eval "$cmd"; then
        echo -e "${GREEN}✓ ${name} passed${NC}"
        echo ""
        return 0
    else
        echo -e "${RED}✗ ${name} FAILED${NC}"
        echo ""
        OVERALL_STATUS=1
        return 1
    fi
}

# 1. Format check (ktlint/spotless)
run_step "Code Format (ktlint)" "./gradlew :app:spotlessCheck --no-daemon"

# 2. Static analysis (detekt)
run_step "Static Analysis (detekt)" "./gradlew :app:detekt --no-daemon"

# 3. Android Lint
run_step "Android Lint" "./gradlew :app:lintDebug --no-daemon"

# 4. Compile Debug (Kotlin + Java)
run_step "Compile Debug" "./gradlew :app:compileDebugKotlin --no-daemon"

# 5. Unit Tests (JVM)
run_step "Unit Tests" "./gradlew :app:testDebugUnitTest --no-daemon"

# 6. Assemble Debug APK
run_step "Assemble Debug APK" "./gradlew :app:assembleDebug --no-daemon"

# 7. Verify APK exists and reasonable size
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo -e "${GREEN}✓ APK generated: ${APK_PATH} (${APK_SIZE})${NC}"
    
    # Warn if APK > 100MB (adjust budget as needed)
    APK_SIZE_BYTES=$(stat -c%s "$APK_PATH" 2>/dev/null || stat -f%z "$APK_PATH")
    if [ "$APK_SIZE_BYTES" -gt 104857600 ]; then
        echo -e "${YELLOW}⚠ APK size exceeds 100MB budget${NC}"
    fi
else
    echo -e "${RED}✗ APK not found at ${APK_PATH}${NC}"
    OVERALL_STATUS=1
fi
echo ""

# 8. Dependency verification (check for duplicate versions)
run_step "Dependency Insight (Media3)" "./gradlew :app:dependencyInsight --dependency media3-exoplayer --configuration releaseRuntimeClasspath --no-daemon"

# Summary
echo -e "${BLUE}========================================${NC}"
if [ $OVERALL_STATUS -eq 0 ]; then
    echo -e "${GREEN}  ALL CHECKS PASSED ✓${NC}"
else
    echo -e "${RED}  SOME CHECKS FAILED ✗${NC}"
fi
echo -e "${BLUE}========================================${NC}"

exit $OVERALL_STATUS
