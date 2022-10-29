import cv2
import os
from tqdm import tqdm

print("Reading train annotations")
data_train = {}
with open("wider_face_split/wider_face_train_bbx_gt.txt") as f:
    lines = f.readlines()
    i = 0
    while i < len(lines):
        line = lines[i].rstrip().split()
        if len(line) == 1:
            img_path = line[0]
            i += 1
            line = lines[i].rstrip().split()
            num_faces = int(line[0])
            data_train[img_path] = {
                "num_faces": num_faces,
                "bboxs": []
            }
            img_shape = cv2.imread(os.path.join("WIDER_train/images", img_path)).shape
        else:
            annotations = [int(x) for x in line]
            annotations[0] /= img_shape[1]
            annotations[1] /= img_shape[0]
            annotations[2] /= img_shape[1]
            annotations[3] /= img_shape[0]
            data_train[img_path]["bboxs"].append(annotations)
        i += 1

print("Reading val annotations")
data_val = {}
with open("wider_face_split/wider_face_val_bbx_gt.txt") as f:
    lines = f.readlines()
    i = 0
    while i < len(lines):
        line = lines[i].rstrip().split()
        if len(line) == 1:
            img_path = line[0]
            i += 1
            line = lines[i].rstrip().split()
            num_faces = int(line[0])
            data_val[img_path] = {
                "num_faces": num_faces,
                "bboxs": []
            }
            img_shape = cv2.imread(os.path.join("WIDER_val/images", img_path)).shape
        else:
            annotations = [int(x) for x in line]
            annotations[0] /= img_shape[1]
            annotations[1] /= img_shape[0]
            annotations[2] /= img_shape[1]
            annotations[3] /= img_shape[0]
            data_val[img_path]["bboxs"].append(annotations)

        i += 1

print("Creating train images/labels")
for img_path in tqdm(data_train):
    curr_data = data_train[img_path]
    img = cv2.imread(os.path.join("WIDER_train/images", img_path))
    img = cv2.resize(img, (640, 640))
    new_img_path = img_path.split("/")[1]

    cv2.imwrite(os.path.join("./custom_dataset/images/train", new_img_path), img)
    out_path = os.path.join("./custom_dataset/labels/train", f"{new_img_path.split('.')[0]}.txt")
    with open(out_path, "w+") as f:
        for bbox in curr_data["bboxs"]:
            new_bbox = [0]
            new_bbox.extend(bbox)
            out_str = " ".join([str(x) for x in new_bbox[:5]])
            f.write(f"{out_str}\n")

print("Creating val images/labels")
for img_path in tqdm(data_val):
    curr_data = data_val[img_path]
    img = cv2.imread(os.path.join("WIDER_val/images", img_path))
    img = cv2.resize(img, (640, 640))
    new_img_path = img_path.split("/")[1]

    cv2.imwrite(os.path.join("./custom_dataset/images/val", new_img_path), img)
    out_path = os.path.join("./custom_dataset/labels/val", f"{new_img_path.split('.')[0]}.txt")
    with open(out_path, "w+") as f:
        for bbox in curr_data["bboxs"]:
            new_bbox = [0]
            new_bbox.extend(bbox)
            out_str = " ".join([str(x) for x in new_bbox[:5]])
            f.write(f"{out_str}\n")
