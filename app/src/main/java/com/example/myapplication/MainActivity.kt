package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope // CRITICAL IMPORT
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch // CRITICAL IMPORT
import com.example.myapplication.Logger
import kotlinx.coroutines.Dispatchers

// TODO: add sound effect when 100%
// TODO: fix dairy check error
// TODO: automate login and password lists


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
                // Note: We don't create geminiLogic here anymore.
        super.onCreate(savedInstanceState)
        setContent {

            MyApplicationTheme {
                LoginScreen(Logger())
            }
        }
    }
}

@Composable
fun MyApp(modifier: Modifier = Modifier) {
    var shouldShowOnboarding by rememberSaveable { mutableStateOf(false) }

    Surface(modifier) {
        if (shouldShowOnboarding) {
            OnboardingScreen(onContinuedClicked = { shouldShowOnboarding = false })
        } else {

            PromptBarScreen()
        }
    }
}


@Composable
fun PromptBarScreen(modifier: Modifier = Modifier) {
    // 1. SETUP STATE
    // Stores what the user types
    var userInput by remember { mutableStateOf("") }
    // Stores the AI response
    var aiResponse by remember { mutableStateOf("Result will appear here...") }
    // Stores the loading state
    var isLoading by remember { mutableStateOf(false) }

    // 2. SETUP COROUTINE SCOPE
    // This allows us to run suspend functions inside the Button onClick
    val scope = rememberCoroutineScope()

    // 3. SETUP LOGIC CLASS
    // We remember this class so it isn't recreated on every recomposition
    val geminiLogic = remember { GeminiLogic() }

    val runLoginScript = remember { Logger() }

    Column(modifier.padding(24.dp)) {

        // Input Field
        TextField(
            value = userInput,
            onValueChange = { userInput = it }, // Allows typing
            label = { Text("Enter prompt") },
            modifier = modifier.padding(vertical = 15.dp)
        )

        // Generate Button
        Button(
            enabled = !isLoading, // D

            // isable button while loading
            onClick = {
                // Launch the Coroutine
                scope.launch {
                    isLoading = true
                    aiResponse = "Loading..."

                    try {
                        val result = geminiLogic.generate_content(userInput)
                        aiResponse = result ?: "No response"
                    } catch (e: Exception) {
                        aiResponse = "Error: ${e.localizedMessage}"
                        Log.e("Gemini", "Error", e)
                    } finally {
                        isLoading = false
                    }
                }
            }
        ) {
            Text(text = if (isLoading) "GENERATING..." else "GENERATE")
        }

        // Display Result
        Text(
            text = aiResponse,
            modifier = Modifier.padding(top = 16.dp)
        )

    }
}
@Composable
fun LoginScreen(logger: Logger) {
    var aiResponse by remember { mutableStateOf("Tayyor...") }
    // New state to track progress bar (starts at 0%)
    var currentProgress by remember { mutableStateOf(0f) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Auto-scroll logic
    LaunchedEffect(aiResponse) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- PROGRESS BAR AREA ---
            Text(text = "Jarayon: ${(currentProgress * 100).toInt()}%") // Shows "50%"

            LinearProgressIndicator(
                progress = currentProgress, // Uses our state
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(10.dp),
                color = Color.Green,
                trackColor = Color.LightGray
            )

            // --- SCROLLABLE LOG AREA ---
            Box(
                modifier = Modifier
                    .height(150.dp)
                    .verticalScroll(scrollState)
                    .background(Color.Black.copy(alpha = 0.05f))
                    .padding(8.dp)
            ) {
                Text(text = aiResponse, fontSize = 14.sp)
            }

            // --- YOUR BUTTON ---
            Button(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .size(width = 250.dp, height = 70.dp),
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        aiResponse = "Boshlandi...\n"
                        currentProgress = 0f // Reset progress bar

                        logger.runLoginScript().collect { state ->
                            // Update the log text
                            aiResponse += "${state.message}\n"
                            // Update the progress bar percentage
                            currentProgress = state.percentage
                        }
                    }
                }
            ) {
                Text("Kirishni boshlash", fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun OnboardingScreen(onContinuedClicked: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to the Basics Codelab!")
        Button(
            modifier = Modifier.padding(vertical = 24.dp),
            onClick = onContinuedClicked
        ) {
            Text("Continue")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        LoginScreen(Logger())
    }
}