package com.rideprofit.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class CalcInput(
    val kmToPickup: String = "",
    val kmToDestination: String = "",
    val fare: String = "",
    val mileage: String = "45",
    val commissionPct: String = "20",
    val fuelPrice: String = "100",
    val minPerKm: String = "8"
)

data class CalcResult(
    val totalKm: Double = 0.0,
    val fuelCost: Double = 0.0,
    val commission: Double = 0.0,
    val netProfit: Double = 0.0,
    val profitPerKm: Double = 0.0,
    val paidKm: Double = 0.0,
    val paidProfitPerKm: Double = 0.0,
    val minPerKm: Double = 0.0,
    val valid: Boolean = false
) {
    val worthTaking: Boolean
        get() = valid && netProfit > 0 && paidProfitPerKm >= minPerKm
}

class RideViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = RideRepository(app)

    private val _input = MutableStateFlow(CalcInput())
    val input: StateFlow<CalcInput> = _input

    private val _dark = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = _dark

    private val _goal = MutableStateFlow(0.0)
    val goal: StateFlow<Double> = _goal

    private val _goalBase = MutableStateFlow(0.0)

    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab

    val records: StateFlow<List<RideRecord>> = repo.records
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalEarned: StateFlow<Double> =
        combine(records, _goalBase) { list, base -> base + list.sumOf { it.netProfit } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val result: StateFlow<CalcResult> = _input.map(::calc)
        .stateIn(viewModelScope, SharingStarted.Eagerly, CalcResult())

    private var _loaded = false

    init {
        // One-shot synchronous load; keeps UI snappy after that
        viewModelScope.launch {
            val s = repo.snap()
            _dark.value = s.dark
            _goal.value = s.goal
            _goalBase.value = s.goalBase
            _input.value = _input.value.copy(
                mileage = s.mileage,
                commissionPct = s.commissionPct,
                fuelPrice = s.fuelPrice,
                minPerKm = s.minPerKm
            )
            _loaded = true
        }
    }

    fun setDark(v: Boolean) {
        _dark.value = v
        viewModelScope.launch { repo.setDark(v) }
    }

    fun setGoal(value: Double) {
        _goal.value = value
        viewModelScope.launch { repo.saveGoal(value) }
    }

    fun update(transform: (CalcInput) -> CalcInput) {
        _input.value = transform(_input.value)
    }

    fun setTab(i: Int) { _activeTab.value = i }

    private fun calc(i: CalcInput): CalcResult {
        val toPickup = i.kmToPickup.toDoubleOrNull() ?: 0.0
        val toDest = i.kmToDestination.toDoubleOrNull() ?: 0.0
        val fare = i.fare.toDoubleOrNull() ?: 0.0
        val mileage = i.mileage.toDoubleOrNull() ?: 0.0
        val comm = i.commissionPct.toDoubleOrNull() ?: 0.0
        val fuel = i.fuelPrice.toDoubleOrNull() ?: 0.0
        val minKm = i.minPerKm.toDoubleOrNull() ?: 0.0

        val totalKm = toPickup + toDest
        if (totalKm <= 0 || fare <= 0 || mileage <= 0) {
            return CalcResult(totalKm = totalKm, minPerKm = minKm)
        }
        val fuelCost = (totalKm / mileage) * fuel
        val commission = fare * (comm / 100.0)
        val net = fare - commission - fuelCost
        val ppk = net / totalKm
        val paidPpk = if (toDest > 0) net / toDest else 0.0
        return CalcResult(
            totalKm = totalKm,
            fuelCost = fuelCost,
            commission = commission,
            netProfit = net,
            profitPerKm = ppk,
            paidKm = toDest,
            paidProfitPerKm = paidPpk,
            minPerKm = minKm,
            valid = true
        )
    }

    fun accept() {
        val r = result.value
        if (!r.valid) return
        val fare = _input.value.fare.toDoubleOrNull() ?: 0.0
        val saved = _input.value
        viewModelScope.launch {
            repo.addRecord(
                RideRecord(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    fare = fare,
                    netProfit = r.netProfit,
                    distance = r.totalKm
                )
            )
            repo.saveDefaults(
                Defaults(saved.mileage, saved.commissionPct, saved.fuelPrice, saved.minPerKm)
            )
        }
        clearPrimary()
    }

    fun reject() {
        val saved = _input.value
        viewModelScope.launch {
            repo.saveDefaults(
                Defaults(saved.mileage, saved.commissionPct, saved.fuelPrice, saved.minPerKm)
            )
        }
        clearPrimary()
    }

    fun deleteRecord(id: String) = viewModelScope.launch { repo.deleteRecord(id) }

    fun clearHistory() = viewModelScope.launch { repo.clear() }

    private fun clearPrimary() {
        _input.update { it.copy(kmToPickup = "", kmToDestination = "", fare = "") }
    }
}
