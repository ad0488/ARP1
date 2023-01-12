import numpy as np
import matplotlib.pyplot as plt
import json

def get_chunks(array):
    return np.diff(np.where(
        np.concatenate(([array[0]], array[:-1] != array[1:],[True]))
    )[0])[::2]


JSON_FILENAME = "data_yawning7.json"
with open(JSON_FILENAME, 'r') as openfile:
    json_object = json.load(openfile)

yawn = np.array(json_object["yawn"])[::4]
chunks = get_chunks(yawn)
unique = np.unique(chunks)
plt.hist(chunks)
plt.show()

