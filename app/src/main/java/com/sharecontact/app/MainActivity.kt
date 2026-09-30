package com.sharecontact.app

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.sharecontact.app.data.CardRepository
import com.sharecontact.app.model.AppSettings
import com.sharecontact.app.model.ShareCard
import com.sharecontact.app.ui.screens.*
import com.sharecontact.app.ui.theme.ShareContactAppTheme
import com.sharecontact.app.util.NfcWriter
import com.sharecontact.app.util.VCardParser
import kotlinx.coroutines.launch

sealed class Screen {
    object Main : Screen()
    data class Editor(val cardId: String?) : Screen()
    object Settings : Screen()
    data class NfcGuide(val returnTo: Screen) : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: CardRepository
    private var nfcAdapter: NfcAdapter? = null

    // NFC writing state
    private var nfcTargetCard by mutableStateOf<ShareCard?>(null)
    private var nfcWriteState by mutableStateOf<NfcWriteState>(NfcWriteState.WaitingForTag)

    // Native Sharesheet export: offers Google Drive, local Files, email, and Quick Share
    private fun shareExportFile() {
        try {
            val jsonContent = repository.exportFullBackupJson()
            val exportFile = java.io.File(cacheDir, "share_card_backup.json")
            exportFile.writeText(jsonContent, Charsets.UTF_8)

            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                exportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Export Backup to..."))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // Storage Access Framework: Restore Full Backup or Import vCard
    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val fileContent = contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().use { it.readText() }
                }
                if (!fileContent.isNullOrBlank()) {
                    lifecycleScope.launch {
                        if (fileContent.contains("BEGIN:VCARD")) {
                            // Single vCard file import
                            val importedCard = VCardParser.parse(fileContent)
                            repository.saveCard(importedCard)
                            Toast.makeText(this@MainActivity, "Imported '${importedCard.displayName}'!", Toast.LENGTH_SHORT).show()
                        } else {
                            // Full JSON app backup import
                            repository.importFullBackupJson(fileContent, merge = false)
                            Toast.makeText(this@MainActivity, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to import file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val EXTRA_CARD_ID = "extra_card_id"
        const val EXTRA_FULLSCREEN_QR = "extra_fullscreen_qr"
    }

    private var targetCardIdFromIntent by mutableStateOf<String?>(null)
    private var openFullscreenFromIntent by mutableStateOf(false)

    private fun handleWidgetIntent(intent: Intent?) {
        val cardId = intent?.getStringExtra(EXTRA_CARD_ID)
        val openFullscreen = intent?.getBooleanExtra(EXTRA_FULLSCREEN_QR, false) ?: false
        if (cardId != null) {
            targetCardIdFromIntent = cardId
            openFullscreenFromIntent = openFullscreen
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = CardRepository.getInstance(applicationContext)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        handleWidgetIntent(intent)

        setContent {
            val cards by repository.cardsFlow.collectAsState()
            val settings by repository.settingsFlow.collectAsState()
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
            var showReorderDialog by remember { mutableStateOf(false) }
            var showAboutDialog by remember { mutableStateOf(false) }

            // Intercept system back gesture to navigate within the app accurately
            BackHandler(enabled = currentScreen != Screen.Main) {
                currentScreen = when (val s = currentScreen) {
                    is Screen.NfcGuide -> s.returnTo
                    else -> Screen.Main
                }
            }

            ShareContactAppTheme(useOledBlack = settings.useOledBlack) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (val screen = currentScreen) {
                        is Screen.Main -> {
                            if (cards.isEmpty()) {
                                OnboardingScreen(
                                    onCreateCard = { currentScreen = Screen.Editor(null) },
                                    onImportBackup = {
                                        importBackupLauncher.launch(arrayOf("application/json", "text/x-vcard", "text/vcard", "*/*"))
                                    }
                                )
                            } else {
                                MainScreen(
                                    cards = cards,
                                    settings = settings,
                                    targetCardId = targetCardIdFromIntent,
                                    openFullscreenQr = openFullscreenFromIntent,
                                    onEditCard = { id -> currentScreen = Screen.Editor(id) },
                                    onAddNewCard = { currentScreen = Screen.Editor(null) },
                                    onOpenSettings = { currentScreen = Screen.Settings },
                                    onOpenReorderCards = { showReorderDialog = true },
                                    onOpenNfcGuide = { currentScreen = Screen.NfcGuide(returnTo = Screen.Main) },
                                    onOpenAbout = { showAboutDialog = true },
                                    onOpenNfcWriter = { card ->
                                        if (nfcAdapter == null) {
                                            Toast.makeText(this, "This device does not support NFC", Toast.LENGTH_SHORT).show()
                                        } else if (!nfcAdapter!!.isEnabled) {
                                            Toast.makeText(this, "Please enable NFC in system settings", Toast.LENGTH_LONG).show()
                                        } else {
                                            nfcTargetCard = card
                                            nfcWriteState = NfcWriteState.WaitingForTag
                                        }
                                    },
                                    onCardChanged = { cardId ->
                                        lifecycleScope.launch {
                                            repository.setLastViewedCard(cardId)
                                        }
                                    }
                                )
                            }
                        }

                        is Screen.Editor -> {
                            val cardToEdit = cards.find { it.id == screen.cardId }
                            CardEditorScreen(
                                existingCard = cardToEdit,
                                onSave = { updatedCard ->
                                    lifecycleScope.launch {
                                        repository.saveCard(updatedCard)
                                        currentScreen = Screen.Main
                                    }
                                },
                                onDelete = if (cardToEdit != null) {
                                    { cardId ->
                                        lifecycleScope.launch {
                                            repository.deleteCard(cardId)
                                            currentScreen = Screen.Main
                                        }
                                    }
                                } else null,
                                onCancel = { currentScreen = Screen.Main }
                            )
                        }

                        is Screen.Settings -> {
                            SettingsScreen(
                                settings = settings,
                                cards = cards,
                                onUpdateSettings = { newSettings ->
                                    lifecycleScope.launch {
                                        repository.updateSettings(newSettings)
                                    }
                                },
                                onExportBackup = {
                                    shareExportFile()
                                },
                                onImportBackup = {
                                    importBackupLauncher.launch(arrayOf("application/json", "text/x-vcard", "text/vcard", "*/*"))
                                },
                                onBack = { currentScreen = Screen.Main }
                            )
                        }

                        is Screen.NfcGuide -> {
                            NfcGuideScreen(
                                onBack = { currentScreen = screen.returnTo }
                            )
                        }
                    }

                    // Rearrange Cards Modal Bottom Sheet
                    if (showReorderDialog && cards.size > 1) {
                        ReorderCardsDialog(
                            initialCards = cards,
                            onSaveOrder = { reorderedList ->
                                lifecycleScope.launch {
                                    repository.reorderCards(reorderedList)
                                    showReorderDialog = false
                                }
                            },
                            onDismiss = { showReorderDialog = false }
                        )
                    }

                    // About App Dialog
                    if (showAboutDialog) {
                        AboutDialog(
                            onDismiss = { showAboutDialog = false }
                        )
                    }

                    // NFC Write Bottom Sheet / Modal
                    val target = nfcTargetCard
                    if (target != null) {
                        NfcWriteDialog(
                            card = target,
                            state = nfcWriteState,
                            onDismiss = {
                                nfcTargetCard = null
                                nfcWriteState = NfcWriteState.WaitingForTag
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        disableNfcForegroundDispatch()
    }

    private fun enableNfcForegroundDispatch() {
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) return

        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else 0

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        )
        adapter.enableForegroundDispatch(this, pendingIntent, filters, null)
    }

    private fun disableNfcForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(this)
    }

    @Suppress("DEPRECATION")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
        val target = nfcTargetCard
        if (target != null && (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
                    intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
                    intent.action == NfcAdapter.ACTION_TECH_DISCOVERED)) {

            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }

            if (tag != null) {
                nfcWriteState = NfcWriteState.Writing
                val message = NfcWriter.createNdefMessage(target)
                val result = NfcWriter.writeTag(tag, message)

                if (result.isSuccess) {
                    nfcWriteState = NfcWriteState.Success
                } else {
                    val errMsg = result.exceptionOrNull()?.localizedMessage ?: "Unknown NFC write error"
                    nfcWriteState = NfcWriteState.Error(errMsg)
                }
            }
        }
    }
}
