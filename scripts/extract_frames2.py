import os
import sys

import cv2

video_path = r"C:\Users\HP 840G5-i7\Downloads\video_2026-11-07_00-41-05.mp4"
out_dir = os.path.join(os.environ["TEMP"], "catbyte_frames2")
os.makedirs(out_dir, exist_ok=True)

cap = cv2.VideoCapture(video_path)
fps = cap.get(cv2.CAP_PROP_FPS) or 30.0

for t in range(45, 192, 3):
    cap.set(cv2.CAP_PROP_POS_MSEC, t * 1000)
    ok, frame = cap.read()
    if not ok:
        break
    h, w = frame.shape[:2]
    s = min(1.0, 900.0 / max(w, h))
    if s < 1.0:
        frame = cv2.resize(frame, (int(w * s), int(h * s)))
    cv2.imwrite(os.path.join(out_dir, f"f_{t:03d}.png"), frame)

print("done")
cap.release()
