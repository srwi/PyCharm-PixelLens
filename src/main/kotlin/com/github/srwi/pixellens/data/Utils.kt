package com.github.srwi.pixellens.data

import org.jetbrains.kotlinx.multik.ndarray.data.DataType
import org.jetbrains.kotlinx.multik.ndarray.data.NDArray
import org.jetbrains.kotlinx.multik.ndarray.operations.toList

class Utils {
    companion object {
        fun formatArrayOrScalar(value: Any): String {
            return when (value) {
                is NDArray<*, *> -> {
                    val valueList = value.toList()
                    when (value.dtype) {
                        DataType.FloatDataType, DataType.DoubleDataType -> {
                            valueList.joinToString(
                                prefix = "[",
                                postfix = "]",
                                separator = ", "
                            ) {
                                String.format("%.3f", it)
                            }
                        }
                        else -> value.toString()
                    }
                }
                is Float, is Double -> String.format("%.3f", value)
                else -> value.toString()
            }
        }
    }
}