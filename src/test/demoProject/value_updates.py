import numpy as np

value = np.random.randint(0, 100, (2000, 3000, 3), dtype=np.uint8)
value += 100
value = value.astype(np.float32) - 100.0
value = np.tile((value, value), 2)
value -= 100
