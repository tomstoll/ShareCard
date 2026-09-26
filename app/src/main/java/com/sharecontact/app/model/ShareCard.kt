package com.sharecontact.app.model

import kotlinx.serialization.Serializable

@Serializable
enum class CardType {
    VCARD,
    WIFI,
    URL,
    TEXT
}

@Serializable
enum class WifiSecurity {
    WPA_WPA2,
    WEP,
    OPEN
}

@Serializable
data class LabeledItem(
    val label: String = "Mobile", // Mobile, Work, Personal, etc.
    val value: String = ""
)

@Serializable
data class PostalAddress(
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    val country: String = ""
)

@Serializable
data class ShareCard(
    val id: String,
    val title: String, // Label displayed prominently above QR code
    val type: CardType = CardType.VCARD,
    
    // vCard Contact fields
    val prefix: String = "",
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val suffix: String = "",
    val organization: String = "",
    val jobTitle: String = "",
    val phones: List<LabeledItem> = emptyList(),
    val emails: List<LabeledItem> = emptyList(),
    val urls: List<LabeledItem> = emptyList(),
    val address: PostalAddress = PostalAddress(),
    val note: String = "",
    
    // Wi-Fi fields
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiSecurity: WifiSecurity = WifiSecurity.WPA_WPA2,
    val wifiHidden: Boolean = false,
    
    // URL or Plain Text fields
    val rawContent: String = "",
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() {
            if (title.isNotBlank()) return title
            return when (type) {
                CardType.VCARD -> {
                    val name = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
                    if (name.isNotBlank()) name else "Untitled Contact"
                }
                CardType.WIFI -> if (wifiSsid.isNotBlank()) "Wi-Fi: $wifiSsid" else "Untitled Wi-Fi"
                CardType.URL -> if (rawContent.isNotBlank()) rawContent else "Untitled Link"
                CardType.TEXT -> "Note"
            }
        }
}
