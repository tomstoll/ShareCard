package com.sharecontact.app.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val boostBrightnessOnQr: Boolean = true,
    val defaultCardMode: DefaultCardMode = DefaultCardMode.LAST_USED,
    val specificDefaultCardId: String = "",
    val lastViewedCardId: String = "",
    val useOledBlack: Boolean = false
)

@Serializable
enum class DefaultCardMode {
    FIRST_CARD,
    SPECIFIC_CARD,
    LAST_USED
}
