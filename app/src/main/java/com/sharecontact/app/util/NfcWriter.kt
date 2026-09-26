package com.sharecontact.app.util

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.sharecontact.app.model.CardType
import com.sharecontact.app.model.ShareCard
import java.nio.charset.Charset

object NfcWriter {

    fun createNdefMessage(card: ShareCard): NdefMessage {
        val payload = QrCodeGenerator.getPayload(card)
        val record = when (card.type) {
            CardType.VCARD -> {
                NdefRecord.createMime("text/vcard", payload.toByteArray(Charset.forName("UTF-8")))
            }
            CardType.URL -> {
                NdefRecord.createUri(payload)
            }
            CardType.WIFI, CardType.TEXT -> {
                NdefRecord.createTextRecord("en", payload)
            }
        }
        return NdefMessage(arrayOf(record))
    }

    fun writeTag(tag: Tag, message: NdefMessage): Result<Unit> {
        return try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    return Result.failure(Exception("This NFC tag is locked and read-only."))
                }
                if (ndef.maxSize < message.byteArrayLength) {
                    return Result.failure(
                        Exception("Tag capacity too small (${ndef.maxSize} bytes available, ${message.byteArrayLength} bytes needed). Please use an NTAG215 or NTAG216 tag.")
                    )
                }
                ndef.writeNdefMessage(message)
                ndef.close()
                Result.success(Unit)
            } else {
                val formatable = NdefFormatable.get(tag)
                if (formatable != null) {
                    formatable.connect()
                    formatable.format(message)
                    formatable.close()
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Tag does not support standard NDEF formatting."))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
