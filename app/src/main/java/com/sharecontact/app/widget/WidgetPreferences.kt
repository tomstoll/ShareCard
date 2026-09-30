package com.sharecontact.app.widget

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey

object WidgetPreferences {
    private const val PREFS_NAME = "share_card_widget_prefs"
    val CARD_ID_KEY = stringPreferencesKey("card_id")

    fun saveWidgetCardId(context: Context, appWidgetId: Int, cardId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("widget_$appWidgetId", cardId)
            .apply()
    }

    fun getWidgetCardId(context: Context, appWidgetId: Int): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("widget_$appWidgetId", null)
    }

    fun removeWidget(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove("widget_$appWidgetId")
            .apply()
    }
}
