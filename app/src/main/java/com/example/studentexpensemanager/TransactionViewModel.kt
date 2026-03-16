package com.example.studentexpensemanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun insert(transaction: TransactionEntity) = viewModelScope.launch {
        transactionDao.insertTransaction(transaction)
    }

    fun delete(transaction: TransactionEntity) = viewModelScope.launch {
        transactionDao.deleteTransaction(transaction)
    }
}
