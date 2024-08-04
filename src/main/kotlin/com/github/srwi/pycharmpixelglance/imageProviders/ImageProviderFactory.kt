package com.github.srwi.pycharmpixelglance.imageProviders

class ImageProviderFactory {
    companion object {
        fun getImageProvider(typeQualifier: String): ImageProvider {
            return when {
                typeQualifier == "numpy" -> NumpyImageProvider()
                typeQualifier == "PIL.Image" -> PillowImageProvider()
                typeQualifier.startsWith("torch") -> PytorchImageProvider()
                typeQualifier.startsWith("tensorflow") -> TensorflowImageProvider()
                else -> throw IllegalArgumentException("Unsupported type qualifier: $typeQualifier")
            }
        }
    }
}