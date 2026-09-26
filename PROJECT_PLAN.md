# Contact QR & NFC Share App — Project Plan & Architecture Specification

## 1. Executive Summary & Vision

A high-performance, **100% offline**, privacy-respecting Android application built with **Kotlin and Jetpack Compose** to share contact details and quick data via scannable QR codes and NFC.

### Why This App Exists
Existing contact-sharing and digital business card solutions suffer from:
- Mandatory account registration and tracking.
- Unnecessary permissions (`INTERNET`, `READ_CONTACTS`, `ACCESS_FINE_LOCATION`, etc.).
- Sluggish startup times and ad-cluttered interfaces.
- Proprietary web links that break if the third-party service shuts down.

### Core Guarantees & Strict Permission Audit
- **Zero Internet Access:** The Android manifest will **never declare** `<uses-permission android:name="android.permission.INTERNET" />`. This provides architectural certainty that user contact data remains strictly on the device.
- **The Only Declared Permission (`android.permission.NFC`):**
  - `android.permission.NFC` is the **single and only permission** in the entire application manifest.
  - In Android, NFC is categorized as a **normal permission** (not a dangerous/runtime permission). Android silently grants it at installation; **the app will never present a permission popup to the user**.
  - Hardware is marked optional (`<uses-feature android:name="android.hardware.nfc" android:required="false" />`), allowing devices without NFC chips to freely install and use all QR and widget features.
- **No Notification Permissions:** `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` is completely omitted.
- **No Storage Permissions Needed:** Does **not** request `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE`. (File import/export is handled securely via the OS Storage Access Framework).
- **No System Settings Permissions Needed:** Per-window screen brightness boosting modifies window attributes locally (`window.attributes.screenBrightness = 1.0f`), requiring **no** `WRITE_SETTINGS` permission.
- **Instant Cold Boot:** Sub-150ms startup directly into the primary QR code with max screen contrast.
- **Universal Cross-Platform Compatibility:** Generates standardized vCard 3.0 payloads parsed natively by both iOS Camera / Apple Contacts and Android Google Lens / Samsung Contacts without requiring any external app.

---

## 2. Technical Stack & Architecture

- **Language:** Kotlin (100% native)
- **UI Framework:** Jetpack Compose + Material 3 (Dynamic Color, Clean Typography)
- **Home Screen Widget:** Jetpack Glance (modern, declarative Compose-based widget API)
- **Data Persistence:** Jetpack DataStore Preferences / Room (local, zero cloud sync)
- **QR Generation Engine:** Offline ZXing / QRgen matrix rasterizer (optimized for bitmap rendering on Canvas)
- **NFC Engine:** Android `android.nfc` API with NDEF record construction

---

## 3. Detailed Feature Breakdown

### 3.1 Primary QR Screen & Contact Management
- **Contact Fields Supported:**
  - Full Name (Prefix, First, Middle, Last, Suffix)
  - Organization & Job Title / Department
  - Phone Numbers (Mobile, Work, Personal, Other)
  - Emails (Personal, Work, Other)
  - Website / Social Profile (LinkedIn, GitHub, Portfolio)
  - Address (Street, City, State, ZIP, Country)
  - Note / Bio
- **vCard 3.0 Formatting:**
  - Strict compliance with RFC 2426.
  - Intelligent payload trimming (stripping blank fields to keep QR code module density low, ensuring fast scans even on budget or low-resolution cameras).
- **Display UX:**
  - High-contrast QR code centered and auto-scaled.
  - Temporary brightness boost toggle (keeps screen bright while showing the code, restores system level on exit). This should be an option in the settings (e.g., "Boost Brightness when showing QR code" toggle).
  - Floating/corner edit button (pencil icon) leading directly to the editor.

### 3.2 Multi-Card Carousel & Diversity of Formats
- **Swipeable Horizontal Pager:**
  - Smooth page transitions with page-indicator dots.
  - **Prominent Label Header:** Anchored clearly above each QR code (e.g., *"💼 Work Contact"*, *"🏡 Personal / Family"*, *"📶 Guest Wi-Fi"*, *"🌐 Portfolio Link"*). These should be editable from the editor screen.
