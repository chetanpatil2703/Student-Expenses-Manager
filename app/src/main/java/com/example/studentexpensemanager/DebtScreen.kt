package com.example.studentexpensemanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studentexpensemanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DebtScreen(viewModel: DebtViewModel = viewModel()) {
    val debtList by viewModel.allDebts.collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }

    val totalLent = debtList.filter { it.isLent && !it.isResolved }.sumOf { it.amount }
    val totalBorrowed = debtList.filter { !it.isLent && !it.isResolved }.sumOf { it.amount }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFF00BCD4),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Debt")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = "Lending & Borrowing",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            DebtSummaryCard(totalLent, totalBorrowed)

            Spacer(modifier = Modifier.height(24.dp))

            if (debtList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "No records found.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(debtList) { debt ->
                        DebtItem(
                            debt = debt,
                            onResolve = { viewModel.resolveDebt(debt) },
                            onDelete = { viewModel.delete(debt) }
                        )
                    }
                }
            }
        }

        if (showDialog) {
            AddDebtDialog(
                onDismiss = { showDialog = false },
                onSave = { name, amount, isLent ->
                    val date = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
                    viewModel.insert(DebtEntity(personName = name, amount = amount, date = date, isLent = isLent))
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun DebtSummaryCard(lent: Double, borrowed: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ItemBackground),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("You'll Get", color = TextSecondary, fontSize = 14.sp)
                Text("₹${lent.toInt()}", color = IncomeColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Divider(modifier = Modifier.width(1.dp).height(40.dp).align(Alignment.CenterVertically), color = Color.Gray.copy(alpha = 0.3f))
            Column(horizontalAlignment = Alignment.End) {
                Text("You Owe", color = TextSecondary, fontSize = 14.sp)
                Text("₹${borrowed.toInt()}", color = ExpenseColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DebtItem(debt: DebtEntity, onResolve: () -> Unit, onDelete: () -> Unit) {
    val statusColor = if (debt.isResolved) Color.Gray else if (debt.isLent) IncomeColor else ExpenseColor
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (debt.isResolved) ItemBackground.copy(alpha = 0.5f) else ItemBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(statusColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = statusColor)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debt.personName,
                    color = if (debt.isResolved) Color.Gray else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    style = if (debt.isResolved) LocalTextStyle.current.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else LocalTextStyle.current
                )
                Text(text = "${if (debt.isLent) "Lent on" else "Borrowed on"} ${debt.date}", color = TextSecondary, fontSize = 12.sp)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${debt.amount.toInt()}",
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (!debt.isResolved) {
                    Row {
                        IconButton(onClick = onResolve, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Resolve", tint = IncomeColor, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                } else {
                    Text("Resolved", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AddDebtDialog(onDismiss: () -> Unit, onSave: (String, Double, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isLent by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Record", color = Color.White) },
        containerColor = ItemBackground,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = isLent,
                        onClick = { isLent = true },
                        label = { Text("Lent") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IncomeColor.copy(alpha = 0.2f), selectedLabelColor = IncomeColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = !isLent,
                        onClick = { isLent = false },
                        label = { Text("Borrowed") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ExpenseColor.copy(alpha = 0.2f), selectedLabelColor = ExpenseColor)
                    )
                }
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Person Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00BCD4))
                )
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00BCD4))
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (name.isNotEmpty() && amt > 0) onSave(name, amt, isLent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}
