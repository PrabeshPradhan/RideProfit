package com.rideprofit.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CalcInput(
    val kmToPickup: String = "",
    val kmToDestination: String = "",
    val fare: String = "",
    val mileage: String = "45",
    val commissionPct: String = "20",
    val fuelPrice: String = "100"
)

data class CalcResult(
    val totalKm: Double = 0.0,
    val fuelCost: Double = 0.0,
    val commission: Double = 0.0,
    val netProfit: Double = 0.0,
    val profitPerKm: Double = 0.0,
    val paidKm: Double = 0.0,
    val paidProfitPerKm: Double = 0.0,
    val valid: Boolean = false
) {
    val worthTaking: Boolean get() = valid && netProfit > 0
}

class RideViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = RideRepository(app)

    private val _input = MutableStateFlow(CalcInput())
    val input: StateFlow<CalcInput> = _input

    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab

    val records: StateFlow<List<RideRecord>> = repo.records
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _dark = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = _dark
    private var darkLoaded = false

    private val _goal = MutableStateFlow(0.0)
    val goal: StateFlow<Double> = _goal

    private val _goalBaseEarned = MutableStateFlow(0.0)
    val goalBaseEarned: StateFlow<Double> = _goalBaseEarned

    val totalEarned: StateFlow<Double> = combine(records, _goalBaseEarned) { list, base ->
        base + list.sumOf { it.netProfit }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    init {
        viewModelScope.launch {
            repo.darkMode.collect { saved ->
                if (saved != null && !darkLoaded) {
                    _dark.value = saved
                    darkLoaded = true
                }
            }
        }
        viewModelScope.launch {
            repo.defaults.collect { d ->
                if (d != null) {
                    _input.update {
                        it.copy(
                            mileage = d.mileage,
                            commissionPct = d.commissionPct,
                            fuelPrice = d.fuelPrice
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            repo.goal.collect { g -> if (g != null) _goal.value = g }
        }
        viewModelScope.launch {
            repo.goalBase.collect { b -> if (b != null) _goalBaseEarned.value = b }
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

    fun resetGoalProgress() {
        val current = totalEarned.value
        _goalBaseEarned.value = current
        viewModelScope.launch {
            repo.saveGoalBase(current)
            repo.clear()
        }
    }

    fun update(transform: (CalcInput) -> CalcInput) {
        _input.value = transform(_input.value)
    }

    fun setTab(i: Int) { _activeTab.value = i }

    val result: StateFlow<CalcResult> = _input.map(::calc)
        .stateIn(viewModelScope, SharingStarted.Eagerly, CalcResult())

    private fun calc(i: CalcInput): CalcResult {
        val toPickup = i.kmToPickup.toDoubleOrNull() ?: 0.0
        val toDest = i.kmToDestination.toDoubleOrNull() ?: 0.0
        val fare = i.fare.toDoubleOrNull() ?: 0.0
        val mileage = i.mileage.toDoubleOrNull() ?: 0.0
        val comm = i.commissionPct.toDoubleOrNull() ?: 0.0
        val fuel = i.fuelPrice.toDoubleOrNull() ?: 0.0

        val totalKm = toPickup + toDest
        if (totalKm <= 0 || fare <= 0 || mileage <= 0) {
            return CalcResult(totalKm = totalKm)
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
                    timestamp = System.currentTimeMillis(),
                    fare = fare,
                    netProfit = r.netProfit,
                    distance = r.totalKm
                )
            )
            repo.saveDefaults(saved.mileage, saved.commissionPct, saved.fuelPrice)
        }
        clearPrimary()
    }

    fun reject() {
        val saved = _input.value
        viewModelScope.launch {
            repo.saveDefaults(saved.mileage, saved.commissionPct, saved.fuelPrice)
        }
        clearPrimary()
    }

    private fun clearPrimary() {
        _input.update {
            it.copy(kmToPickup = "", kmToDestination = "", fare = "")
        }
    }

    fun clearHistory() = viewModelScope.launch { repo.clear() }
}
