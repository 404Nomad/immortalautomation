package com.cfa.immortalautomation.model

import kotlinx.serialization.Serializable

@Serializable
data class ClickAction(
    val x: Float,
    val y: Float,
    val delayAfter: Long = 300L   // milliseconds to wait before next click
)
