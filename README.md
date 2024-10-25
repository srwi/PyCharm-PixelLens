# PixelLens for PyCharm <img src="src/main/resources/META-INF/pluginIcon.svg" align="right" width="25%"/>

<a href="https://paypal.me/rumswinkel"><img src="https://img.shields.io/static/v1?label=Donate&message=%E2%9D%A4&logo=PayPal&color=%23009cde"/></a>
[![Version](https://img.shields.io/jetbrains/plugin/v/25039-pixellens.svg)](https://plugins.jetbrains.com/plugin/25039-pixellens)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/25039-pixellens.svg)](https://plugins.jetbrains.com/plugin/25039-pixellens)

<!-- Plugin description -->
[PixelLens](https://plugins.jetbrains.com/plugin/25039-pixellens) is a free and [fully open source](https://github.com/srwi/PyCharm-PixelLens) PyCharm plugin designed to enhance the image visualization capabilities while debugging NumPy, OpenCV, PyTorch, TensorFlow, JAX or Pillow (PIL) image data.

PixelLens is capable of displaying 1D, 2D, 3D or 4D data by right clicking any supported variable in the debugger and selecting <kbd>View as Image</kbd>.

## Features

- View NumPy, OpenCV, PyTorch, TensorFlow, JAX and Pillow (PIL) data as image
- View entire batches (fourth dimension) or individual channels of the data
- Apply value normalization or Viridis colormap for better visibility
- Transpose dimensions (CHW ↔ HWC) or reverse channels (RGB ↔ BGR)
- Save or copy the displayed image for easy sharing
<!-- Plugin description end -->

<p align="center">
  <img src="https://github.com/user-attachments/assets/2ad50efd-efa3-4cd8-8789-44dfb1b277ce" width="550">
<p>

## Installation

- Get it from the [JetBrain Marketplace](https://plugins.jetbrains.com/plugin/25039-pixellens)

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "PixelLens"</kbd> > <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/srwi/PyCharm-PixelLens/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## License

This project is licensed under the [GPLv3](https://github.com/srwi/PyCharm-PixelLens/blob/master/LICENSE) license.
