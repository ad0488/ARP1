import numpy as np
import matplotlib.pyplot as plt
import json

def get_chunks(array):
    return np.diff(np.where(
        np.concatenate(([array[0]], array[:-1] != array[1:],[True]))
    )[0])[::2]


JSON_FILENAME = "data_talking4.json"
with open(JSON_FILENAME, 'r') as openfile:
    json_object_talk = json.load(openfile)

JSON_FILENAME = "data_yawning7.json"
with open(JSON_FILENAME, 'r') as openfile:
    json_object_yawn = json.load(openfile)

yawn_talking = np.array(json_object_talk["yawn"])[::6]
chunks_talking = get_chunks(yawn_talking)

yawn_yawning = np.array(json_object_yawn["yawn"])[::6]
chunks_yawning = get_chunks(yawn_yawning)

plt.subplot(1,2,1)
plt.hist(chunks_talking)
plt.gca().set_title('Subject Talking')

plt.subplot(1,2,2)
plt.hist(chunks_yawning)
plt.gca().set_title('Subject Yawning')

plt.tight_layout()
plt.savefig("yawn_comparison.png")

