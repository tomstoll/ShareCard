package com.sharecontact.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.sharecontact.app.model.*
import com.sharecontact.app.ui.components.CountryPickerDialog
import com.sharecontact.app.util.Country
import com.sharecontact.app.util.CountryCodeHelper
import com.sharecontact.app.util.QrCodeGenerator
import com.sharecontact.app.util.TagCapacityCalculator
import com.sharecontact.app.util.autofill
import java.util.UUID

data class PhoneFieldItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "Mobile",
    val country: Country,
    val nationalNumber: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun CardEditorScreen(
    existingCard: ShareCard?,
    onSave: (ShareCard) -> Unit,
    onDelete: ((String) -> Unit)?,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(existingCard?.title ?: "") }
    var cardType by remember { mutableStateOf(existingCard?.type ?: CardType.VCARD) }

    // Contact fields
    var prefix by remember { mutableStateOf(existingCard?.prefix ?: "") }
    var firstName by remember { mutableStateOf(existingCard?.firstName ?: "") }
    var middleName by remember { mutableStateOf(existingCard?.middleName ?: "") }
    var lastName by remember { mutableStateOf(existingCard?.lastName ?: "") }
    var suffix by remember { mutableStateOf(existingCard?.suffix ?: "") }
    var organization by remember { mutableStateOf(existingCard?.organization ?: "") }
    var jobTitle by remember { mutableStateOf(existingCard?.jobTitle ?: "") }

    var phoneItems by remember {
        val initialList = existingCard?.phones?.ifEmpty { listOf(LabeledItem("Mobile", "")) }
            ?: listOf(LabeledItem("Mobile", ""))
        mutableStateOf(
            initialList.map { item ->
                val (country, national) = CountryCodeHelper.parsePhoneNumber(item.value)
                PhoneFieldItem(label = item.label, country = country, nationalNumber = national)
            }
        )
    }

    val phones = remember(phoneItems) {
        phoneItems.map { item ->
            LabeledItem(
                label = item.label,
                value = CountryCodeHelper.formatFullNumber(item.country, item.nationalNumber)
            )
        }
    }

    var activeCountryPickerIndex by remember { mutableStateOf<Int?>(null) }
    var emails by remember {
        mutableStateOf(
            existingCard?.emails?.ifEmpty { listOf(LabeledItem("Personal", "")) }
                ?: listOf(LabeledItem("Personal", ""))
        )
    }
    var urls by remember {
        mutableStateOf(
            existingCard?.urls?.ifEmpty { listOf(LabeledItem("Website", "")) }
                ?: listOf(LabeledItem("Website", ""))
        )
    }

    var street by remember { mutableStateOf(existingCard?.address?.street ?: "") }
    var extended by remember { mutableStateOf(existingCard?.address?.extended ?: "") }
    var city by remember { mutableStateOf(existingCard?.address?.city ?: "") }
    var state by remember { mutableStateOf(existingCard?.address?.state ?: "") }
    var zip by remember { mutableStateOf(existingCard?.address?.zip ?: "") }
    var country by remember {
        mutableStateOf(
            existingCard?.address?.country?.ifBlank { CountryCodeHelper.getDefaultCountry().name }
                ?: CountryCodeHelper.getDefaultCountry().name
        )
    }
    var showAddressCountryPicker by remember { mutableStateOf(false) }

    val addressCountry = remember(country) {
        CountryCodeHelper.findCountryByNameOrCode(country) ?: CountryCodeHelper.getDefaultCountry()
    }
    val addressLabels = remember(addressCountry) {
        CountryCodeHelper.getAddressLabels(addressCountry.code)
    }
    var note by remember { mutableStateOf(existingCard?.note ?: "") }

    // Wi-Fi fields
    var wifiSsid by remember { mutableStateOf(existingCard?.wifiSsid ?: "") }
    var wifiPassword by remember { mutableStateOf(existingCard?.wifiPassword ?: "") }
    var wifiSecurity by remember { mutableStateOf(existingCard?.wifiSecurity ?: WifiSecurity.WPA_WPA2) }
    var wifiHidden by remember { mutableStateOf(existingCard?.wifiHidden ?: false) }

    // URL / Text field
    var rawContent by remember { mutableStateOf(existingCard?.rawContent ?: "") }

    fun cleanAndSave() {
        val hasAddressContent = street.isNotBlank() || extended.isNotBlank() || city.isNotBlank() || state.isNotBlank() || zip.isNotBlank()
        val cleanedAddress = if (hasAddressContent) {
            PostalAddress(
                street = street.trim(),
                extended = extended.trim(),
                city = city.trim(),
                state = state.trim(),
                zip = zip.trim(),
                country = country.trim()
            )
        } else {
            PostalAddress()
        }

        val trimmed = ShareCard(
            id = existingCard?.id ?: UUID.randomUUID().toString(),
            title = title.trim(),
            type = cardType,
            prefix = prefix.trim(),
            firstName = firstName.trim(),
            middleName = middleName.trim(),
            lastName = lastName.trim(),
            suffix = suffix.trim(),
            organization = organization.trim(),
            jobTitle = jobTitle.trim(),
            phones = phones.map { it.copy(label = it.label.trim(), value = it.value.trim()) }.filter { it.value.isNotBlank() },
            emails = emails.map { it.copy(label = it.label.trim(), value = it.value.trim()) }.filter { it.value.isNotBlank() },
            urls = urls.map { it.copy(label = it.label.trim(), value = it.value.trim()) }.filter { it.value.isNotBlank() },
            address = cleanedAddress,
            note = note.trim(),
            wifiSsid = wifiSsid.trim(),
            wifiPassword = wifiPassword.trim(),
            wifiSecurity = wifiSecurity,
            wifiHidden = wifiHidden,
            rawContent = rawContent.trim()
        )
        onSave(trimmed)
    }

    // Form constructed card for live preview & calculation
    val previewCard = remember(
        title, cardType, prefix, firstName, middleName, lastName, suffix,
        organization, jobTitle, phones, emails, urls, street, extended, city, state, zip, country,
        note, wifiSsid, wifiPassword, wifiSecurity, wifiHidden, rawContent
    ) {
        val hasAddress = street.isNotBlank() || extended.isNotBlank() || city.isNotBlank() || state.isNotBlank() || zip.isNotBlank()
        val constructedAddress = if (hasAddress) {
            PostalAddress(
                street = street,
                extended = extended,
                city = city,
                state = state,
                zip = zip,
                country = country
            )
        } else {
            PostalAddress()
        }
        ShareCard(
            id = existingCard?.id ?: UUID.randomUUID().toString(),
            title = title,
            type = cardType,
            prefix = prefix,
            firstName = firstName,
            middleName = middleName,
            lastName = lastName,
            suffix = suffix,
            organization = organization,
            jobTitle = jobTitle,
            phones = phones.filter { it.value.isNotBlank() },
            emails = emails.filter { it.value.isNotBlank() },
            urls = urls.filter { it.value.isNotBlank() },
            address = constructedAddress,
            note = note,
            wifiSsid = wifiSsid,
            wifiPassword = wifiPassword,
            wifiSecurity = wifiSecurity,
            wifiHidden = wifiHidden,
            rawContent = rawContent
        )
    }

    val previewPayload = remember(previewCard) { QrCodeGenerator.getPayload(previewCard) }
    val capacityInfo = remember(previewPayload) { TagCapacityCalculator.calculate(previewPayload) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingCard != null) "Edit Card" else "New Card") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    if (existingCard != null && onDelete != null) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Card", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = { cleanAndSave() }) {
                        Icon(Icons.Default.Done, contentDescription = "Save Card", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Live Preview Card & Byte Count Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QrCodeImage(content = previewPayload, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live QR & NFC Preview",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${capacityInfo.byteCount} bytes",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "NFC: ${capacityInfo.recommendedChip}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (capacityInfo.fitsNtag215) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Card Title (Displayed above QR code)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Card Header / Label (e.g., Work Contact)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card Type Selector
            Text("Card Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    CardType.VCARD to "Contact",
                    CardType.WIFI to "Wi-Fi",
                    CardType.URL to "Web Link",
                    CardType.TEXT to "Text"
                ).forEach { (type, label) ->
                    FilterChip(
                        selected = cardType == type,
                        onClick = { cardType = type },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Fields depending on CardType
            when (cardType) {
                CardType.VCARD -> {
                    Text("Personal Information", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("First Name") },
                            modifier = Modifier
                                .weight(1f)
                                .autofill(listOf(AutofillType.PersonFirstName, AutofillType.PersonFullName)) { firstName = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Last Name") },
                            modifier = Modifier
                                .weight(1f)
                                .autofill(listOf(AutofillType.PersonLastName, AutofillType.PersonFullName)) { lastName = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = organization,
                            onValueChange = { organization = it },
                            label = { Text("Company / Organization") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )
                        OutlinedTextField(
                            value = jobTitle,
                            onValueChange = { jobTitle = it },
                            label = { Text("Job Title") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Phone numbers
                    Text("Phone Numbers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    phoneItems.forEachIndexed { index, phoneItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Country selector chip / button
                            OutlinedCard(
                                onClick = { activeCountryPickerIndex = index },
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(phoneItem.country.flagEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        phoneItem.country.dialCode,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Change country code",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = phoneItem.nationalNumber,
                                onValueChange = { newVal ->
                                    val trimmed = newVal.trim()
                                    val list = phoneItems.toMutableList()
                                    if (trimmed.startsWith("+")) {
                                        val (detectedCountry, detectedNational) = CountryCodeHelper.parsePhoneNumber(trimmed)
                                        list[index] = phoneItem.copy(country = detectedCountry, nationalNumber = detectedNational)
                                    } else if (trimmed.length == 11 && trimmed.startsWith("1") && phoneItem.country.dialCode == "+1") {
                                        list[index] = phoneItem.copy(nationalNumber = CountryCodeHelper.formatUsCaNumber(trimmed.substring(1)))
                                    } else {
                                        val formatted = if (phoneItem.country.dialCode == "+1") CountryCodeHelper.formatUsCaNumber(newVal) else newVal
                                        list[index] = phoneItem.copy(nationalNumber = formatted)
                                    }
                                    phoneItems = list
                                },
                                label = { Text("${phoneItem.label} Phone") },
                                placeholder = {
                                    Text(
                                        if (phoneItem.country.dialCode == "+1") "(555) 000-0000" else "Phone number",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                },
                                supportingText = {
                                    if (phoneItem.nationalNumber.isNotBlank()) {
                                        val fullNum = CountryCodeHelper.formatFullNumber(phoneItem.country, phoneItem.nationalNumber)
                                        Text("✓ Full: $fullNum", color = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Text("Dial code ${phoneItem.country.dialCode} will be included")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .autofill(listOf(AutofillType.PhoneNumber, AutofillType.PhoneNumberDevice)) { newVal ->
                                        val trimmed = newVal.trim()
                                        val list = phoneItems.toMutableList()
                                        if (trimmed.startsWith("+")) {
                                            val (detectedCountry, detectedNational) = CountryCodeHelper.parsePhoneNumber(trimmed)
                                            list[index] = phoneItem.copy(country = detectedCountry, nationalNumber = detectedNational)
                                        } else {
                                            val formatted = if (phoneItem.country.dialCode == "+1") CountryCodeHelper.formatUsCaNumber(newVal) else newVal
                                            list[index] = phoneItem.copy(nationalNumber = formatted)
                                        }
                                        phoneItems = list
                                    },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                )
                            )

                            if (phoneItems.size > 1) {
                                IconButton(
                                    onClick = {
                                        phoneItems = phoneItems.toMutableList().apply { removeAt(index) }
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                    TextButton(onClick = {
                        val nextLabel = when (phoneItems.size) {
                            0 -> "Mobile"
                            1 -> "Work"
                            2 -> "Home"
                            else -> "Other"
                        }
                        phoneItems = phoneItems + PhoneFieldItem(
                            label = nextLabel,
                            country = CountryCodeHelper.getDefaultCountry(),
                            nationalNumber = ""
                        )
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Phone Number")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Emails
                    Text("Email Addresses", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    emails.forEachIndexed { index, email ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = email.value,
                                onValueChange = { newVal ->
                                    val list = emails.toMutableList()
                                    list[index] = email.copy(value = newVal)
                                    emails = list
                                },
                                label = { Text("${email.label} Email") },
                                modifier = Modifier
                                    .weight(1f)
                                    .autofill(listOf(AutofillType.EmailAddress)) { newVal ->
                                        val list = emails.toMutableList()
                                        list[index] = email.copy(value = newVal)
                                        emails = list
                                    },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                )
                            )
                            if (emails.size > 1) {
                                IconButton(onClick = {
                                    emails = emails.toMutableList().apply { removeAt(index) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                    TextButton(onClick = { emails = emails + LabeledItem("Work", "") }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Email")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Websites
                    Text("Links / Social / Portfolio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    urls.forEachIndexed { index, urlItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = urlItem.value,
                                onValueChange = { newVal ->
                                    val list = urls.toMutableList()
                                    list[index] = urlItem.copy(value = newVal)
                                    urls = list
                                },
                                label = { Text("Website / URL") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Next
                                )
                            )
                            if (urls.size > 1) {
                                IconButton(onClick = {
                                    urls = urls.toMutableList().apply { removeAt(index) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                    TextButton(onClick = { urls = urls + LabeledItem("Website", "https://") }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Link")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Address
                    Text("Address", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    // Country / Region Selector
                    Text(
                        "Country / Region",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedCard(
                        onClick = { showAddressCountryPicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(addressCountry.flagEmoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = addressCountry.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Change country",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Street Address Line 1
                    OutlinedTextField(
                        value = street,
                        onValueChange = { street = it },
                        label = { Text(addressLabels.street) },
                        placeholder = { Text("e.g. 123 Main St") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .autofill(listOf(AutofillType.AddressStreet, AutofillType.PostalAddress)) { street = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Street Address Line 2 (Extended Address)
                    OutlinedTextField(
                        value = extended,
                        onValueChange = { extended = it },
                        label = { Text(addressLabels.extended) },
                        placeholder = { Text("e.g. Apt 4B, Suite 200, Building C") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .autofill(listOf(AutofillType.AddressStreet)) { extended = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // City / Locality
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text(addressLabels.city) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .autofill(listOf(AutofillType.AddressLocality)) { city = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // State / Region & Postal Code / ZIP side by side
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text(addressLabels.state) },
                            modifier = Modifier
                                .weight(1.1f)
                                .autofill(listOf(AutofillType.AddressRegion)) { state = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            )
                        )
                        OutlinedTextField(
                            value = zip,
                            onValueChange = { zip = it },
                            label = { Text(addressLabels.zip) },
                            modifier = Modifier
                                .weight(0.9f)
                                .autofill(listOf(AutofillType.PostalCode)) { zip = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Note
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note / Bio") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        )
                    )
                }

                CardType.WIFI -> {
                    Text("Wi-Fi Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = wifiSsid,
                        onValueChange = { wifiSsid = it },
                        label = { Text("Network Name (SSID)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = wifiPassword,
                        onValueChange = { wifiPassword = it },
                        label = { Text("Password") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .autofill(listOf(AutofillType.Password)) { wifiPassword = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = wifiHidden, onCheckedChange = { wifiHidden = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hidden Network")
                    }
                }

                CardType.URL -> {
                    Text("Website / Link", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rawContent,
                        onValueChange = { rawContent = it },
                        label = { Text("URL (e.g. https://linkedin.com/in/...)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        )
                    )
                }

                CardType.TEXT -> {
                    Text("Plain Note", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rawContent,
                        onValueChange = { rawContent = it },
                        label = { Text("Custom text or note") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { cleanAndSave() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Save Card", fontSize = 16.sp)
            }

            // Generous scrolling space so user can scroll any field well above the keyboard
            Spacer(modifier = Modifier.height(180.dp))
        }
    }

    if (showDeleteConfirmDialog && existingCard != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Card?") },
            text = { Text("Are you sure you want to delete '${existingCard.displayName}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(existingCard.id)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    activeCountryPickerIndex?.let { pickerIdx ->
        val currentItem = phoneItems.getOrNull(pickerIdx)
        if (currentItem != null) {
            CountryPickerDialog(
                selectedCountry = currentItem.country,
                onSelectCountry = { newCountry ->
                    val list = phoneItems.toMutableList()
                    list[pickerIdx] = currentItem.copy(country = newCountry)
                    phoneItems = list
                },
                onDismissRequest = { activeCountryPickerIndex = null }
            )
        }
    }

    if (showAddressCountryPicker) {
        CountryPickerDialog(
            selectedCountry = addressCountry,
            onSelectCountry = { picked ->
                country = picked.name
                showAddressCountryPicker = false
            },
            onDismissRequest = { showAddressCountryPicker = false }
        )
    }
}
