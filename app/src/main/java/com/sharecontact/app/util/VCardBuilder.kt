package com.sharecontact.app.util

import com.sharecontact.app.model.ShareCard

/**
 * Builds RFC 2426 compliant vCard 3.0 strings.
 * vCard 3.0 is the gold standard for universal camera and lens parsing
 * across Apple iOS and Android Google Lens.
 */
object VCardBuilder {

    fun build(card: ShareCard): String {
        val sb = StringBuilder()
        sb.append("BEGIN:VCARD\n")
        sb.append("VERSION:3.0\n")

        // Formatted Name (FN)
        val fullNameParts = listOf(card.prefix, card.firstName, card.middleName, card.lastName, card.suffix)
            .filter { it.isNotBlank() }
        val formattedName = if (fullNameParts.isNotEmpty()) fullNameParts.joinToString(" ") else card.title
        if (formattedName.isNotBlank()) {
            sb.append("FN:").append(escape(formattedName)).append("\n")
        }

        // Structured Name (N:LastName;FirstName;Middle;Prefix;Suffix)
        if (card.lastName.isNotBlank() || card.firstName.isNotBlank() || card.middleName.isNotBlank() ||
            card.prefix.isNotBlank() || card.suffix.isNotBlank()) {
            sb.append("N:")
                .append(escape(card.lastName)).append(";")
                .append(escape(card.firstName)).append(";")
                .append(escape(card.middleName)).append(";")
                .append(escape(card.prefix)).append(";")
                .append(escape(card.suffix)).append("\n")
        }

        // Organization & Title
        if (card.organization.isNotBlank()) {
            sb.append("ORG:").append(escape(card.organization)).append("\n")
        }
        if (card.jobTitle.isNotBlank()) {
            sb.append("TITLE:").append(escape(card.jobTitle)).append("\n")
        }

        // Phone numbers
        for (item in card.phones) {
            val trimmedVal = item.value.trim()
            if (trimmedVal.isNotBlank()) {
                val type = mapPhoneType(item.label)
                sb.append("TEL;TYPE=").append(type).append(":").append(escape(trimmedVal)).append("\n")
            }
        }

        // Emails
        for (item in card.emails) {
            val trimmedVal = item.value.trim()
            if (trimmedVal.isNotBlank()) {
                val type = mapEmailType(item.label)
                if (type != null) {
                    sb.append("EMAIL;TYPE=").append(type).append(":").append(escape(trimmedVal)).append("\n")
                } else {
                    sb.append("EMAIL:").append(escape(trimmedVal)).append("\n")
                }
            }
        }

        // URLs
        for (item in card.urls) {
            val trimmedVal = item.value.trim()
            if (trimmedVal.isNotBlank()) {
                sb.append("URL:").append(escape(trimmedVal)).append("\n")
            }
        }

        // Address (ADR:;;Street;City;State;PostalCode;Country)
        val addr = card.address
        if (addr.street.isNotBlank() || addr.city.isNotBlank() || addr.state.isNotBlank() ||
            addr.zip.isNotBlank() || addr.country.isNotBlank()) {
            sb.append("ADR;TYPE=HOME,POSTAL:;;")
                .append(escape(addr.street)).append(";")
                .append(escape(addr.city)).append(";")
                .append(escape(addr.state)).append(";")
                .append(escape(addr.zip)).append(";")
                .append(escape(addr.country)).append("\n")
        }

        // Note
        if (card.note.isNotBlank()) {
            sb.append("NOTE:").append(escape(card.note)).append("\n")
        }

        sb.append("END:VCARD")
        return sb.toString()
    }

    private fun escape(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
    }

    private fun mapPhoneType(label: String): String {
        return when (label.trim().lowercase()) {
            "work" -> "WORK,VOICE"
            "home" -> "HOME,VOICE"
            "mobile", "cell" -> "CELL,VOICE"
            "fax" -> "FAX"
            else -> "CELL,VOICE"
        }
    }

    private fun mapEmailType(label: String): String? {
        return when (label.trim().lowercase()) {
            "work" -> "WORK"
            "home", "personal" -> "HOME"
            "other" -> "OTHER"
            "", "email" -> null
            else -> label.trim().uppercase()
        }
    }
}
