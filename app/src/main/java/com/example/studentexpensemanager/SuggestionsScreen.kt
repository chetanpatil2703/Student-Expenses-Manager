package com.example.studentexpensemanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studentexpensemanager.ui.theme.*
import java.util.Locale
import kotlin.math.abs

@Composable
fun SuggestionsScreen(viewModel: TransactionViewModel = viewModel()) {
    val transactionList by viewModel.allTransactions.collectAsState(initial = emptyList())
    
    val totalIncome = transactionList.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = transactionList.filter { !it.isIncome }.sumOf { abs(it.amount) }
    
    val suggestions = remember(totalIncome, totalExpense, transactionList) {
        generateSuggestions(totalIncome, totalExpense, transactionList)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Financial Insights",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Based on the 50/30/20 rule and your spending habits",
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (totalIncome == 0.0) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Add your income to see personalized financial suggestions.",
                    color = TextSecondary,
                    modifier = Modifier.padding(32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(suggestions) { suggestion ->
                    SuggestionCard(suggestion)
                }
            }
        }
    }
}

data class Suggestion(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val priority: SuggestionPriority
)

enum class SuggestionPriority { HIGH, MEDIUM, LOW }

fun generateSuggestions(income: Double, expense: Double, transactions: List<TransactionEntity>): List<Suggestion> {
    val list = mutableListOf<Suggestion>()
    
    // 50/30/20 Rule Analysis
    val wantsLimit = income * 0.3
    val savingsTarget = income * 0.2
    
    // Categorize transactions (Simplified logic)
    val wants = transactions.filter { !it.isIncome && (it.category == "Shopping" || it.category == "Other") }.sumOf { abs(it.amount) }
    val actualSavings = income - expense

    // 1. Savings Analysis
    if (actualSavings < savingsTarget) {
        list.add(Suggestion(
            "Low Savings Rate",
            "You've saved ₹${String.format(Locale.US, "%.0f", actualSavings)}. Following the 20% rule, you should aim to save ₹${String.format(Locale.US, "%.0f", savingsTarget)}.",
            Icons.Default.Warning,
            ExpenseColor,
            SuggestionPriority.HIGH
        ))
    } else {
        list.add(Suggestion(
            "Great Saving Habit",
            "You are saving ${String.format(Locale.US, "%.1f", (actualSavings/income)*100)}% of your income, which exceeds the recommended 20% target!",
            Icons.Default.CheckCircle,
            IncomeColor,
            SuggestionPriority.LOW
        ))
    }

    // 2. Wants vs Needs
    if (wants > wantsLimit) {
        list.add(Suggestion(
            "High Lifestyle Spending",
            "Your 'Wants' spending (Shopping/Other) is ₹${String.format(Locale.US, "%.0f", wants)}, which is over your 30% limit of ₹${String.format(Locale.US, "%.0f", wantsLimit)}.",
            Icons.Default.Lightbulb,
            Color(0xFFFFB300),
            SuggestionPriority.MEDIUM
        ))
    }

    // 3. Food Spending (Specific Student Suggestion)
    val foodSpending = transactions.filter { it.category == "Food" }.sumOf { abs(it.amount) }
    if (foodSpending > income * 0.15) {
        list.add(Suggestion(
            "Dining Out Alert",
            "Food expenses are taking up ${String.format(Locale.US, "%.0f", (foodSpending/income)*100)}% of your budget. Try meal prepping to save more.",
            Icons.Default.Lightbulb,
            Color(0xFF00BCD4),
            SuggestionPriority.MEDIUM
        ))
    }

    // 4. Emergency Fund
    if (actualSavings > 0 && actualSavings < (expense * 3)) {
        list.add(Suggestion(
            "Build Emergency Fund",
            "Try to keep 3-6 months of expenses (approx. ₹${String.format(Locale.US, "%.0f", expense * 3)}) in a separate account for emergencies.",
            Icons.Default.Lightbulb,
            Color(0xFF00BCD4),
            SuggestionPriority.LOW
        ))
    }

    return list.sortedBy { it.priority }
}

@Composable
fun SuggestionCard(suggestion: Suggestion) {
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
                    .size(48.dp)
                    .background(suggestion.color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = suggestion.icon,
                    contentDescription = null,
                    tint = suggestion.color
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = suggestion.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = suggestion.description,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
