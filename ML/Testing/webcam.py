import cv2
import os
import numpy as np
import tensorflow as tf

from interpreter import Interpreter
from utils import *


yolo_model = "./models/yolo.tflite"
yolo_interpreter = Interpreter(yolo_model, output_index=3)

yawn_model = "./models/yawn.tflite"
yawn_interpreter = Interpreter(yawn_model)

eye_model = "./models/eye.tflite"
eye_interpreter = Interpreter(eye_model)

vid = cv2.VideoCapture(1)

while(True):
    ret, frame = vid.read()

    yolo_frame = preprocess_yolo(frame)
    yolo_output = yolo_interpreter.predict(yolo_frame)[0]

    face = postprocess_yolo(yolo_output)
    if np.all(face == -1):
        cv2.imshow("out", frame)

    out_frame, face_bbox, eye_keypoints = draw_face(frame, face)

    yawn_frame = preprocess_yawn(frame, face_bbox)
    yawn_output = yawn_interpreter.predict(yawn_frame).squeeze()

    yawn_class = yawn_output > 0.5

    out_frame = draw_yawn(out_frame, yawn_class)

    eye_frame_left = preprocess_eye(frame, face_bbox, eye_keypoints[0])
    eye_frame_right = preprocess_eye(frame, face_bbox, eye_keypoints[1])

    eye_left_output = eye_interpreter.predict(eye_frame_left).squeeze()
    eye_left_class = eye_left_output > 0.5

    eye_right_output = eye_interpreter.predict(eye_frame_right).squeeze()
    eye_right_class = eye_right_output > 0.5

    out_frame = draw_eye(out_frame, [eye_left_class, eye_right_class])

    cv2.imshow("out", out_frame)

    if cv2.waitKey(1) & 0xFF == ord('q'):
        break
  
# After the loop release the cap object
vid.release()
# Destroy all the windows
cv2.destroyAllWindows()