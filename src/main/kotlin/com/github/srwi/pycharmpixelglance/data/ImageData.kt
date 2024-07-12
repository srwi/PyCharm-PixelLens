import org.jetbrains.kotlinx.multik.ndarray.data.DN
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import org.jetbrains.kotlinx.multik.ndarray.operations.map

class CustomImage(
    originalImage: NDArray<Any, DN>,
) {
    private val image: NDArray<Float, DN>
    val shape: IntArray
    val height: Int
    val width: Int
    val channels: Int

    init {
        shape = originalImage.shape
        require(shape.size in 2..3) { "Image must be 2D or 3D" }
        height = shape[0]
        width = shape[1]
        channels = if (shape.size == 3) shape[2] else 1
        image = convertToFloatArray(originalImage)
    }

    private fun convertToFloatArray(input: NDArray<Any, DN>): NDArray<Float, DN> {
        return input.map { value ->
            when (value) {
                is Byte -> if (value < 0) (value.toInt() + 128) / 255f else value.toInt() / 255f
                is Short -> if (value < 0) (value.toLong() + 32768) / 65280f else value.toLong() / 65280f
                is Int -> if (value < 0) (value.toLong() + 2147483648) / 4294967295f else value.toLong() / 4294967295f
                is Long -> if (value < 0) (value + 9223372036854775807) / 18446744073709551615.0f else value / 18446744073709551615.0f
                is Float -> value
                is Double -> value.toFloat()
                is Boolean -> if (value) 1f else 0f
                else -> throw IllegalArgumentException("Unsupported data type: ${value::class.simpleName}")
            }
        }
    }

//    fun getShape(): IntArray = shape
//
//    fun invert(): CustomImage {
//        val invertedImage = image.map { 1f - it } as NDArray<Any, DN>
//        return CustomImage(invertedImage)
//    }
//
//    fun extractChannel(channel: Int): CustomImage {
//        require(channel in 0 until channels) { "Invalid channel index" }
//        val extractedImage = if (channels == 1) {
//            image
//        } else {
//            image.slice(axes = listOf(0, 1), select = listOf(channel))
//        }
//        return CustomImage(extractedImage)
//    }
//
//    fun normalize(): CustomImage {
//        val min = image.min()
//        val max = image.max()
//        val range = max - min
//        val normalizedImage = image.map { (it - min) / range }
//        return CustomImage(normalizedImage)
//    }
//
//    fun reverseChannels(): CustomImage {
//        require(channels == 3) { "Reverse channels only applicable to 3-channel images" }
//        val reversedImage = mk.ndarray(image.toList().chunked(3) { it.reversed() }.flatten(), shape)
//        return CustomImage(reversedImage)
//    }
}