package com.sharecontact.app.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.sharecontact.app.model.CardType
import com.sharecontact.app.model.ShareCard
import java.util.EnumMap

object QrCodeGenerator {

    /**
     * Resolves the raw string payload for any given ShareCard
     */
    fun getPayload(card: ShareCard): String {
        return when (card.type) {
            CardType.VCARD -> VCardBuilder.build(card)
            CardType.WIFI -> WifiQrFormatter.build(card)
            CardType.URL -> card.rawContent.trim()
            CardType.TEXT -> card.rawContent
        }
    }

    /**
     * Generates a high-contrast black-and-white Bitmap from content string.
     * 100% offline, memory-efficient.
     */
    fun generateBitmap(
        content: String,
        sizePixels: Int = 1024,
        margin: Int = 1
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, margin)
            }

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePixels, sizePixels, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
