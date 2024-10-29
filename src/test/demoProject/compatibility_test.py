from PIL import Image
import dataclasses
import numpy as np
from typing import Any


@dataclasses.dataclass
class ImageContainer:
    image: Any


foo_object = ImageContainer(np.array(Image.open("python.png")))

pass