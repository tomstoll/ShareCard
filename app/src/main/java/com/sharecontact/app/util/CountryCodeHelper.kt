package com.sharecontact.app.util

import java.util.Locale

data class Country(
    val code: String,       // ISO 3166-1 alpha-2, e.g. "US"
    val name: String,       // e.g. "United States"
    val dialCode: String    // e.g. "+1"
) {
    val flagEmoji: String get() = countryCodeToEmoji(code)

    companion object {
        fun countryCodeToEmoji(countryCode: String): String {
            if (countryCode.length != 2) return "🌐"
            val upper = countryCode.uppercase()
            val first = Character.codePointAt(upper, 0) - 0x41 + 0x1F1E6
            val second = Character.codePointAt(upper, 1) - 0x41 + 0x1F1E6
            if (first !in 0x1F1E6..0x1F1FF || second !in 0x1F1E6..0x1F1FF) return "🌐"
            return String(Character.toChars(first)) + String(Character.toChars(second))
        }
    }
}

object CountryCodeHelper {

    val allCountries: List<Country> = listOf(
        Country("US", "United States", "+1"),
        Country("CA", "Canada", "+1"),
        Country("GB", "United Kingdom", "+44"),
        Country("AU", "Australia", "+61"),
        Country("DE", "Germany", "+49"),
        Country("FR", "France", "+33"),
        Country("IT", "Italy", "+39"),
        Country("ES", "Spain", "+34"),
        Country("NL", "Netherlands", "+31"),
        Country("IN", "India", "+91"),
        Country("MX", "Mexico", "+52"),
        Country("BR", "Brazil", "+55"),
        Country("JP", "Japan", "+81"),
        Country("KR", "South Korea", "+82"),
        Country("CN", "China", "+86"),
        Country("TW", "Taiwan", "+886"),
        Country("HK", "Hong Kong", "+852"),
        Country("SG", "Singapore", "+65"),
        Country("NZ", "New Zealand", "+64"),
        Country("IE", "Ireland", "+353"),
        Country("CH", "Switzerland", "+41"),
        Country("AT", "Austria", "+43"),
        Country("SE", "Sweden", "+46"),
        Country("NO", "Norway", "+47"),
        Country("DK", "Denmark", "+45"),
        Country("FI", "Finland", "+358"),
        Country("BE", "Belgium", "+32"),
        Country("PT", "Portugal", "+351"),
        Country("PL", "Poland", "+48"),
        Country("GR", "Greece", "+30"),
        Country("CZ", "Czech Republic", "+420"),
        Country("RO", "Romania", "+40"),
        Country("HU", "Hungary", "+36"),
        Country("IL", "Israel", "+972"),
        Country("AE", "United Arab Emirates", "+971"),
        Country("SA", "Saudi Arabia", "+966"),
        Country("ZA", "South Africa", "+27"),
        Country("PH", "Philippines", "+63"),
        Country("MY", "Malaysia", "+60"),
        Country("TH", "Thailand", "+66"),
        Country("VN", "Vietnam", "+84"),
        Country("ID", "Indonesia", "+62"),
        Country("AR", "Argentina", "+54"),
        Country("CL", "Chile", "+56"),
        Country("CO", "Colombia", "+57"),
        Country("PE", "Peru", "+51"),
        Country("TR", "Turkey", "+90"),
        Country("UA", "Ukraine", "+380"),
        Country("EG", "Egypt", "+20"),
        Country("NG", "Nigeria", "+234"),
        Country("KE", "Kenya", "+254"),
        Country("PK", "Pakistan", "+92"),
        Country("BD", "Bangladesh", "+880"),
        Country("PR", "Puerto Rico", "+1"),
        Country("JM", "Jamaica", "+1"),
        Country("BS", "Bahamas", "+1"),
        Country("BB", "Barbados", "+1"),
        Country("TT", "Trinidad and Tobago", "+1"),
        Country("DO", "Dominican Republic", "+1"),
        Country("IS", "Iceland", "+354"),
        Country("LU", "Luxembourg", "+352"),
        Country("HR", "Croatia", "+385"),
        Country("SK", "Slovakia", "+421"),
        Country("BG", "Bulgaria", "+359"),
        Country("SI", "Slovenia", "+386"),
        Country("EE", "Estonia", "+372"),
        Country("LV", "Latvia", "+371"),
        Country("LT", "Lithuania", "+370"),
        Country("CY", "Cyprus", "+357"),
        Country("MT", "Malta", "+356"),
        Country("RS", "Serbia", "+381"),
        Country("CR", "Costa Rica", "+506"),
        Country("PA", "Panama", "+507"),
        Country("UY", "Uruguay", "+598"),
        Country("EC", "Ecuador", "+593"),
        Country("GT", "Guatemala", "+502"),
        Country("QA", "Qatar", "+974"),
        Country("KW", "Kuwait", "+965"),
        Country("OM", "Oman", "+968"),
        Country("BH", "Bahrain", "+973"),
        Country("JO", "Jordan", "+962"),
        Country("LB", "Lebanon", "+961"),
        Country("MA", "Morocco", "+212"),
        Country("GH", "Ghana", "+233"),
        Country("ET", "Ethiopia", "+251"),
        Country("TZ", "Tanzania", "+255"),
        Country("UG", "Uganda", "+256"),
        Country("NP", "Nepal", "+977"),
        Country("LK", "Sri Lanka", "+94")
    ).sortedBy { it.name }

