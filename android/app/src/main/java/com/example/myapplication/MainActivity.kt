package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Logic Classes
    val logger = remember { Logger(context) }
    val credManager = remember { CredentialsManager(context) }
    val updater = remember { AppUpdater(context) }

    // --- STRINGS (Localized) ---
    val strLogsDefault = stringResource(R.string.logs_default)
    val strParsing = stringResource(R.string.parsing)
    val strScriptFinished = stringResource(R.string.script_finished)
    val strStart = stringResource(R.string.start_btn)
    val strRunning = stringResource(R.string.running_btn)
    val strNoExcel = stringResource(R.string.no_excel)
    val strUpload = stringResource(R.string.upload_btn)

    // Update Dialog Strings
    val strUpdateTitle = stringResource(R.string.update_title)
    val strUpdateMsg = stringResource(R.string.update_msg)
    val strDownloading = stringResource(R.string.downloading)
    val strUpdateConfirm = stringResource(R.string.update_confirm)
    val strCancel = stringResource(R.string.cancel)

    // --- UI STATES ---
    var logs by remember { mutableStateOf("$strLogsDefault\n") }
    var progress by remember { mutableFloatStateOf(0f) }
    var isRunning by remember { mutableStateOf(false) }
    var userCountMsg by remember { mutableStateOf("") }

    // --- UPDATE STATES ---
    var updateAvailableUrl by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    val scrollState = rememberScrollState()

    // Current App Version
    val currentVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) { "1.0" }
    }

    // --- AUTO-UPDATE CHECK (Runs once when app opens) ---
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val url = updater.checkForUpdate(currentVersionName)
            if (url != null) {
                // If update found, show dialog
                updateAvailableUrl = url
            }
        }
    }

    // Auto-scroll logs
    LaunchedEffect(logs) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // File Picker
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) { userCountMsg = strParsing }
                val count = credManager.parseExcelAndSave(uri)
                withContext(Dispatchers.Main) {
                    if (count >= 0) {
                        userCountMsg = context.getString(R.string.excel_loaded, count)
                        logs += "System: $userCountMsg\n"
                    } else {
                        userCountMsg = context.getString(R.string.excel_error)
                        logs += "System: $userCountMsg\n"
                    }
                }
            }
        }
    }

    // --- UI LAYOUT ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 1. Upload Button
        Button(
            onClick = {
                fileLauncher.launch(arrayOf(
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
            },
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strUpload)
        }

        // Status Text
        Text(
            text = if (userCountMsg.isEmpty()) strNoExcel else userCountMsg,
            fontSize = 12.sp,
            color = if (userCountMsg.contains("✅")) Color.Green else Color.Gray,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Divider()
        Spacer(modifier = Modifier.height(10.dp))

        // Progress
        Text(text = "Progress: ${(progress * 100).toInt()}%")
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .padding(vertical = 5.dp),
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Logcat Terminal
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = logs,
                    color = Color.Green,
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Start Button
        Button(
            onClick = {
                if (!isRunning) {
                    isRunning = true
                    progress = 0f
                    logs = "🚀 $strRunning\n"

                    scope.launch(Dispatchers.IO) {
                        logger.runLoginScript().collect { update ->
                            withContext(Dispatchers.Main) {
                                logs += "${update.message}\n"
                                progress = update.percentage
                            }
                        }

                        withContext(Dispatchers.Main) {
                            isRunning = false
                            logs += "$strScriptFinished\n"
                        }
                    }
                }
            },
            enabled = !isRunning,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Color.Gray else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isRunning) strRunning else strStart,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Version Footer
        Text(
            text = "v$currentVersionName",
            fontSize = 10.sp,
            color = Color.Gray
        )
    }

    // --- UPDATE DIALOG (Shows only if update found) ---
    if (updateAvailableUrl != null) {
        AlertDialog(
            onDismissRequest = { updateAvailableUrl = null },
            title = { Text(strUpdateTitle) },
            text = {
                if (isDownloading) {
                    Column {
                        Text("$strDownloading ${(downloadProgress * 100).toInt()}%")
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { downloadProgress })
                    }
                } else {
                    Text(strUpdateMsg)
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    Button(onClick = {
                        isDownloading = true
                        scope.launch {
                            // Android 8+ Permission Check
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                if (!context.packageManager.canRequestPackageInstalls()) {
                                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                    intent.data = Uri.parse("package:${context.packageName}")
                                    context.startActivity(intent)
                                    isDownloading = false
                                    return@launch
                                }
                            }
                            // Download
                            val file = updater.downloadApk(updateAvailableUrl!!) { p ->
                                downloadProgress = p
                            }
                            // Install
                            if (file != null) updater.installApk(file)
                            else Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()

                            isDownloading = false
                            updateAvailableUrl = null
                        }
                    }) { Text(strUpdateConfirm) }
                }
            },
            dismissButton = {
                if (!isDownloading) {
                    TextButton(onClick = { updateAvailableUrl = null }) {
                        Text(strCancel)
                    }
                }
            }
        )
    }
}