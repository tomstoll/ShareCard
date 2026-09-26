# ShareCard

A 100% offline, privacy-first Android app for instantly sharing contact information (vCard 3.0), Wi-Fi credentials, web links, and custom notes via high-contrast QR codes and physical NFC tags (NTAG213, NTAG215, NTAG216).

---

## 🔒 Privacy First

- **Zero Internet Permission**: The app cannot connect to the internet. Declared permissions: only `android.permission.NFC` (`android:required="false"`).
- **No Accounts, No Tracking**: Everything stays locally on your device in private encrypted/app storage.
- **Universal Standards**: Generates RFC 2426 compliant vCard 3.0 QR codes natively readable by iOS Camera/Contacts and Android Google Lens without needing any companion app installed on the recipient's phone.

---

## ✨ Features

- **Multiple Card Types**:
  - **Contact Cards (vCard 3.0)**: Name, title, organization, multiple phone numbers, emails, websites, physical addresses, and bio/notes.
  - **Wi-Fi Cards**: Quick-connect QR codes for WPA/WPA2/WPA3 networks with optional hidden SSID support.
  - **Web Links**: Social profiles, portfolios, Linktree, and custom URLs.
  - **Plain Text / Notes**: Freeform snippets or instructions.
- **Physical NFC Tag Writer**:
  - Write standard NDEF records (`text/vcard` or URI) directly onto blank NTAG213, NTAG215, or NTAG216 stickers, key fobs, and plastic business cards.
  - Built-in sensor positioning guide with animated alignment visualization.
- **Live Byte Counter & Chip Fit Indicator**:
  - Real-time payload size calculation as you type.
  - Instantly informs you whether your card fits comfortably on an NTAG215 (504 bytes) or requires an NTAG216 (888 bytes).
- **Display & Ergonomics**:
  - **Autofill Integration**: Fields support Android Autofill and keyboard quick-suggestions (Gboard personal contact strips).
  - **OLED Pure Black**: Optional #000000 theme for deep contrast and maximum battery efficiency.
  - **Brightness Boost**: Optional automatic brightness boost to ensure instant scanning under any lighting condition.
  - **Configurable Default Startup Card**: Open to your last-used card, top card, or a specific favorite card.
- **100% Offline Backup & Portability**:
  - One-tap export via Android's native Sharesheet (Save to Google Drive, Files, or send to yourself).
  - Restore cards and settings from JSON backups or import existing `.vcf` contact files.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.0
- **UI Framework**: Modern Jetpack Compose with Material 3 design system
- **Architecture**: Unidirectional Data Flow (StateFlow + Compose State)
- **Serialization**: Kotlinx Serialization (Zero-reflection, lightweight JSON)
- **QR Generation**: ZXing Core (pure offline bitmap generation)
- **Target SDK**: Android 15 (API 35), Minimum SDK: Android 8.0 (API 26)

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 35
- JDK 17 or JDK 21

### Local Build
```bash
git clone https://github.com/tomba/share_contact_app.git
cd share_contact_app
```
Open the project in Android Studio and click **Run** (or use `./gradlew assembleDebug`).

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) - see the [LICENSE](LICENSE) file for details.