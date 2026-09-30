package com.sharecontact.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShareCardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShareCardWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (id in appWidgetIds) {
            WidgetPreferences.removeWidget(context, id)
        }
    }
}

object WidgetUpdater {
    fun updateAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ShareCardWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
