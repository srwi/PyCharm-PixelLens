package com.github.srwi.pixellens.data

data class Batch(
    val expression: String,
    val shape: List<Int>,
    val dtype: String,
    val data: BatchData
)