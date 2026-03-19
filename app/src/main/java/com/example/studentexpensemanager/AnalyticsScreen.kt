package com.example.studentexpensemanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studentexpensemanager.ui.theme.DarkBackground
import com.example.studentexpensemanager.ui.theme.ItemBackground
import com.example.studentexpensemanager.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class TimePeriod(val label: String) {
    MONTHLY("Monthly"),
    ANNUALLY("Annually")
}

@Composable
fun AnalyticsScreen(viewModel: TransactionViewModel = viewModel()) {
    val transactionList by viewModel.allTransactions.collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) } // 0 for Expense, 1 for Income
    var selectedPeriod by remember { mutableStateOf(TimePeriod.MONTHLY) }
    var showPeriodMenu by remember { mutableStateOf(false) }

    val currentCalendar = Calendar.getInstance()
    var selectedMonth by remember { mutableIntStateOf(currentCalendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableIntStateOf(currentCalendar.get(Calendar.YEAR)) }
    
    var showMonthMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }

    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    val availableYears = (2020..currentCalendar.get(Calendar.YEAR) + 1).toList().reversed()

    val filteredTransactions = remember(transactionList, selectedTab, selectedPeriod, selectedMonth, selectedYear) {
        transactionList.filter { transaction ->
            transaction.isIncome == (selectedTab == 1) && 
            isWithinSelection(transaction.date, selectedPeriod, selectedMonth, selectedYear)
        }
    }

    val categoryTotals = filteredTransactions.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { abs(it.amount) } }
    
    val totalAmount = categoryTotals.values.sum()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            // Period Selector (Monthly/Annually)
            Box {
                Surface(
                    onClick = { showPeriodMenu = true },
                    color = ItemBackground,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedPeriod.label, color = Color.White, fontSize = 14.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }
                }
                
                DropdownMenu(
                    expanded = showPeriodMenu,
                    onDismissRequest = { showPeriodMenu = false },
                    modifier = Modifier.background(ItemBackground)
                ) {
                    TimePeriod.values().forEach { period ->
                        DropdownMenuItem(
                            text = { Text(period.label, color = Color.White) },
                            onClick = {
                                selectedPeriod = period
                                showPeriodMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Month and Year Selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedPeriod == TimePeriod.MONTHLY) {
                // Month selector
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { showMonthMenu = true },
                        color = ItemBackground,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(months[selectedMonth], color = Color.White, fontSize = 14.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                        }
                    }
                    DropdownMenu(
                        expanded = showMonthMenu,
                        onDismissRequest = { showMonthMenu = false },
                        modifier = Modifier.background(ItemBackground)
                    ) {
                        months.forEachIndexed { index, month ->
                            DropdownMenuItem(
                                text = { Text(month, color = Color.White) },
                                onClick = {
                                    selectedMonth = index
                                    showMonthMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Year selector
            Box(modifier = Modifier.weight(if (selectedPeriod == TimePeriod.MONTHLY) 0.6f else 1f)) {
                Surface(
                    onClick = { showYearMenu = true },
                    color = ItemBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(selectedYear.toString(), color = Color.White, fontSize = 14.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }
                }
                DropdownMenu(
                    expanded = showYearMenu,
                    onDismissRequest = { showYearMenu = false },
                    modifier = Modifier.background(ItemBackground)
                ) {
                    availableYears.forEach { year ->
                        DropdownMenuItem(
                            text = { Text(year.toString(), color = Color.White) },
                            onClick = {
                                selectedYear = year
                                showYearMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ItemBackground,
            contentColor = Color(0xFF00BCD4),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF00BCD4)
                )
            },
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Expenses", color = if (selectedTab == 0) Color.White else TextSecondary) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Income", color = if (selectedTab == 1) Color.White else TextSecondary) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (categoryTotals.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(text = "No data for this selection", color = TextSecondary)
            }
        } else {
            // Pie Chart Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                PieChart(categoryTotals, isIncome = selectedTab == 1)
                
                // Center Total Text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Total", color = TextSecondary, fontSize = 12.sp)
                    Text(
                        text = "₹${String.format(Locale.US, "%.0f", totalAmount)}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Category Breakdown List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categoryTotals.toList().sortedByDescending { it.second }) { (categoryName, amount) ->
                    val catInfo = if (selectedTab == 0) {
                        expenseCategories.find { it.name == categoryName }
                    } else {
                        incomeCategories.find { it.name == categoryName }
                    } ?: Category("Other", "⋯", Color.Gray)

                    CategoryBreakdownItem(catInfo, amount, totalAmount)
                }
            }
        }
    }
}

fun isWithinSelection(dateStr: String, period: TimePeriod, targetMonth: Int, targetYear: Int): Boolean {
    return try {
        val sdf = if (dateStr.contains(",")) {
            SimpleDateFormat("MMM dd, yyyy", Locale.US)
        } else {
            SimpleDateFormat("MMM dd", Locale.US)
        }
        
        val date = sdf.parse(dateStr) ?: return false
        val cal = Calendar.getInstance()
        cal.time = date
        
        if (!dateStr.contains(",")) {
            cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
        }

        when (period) {
            TimePeriod.MONTHLY -> {
                cal.get(Calendar.MONTH) == targetMonth && cal.get(Calendar.YEAR) == targetYear
            }
            TimePeriod.ANNUALLY -> {
                cal.get(Calendar.YEAR) == targetYear
            }
        }
    } catch (e: Exception) {
        false
    }
}

@Composable
fun PieChart(categoryTotals: Map<String, Double>, isIncome: Boolean) {
    val total = categoryTotals.values.sum()
    var startAngle = -90f
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = Modifier.size(240.dp)) {
        val radius = size.minDimension / 3f
        val center = Offset(size.width / 2, size.height / 2)

        categoryTotals.forEach { (catName, amount) ->
            val cat = (if (isIncome) incomeCategories else expenseCategories).find { it.name == catName }
                ?: Category("Other", "⋯", Color.Gray)
            
            val sweepAngle = (amount / total * 360).toFloat()
            val middleAngle = startAngle + sweepAngle / 2
            
            // 1. Draw the donut segment
            drawArc(
                color = cat.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 30.dp.toPx())
            )

            // 2. Draw Callout Line and Label if segment is large enough
            if (sweepAngle > 10) {
                val cos = cos(Math.toRadians(middleAngle.toDouble())).toFloat()
                val sin = sin(Math.toRadians(middleAngle.toDouble())).toFloat()
                
                // Line points
                val lineStart = Offset(
                    center.x + (radius + 15.dp.toPx()) * cos,
                    center.y + (radius + 15.dp.toPx()) * sin
                )
                val lineEnd = Offset(
                    center.x + (radius + 35.dp.toPx()) * cos,
                    center.y + (radius + 35.dp.toPx()) * sin
                )
                
                // Draw small line pointing to label
                drawLine(
                    color = cat.color.copy(alpha = 0.5f),
                    start = lineStart,
                    end = lineEnd,
                    strokeWidth = 2.dp.toPx()
                )

                // Draw Icon Label
                val textLayoutResult = textMeasurer.measure(
                    text = cat.icon,
                    style = TextStyle(fontSize = 18.sp)
                )
                
                val labelOffset = Offset(
                    center.x + (radius + 50.dp.toPx()) * cos - textLayoutResult.size.width / 2,
                    center.y + (radius + 50.dp.toPx()) * sin - textLayoutResult.size.height / 2
                )
                
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = labelOffset
                )
            }
            
            startAngle += sweepAngle
        }
    }
}

@Composable
fun CategoryBreakdownItem(category: Category, amount: Double, total: Double) {
    val percentage = if (total > 0) (amount / total * 100).toInt() else 0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ItemBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(category.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = category.icon, fontSize = 20.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = category.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                
                // Mini progress bar for category percentage
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (amount / total).toFloat() },
                    modifier = Modifier.fillMaxWidth(0.6f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = category.color,
                    trackColor = Color.White.copy(alpha = 0.1f),
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", amount)}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(text = "$percentage%", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}
