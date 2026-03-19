package com.example.studentexpensemanager

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studentexpensemanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DebtScreen(viewModel: DebtViewModel = viewModel()) {
    val debtList by viewModel.allDebts.collectAsState(initial = emptyList())
    var selectedPerson by remember { mutableStateOf<String?>(null) }
    
    if (selectedPerson == null) {
        DebtListScreen(
            debtList = debtList,
            onPersonClick = { selectedPerson = it }
        )
    } else {
        PersonDetailScreen(
            personName = selectedPerson!!,
            debts = debtList.filter { it.personName == selectedPerson },
            onBack = { selectedPerson = null },
            viewModel = viewModel
        )
    }
}

@Composable
fun DebtListScreen(
    debtList: List<DebtEntity>,
    onPersonClick: (String) -> Unit,
    viewModel: DebtViewModel = viewModel()
) {
    var showDialog by remember { mutableStateOf(false) }
    
    val groupedDebts = debtList.groupBy { it.personName }
    val personSummaries = groupedDebts.map { (name, debts) ->
        val balance = debts.sumOf { if (it.isLent) it.amount else -it.amount }
        name to balance
    }.sortedByDescending { it.second }

    val totalToGet = personSummaries.filter { it.second > 0 }.sumOf { it.second }
    val totalToGive = personSummaries.filter { it.second < 0 }.sumOf { Math.abs(it.second) }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFF00BCD4),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Partner")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                text = "Khatabook",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            DebtSummaryCard(totalToGet, totalToGive)
            
            Spacer(modifier = Modifier.height(24.dp))

            if (personSummaries.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No partners added yet", color = TextSecondary)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(personSummaries) { (name, balance) ->
                        PersonListItem(name, balance, onClick = { onPersonClick(name) })
                    }
                }
            }
        }
        
        if (showDialog) {
            AddDebtDialog(
                onDismiss = { showDialog = false },
                onSave = { name, amount, isLent, note ->
                    val date = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
                    viewModel.insert(DebtEntity(personName = name, amount = amount, date = date, isLent = isLent, note = note))
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun PersonListItem(name: String, balance: Double, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = ItemBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(Color(0xFF00BCD4).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(name.first().uppercase(), color = Color(0xFF00BCD4), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(name, color = Color.White, modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            
            Column(horizontalAlignment = Alignment.End) {
                val color = if (balance > 0) IncomeColor else if (balance < 0) ExpenseColor else Color.Gray
                val text = if (balance > 0) "₹${balance.toInt()}" else if (balance < 0) "₹${Math.abs(balance).toInt()}" else "Settled"
                Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (balance != 0.0) {
                    Text(if (balance > 0) "You will get" else "You will give", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personName: String,
    debts: List<DebtEntity>,
    onBack: () -> Unit,
    viewModel: DebtViewModel
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var isLentForNewEntry by remember { mutableStateOf(true) }
    
    val currentBalance = debts.sumOf { if (it.isLent) it.amount else -it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(personName.first().uppercase(), color = Color.White, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(personName, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        bottomBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { isLentForNewEntry = false; showDialog = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("YOU GAVE ₹") }
                
                Button(
                    onClick = { isLentForNewEntry = true; showDialog = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("YOU GOT ₹") }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Net Balance Header
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (currentBalance >= 0) "You will get" else "You will give", color = Color.Black)
                    Text(
                        "₹${Math.abs(currentBalance).toInt()}",
                        color = if (currentBalance >= 0) Color(0xFF43A047) else Color(0xFFE53935),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                items(debts.reversed()) { debt ->
                    TransactionRow(debt, onClick = { editingDebt = debt })
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                }
            }
        }

        if (showDialog) {
            QuickAddDialog(
                isLent = isLentForNewEntry,
                onDismiss = { showDialog = false },
                onSave = { amount, note ->
                    val date = SimpleDateFormat("dd MMM yy • hh:mm a", Locale.US).format(Date())
                    viewModel.insert(DebtEntity(personName = personName, amount = amount, date = date, isLent = isLentForNewEntry, note = note))
                    showDialog = false
                }
            )
        }

        if (editingDebt != null) {
            QuickAddDialog(
                isLent = editingDebt!!.isLent,
                initialAmount = editingDebt!!.amount,
                initialNote = editingDebt!!.note,
                isEditing = true,
                onDismiss = { editingDebt = null },
                onSave = { amount, note ->
                    viewModel.update(editingDebt!!.copy(amount = amount, note = note))
                    editingDebt = null
                },
                onDelete = {
                    viewModel.delete(editingDebt!!)
                    editingDebt = null
                }
            )
        }
    }
}

@Composable
fun TransactionRow(debt: DebtEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(debt.date, color = TextSecondary, fontSize = 12.sp)
            if (debt.note.isNotEmpty()) {
                Text(debt.note, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Text("Bal. ₹${debt.amount.toInt()}", color = TextSecondary, fontSize = 11.sp)
        }
        
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (!debt.isLent) Text("₹${debt.amount.toInt()}", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (debt.isLent) Text("₹${debt.amount.toInt()}", color = Color(0xFF43A047), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickAddDialog(
    isLent: Boolean,
    initialAmount: Double = 0.0,
    initialNote: String = "",
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var amount by remember { mutableStateOf(if (initialAmount > 0) initialAmount.toInt().toString() else "") }
    var note by remember { mutableStateOf(initialNote) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Transaction" else if (isLent) "You Got" else "You Gave", color = Color.White) },
        containerColor = ItemBackground,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Add Note (e.g. For dinner, Room rent)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                amount.toDoubleOrNull()?.let { onSave(it, note) } 
            }) { Text(if (isEditing) "Update" else "Save") }
        },
        dismissButton = {
            Row {
                if (isEditing && onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = Color.Red) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
fun DebtSummaryCard(get: Double, give: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ItemBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("You will give", color = TextSecondary, fontSize = 12.sp)
                Text("₹${give.toInt()}", color = Color(0xFFE53935), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            VerticalDivider(modifier = Modifier.height(40.dp).width(1.dp), color = Color.Gray.copy(alpha = 0.2f))
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("You will get", color = TextSecondary, fontSize = 12.sp)
                Text("₹${get.toInt()}", color = Color(0xFF43A047), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AddDebtDialog(onDismiss: () -> Unit, onSave: (String, Double, Boolean, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isLent by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    var showContactPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    LaunchedEffect(Unit) {
        if (hasPermission) showContactPicker = true else launcher.launch(Manifest.permission.READ_CONTACTS)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Khata Partner", color = Color.White) },
        containerColor = ItemBackground,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Name") },
                    placeholder = { Text("Select from contacts") },
                    trailingIcon = {
                        IconButton(onClick = { 
                            if (hasPermission) showContactPicker = true else launcher.launch(Manifest.permission.READ_CONTACTS)
                        }) {
                            Icon(Icons.Default.ContactPage, contentDescription = null, tint = Color(0xFF00BCD4))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (hasPermission) showContactPicker = true else launcher.launch(Manifest.permission.READ_CONTACTS)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, 
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Opening Balance (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Add Note") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isLent, onClick = { isLent = true })
                    Text("You got", color = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = !isLent, onClick = { isLent = false })
                    Text("You gave", color = Color.White)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotEmpty(),
                onClick = { onSave(name, amount.toDoubleOrNull() ?: 0.0, isLent, note) }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showContactPicker) {
        ContactPickerModal(
            onContactSelected = { name = it; showContactPicker = false },
            onDismiss = { showContactPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerModal(onContactSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val contacts = remember { fetchContacts(context.contentResolver) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredContacts = contacts.filter { it.contains(searchQuery, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ItemBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.8f)) {
            Text("Select Contact", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(filteredContacts) { contact ->
                    ListItem(
                        headlineContent = { Text(contact, color = Color.White) },
                        modifier = Modifier.clickable { onContactSelected(contact) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@SuppressLint("Range")
fun fetchContacts(contentResolver: ContentResolver): List<String> {
    val contactList = mutableListOf<String>()
    val cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
    cursor?.use {
        while (it.moveToNext()) {
            val name = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
            if (name != null) contactList.add(name)
        }
    }
    return contactList.distinct().sorted()
}
