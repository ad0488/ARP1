import cv2
import numpy as np

def preprocess_yolo(img):
    IMAGE_SIZE = 640
    IMG_MEAN = 127.5
    IMG_SCALE = 1/127.5

    img = img[:, :, ::-1]
    img = cv2.resize(img, (IMAGE_SIZE,IMAGE_SIZE), interpolation=cv2.INTER_LINEAR)
    img = (img - IMG_MEAN) * IMG_SCALE
    img = np.asarray(img, dtype=np.float32)
    img = np.expand_dims(img,0)
    return img

def preprocess_yolo_onnx(img):
    IMAGE_SIZE = 640
    IMG_MEAN = 127.5
    IMG_SCALE = 1/127.5

    img = img[:, :, ::-1]
    img = cv2.resize(img, (IMAGE_SIZE,IMAGE_SIZE), interpolation=cv2.INTER_LINEAR)
    img = (img - IMG_MEAN) * IMG_SCALE
    img = np.asarray(img, dtype=np.float32)
    img = np.expand_dims(img,0)
    img = img.transpose(0,3,1,2)
    return img


def postprocess_yolo(data, conf_threshold=0.4):
    best_score = -1
    best_box = None
    for i in range(data.shape[0]):
        curr_score = data[i][4]
        if curr_score > best_score:
            best_score = curr_score
            best_box = data[i]
        
    if best_score < conf_threshold:
        return -1
    else:
        return best_box

def draw_face(img, output, type="tflite"):
    img_out = img.copy()
    det_bbox, kpt = output[0:4], output[6:]
    colors = [(255,0,0),(0,255,0),(0,0,255),(255,255,0),(0,255,255)]

    scale_h = img.shape[0] / 640
    scale_w = img.shape[1] / 640
    if type == "tflite":
        det_bbox = [int(det_bbox[0]-det_bbox[2]/2), int(det_bbox[1]-det_bbox[3]/2), int(det_bbox[2]), int(det_bbox[3])]
    elif type == "onnx":
        det_bbox = [int(det_bbox[0]), int(det_bbox[1]), int(det_bbox[2]), int(det_bbox[3])]
    else:
        raise Exception("Invalid type")

    for i in range(4):
        if i%2 == 0:
            det_bbox[i] = int(det_bbox[i] * scale_w)
        else:
            det_bbox[i] = int(det_bbox[i] * scale_h)

    img_out = cv2.rectangle(img_out, (det_bbox[0], det_bbox[1]), (det_bbox[2], det_bbox[3]), (255,0,0), 2)

    eye_keypoints = []
    for i in range(5):
        point_x = int(kpt[3 * i] * scale_w)
        point_y = int(kpt[3 * i + 1] * scale_h)
        
        if i in [0,1]:
            eye_keypoints.append([point_x, point_y])

        conf = kpt[3*i + 2]
        if conf >  0.5:
            cv2.circle(img_out, (point_x, point_y), 2, colors[i], -1)

    return img_out, det_bbox, eye_keypoints


def preprocess_yawn(img, bbox, type="tflite"):
    IMAGE_SIZE = 512
    if type == "tflite":
        img = img[bbox[1]:bbox[3]+bbox[1], bbox[0]:bbox[2]+bbox[0],:]
    elif type == "tf":
        img = img[bbox[1]:bbox[3], bbox[0]:bbox[2],:]
    else:
        raise Exception("Invalid type")

    img = img[:, :, ::-1]
    img = cv2.resize(img, (IMAGE_SIZE,IMAGE_SIZE), interpolation=cv2.INTER_LINEAR)
    img = img / 255
    img = np.asarray(img, dtype=np.float32)
    img = np.expand_dims(img,0)
    return img

def draw_yawn(img, output):
    img_out = img.copy()
    text = f"Yawn: {'Yes' if output else 'No'}" 
    img_out = cv2.putText(img_out, text, (50,50), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 0, 0), 2)
    return img_out

def preprocess_eye(img, bbox, keypoint, type="tflite"):
    IMAGE_SIZE = 128
    
    if type == "tflite":
        face_width = bbox[2]
        face_height = bbox[3]
    elif type == "tf":
        face_width = bbox[2]-bbox[0]
        face_height = bbox[3]-bbox[1]
    else:
        raise Exception("Invalid type")

    eye_width = int(face_width*0.2)
    eye_height = int(face_height*0.1)

    img = img[keypoint[1]-eye_width:keypoint[1]+eye_width, keypoint[0]-eye_height:keypoint[0]+eye_height, :]
    img_gray = cv2.cvtColor(img, cv2.COLOR_RGB2GRAY)
    img = np.stack([img_gray,img_gray,img_gray], axis=-1)
    img = cv2.resize(img, (IMAGE_SIZE,IMAGE_SIZE), interpolation=cv2.INTER_LINEAR)
    
    img = img / 255
    img = np.asarray(img, dtype=np.float32)
    img = np.expand_dims(img,0)
    return img

def draw_eye(img, outputs):
    img_out = img.copy()
    pos_x = 70
    for i, output in enumerate(outputs):
        text = f"Eye {i}: {'Yes' if output else 'No'}"
        img_out = cv2.putText(img_out, text, (50,pos_x+i*20), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 0, 0), 2)
    return img_out