package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubTask(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)
