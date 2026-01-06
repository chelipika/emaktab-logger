package com.example.myapplication

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch



class MyViewModelKt(application: Application) : AndroidViewModel(application){
    private val repository = Logger(application.applicationContext)

    // This variable holds the text we want to show in the UI
    var statusText = mutableStateOf("Idle")
        private set

    fun startTask() {
        viewModelScope.launch {
            repository.runLoginScript().collect { detail ->
                // Every time the flow emits, this updates the UI state
                statusText.value = detail.toString()
            }
        }
    }
}