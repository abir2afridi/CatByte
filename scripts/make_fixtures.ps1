# CatByte Synthetic Media Fixture Generator
# Per docs+/plan.md Section 8: Generates synthetic media for playback, decode, and export testing.

param(
    [string]$OutputDir = "fixtures",
    [switch]$Force
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$ffmpegCmd = Get-Command ffmpeg -ErrorAction SilentlyContinue

if (-not $ffmpegCmd) {
    Write-Warning "ffmpeg was not found in PATH. Synthetic media fixtures can be generated with ffmpeg or via in-app MediaCodec instrumented tests."
    Write-Host "Install ffmpeg (e.g. 'winget install Gyan.FFmpeg' or 'choco install ffmpeg') to generate MP4/AAC test files directly."
    exit 0
}

Write-Host "Found ffmpeg: $($ffmpegCmd.Source)"
Write-Host "Generating test fixtures into: $OutputDir"

# 1. 1080p 30fps landscape video with testsrc2 and 1kHz sine wave
$fixture1 = Join-Path $OutputDir "test_1080p_30fps.mp4"
if ($Force -or -not (Test-Path $fixture1)) {
    Write-Host "Generating: test_1080p_30fps.mp4 (5s, 1920x1080, 30fps)"
    & ffmpeg -y -f lavfi -i "testsrc2=size=1920x1080:rate=30" -f lavfi -i "sine=frequency=1000:sample_rate=48000" -t 5 -c:v libx264 -pix_fmt yuv420p -c:a aac -b:a 192k $fixture1
}

# 2. 9:16 portrait video (1080x1920) for TikTok/Reels testing
$fixture2 = Join-Path $OutputDir "test_portrait_9_16.mp4"
if ($Force -or -not (Test-Path $fixture2)) {
    Write-Host "Generating: test_portrait_9_16.mp4 (5s, 1080x1920, 30fps)"
    & ffmpeg -y -f lavfi -i "testsrc2=size=1080x1920:rate=30" -f lavfi -i "sine=frequency=440:sample_rate=48000" -t 5 -c:v libx264 -pix_fmt yuv420p -c:a aac -b:a 192k $fixture2
}

# 3. Audio-only AAC file (10s music simulation)
$fixture3 = Join-Path $OutputDir "test_audio_only.m4a"
if ($Force -or -not (Test-Path $fixture3)) {
    Write-Host "Generating: test_audio_only.m4a (10s, 48kHz stereo AAC)"
    & ffmpeg -y -f lavfi -i "sine=frequency=523.25:sample_rate=48000" -t 10 -c:a aac -b:a 192k $fixture3
}

# 4. 4K 60fps landscape video
$fixture4 = Join-Path $OutputDir "test_4k_60fps.mp4"
if ($Force -or -not (Test-Path $fixture4)) {
    Write-Host "Generating: test_4k_60fps.mp4 (3s, 3840x2160, 60fps)"
    & ffmpeg -y -f lavfi -i "testsrc2=size=3840x2160:rate=60" -t 3 -c:v libx264 -pix_fmt yuv420p -an $fixture4
}

Write-Host "All test fixtures generated successfully."

