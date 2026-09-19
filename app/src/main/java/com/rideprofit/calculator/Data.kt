package com.rideprofit.calculator

data class RideInput(
    val distanceMiles: Double = 0.0,
    val fuelCost: Double = 0.0,
    val maintenanceCost: Double = 0.0,
    val tripRevenue: Double = 0.0,
    val driverEarnings: Double = 0.0
)

data class RideSummary(
    val netProfit: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val roi: Double = 0.0
)
