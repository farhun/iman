package com.imn.iman.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DhikrItem(
    val name: String,
    val targetCount: Int,
)
