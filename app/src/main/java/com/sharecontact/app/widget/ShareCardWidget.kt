package com.sharecontact.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sharecontact.app.MainActivity
import com.sharecontact.app.data.CardRepository
import com.sharecontact.app.model.CardType
import com.sharecontact.app.model.ShareCard
import com.sharecontact.app.util.QrCodeGenerator

class ShareCardWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (e: Exception) {
            -1
        }

        provideContent {
            val prefs = currentState<Preferences>()
            val stateCardId = prefs[WidgetPreferences.CARD_ID_KEY]
            val savedCardId = if (appWidgetId != -1) {
                WidgetPreferences.getWidgetCardId(context, appWidgetId)
            } else null

            val targetCardId = stateCardId ?: savedCardId

            val repo = CardRepository.getInstance(context)
            val allCards = repo.cardsFlow.value

            val card = allCards.find { it.id == targetCardId }
                ?: if (targetCardId == null) allCards.firstOrNull() else null

            WidgetBody(context = context, card = card, appWidgetId = appWidgetId)
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetBody(context: Context, card: ShareCard?, appWidgetId: Int) {
    if (card != null) {
        val payload = QrCodeGenerator.getPayload(card)
        val qrBitmap = QrCodeGenerator.generateBitmap(payload, sizePixels = 512, margin = 1)

        val clickIntent = Intent(context, QrDisplayActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(QrDisplayActivity.EXTRA_CARD_ID, card.id)
            if (appWidgetId != -1) {
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF18181B))
                .cornerRadius(18.dp)
                .padding(10.dp)
                .clickable(actionStartActivity(clickIntent)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.displayName,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Centered QR Code with High-Contrast White Background
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(Color.White)
                        .cornerRadius(12.dp)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            provider = ImageProvider(qrBitmap),
                            contentDescription = "QR Code for ${card.displayName}",
                            modifier = GlanceModifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Tap to scan hint
                Text(
                    text = "Tap to enlarge",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFA1A1AA)),
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
            }
        }
    } else {
        // Empty State: Prompt user to configure card
        val configIntent = Intent(context, ShareCardWidgetConfigureActivity::class.java).apply {
            action = "android.appwidget.action.APPWIDGET_CONFIGURE"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (appWidgetId != -1) {
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF18181B))
                .cornerRadius(18.dp)
                .padding(12.dp)
                .clickable(actionStartActivity(configIntent)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ShareCard",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = "Tap to select card",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF60A5FA)),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
