package com.sharecontact.app.data

import android.content.Context
import com.sharecontact.app.model.AppSettings
import com.sharecontact.app.model.CardType
import com.sharecontact.app.model.DefaultCardMode
import com.sharecontact.app.model.LabeledItem
import com.sharecontact.app.model.PostalAddress
import com.sharecontact.app.model.ShareCard
import com.sharecontact.app.util.VCardBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class CardRepository private constructor(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val cardsFile: File
        get() = File(context.filesDir, "share_cards.json")

    private val settingsFile: File
        get() = File(context.filesDir, "app_settings.json")

    private val _cardsFlow = MutableStateFlow<List<ShareCard>>(emptyList())
    val cardsFlow: StateFlow<List<ShareCard>> = _cardsFlow.asStateFlow()

    private val _settingsFlow = MutableStateFlow(AppSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        try {
            // Load settings
            if (settingsFile.exists()) {
                val settingsJson = settingsFile.readText()
                val loadedSettings = json.decodeFromString<AppSettings>(settingsJson)
                _settingsFlow.value = loadedSettings
            }

            // Load cards
            if (cardsFile.exists()) {
                val cardsJson = cardsFile.readText()
                val loadedCards = json.decodeFromString<List<ShareCard>>(cardsJson)
                if (loadedCards.isNotEmpty()) {
                    _cardsFlow.value = loadedCards
                    return
                }
            }
            
            // First time initialization: Create a clean sample card
            val defaultCard = ShareCard(
                id = UUID.randomUUID().toString(),
                title = "💼 My Contact Card",
                type = CardType.VCARD,
                firstName = "Your",
                lastName = "Name",
                jobTitle = "Software Developer",
                organization = "My Company",
                phones = listOf(LabeledItem("Mobile", "555-0199")),
                emails = listOf(LabeledItem("Work", "hello@example.com")),
                urls = listOf(LabeledItem("Website", "https://example.com")),
                address = PostalAddress(city = "San Francisco", state = "CA", country = "USA"),
                note = "Tap the edit icon to customize this card!"
            )
            val initialList = listOf(defaultCard)
            _cardsFlow.value = initialList
            saveCardsToFile(initialList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveCardsToFile(cards: List<ShareCard>) {
        try {
            val content = json.encodeToString(cards)
            cardsFile.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveSettingsToFile(settings: AppSettings) {
        try {
            val content = json.encodeToString(settings)
            settingsFile.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveCard(card: ShareCard) = withContext(Dispatchers.IO) {
        val current = _cardsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == card.id }
        if (index >= 0) {
            current[index] = card.copy(updatedAt = System.currentTimeMillis())
        } else {
            current.add(card)
        }
        _cardsFlow.value = current
        saveCardsToFile(current)
    }

    suspend fun deleteCard(cardId: String) = withContext(Dispatchers.IO) {
        val current = _cardsFlow.value.filter { it.id != cardId }
        _cardsFlow.value = current
        saveCardsToFile(current)
    }

    suspend fun reorderCards(newOrder: List<ShareCard>) = withContext(Dispatchers.IO) {
        _cardsFlow.value = newOrder
        saveCardsToFile(newOrder)
    }

    suspend fun updateSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        _settingsFlow.value = settings
        saveSettingsToFile(settings)
    }

    suspend fun setLastViewedCard(cardId: String) = withContext(Dispatchers.IO) {
        val updated = _settingsFlow.value.copy(lastViewedCardId = cardId)
        _settingsFlow.value = updated
        saveSettingsToFile(updated)
    }

    // Export / Import
    fun exportFullBackupJson(): String {
        val backupData = FullBackupData(
            cards = _cardsFlow.value,
            settings = _settingsFlow.value,
            exportedAt = System.currentTimeMillis()
        )
        return json.encodeToString(backupData)
    }

    suspend fun importFullBackupJson(jsonString: String, merge: Boolean = false) = withContext(Dispatchers.IO) {
        val backupData = json.decodeFromString<FullBackupData>(jsonString)
        val finalCards = if (merge) {
            val existingIds = _cardsFlow.value.map { it.id }.toSet()
            val newCards = backupData.cards.filter { it.id !in existingIds }
            _cardsFlow.value + newCards
        } else {
            backupData.cards
        }
        _cardsFlow.value = finalCards
        saveCardsToFile(finalCards)
        
        if (!merge) {
            _settingsFlow.value = backupData.settings
            saveSettingsToFile(backupData.settings)
        }
    }

    fun exportSingleCardVcf(card: ShareCard): String {
        return VCardBuilder.build(card)
    }

    companion object {
        @Volatile
        private var INSTANCE: CardRepository? = null

        fun getInstance(context: Context): CardRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CardRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class FullBackupData(
    val cards: List<ShareCard>,
    val settings: AppSettings,
    val exportedAt: Long
)
