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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideApp(vm: RideViewModel) {
    val dark by vm.darkMode.collectAsStateWithLifecycle()
    val tab by vm.activeTab.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Ride Profit",
                    Modifier.padding(horizontal = 20.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Spacer(Modifier.height(16.dp))
                DrawerItem("Calculate", Icons.Filled.Calculate, tab == 0) {
                    vm.setTab(0); scope.launch { drawerState.close() }
                }
                DrawerItem("Earnings", Icons.Filled.BarChart, tab == 1) {
                    vm.setTab(1); scope.launch { drawerState.close() }
                }
                DrawerItem("Savings Goal", Icons.Filled.Savings, tab == 2) {
                    vm.setTab(2); scope.launch { drawerState.close() }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                DrawerItem(
                    if (dark) "Light mode" else "Dark mode",
                    if (dark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    false
                ) {
                    vm.setDark(!dark)
                }
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(
                        when (tab) {
                            0 -> "Calculate"
                            1 -> "Earnings"
                            else -> "Savings Goal"
                        },
                        fontWeight = FontWeight.Bold
                    ) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
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
                    else -> GoalScreen(vm)
                }
            }
        }
    }
}

@Composable
fun DrawerItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, null) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

// ============ CALCULATOR ============
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
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Km to pickup", input.kmToPickup) { v -> vm.update { it.copy(kmToPickup = v) } }
                NumField("Km to destination", input.kmToDestination) { v -> vm.update { it.copy(kmToDestination = v) } }
                NumField("Fare (Rs)", input.fare) { v -> vm.update { it.copy(fare = v) } }
            }
        }

        LiveResultCard(result)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { vm.reject() },
                modifier = Modifier.weight(1f).height(58.dp),
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
                modifier = Modifier.weight(1f).height(58.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.width(6.dp))
                Text("Accept", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

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
                        Icons.Filled.Tune, null, Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Vehicle defaults", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            "${input.mileage} km/L, ${input.commissionPct}%, Rs ${input.fuelPrice}/L, min Rs ${input.minPerKm}/km",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    IconButton(onClick = {
                        vm.update {
                            it.copy(mileage = "45", commissionPct = "20", fuelPrice = "100", minPerKm = "8")
                        }
                    }) {
                        Icon(Icons.Filled.RestartAlt, "Reset defaults",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
                        NumField("Min Rs per paid km", input.minPerKm) { v -> vm.update { it.copy(minPerKm = v) } }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
fun NumField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { s ->
            if (s.isEmpty() || s.matches(Regex("^\\d*\\.?\\d*$"))) onChange(s)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun LiveResultCard(r: CalcResult) {
    val worth = r.worthTaking
    val onSurface = MaterialTheme.colorScheme.onSurface
    val verdictColor = when {
        !r.valid -> onSurface.copy(alpha = 0.65f)
        worth -> Color(0xFF2E9E6B)
        else -> MaterialTheme.colorScheme.error
    }
    val verdictText = when {
        !r.valid -> "Enter fare and distance"
        worth -> "TAKE IT"
        r.netProfit <= 0 -> "SKIP - no profit"
        else -> "SKIP - below Rs ${"%.2f".format(r.minPerKm)}/km"
    }
    val animatedProfit by animateFloatAsState(r.netProfit.toFloat(), label = "profit")

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        )
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(verdictText, color = verdictColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Rs ${"%.2f".format(animatedProfit)}",
                fontWeight = FontWeight.Bold, fontSize = 38.sp, color = verdictColor
            )
            Text("Net profit",
                style = MaterialTheme.typography.labelSmall,
                color = onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("Total km", "%.1f".format(r.totalKm), Modifier.weight(1f))
                Stat("Rs/paid km", "%.2f".format(r.paidProfitPerKm), Modifier.weight(1f))
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
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
    }
}

// ============ EARNINGS ============
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
        Modifier.fillMaxSize().padding(16.dp),
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
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Rs ${"%.2f".format(totalProfit)}",
                    fontSize = 34.sp, fontWeight = FontWeight.Bold,
                    color = if (totalProfit >= 0) Color(0xFF2E9E6B) else MaterialTheme.colorScheme.error
                )
                Text("Net earnings, ${period.name.lowercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
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
                    Icon(Icons.Filled.DeleteSweep, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear all")
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Inbox, null, Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Spacer(Modifier.height(8.dp))
                    Text("No accepted rides yet",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered.sortedByDescending { it.timestamp }, key = { it.id }) { rec ->
                    RecordRow(rec, onDelete = { vm.deleteRecord(rec.id) })
                }
            }
        }
    }
}

@Composable
fun RecordRow(r: RideRecord, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val onSurface = MaterialTheme.colorScheme.onSurface
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(
                    if (r.netProfit >= 0) Color(0xFF2E9E6B).copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (r.netProfit >= 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (r.netProfit >= 0) Color(0xFF2E9E6B) else MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(fmt.format(Date(r.timestamp)), fontWeight = FontWeight.Medium, color = onSurface)
                Text(
                    "%.1f km - Fare Rs %.0f".format(r.distance, r.fare),
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurface.copy(alpha = 0.65f)
                )
            }
            Text(
                "Rs %.2f".format(r.netProfit),
                fontWeight = FontWeight.Bold,
                color = if (r.netProfit >= 0) Color(0xFF2E9E6B) else MaterialTheme.colorScheme.error
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete", tint = onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

// ============ GOAL ============
@Composable
fun GoalScreen(vm: RideViewModel) {
    val goal by vm.goal.collectAsStateWithLifecycle()
    val totalEarned by vm.totalEarned.collectAsStateWithLifecycle()
    val records by vm.records.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }

    val onSurface = MaterialTheme.colorScheme.onSurface
    val todayEarned = remember(records) {
        filterRecords(records, Period.DAY).sumOf { it.netProfit }
    }
    val progress = if (goal > 0) (totalEarned / goal).coerceIn(0.0, 1.0).toFloat() else 0f
    val remaining = (goal - totalEarned).coerceAtLeast(0.0)
    val achieved = goal > 0 && totalEarned >= goal

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Savings, null,
                    Modifier.size(48.dp),
                    tint = if (achieved) Color(0xFF2E9E6B) else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Rs ${"%.2f".format(totalEarned)}",
                    fontSize = 34.sp, fontWeight = FontWeight.Bold,
                    color = if (achieved) Color(0xFF2E9E6B) else onSurface
                )
                Text("Earned so far",
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurface.copy(alpha = 0.7f))
                Spacer(Modifier.height(16.dp))

                if (goal <= 0 || editing) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { s ->
                                if (s.isEmpty() || s.matches(Regex("^\\d*\\.?\\d*$"))) input = s
                            },
                            label = { Text("Target amount (Rs)") },
