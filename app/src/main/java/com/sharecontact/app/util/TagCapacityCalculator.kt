package com.sharecontact.app.util

data class TagCapacityInfo(
    val byteCount: Int,
    val fitsNtag213: Boolean, // Max usable: ~137 bytes
    val fitsNtag215: Boolean, // Max usable: ~492 bytes
    val fitsNtag216: Boolean, // Max usable: ~872 bytes
    val recommendedChip: String
)

object TagCapacityCalculator {

    // Common NDEF usable capacities (taking into account NDEF record headers)
    const val USABLE_NTAG213 = 137
    const val USABLE_NTAG215 = 492
    const val USABLE_NTAG216 = 872

    fun calculate(payload: String): TagCapacityInfo {
        // NDEF MIME record overhead for "text/vcard" is approximately 16 bytes
        val rawBytes = payload.toByteArray(Charsets.UTF_8).size
        val estimatedNdefBytes = rawBytes + 16

        val fits213 = estimatedNdefBytes <= USABLE_NTAG213
        val fits215 = estimatedNdefBytes <= USABLE_NTAG215
        val fits216 = estimatedNdefBytes <= USABLE_NTAG216

        val recommended = when {
            fits213 -> "NTAG213, NTAG215, or NTAG216"
            fits215 -> "NTAG215 or NTAG216 (Too large for NTAG213)"
            fits216 -> "NTAG216 only"
            else -> "Exceeds standard NFC tags (Use QR Code)"
        }

        return TagCapacityInfo(
            byteCount = estimatedNdefBytes,
            fitsNtag213 = fits213,
            fitsNtag215 = fits215,
            fitsNtag216 = fits216,
            recommendedChip = recommended
        )
    }
}
