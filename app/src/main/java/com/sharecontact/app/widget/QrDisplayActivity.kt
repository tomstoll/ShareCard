package com.sharecontact.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.sharecontact.app.MainActivity
import com.sharecontact.app.data.CardRepository
import com.sharecontact.app.model.CardType
import com.sharecontact.app.ui.screens.QrCodeImage
import com.sharecontact.app.ui.theme.ShareContactAppTheme
import com.sharecontact.app.util.QrCodeGenerator
import kotlinx.coroutines.launch

class QrDisplayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CARD_ID = "extra_card_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup background dimming & system window blur behind for API 31+
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.65f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val lp = window.attributes
            lp.blurBehindRadius = 45
            window.attributes = lp
        }

        val initialCardId = intent?.getStringExtra(EXTRA_CARD_ID)
        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val repository = CardRepository.getInstance(applicationContext)

        // Apply screen brightness boost if enabled in settings
        val settings = repository.settingsFlow.value
        if (settings.boostBrightnessOnQr) {
            val lp = window.attributes
            lp.screenBrightness = 1.0f
            window.attributes = lp
        }

        setContent {
            val cards by repository.cardsFlow.collectAsState()
            val currentSettings by repository.settingsFlow.collectAsState()
            var currentCardId by remember { mutableStateOf(initialCardId) }
            var showCardSwitchDialog by remember { mutableStateOf(false) }
            var isBrightnessBoosted by remember { mutableStateOf(currentSettings.boostBrightnessOnQr) }

            LaunchedEffect(isBrightnessBoosted) {
                val lp = window.attributes
                lp.screenBrightness = if (isBrightnessBoosted) 1.0f else -1f
                window.attributes = lp
            }

            val card = cards.find { it.id == currentCardId }
                ?: cards.find { it.id == initialCardId }
                ?: cards.firstOrNull()

            ShareContactAppTheme(useOledBlack = currentSettings.useOledBlack) {
                // Outer transparent container: tapping outside card dismisses dialog to home screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { finish() },
                    contentAlignment = Alignment.Center
                ) {
                    if (card != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { /* Prevent clicks inside card from closing */ },
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Title Header: Tap name to switch card, close button on right
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = { showCardSwitchDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                                        ) {
                                            Text(
                                                text = card.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Switch Card",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { finish() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close")
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // QR Code in high-contrast pure white container
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .background(Color.White, RoundedCornerShape(16.dp))
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    QrCodeImage(
                                        content = QrCodeGenerator.getPayload(card),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Bottom Actions: "Open in App" (left) & Brightness Toggle (right)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            val appIntent = Intent(this@QrDisplayActivity, MainActivity::class.java).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                putExtra(MainActivity.EXTRA_CARD_ID, card.id)
                                            }
                                            startActivity(appIntent)
                                            finish()
                                        }
                                    ) {
                                        Text("Open App")
                                    }

                                    OutlinedButton(
                                        onClick = { isBrightnessBoosted = !isBrightnessBoosted },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isBrightnessBoosted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (isBrightnessBoosted) Icons.Default.BrightnessHigh else Icons.Default.BrightnessMedium,
                                            contentDescription = "Toggle Brightness",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (isBrightnessBoosted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBrightnessBoosted) "Boosted" else "Boost",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isBrightnessBoosted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Card not found or deleted
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(16.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Card not found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "This card may have been deleted.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { finish() }) {
                                    Text("Close")
                                }
                            }
                        }
                    }
                }

                // Card Selection Dialog (same style as initial widget placement)
                if (showCardSwitchDialog) {
                    AlertDialog(
                        onDismissRequest = { showCardSwitchDialog = false },
                        title = {
                            Text(
                                text = "Select Widget Card",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            if (cards.isEmpty()) {
                                Text(
                                    text = "No cards available.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 380.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(cards, key = { it.id }) { item ->
                                        val isSelected = item.id == card?.id
                                        val itemTypeLabel = when (item.type) {
                                            CardType.VCARD -> "Contact Card"
                                            CardType.WIFI -> "Wi-Fi Network"
                                            CardType.URL -> "Web Link"
                                            CardType.TEXT -> "Text"
                                        }

                                        OutlinedCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    currentCardId = item.id
                                                    showCardSwitchDialog = false

                                                    // Update widget preferences & Glance state if launched from a widget
                                                    if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                                        WidgetPreferences.saveWidgetCardId(
                                                            this@QrDisplayActivity,
                                                            appWidgetId,
                                                            item.id
                                                        )
                                                        lifecycleScope.launch {
                                                            try {
                                                                val glanceManager = GlanceAppWidgetManager(this@QrDisplayActivity)
                                                                val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                                                                updateAppWidgetState(this@QrDisplayActivity, glanceId) { prefs ->
                                                                    prefs[WidgetPreferences.CARD_ID_KEY] = item.id
                                                                }
                                                                ShareCardWidget().update(this@QrDisplayActivity, glanceId)
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                            }
                                                            WidgetUpdater.updateAll(this@QrDisplayActivity)
                                                        }
                                                    } else {
                                                        WidgetUpdater.updateAll(this@QrDisplayActivity)
                                                    }
                                                },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = if (isSelected) {
                                                CardDefaults.outlinedCardColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                )
                                            } else {
                                                CardDefaults.outlinedCardColors()
                                            },
                                            border = if (isSelected) {
                                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                            } else {
                                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .background(Color.White, RoundedCornerShape(8.dp))
                                                        .padding(4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    QrCodeImage(
                                                        content = QrCodeGenerator.getPayload(item),
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.displayName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = itemTypeLabel,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showCardSwitchDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}
