import cv2
from tqdm import tqdm

from interpreter import InterpreterOnnx, InterpreterTF
from utils import *

SHOW = False

yolo_model = "./models/yolo.onnx"
yolo_interpreter = InterpreterOnnx(yolo_model)

yawn_model = "./models/yawn.h5"
yawn_interpreter = InterpreterTF(yawn_model, type="yawn")

eye_model = "./models/eye.h5"
eye_interpreter = InterpreterTF(eye_model, type="eye")

# video_path = "data/Fold1_part1/05/0.MOV"
# video_path = "data/YawDD dataset/Mirror/Male_mirror Avi Videos/7-MaleGlasses-Normal.avi"
# video_path = "data/YawDD dataset/Mirror/Male_mirror Avi Videos/7-MaleGlasses-Talking.avi"
video_path = "data/YawDD dataset/Mirror/Male_mirror Avi Videos/7-MaleGlasses-Yawning.avi"
out_json_name = "data_yawning7.json"

cap = cv2.VideoCapture(video_path)

counter = 0
out_frames = []
pbar = tqdm(total = 1000)
data = {
    "yawn": [],
    "eye_left": [],
    "eye_right": [],
}
while cap.isOpened():
    ret, frame = cap.read()
    if not ret:
        print("Error")
        break
    counter += 1
    # if counter < 5000:
    #     continue
    # if counter > 1000:
    #     break

    frame = cv2.resize(frame, (640,640))
    # frame = cv2.flip(frame, 0)
    yolo_frame = preprocess_yolo_onnx(frame)
    face = yolo_interpreter.predict(yolo_frame)[0]

    if len(face) == 0:
        if SHOW:
            cv2.imshow("out", frame)
        continue

    out_frame, face_bbox, eye_keypoints = draw_face(frame, face, type="onnx")

    yawn_frame = preprocess_yawn(frame, face_bbox, type="tf")
    yawn_output = yawn_interpreter.predict(yawn_frame).squeeze()

    yawn_class = yawn_output > 0.5
    # out_frame = draw_yawn(out_frame, yawn_class)

    if len(eye_keypoints) == 2:
        eye_frame_left = preprocess_eye(frame, face_bbox, eye_keypoints[0], type="tf")
        eye_frame_right = preprocess_eye(frame, face_bbox, eye_keypoints[1], type="tf")

        eye_left_output = eye_interpreter.predict(eye_frame_left).squeeze()
        eye_left_class = eye_left_output > 0.5

        eye_right_output = eye_interpreter.predict(eye_frame_right).squeeze()
        eye_right_class = eye_right_output > 0.5

        # out_frame = draw_eye(out_frame, [eye_left_class, eye_right_class])
        data["yawn"].append(int(yawn_class))
        data["eye_left"].append(int(eye_left_class))
        data["eye_right"].append(int(eye_right_class))

    # out_frames.append(out_frame)
    pbar.update(1)

    if SHOW:
        cv2.imshow("out", out_frame)
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

import json
json_object = json.dumps(data, indent=4)
with open(out_json_name, "w") as outfile:
    outfile.write(json_object)

# temp = video_path.split("/")
# out_video_path = "/".join(temp[:-1]) +"/"+ temp[-1].split(".")[0]+"_predicted.mp4"
# out_video_path = "test2.mp4"
# print("Saving video:", out_video_path)

# fourcc = cv2.VideoWriter_fourcc(*'mp4v') 
# video = cv2.VideoWriter(out_video_path, fourcc, 24, (640,640))

# for img in out_frames:
#     video.write(img)

# video.release()
cap.release()
cv2.destroyAllWindows()