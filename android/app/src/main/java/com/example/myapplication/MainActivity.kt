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
import androidx.compose.ui.res.stringResource // CRITICAL IMPORT
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

    val logger = remember { Logger(context) }
    val credManager = remember { CredentialsManager(context) }
    val updater = remember { AppUpdater(context) }

    // Get localized strings for dynamic logic
    val strLogsDefault = stringResource(R.string.logs_default)
    val strParsing = stringResource(R.string.parsing)
    val strScriptFinished = stringResource(R.string.script_finished)
    val strStart = stringResource(R.string.start_btn)
    val strRunning = stringResource(R.string.running_btn)

    // --- UI STATES ---
    var logs by remember { mutableStateOf("$strLogsDefault\n") }
    var progress by remember { mutableFloatStateOf(0f) }
    var isRunning by remember { mutableStateOf(false) }
    var userCountMsg by remember { mutableStateOf("") } // Intentionally empty initially

    val scrollState = rememberScrollState()

    LaunchedEffect(logs) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) { userCountMsg = strParsing }
                val count = credManager.parseExcelAndSave(uri)
                withContext(Dispatchers.Main) {
                    if (count >= 0) {
                        // Use context.getString for formatting parameters (%d)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        UpdateCheckerSection(updater, context)

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Upload Button (Localized)
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
            Text(stringResource(R.string.upload_btn))
        }

        // Status Text
        Text(
            text = if (userCountMsg.isEmpty()) stringResource(R.string.no_excel) else userCountMsg,
            fontSize = 12.sp,
            color = if (userCountMsg.contains("✅")) Color.Green else Color.Gray,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Divider()
        Spacer(modifier = Modifier.height(10.dp))

        Text(text = "Progress: ${(progress * 100).toInt()}%")
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .padding(vertical = 5.dp),
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Logcat
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

        // 2. Start Button (Localized)
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
    }
}

@Composable
fun UpdateCheckerSection(updater: AppUpdater, context: android.content.Context) {
    val scope = rememberCoroutineScope()
    var updateAvailableUrl by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    val currentVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) { "1.0" }
    }

    // Strings for logic
    val strChecking = stringResource(R.string.checking)
    val strLatest = stringResource(R.string.latest_version)
    val strDownloadFail = "Download failed"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("v$currentVersionName", fontSize = 12.sp, color = Color.Gray)

        TextButton(onClick = {
            scope.launch {
                Toast.makeText(context, strChecking, Toast.LENGTH_SHORT).show()
                val url = updater.checkForUpdate(currentVersionName)
                if (url != null) {
                    updateAvailableUrl = url
                } else {
                    Toast.makeText(context, strLatest, Toast.LENGTH_SHORT).show()
                }
            }
        }, enabled = !isDownloading) {
            Text(stringResource(R.string.check_update))
        }
    }

    if (updateAvailableUrl != null) {
        AlertDialog(
            onDismissRequest = { updateAvailableUrl = null },
            title = { Text(stringResource(R.string.update_title)) },
            text = {
                if (isDownloading) {
                    Column {
                        Text("${stringResource(R.string.downloading)} ${(downloadProgress * 100).toInt()}%")
                        LinearProgressIndicator(progress = { downloadProgress })
                    }
                } else {
                    Text(stringResource(R.string.update_msg))
                }
            },
            confirmButton = {
                if (!isDownloading) {
                    Button(onClick = {
                        isDownloading = true
                        scope.launch {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                if (!context.packageManager.canRequestPackageInstalls()) {
                                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                    intent.data = Uri.parse("package:${context.packageName}")
                                    context.startActivity(intent)
                                    isDownloading = false
                                    return@launch
                                }
                            }
                            val file = updater.downloadApk(updateAvailableUrl!!) { p ->
                                downloadProgress = p
                            }
                            if (file != null) updater.installApk(file)
                            else Toast.makeText(context, strDownloadFail, Toast.LENGTH_SHORT).show()

                            isDownloading = false
                            updateAvailableUrl = null
                        }
                    }) { Text(stringResource(R.string.update_confirm)) }
                }
            },
            dismissButton = {
                if (!isDownloading) TextButton(onClick = { updateAvailableUrl = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}