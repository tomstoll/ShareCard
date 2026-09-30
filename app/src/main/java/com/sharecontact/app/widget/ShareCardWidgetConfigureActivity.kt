package com.sharecontact.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.sharecontact.app.data.CardRepository
import com.sharecontact.app.model.CardType
import com.sharecontact.app.model.ShareCard
import com.sharecontact.app.ui.screens.QrCodeImage
import com.sharecontact.app.ui.theme.ShareContactAppTheme
import com.sharecontact.app.util.QrCodeGenerator
import kotlinx.coroutines.launch

class ShareCardWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set result CANCELED by default in case the user backs out
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val repository = CardRepository.getInstance(applicationContext)

        setContent {
            val cards by repository.cardsFlow.collectAsState()
            val settings by repository.settingsFlow.collectAsState()

            ShareContactAppTheme(useOledBlack = settings.useOledBlack) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Select Card for Widget", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    if (cards.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No cards available. Open the app to create a card first.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "Choose which card to display on your home screen:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            items(cards, key = { it.id }) { card ->
                                CardSelectionItem(
                                    card = card,
                                    onSelect = { selectCardAndFinish(card) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun selectCardAndFinish(card: ShareCard) {
        lifecycleScope.launch {
            WidgetPreferences.saveWidgetCardId(this@ShareCardWidgetConfigureActivity, appWidgetId, card.id)

            try {
                val glanceManager = GlanceAppWidgetManager(this@ShareCardWidgetConfigureActivity)
                val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                updateAppWidgetState(this@ShareCardWidgetConfigureActivity, glanceId) { prefs ->
                    prefs[WidgetPreferences.CARD_ID_KEY] = card.id
                }
                ShareCardWidget().update(this@ShareCardWidgetConfigureActivity, glanceId)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}

@Composable
private fun CardSelectionItem(
    card: ShareCard,
    onSelect: () -> Unit
) {
    val typeIcon = when (card.type) {
        CardType.VCARD -> "📇"
        CardType.WIFI -> "📶"
        CardType.URL -> "🔗"
        CardType.TEXT -> "📝"
    }

    val typeLabel = when (card.type) {
        CardType.VCARD -> "Contact Card"
        CardType.WIFI -> "Wi-Fi Network"
        CardType.URL -> "Web Link"
        CardType.TEXT -> "Text"
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // QR Code thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                QrCodeImage(
                    content = QrCodeGenerator.getPayload(card),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(typeIcon, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = card.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (card.organization.isNotBlank()) {
                    Text(
                        text = card.organization,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