- **Supported Card Types:**
  1. **vCard (Contact Card):** Full contact card.
  2. **Wi-Fi QR:** Standard `WIFI:T:WPA;S:NetworkSSID;P:NetworkPassword;;` format for 1-tap guest connections.
  3. **Web / Social Link:** Direct URLs (LinkedIn, GitHub, Personal Website).
  4. **Plain Text / Crypto / Custom Note:** Formatted raw text.
- **Card Management:**
  - Add new card (template selector: Contact, Wi-Fi, URL, Text).
  - Reorder, duplicate, rename, or delete cards.
  - Settings menu should have an option to select the default card to display when the app is opened, with a 'last used' option that remembers the last card that was displayed.


### 3.3 Android Home Screen Widget
- **What is it?** A standard, native Android home screen widget placed directly on your phone launcher. (*Technical note: "Jetpack Glance" is Google's modern framework for developing standard Android widgets using Compose instead of legacy XML layout code; to the user, it is identical to any regular Android widget.*)
- **Instant Scan from Home Screen:** A resizable widget displaying the QR code of the user's choosing (selected when configuring the widget).
- **Widget Configuration:** Select which card from the carousel is pinned to the widget. Multiple widgets can be added to the home screen for different cards (e.g. one for Work, one for Personal).
- **Widget Tap Behavior:** Tapping launches a full-screen modal for effortless scanning. If the setting *"Boost Brightness when showing QR code"* is enabled, the screen brightness will temporarily boost; if disabled, brightness remains unchanged. It acts seamlessly as if the user tapped the QR code from within the main app.

### 3.4 NFC Sharing: Physical Tags vs. Phone-to-Phone Tap
#### Why Physical NFC Tags are the Gold Standard for iOS + Android
- **Apple iOS Limitations:** Apple restricts background NFC tag reading on iPhones (iPhone XR through iPhone 16) to **passive NDEF tags** (like NTAG213, NTAG215, NTAG216). iPhones **do not** support Android Beam (deprecated by Google in Android 10), and iOS does not reliably read Host Card Emulation (HCE) phone-to-phone taps.
- **The Solution:** 
  1. **Physical Tag Writer (Core Focus):** Write an NDEF `text/vcard` or NDEF URI payload to an inexpensive NFC sticker (e.g. affixed to the back of your phone case) or an NFC smart business card. When tapped against *any* modern iPhone or Android, it instantly pops up native "Add Contact" or opens the URL without any app installed.
  2. **In-App "NFC Buyer's Guide & How-To" Screen:** A dedicated info screen detailing:
     - What users should look to buy (e.g. NTAG215 or NTAG216 stickers/cards; avoid NTAG213 if contacts have photos/addresses due to 144-byte limit).
     - Step-by-step instructions on where to tap on different devices (top of iPhone, center/top back on Android).
     - How to write-lock or keep tags rewritable.
  3. **Tag Capacity Calculator:** Real-time byte counter in the editor and writer showing exact payload size and chip compatibility (NTAG213: 144B, NTAG215: 504B, NTAG216: 888B).
  4. **Experimental HCE (Phone-to-Phone):** Provide an optional Host Card Emulation mode for Android-to-Android tap sharing.

### 3.5 Backup, Export & Cloud/Local Portability (Via Storage Access Framework)
- **Zero Storage & Zero Network Permissions Required:**
  - Modern Android provides the **Storage Access Framework (SAF)** via system contracts (`ActivityResultContracts.CreateDocument` and `ActivityResultContracts.OpenDocument`).
  - When the user taps "Export Backup" or "Import Backup", Android displays its native, secure OS document picker.
  - Through this OS picker, the user can directly choose **Google Drive, OneDrive, Nextcloud, Dropbox, USB OTG drives, or local device storage**.
  - **Security & Privacy Isolation:** The Android operating system itself handles the cloud authentication, network syncing, and filesystem security. The app never accesses the network or storage directly; the OS simply streams the chosen `.json` or `.vcf` file into our sandboxed environment.
  - Future iOS port compatibility: Apple's `UIDocumentPickerViewController` mirrors this exact architecture with **iCloud Drive** and Google Drive, also requiring zero app permissions.
- **Why Google Wallet is Excluded:** Official Google Wallet passes require a verified Google Pay Issuer Account, Google Cloud project, and an online server to cryptographically sign JWT passes with private keys. This is fundamentally incompatible with a 100% offline, zero-permission app.
- **Granular Data Portability (VCF & JSON):**
  - **Individual Card Level:**
    - Export any individual contact card as a standard `.vcf` file (shareable directly via Android system Sharesheet to email, messaging, or files) or single-card `.json`.
    - Import an existing `.vcf` file from storage or Google Drive to quickly populate a new card.
  - **Full App Backup & Restore:**
    - Export the entire database (all cards, custom ordering, custom labels, and all user settings/preferences) as a clean `.json` backup file directly to Google Drive or local storage.
    - Restore from `.json` backup with option to replace or merge existing cards.

### 3.6 Support, Feedback & App Metadata
- **Privacy-Preserving User Support (No Personal Email Exposure):**
  - All bug reports, feature suggestions, and discussions are routed directly to the public GitHub repository Issues tracker (`https://github.com/tomba/share_contact_app/issues`).
  - This avoids publishing a personal email address in the app or store listing, while providing an open, transparent forum for community contributions.
  - Play Store ratings link via standard Android `market://` intent when the app is published.
- **About & Licensing:**
  - **App Name:** ShareCard
  - **Author:** Thomas Stoll
  - **License:** MIT License (Open Source)
  - **Repository:** Hosted on personal GitHub (`https://github.com/tomba/share_contact_app`)
  - **Core Guarantee:** 100% Offline • Zero Trackers • Zero Accounts • Strict Local Storage

---

## 4. Implementation Milestones

```
+--------------------------------------------------------------------------+
| MILESTONE 1: Core Foundation & Single vCard QR                   [DONE]  |
| [x] Project initialization (Zero INTERNET, Zero POST_NOTIFICATIONS)      |
| [x] DataStore / Room repository for contact card                         |
| [x] vCard 3.0 encoder & offline ZXing QR renderer                        |
| [x] Main Screen (prominent QR, brightness boost toggle, corner edit icon)|
| [x] Edit Screen (field editing, live preview, auto-trim, imePadding)     |
| [x] Empty state Onboarding Screen (Create Card or Import)                |
+--------------------------------------------------------------------------+
                                    |
                                    v
+--------------------------------------------------------------------------+
| MILESTONE 2: Multi-Card Carousel & Diversified Formats           [DONE]  |
| [x] HorizontalPager with editable card header label & page indicator     |
| [x] Card types: vCard, Wi-Fi, URL, Custom Note                           |
| [x] Card Manager (Add, Edit, Rearrange with Up/Down buttons, Delete)     |
| [x] Settings: Default card on launch (Last Used, First Card, or Custom)  |
+--------------------------------------------------------------------------+
                                    |
                                    v
+--------------------------------------------------------------------------+
| MILESTONE 3: Home Screen Widget                                          |
| [ ] Native Android Home Screen Widget (Jetpack Glance)                   |
| [ ] Widget configuration activity (card selector)                        |
| [ ] Tap behavior: Full-screen modal (respects brightness boost setting)  |
+--------------------------------------------------------------------------+
                                    |
                                    v
+--------------------------------------------------------------------------+
| MILESTONE 4: NFC Tag Writer & Guide                              [DONE]  |
| [x] Android NFC permission & lifecycle integration (0 runtime popups)    |
| [x] NDEF vCard (`text/vcard`) and URI writer                             |
| [x] Byte counter & tag capacity guidance (NTAG213 / 215 / 216)           |
| [x] Dedicated in-app "NFC Guide" info screen (with sensor warning)       |
| [x] Tag write verification & status feedback                             |
| [ ] Experimental HCE Phone-to-Phone Emulation                            |
+--------------------------------------------------------------------------+
                                    |
                                    v
+--------------------------------------------------------------------------+
| MILESTONE 5: Polish, Themes & Data Portability                  [PARTIAL]|
| [x] OLED Pure Black & Material You dynamic themes                        |
| [x] Individual record vCard (.vcf) import and parse                      |
| [x] Full app backup & restore (.json) via Storage Access Framework       |
| [x] Support & Feedback section (GitHub issue templates)                  |
| [x] About dialog & metadata (Thomas Stoll, MIT License)                  |
| [ ] Play Store / F-Droid release packaging                               |
+--------------------------------------------------------------------------+
```

---

## 5. Next Steps

1. Implement **Milestone 3 (Home Screen Widget)** using Jetpack Glance.
2. (Optional) Implement experimental Host Card Emulation (HCE) for phone-to-phone Android NFC tap.
3. Prepare release build & Play Store assets when ready.
