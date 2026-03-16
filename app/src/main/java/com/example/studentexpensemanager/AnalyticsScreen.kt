package com.example.studentexpensemanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studentexpensemanager.ui.theme.DarkBackground
import com.example.studentexpensemanager.ui.theme.ItemBackground
import com.example.studentexpensemanager.ui.theme.TextSecondary
import kotlin.math.abs

@Composable
fun AnalyticsScreen(viewModel: TransactionViewModel = viewModel()) {
    val transactionList by viewModel.allTransactions.collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) } // 0 for Expense, 1 for Income

    val filteredTransactions = transactionList.filter { it.isIncome == (selectedTab == 1) }
    val categoryTotals = filteredTransactions.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { abs(it.amount) } }
    
    val totalAmount = categoryTotals.values.sum()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Analytics",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

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
                Text(text = "No data available", color = TextSecondary)
            }
        } else {
            // Pie Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                PieChart(categoryTotals)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Total", color = TextSecondary, fontSize = 14.sp)
                    Text(
                        text = "₹${String.format("%.0f", totalAmount)}",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

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

@Composable
fun PieChart(categoryTotals: Map<String, Double>) {
    val total = categoryTotals.values.sum()
    var startAngle = -90f

    Canvas(modifier = Modifier.size(180.dp)) {
        categoryTotals.forEach { (catName, amount) ->
            val cat = expenseCategories.find { it.name == catName } 
                ?: incomeCategories.find { it.name == catName }
                ?: Category("Other", "⋯", Color.Gray)
            
            val sweepAngle = (amount / total * 360).toFloat()
            drawArc(
                color = cat.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 30.dp.toPx())
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun CategoryBreakdownItem(category: Category, amount: Double, total: Double) {
    val percentage = (amount / total * 100).toInt()
    
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(category.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = category.icon, fontSize = 20.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = category.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(text = "$percentage%", color = TextSecondary, fontSize = 12.sp)
            }
            
            Text(
                text = "₹${String.format("%.2f", amount)}",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