    fun getDefaultCountry(): Country {
        val localeCountry = Locale.getDefault().country
        return allCountries.find { it.code.equals(localeCountry, ignoreCase = true) }
            ?: allCountries.find { it.code == "US" }
            ?: allCountries.first()
    }

    /**
     * Splits a raw phone string into its Country and National Number.
     * Handles inputs like "+1 (555) 123-4567", "+447911123456", "5551234567", "15551234567".
     */
    fun parsePhoneNumber(raw: String): Pair<Country, String> {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            return Pair(getDefaultCountry(), "")
        }

        if (trimmed.startsWith("+")) {
            // Sort by dial code length descending (e.g. +1868 before +1)
            val sortedByDialLength = allCountries.sortedByDescending { it.dialCode.length }
            val defaultCountry = getDefaultCountry()

            // Check if default country matches the prefix
            val preferred = if (trimmed.startsWith(defaultCountry.dialCode)) defaultCountry else null
            val matched = preferred ?: sortedByDialLength.firstOrNull { trimmed.startsWith(it.dialCode) }

            if (matched != null) {
                val national = trimmed.removePrefix(matched.dialCode).trim()
                return Pair(matched, formatUsCaNumber(national))
            }
        }

        val digitsOnly = trimmed.filter { it.isDigit() }
        val defaultCountry = getDefaultCountry()

        // 11 digits starting with 1 in NANP region (e.g. 15551234567)
        if (digitsOnly.length == 11 && digitsOnly.startsWith("1") && defaultCountry.dialCode == "+1") {
            val national = formatUsCaNumber(digitsOnly.substring(1))
            return Pair(defaultCountry, national)
        }

        return Pair(defaultCountry, formatUsCaNumber(trimmed))
    }

    /**
     * Formats US/Canada 10-digit numbers into standard readable (XXX) XXX-XXXX format.
     */
    fun formatUsCaNumber(input: String): String {
        val digits = input.filter { it.isDigit() }
        return when {
            digits.length == 10 && !input.contains("(") -> {
                "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
            }
            digits.length == 7 && !input.contains("-") -> {
                "${digits.substring(0, 3)}-${digits.substring(3)}"
            }
            digits.length == 11 && digits.startsWith("1") && !input.contains("(") -> {
                "(${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7)}"
            }
            else -> input
        }
    }

    /**
     * Combines the selected country dial code and national number into an international E.164-compatible string.
     */
    fun formatFullNumber(country: Country, nationalNumber: String): String {
        val trimmed = nationalNumber.trim()
        if (trimmed.isBlank()) return ""
        if (trimmed.startsWith("+")) return trimmed
        val formatted = if (country.dialCode == "+1") formatUsCaNumber(trimmed) else trimmed
        return "${country.dialCode} $formatted"
    }
}
