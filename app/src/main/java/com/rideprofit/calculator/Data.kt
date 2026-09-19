package com.rideprofit.calculator

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class RideRecord(
    val timestamp: Long,
    val fare: Double,
    val netProfit: Double,
    val distance: Double
)

data class Defaults(
    val mileage: String,
    val commissionPct: String,
    val fuelPrice: String
)

val Context.dataStore by preferencesDataStore("ride_prefs")

class RideRepository(private val context: Context) {
    private val key = stringPreferencesKey("records")
    private val darkKey = stringPreferencesKey("dark_mode")
    private val mileageKey = stringPreferencesKey("mileage")
    private val commKey = stringPreferencesKey("commission")
    private val fuelKey = stringPreferencesKey("fuel")
    private val goalKey = stringPreferencesKey("goal")
    private val goalBaseKey = stringPreferencesKey("goal_base")

    val records: Flow<List<RideRecord>> = context.dataStore.data.map { prefs ->
        val raw = prefs[key] ?: return@map emptyList()
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RideRecord(
                o.getLong("t"),
                o.getDouble("f"),
                o.getDouble("p"),
                o.getDouble("d")
            )
        }
    }

    val darkMode: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        if (prefs.contains(darkKey)) prefs[darkKey].toBoolean() else null
    }

    val defaults: Flow<Defaults?> = context.dataStore.data.map { prefs ->
        val m = prefs[mileageKey] ?: return@map null
        val c = prefs[commKey] ?: return@map null
        val f = prefs[fuelKey] ?: return@map null
        Defaults(m, c, f)
    }

    val goal: Flow<Double?> = context.dataStore.data.map { prefs ->
        prefs[goalKey]?.toDoubleOrNull()
    }

    val goalBase: Flow<Double?> = context.dataStore.data.map { prefs ->
        prefs[goalBaseKey]?.toDoubleOrNull()
    }

    suspend fun addRecord(r: RideRecord) {
        context.dataStore.edit { prefs ->
            val existing = prefs[key]?.let { JSONArray(it) } ?: JSONArray()
            existing.put(JSONObject().apply {
                put("t", r.timestamp)
                put("f", r.fare)
                put("p", r.netProfit)
                put("d", r.distance)
            })
            prefs[key] = existing.toString()
        }
    }

    suspend fun saveDefaults(mileage: String, commission: String, fuel: String) {
        context.dataStore.edit { prefs ->
            prefs[mileageKey] = mileage
            prefs[commKey] = commission
            prefs[fuelKey] = fuel
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(key) }
    }

    suspend fun setDark(enabled: Boolean) {
        context.dataStore.edit { it[darkKey] = enabled.toString() }
    }

    suspend fun saveGoal(value: Double) {
        context.dataStore.edit { it[goalKey] = value.toString() }
    }

    suspend fun saveGoalBase(value: Double) {
        context.dataStore.edit { it[goalBaseKey] = value.toString() }
    }
}

enum class Period { DAY, WEEK, MONTH }

fun nowBucketStart(period: Period): Long {
    val c = Calendar.getInstance()
    when (period) {
        Period.DAY -> {
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        }
        Period.WEEK -> {
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            val day = c.get(Calendar.DAY_OF_WEEK)
            val diff = if (day == Calendar.SUNDAY) 6 else day - Calendar.MONDAY
            c.add(Calendar.DAY_OF_MONTH, -diff)
        }
        Period.MONTH -> {
            c.set(Calendar.DAY_OF_MONTH, 1); c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
        }
    }
    return c.timeInMillis
}

fun filterRecords(list: List<RideRecord>, period: Period): List<RideRecord> {
    val start = nowBucketStart(period)
    return list.filter { it.timestamp >= start }
}
