package com.example.nfcreaderwriter.data.local

import android.content.Context
import androidx.core.content.edit
import com.example.nfcreaderwriter.data.models.HistoryItem
import com.example.nfcreaderwriter.data.models.OperationType
import org.json.JSONArray
import org.json.JSONObject

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("nfc_utility_preferences", Context.MODE_PRIVATE)

    var automaticallyVerify: Boolean
        get() = preferences.getBoolean(KEY_VERIFY, true)
        set(value) = preferences.edit { putBoolean(KEY_VERIFY, value) }

    var saveHistory: Boolean
        get() = preferences.getBoolean(KEY_HISTORY_ENABLED, true)
        set(value) = preferences.edit { putBoolean(KEY_HISTORY_ENABLED, value) }

    fun loadHistory(): List<HistoryItem> = runCatching {
        val array = JSONArray(preferences.getString(KEY_HISTORY, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    HistoryItem(
                        id = item.getString("id"),
                        operation = OperationType.valueOf(item.getString("operation")),
                        contentPreview = item.optString("preview"),
                        timestamp = item.getLong("timestamp"),
                        successful = item.getBoolean("successful")
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    fun addHistory(item: HistoryItem): List<HistoryItem> {
        if (!saveHistory) return loadHistory()
        val updated = (listOf(item) + loadHistory()).take(MAX_HISTORY_ITEMS)
        saveHistory(updated)
        return updated
    }

    fun clearHistory() {
        preferences.edit { remove(KEY_HISTORY) }
    }

    private fun saveHistory(items: List<HistoryItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("operation", item.operation.name)
                    .put("preview", item.contentPreview)
                    .put("timestamp", item.timestamp)
                    .put("successful", item.successful)
            )
        }
        preferences.edit { putString(KEY_HISTORY, array.toString()) }
    }

    private companion object {
        const val KEY_VERIFY = "automatically_verify"
        const val KEY_HISTORY_ENABLED = "save_history"
        const val KEY_HISTORY = "operation_history"
        const val MAX_HISTORY_ITEMS = 100
    }
}
