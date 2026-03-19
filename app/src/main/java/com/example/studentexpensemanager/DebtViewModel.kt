package com.example.studentexpensemanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DebtViewModel(application: Application) : AndroidViewModel(application) {
    private val debtDao = AppDatabase.getDatabase(application).debtDao()
    val allDebts: Flow<List<DebtEntity>> = debtDao.getAllDebts()

    fun insert(debt: DebtEntity) {
        viewModelScope.launch {
            debtDao.insertDebt(debt)
        }
    }

    fun update(debt: DebtEntity) {
        viewModelScope.launch {
            debtDao.updateDebt(debt)
        }
    }

    fun delete(debt: DebtEntity) {
        viewModelScope.launch {
            debtDao.deleteDebt(debt)
        }
    }

    fun resolveDebt(debt: DebtEntity) {
        viewModelScope.launch {
            debtDao.updateDebt(debt.copy(isResolved = true))
        }
    }
}
