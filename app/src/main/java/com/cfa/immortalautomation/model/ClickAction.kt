package com.cfa.immortalautomation.model

import kotlinx.serialization.Serializable

@Serializable
data class ClickAction(
    val x: Float,
    val y: Float,
    val delayAfter: Long = 0 // Ajout de la propriété delayAfter avec une valeur par défaut
)