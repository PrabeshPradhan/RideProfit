package com.rideprofit.calculator

import androidx.lifecycle.ViewModel

class RideViewModel : ViewModel() {
    fun calculateRide(input: RideInput): RideSummary {
        val totalRevenue = input.tripRevenue + input.driverEarnings
        val totalCost = input.fuelCost + input.maintenanceCost
        val netProfit = totalRevenue - totalCost
        val roi = if (totalCost > 0.0) ((netProfit / totalCost) * 100.0) else 0.0

        return RideSummary(
            netProfit = netProfit,
            totalCost = totalCost,
            totalRevenue = totalRevenue,
            roi = roi
        )
    }
}
