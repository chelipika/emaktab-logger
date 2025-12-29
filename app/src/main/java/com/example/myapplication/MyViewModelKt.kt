package com.example.myapplication

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch



class MyViewModelKt : ViewModel(){
    private val repository = Logger()

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