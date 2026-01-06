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

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File



// TODO: add sound effect when 100%✅✅✅✅✅✅✅
// TODO: fix dairy check error
// TODO: automate login and password lists
// TODO: ADD SELF UPDATE 🦅🦅🦅🦅🦅🦅🦅🦅🦅🦅🦅✅✅✅✅✅✅✅

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
                // Note: We don't create geminiLogic here anymore.
        super.onCreate(savedInstanceState)

        setContent {

            MyApplicationTheme {
                MyApp()
            }
        }
    }
}

@Composable
fun MyApp(modifier: Modifier = Modifier) {
    var shouldShowOnboarding by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    Surface(modifier) {
        UpdateScreen()
        LoginScreen(Logger(context))
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    val context = LocalContext.current
    MyApplicationTheme {
        LoginScreen(Logger(context))
    }
}

@Composable
fun UpdateScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updater = remember { AppUpdater(context) }

    // State
    var updateAvailableUrl by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    // Current App Version
    val currentVersionName = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            // If versionName is null, default to "1.0"
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }



    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Current Version: $currentVersionName")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            scope.launch {
                // Check for updates
                val url = updater.checkForUpdate(currentVersionName)
                if (url != null) {
                    updateAvailableUrl = url
                } else {
                    Toast.makeText(context, "No update available", Toast.LENGTH_SHORT).show()
                }
            }
        }, enabled = !isDownloading) {
            Text("Check for Updates")
        }

        // Show Dialog if update is found
        if (updateAvailableUrl != null) {
            AlertDialog(
                onDismissRequest = { updateAvailableUrl = null },
                title = { Text("Update Available") },
                text = {
                    if (isDownloading) {
                        Column {
                            Text("Downloading...")
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(progress = { downloadProgress })
                        }
                    } else {
                        Text("A new version is available. Download now?")
                    }
                },
                confirmButton = {
                    if (!isDownloading) {
                        Button(onClick = {
                            isDownloading = true
                            scope.launch {
                                // 1. Check Android 8+ Permission
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    if (!context.packageManager.canRequestPackageInstalls()) {
                                        // Open Settings to allow install
                                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                        intent.data = Uri.parse("package:${context.packageName}")
                                        context.startActivity(intent)
                                        isDownloading = false // Reset UI
                                        return@launch
                                    }
                                }

                                // 2. Download
                                val file = updater.downloadApk(updateAvailableUrl!!) { progress ->
                                    downloadProgress = progress
                                }

                                // 3. Install
                                if (file != null) {
                                    updater.installApk(file)
                                } else {
                                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                }
                                isDownloading = false
                                updateAvailableUrl = null // Close dialog
                            }
                        }) {
                            Text("Update")
                        }
                    }
                },
                dismissButton = {
                    if (!isDownloading) {
                        TextButton(onClick = { updateAvailableUrl = null }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
}