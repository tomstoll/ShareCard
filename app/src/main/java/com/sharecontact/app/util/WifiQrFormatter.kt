package com.sharecontact.app.util

import com.sharecontact.app.model.ShareCard
import com.sharecontact.app.model.WifiSecurity

object WifiQrFormatter {

    fun build(card: ShareCard): String {
        val authType = when (card.wifiSecurity) {
            WifiSecurity.WPA_WPA2 -> "WPA"
            WifiSecurity.WEP -> "WEP"
            WifiSecurity.OPEN -> "nopass"
        }
        val ssid = escape(card.wifiSsid)
        val password = if (card.wifiSecurity == WifiSecurity.OPEN) "" else escape(card.wifiPassword)
        val hidden = if (card.wifiHidden) "true" else "false"

        return "WIFI:T:$authType;S:$ssid;P:$password;H:$hidden;;"
    }

    private fun escape(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace(":", "\\:")
            .replace("\"", "\\\"")
    }
}
