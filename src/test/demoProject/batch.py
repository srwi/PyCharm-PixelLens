from PIL import Image
import numpy as np

image = Image.open("python.png")
array = np.array(image)
batch = np.tile(array, (8, 1, 1, 1))

pass