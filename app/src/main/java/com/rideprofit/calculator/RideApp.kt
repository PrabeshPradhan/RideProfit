package com.rideprofit.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideApp(vm: RideViewModel) {
    val dark by vm.darkMode.collectAsStateWithLifecycle()
    val tab by vm.activeTab.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Ride Profit", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { vm.setDark(!dark) }) {
                        Icon(
                            if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { vm.setTab(0) },
                    icon = { Icon(Icons.Filled.Calculate, null) },
                    label = { Text("Calculate") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { vm.setTab(1) },
                    icon = { Icon(Icons.Filled.BarChart, null) },
                    label = { Text("Earnings") }
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(if (dark) DarkGradient else LightGradient)
                .padding(padding)
        ) {
            when (tab) {
                0 -> CalculatorScreen(vm)
                1 -> EarningsScreen(vm)
            }
        }
    }
}

@Composable
fun CalculatorScreen(vm: RideViewModel) {
    val input by vm.input.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    var advancedOpen by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // PRIMARY
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        ) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NumField(
                    label = "Km to pickup",
                    value = input.kmToPickup,
                    onChange = { v -> vm.update { it.copy(kmToPickup = v) } }
                )
                NumField(
                    label = "Km to destination",
                    value = input.kmToDestination,
                    onChange = { v -> vm.update { it.copy(kmToDestination = v) } }
                )
                NumField(
                    label = "Fare (Rs)",
                    value = input.fare,
                    onChange = { v -> vm.update { it.copy(fare = v) } }
                )
            }
        }

        // RESULT
        LiveResultCard(result)

        // ACTIONS
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { vm.reject() },
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Filled.Close, null)
                Spacer(Modifier.width(6.dp))
                Text("Reject", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Button(
                onClick = { vm.accept() },
                enabled = result.valid,
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.width(6.dp))
                Text("Accept", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

        // SECONDARY
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { advancedOpen = !advancedOpen }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Tune,
                        null,
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Vehicle defaults",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            "${input.mileage} km/L • ${input.commissionPct}% • Rs ${input.fuelPrice}/L",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Icon(
                        if (advancedOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                AnimatedVisibility(advancedOpen) {
                    Column(
                        Modifier.padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NumField("Mileage (km/L)", input.mileage) { v -> vm.update { it.copy(mileage = v) } }
                        NumField("Commission (%)", input.commissionPct) { v -> vm.update { it.copy(commissionPct = v) } }
                        NumField("Fuel price (Rs/L)", input.fuelPrice) { v -> vm.update { it.copy(fuelPrice = v) } }
                        Text(
                            "These stay between rides.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
fun NumField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { s ->
            if (s.isEmpty() || s.matches(Regex("^\\d*\\.?\\d*$"))) onChange(s)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun LiveResultCard(r: CalcResult) {
    val worth = r.worthTaking
    val verdictColor = when {
        !r.valid -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        worth -> Color(0xFF2E9E6B)
        else -> MaterialTheme.colorScheme.error
    }
    val verdictText = when {
        !r.valid -> "Enter fare & distance"
        worth -> "TAKE IT"
        else -> "SKIP — no profit"
    }

    val animatedProfit by animateFloatAsState(
        targetValue = r.netProfit.toFloat(),
        label = "profit"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                verdictText,
                color = verdictColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                "Rs ${"%.2f".format(animatedProfit)}",
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                color = verdictColor
            )
            Text(
                "Net profit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("Total km", "%.1f".format(r.totalKm), Modifier.weight(1f))
                Stat("Profit/km", "Rs %.2f".format(r.profitPerKm), Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("Fuel cost", "Rs %.2f".format(r.fuelCost), Modifier.weight(1f))
                Stat("Commission", "Rs %.2f".format(r.commission), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(vm: RideViewModel) {
    val all by vm.records.collectAsStateWithLifecycle()
    var period by remember { mutableStateOf(Period.DAY) }

    val filtered = remember(all, period) { filterRecords(all, period) }
    val totalProfit = filtered.sumOf { it.netProfit }
    val totalFare = filtered.sumOf { it.fare }
    val totalKm = filtered.sumOf { it.distance }
    val count = filtered.size
    val avgPerKm = if (totalKm > 0) totalProfit / totalKm else 0.0

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Period.entries.forEachIndexed { index, p ->
                SegmentedButton(
                    selected = period == p,
                    onClick = { period = p },
                    shape = SegmentedButtonDefaults.itemShape(index, Period.entries.size),
                    label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Rs ${"%.2f".format(totalProfit)}",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (totalProfit >= 0) Color(0xFF2E9E6B) else MaterialTheme.colorScheme.error
                )
                Text(
                    "Net earnings • ${period.name.lowercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth()) {
                    Stat("Rides", count.toString(), Modifier.weight(1f))
                    Stat("Fare total", "Rs %.0f".format(totalFare), Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    Stat("Distance", "%.1f km".format(totalKm), Modifier.weight(1f))
                    Stat("Avg Rs/km", "%.2f".format(avgPerKm), Modifier.weight(1f))
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (all.isNotEmpty()) {
                TextButton(onClick = { vm.clearHistory() }) {
                    Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear all")
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Inbox, null, Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No accepted rides yet",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered.sortedByDescending { it.timestamp }) { rec ->
                    RecordRow(rec)
                }
            }
        }
    }
}

@Composable
fun RecordRow(r: RideRecord) {
    val fmt = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (r.netProfit >= 0) Color(0xFF2E9E6B).copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (r.netProfit >= 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                    null,
                    tint = if (r.netProfit >= 0) Color(0xFF2E9E6B) else MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(fmt.format(Date(r.timestamp)), fontWeight = FontWeight.Medium)
                Text(
                    "%.1f km • Fare Rs %.0f".format(r.distance, r.fare),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            Text(
                "Rs %.2f".format(r.netProfit),
                fontWeight = FontWeight.Bold,
                color = if (r.netProfit >= 0) Color(0xFF2E9E6B) else MaterialTheme.colorScheme.error
            )
        }
    }
}
