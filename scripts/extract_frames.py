import os
import sys

import cv2

video_path = sys.argv[1]
out_dir = sys.argv[2]
step_seconds = float(sys.argv[3]) if len(sys.argv) > 3 else 1.0
max_frames = int(sys.argv[4]) if len(sys.argv) > 4 else 60

os.makedirs(out_dir, exist_ok=True)
cap = cv2.VideoCapture(video_path)
if not cap.isOpened():
    print("ERROR: cannot open video")
    sys.exit(1)

fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
total = int(cap.get(cv2.CAP_PROP_FRAME_COUNT) or 0)
duration = total / fps if fps else 0.0
print(f"fps={fps:.3f} frames={total} duration={duration:.2f}s")

step = max(1, int(round(fps * step_seconds)))
saved = 0
frame_index = 0
while saved < max_frames:
    cap.set(cv2.CAP_PROP_POS_FRAMES, frame_index)
    ok, frame = cap.read()
    if not ok:
        break
    height, width = frame.shape[:2]
    scale = min(1.0, 900.0 / max(width, height))
    if scale < 1.0:
        frame = cv2.resize(frame, (int(width * scale), int(height * scale)))
    out_path = os.path.join(out_dir, f"frame_{frame_index:05d}.png")
    cv2.imwrite(out_path, frame)
    saved += 1
    frame_index += step

print(f"saved={saved}")
cap.release()
