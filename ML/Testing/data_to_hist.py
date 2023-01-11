import numpy as np
import matplotlib.pyplot as plt
import json

def get_chunks(array):
    return np.diff(np.where(
        np.concatenate(([array[0]], array[:-1] != array[1:],[True]))
    )[0])[::2]


JSON_FILENAME = "data2.json"
with open(JSON_FILENAME, 'r') as openfile:
    json_object = json.load(openfile)

eye_left = np.array(json_object["eye_right"])
chunks = get_chunks(eye_left)
unique = np.unique(chunks)
plt.hist(chunks)
plt.show()

