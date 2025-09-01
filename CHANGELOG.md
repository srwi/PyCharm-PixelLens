<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# PixelLens Changelog

## [Unreleased]

## [0.4.4]

### Changes

- Disabled PixelLens for empty images
- Added a warning when attempting to view variables as image in a Jupyter Notebook without debugging
- Improved error handling

## [0.4.3]

### Changes

- Added support for PyCharm 2025.2

## [0.4.2]

### Changes

- Added support for PyCharm 2025.1

## [0.4.1]

### Changes

- Added a warning informing the user about broken compatibility with Python 3.13.0

## [0.4.0]

### Changes

- Added an option to choose between popup or dialog for image viewing

## [0.3.2]

### Fixed

- Improved zooming behavior when zooming in with the mouse wheel
- Added a setting to control the data transmission method when viewing images (for diagnostic purposes)
- Improved error handling

## [0.3.1]

### Added

- Added support for PyCharm 2024.3

## [0.3.0]

### Added

- Added support for JAX
- Added the ability to middle-click a variable in the debugger to view as image

## [0.2.4]

### Fixed

- Fixed viewing of images nested in other types (e.g. inside classes or lists)

## [0.2.3]

### Fixed

- Implemented a workaround for a bug in PyCharm 2024.2.1 that affects the ability to view images when debugging via SSH

## [0.2.2]

### Fixed

- Fixed a crash when attempting to view expression that contains special characters

## [0.2.1]

### Changes

- Improved support for evaluate expressions

### Fixed

- Fixed crash when viewing non-contiguous PyTorch tensor
- Fixed incorrect transposition of BCHW images

## [0.2.0]

### Added

- Added Python console support
- Added progress indicator while loading image
- Added a hint whenever values are outside the displayable range
- Improved error handling and reporting actions

### Fixed

- Greatly improved overall performance
- Fixed a memory leak

## [0.1.2]

### Added

- Added support for PyCharm 2024.2

## [0.1.1]

### Added

- Added support for some missing PIL types

## [0.1.0]

### Added

- Initial release of PixelLens 🎉
