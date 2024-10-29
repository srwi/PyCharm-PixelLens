import numpy as np
import tensorflow as tf
import torch
from PIL import Image

pil_RGBA = Image.open("python.png")
pil_RGB = pil_RGBA.convert("RGB")
pil_L = pil_RGBA.convert("L")
pil_P = pil_RGBA.convert("P")
pil_I = pil_RGBA.convert("I")
pil_F = pil_RGBA.convert("F")

np_array = np.array(pil_RGBA).astype(np.float64)

np_int8 = (np_array - 128).astype(np.int8)
np_uint8 = np_array.astype(np.uint8)
np_int16 = ((np_array - 128) * 256).astype(np.int16)
np_uint16 = (np_array * 256).astype(np.uint16)
np_int32 = ((np_array - 128) * 256).astype(np.int32)
np_uint32 = (np_array * 256).astype(np.uint32)
np_int64 = ((np_array - 128) * 256).astype(np.int64)
np_uint64 = (np_array * 256).astype(np.uint64)
np_float16 = (np_array / 255).astype(np.float16)
np_float32 = (np_array / 255).astype(np.float32)
np_float64 = (np_array / 255).astype(np.float64)
np_bool = (np_array > 128).astype(bool)

tf_int8 = tf.convert_to_tensor(np_int8, dtype=tf.int8)
tf_uint8 = tf.convert_to_tensor(np_uint8, dtype=tf.uint8)
tf_int16 = tf.convert_to_tensor(np_int16, dtype=tf.int16)
tf_uint16 = tf.convert_to_tensor(np_uint16, dtype=tf.uint16)
tf_int32 = tf.convert_to_tensor(np_int32, dtype=tf.int32)
tf_uint32 = tf.convert_to_tensor(np_uint32, dtype=tf.uint32)
tf_int64 = tf.convert_to_tensor(np_int64, dtype=tf.int64)
tf_uint64 = tf.convert_to_tensor(np_uint64, dtype=tf.uint64)
tf_float16 = tf.convert_to_tensor(np_float16, dtype=tf.float16)
tf_float32 = tf.convert_to_tensor(np_float32, dtype=tf.float32)
tf_float64 = tf.convert_to_tensor(np_float64, dtype=tf.float64)
tf_bool = tf.convert_to_tensor(np_bool, dtype=tf.bool)

torch_int8 = torch.tensor(np_int8, dtype=torch.int8)
torch_uint8 = torch.tensor(np_uint8, dtype=torch.uint8)
torch_int16 = torch.tensor(np_int16, dtype=torch.int16)
torch_int32 = torch.tensor(np_int32, dtype=torch.int32)
torch_int64 = torch.tensor(np_int64, dtype=torch.int64)
torch_float16 = torch.tensor(np_float16, dtype=torch.float16)
torch_float32 = torch.tensor(np_float32, dtype=torch.float32)
torch_float64 = torch.tensor(np_float64, dtype=torch.float64)
torch_bool = torch.tensor(np_bool, dtype=torch.bool)

pass