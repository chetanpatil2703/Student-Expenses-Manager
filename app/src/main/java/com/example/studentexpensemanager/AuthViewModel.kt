package com.example.studentexpensemanager

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.dataStore

    companion object {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_PASSWORD = stringPreferencesKey("user_password")
        val USER_NAME = stringPreferencesKey("user_name")
    }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: false
    }

    val userName: Flow<String?> = dataStore.data.map { it[USER_NAME] }

    fun register(name: String, email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[USER_NAME] = name
                prefs[USER_EMAIL] = email
                prefs[USER_PASSWORD] = password
            }
            onResult(true)
        }
    }

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            dataStore.data.map { prefs ->
                val savedEmail = prefs[USER_EMAIL]
                val savedPassword = prefs[USER_PASSWORD]
                email == savedEmail && password == savedPassword
            }.collect { success ->
                if (success) {
                    dataStore.edit { it[IS_LOGGED_IN] = true }
                }
                onResult(success)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            dataStore.edit { it[IS_LOGGED_IN] = false }
        }
    }
}
