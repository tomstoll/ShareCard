package com.sharecontact.app.util

import com.sharecontact.app.model.CardType
import com.sharecontact.app.model.LabeledItem
import com.sharecontact.app.model.PostalAddress
import com.sharecontact.app.model.ShareCard
import java.util.UUID

object VCardParser {

    fun parse(vcardText: String): ShareCard {
        var fullName = ""
        var firstName = ""
        var lastName = ""
        var organization = ""
        var jobTitle = ""
        val phones = mutableListOf<LabeledItem>()
        val emails = mutableListOf<LabeledItem>()
        val urls = mutableListOf<LabeledItem>()
        var street = ""
        var city = ""
        var state = ""
        var zip = ""
        var country = ""
        var note = ""

        val lines = vcardText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val colonIdx = trimmed.indexOf(':')
            if (colonIdx <= 0) continue

            val keyPart = trimmed.substring(0, colonIdx).uppercase()
            val valPart = trimmed.substring(colonIdx + 1).replace("\\,", ",").replace("\\;", ";").replace("\\n", "\n")

            when {
                keyPart == "FN" -> fullName = valPart
                keyPart.startsWith("N") && !keyPart.startsWith("NOTE") -> {
                    val parts = valPart.split(";")
                    if (parts.isNotEmpty()) lastName = parts[0]
                    if (parts.size > 1) firstName = parts[1]
                }
                keyPart.startsWith("ORG") -> organization = valPart
                keyPart.startsWith("TITLE") -> jobTitle = valPart
                keyPart.startsWith("TEL") -> {
                    val label = when {
                        keyPart.contains("WORK") -> "Work"
                        keyPart.contains("HOME") -> "Home"
                        else -> "Mobile"
                    }
                    phones.add(LabeledItem(label, valPart))
                }
                keyPart.startsWith("EMAIL") -> {
                    val label = if (keyPart.contains("WORK")) "Work" else "Personal"
                    emails.add(LabeledItem(label, valPart))
                }
                keyPart.startsWith("URL") -> {
                    urls.add(LabeledItem("Website", valPart))
                }
                keyPart.startsWith("ADR") -> {
                    val parts = valPart.split(";")
                    if (parts.size > 2) street = parts[2]
                    if (parts.size > 3) city = parts[3]
                    if (parts.size > 4) state = parts[4]
                    if (parts.size > 5) zip = parts[5]
                    if (parts.size > 6) country = parts[6]
                }
                keyPart.startsWith("NOTE") -> note = valPart
            }
        }

        val title = if (fullName.isNotBlank()) fullName else if (firstName.isNotBlank() || lastName.isNotBlank()) "$firstName $lastName".trim() else "Imported Contact"

        return ShareCard(
            id = UUID.randomUUID().toString(),
            title = title,
            type = CardType.VCARD,
            firstName = firstName,
            lastName = lastName,
            organization = organization,
            jobTitle = jobTitle,
            phones = phones,
            emails = emails,
            urls = urls,
            address = PostalAddress(street, city, state, zip, country),
            note = note
        )
    }
}
