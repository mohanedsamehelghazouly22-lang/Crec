#!/usr/bin/env python3
"""Inspect a gameplay recording and emit sampling metadata; this tool never invents card labels."""
import argparse, json
import cv2

p=argparse.ArgumentParser(); p.add_argument('video'); p.add_argument('--step',type=float,default=5.0); a=p.parse_args()
cap=cv2.VideoCapture(a.video)
if not cap.isOpened(): raise SystemExit('cannot open video')
fps=cap.get(cv2.CAP_PROP_FPS) or 0
frames=int(cap.get(cv2.CAP_PROP_FRAME_COUNT)); w=int(cap.get(cv2.CAP_PROP_FRAME_WIDTH)); h=int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
duration=frames/fps if fps else 0
samples=[]; t=0.0
while t<=duration:
    cap.set(cv2.CAP_PROP_POS_MSEC,t*1000); ok,frame=cap.read()
    if not ok: break
    samples.append({'time_s':round(t,3),'width':frame.shape[1],'height':frame.shape[0]})
    t+=a.step
cap.release()
print(json.dumps({'fps':fps,'frames':frames,'width':w,'height':h,'duration_s':duration,'samples':samples},indent=2))
