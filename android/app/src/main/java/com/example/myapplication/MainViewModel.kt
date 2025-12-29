package com.example.myapplication

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


class MainViewModel {
    // 1. This is your "State" - the frontend observes this
    private val _displayText = MutableStateFlow("")
    val displayText = _displayText.asStateFlow()

    // 2. This function simulates getting data from a backend/database
    fun onButtonClicked() {
        // In a real app, you might call a Repository or API here
        _displayText.value = "Hello! This data came from the Backend."
    }
}