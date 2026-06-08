package com.example.snakeladder

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class ProductAnalyticsSnapshot(
    val matchStarts: Int = 0,
    val matchCompletions: Int = 0,
    val earlyExits: Int = 0,
    val modePopularity: Map<String, Int> = emptyMap(),
    val churnPoints: Map<String, Int> = emptyMap(),
    val activeDays: Set<String> = emptySet()
)

internal object ProductAnalyticsStore {
    private const val PREFS_NAME = "snake_ladder_product_analytics"
    private const val KEY_ANALYTICS = "analytics"

    fun load(context: Context): ProductAnalyticsSnapshot {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ANALYTICS, null) ?: return ProductAnalyticsSnapshot()
        return runCatching {
            val json = JSONObject(raw)
            ProductAnalyticsSnapshot(
                matchStarts = json.optInt("matchStarts", 0),
                matchCompletions = json.optInt("matchCompletions", 0),
                earlyExits = json.optInt("earlyExits", 0),
                modePopularity = json.optMap("modePopularity"),
                churnPoints = json.optMap("churnPoints"),
                activeDays = json.optStringSet("activeDays")
            )
        }.getOrDefault(ProductAnalyticsSnapshot())
    }

    fun recordMatchStarted(context: Context, mode: GameMode, matchMode: MatchModePreset) {
        val snapshot = load(context)
        save(
            context,
            snapshot.copy(
                matchStarts = snapshot.matchStarts + 1,
                modePopularity = snapshot.modePopularity.bump("${mode.name}/${matchMode.name}"),
                activeDays = snapshot.activeDays + todayKey()
            )
        )
    }

    fun recordMatchCompleted(context: Context, state: GameState) {
        val snapshot = load(context)
        save(
            context,
            snapshot.copy(
                matchCompletions = snapshot.matchCompletions + 1,
                activeDays = snapshot.activeDays + todayKey()
            )
        )
    }

    fun recordEarlyExit(context: Context, state: GameState) {
        val snapshot = load(context)
        val churnKey = "${state.gameMode.name}/${state.matchMode.name}/turn_${state.matchEvents.size.coerceAtMost(20)}"
        save(
            context,
            snapshot.copy(
                earlyExits = snapshot.earlyExits + 1,
                churnPoints = snapshot.churnPoints.bump(churnKey),
                activeDays = snapshot.activeDays + todayKey()
            )
        )
    }

    private fun save(context: Context, snapshot: ProductAnalyticsSnapshot) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ANALYTICS, snapshot.toJson().toString())
            .apply()
    }

    private fun ProductAnalyticsSnapshot.toJson(): JSONObject {
        return JSONObject().apply {
            put("matchStarts", matchStarts)
            put("matchCompletions", matchCompletions)
            put("earlyExits", earlyExits)
            put("modePopularity", JSONObject().apply {
                modePopularity.forEach { (key, value) -> put(key, value) }
            })
            put("churnPoints", JSONObject().apply {
                churnPoints.forEach { (key, value) -> put(key, value) }
            })
            put("activeDays", org.json.JSONArray().apply {
                activeDays.sorted().forEach { put(it) }
            })
        }
    }

    private fun JSONObject.optMap(key: String): Map<String, Int> {
        val obj = optJSONObject(key) ?: return emptyMap()
        return buildMap {
            obj.keys().forEach { itemKey ->
                put(itemKey, obj.optInt(itemKey, 0))
            }
        }
    }

    private fun JSONObject.optStringSet(key: String): Set<String> {
        val arr = optJSONArray(key) ?: return emptySet()
        return buildSet {
            for (index in 0 until arr.length()) {
                val value = arr.optString(index)
                if (value.isNotBlank()) add(value)
            }
        }
    }

    private fun Map<String, Int>.bump(key: String): Map<String, Int> {
        return this + (key to ((this[key] ?: 0) + 1))
    }

    private fun todayKey(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    }
}
