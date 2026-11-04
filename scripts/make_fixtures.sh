#!/usr/bin/env bash
# CatByte Synthetic Media Fixture Generator
# Per docs+/plan.md Section 8

set -euo pipefail

OUTPUT_DIR="${1:-fixtures}"
mkdir -p "$OUTPUT_DIR"

if ! command -v ffmpeg &> /dev/null; then
    echo "Warning: ffmpeg not found in PATH."
    echo "Install ffmpeg to generate MP4/AAC fixtures."
    exit 0
fi

echo "Generating fixtures into $OUTPUT_DIR..."

ffmpeg -y -f lavfi -i "testsrc2=size=1920x1080:rate=30" -f lavfi -i "sine=frequency=1000:sample_rate=48000" -t 5 -c:v libx264 -pix_fmt yuv420p -c:a aac -b:a 192k "$OUTPUT_DIR/test_1080p_30fps.mp4"
ffmpeg -y -f lavfi -i "testsrc2=size=1080x1920:rate=30" -f lavfi -i "sine=frequency=440:sample_rate=48000" -t 5 -c:v libx264 -pix_fmt yuv420p -c:a aac -b:a 192k "$OUTPUT_DIR/test_portrait_9_16.mp4"
ffmpeg -y -f lavfi -i "sine=frequency=523.25:sample_rate=48000" -t 10 -c:a aac -b:a 192k "$OUTPUT_DIR/test_audio_only.m4a"
ffmpeg -y -f lavfi -i "testsrc2=size=3840x2160:rate=60" -t 3 -c:v libx264 -pix_fmt yuv420p -an "$OUTPUT_DIR/test_4k_60fps.mp4"

echo "Fixtures generated."

