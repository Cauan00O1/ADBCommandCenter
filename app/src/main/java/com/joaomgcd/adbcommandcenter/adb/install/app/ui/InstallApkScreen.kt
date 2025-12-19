package com.joaomgcd.adbcommandcenter.adb.install.app.ui

import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joaomgcd.adbcommandcenter.adb.install.app.domain.InstallableFileInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallApkScreen(
    viewModel: InstallApkViewModel = hiltViewModel(),
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Install Application") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is InstallApkUiState.Loading -> CircularProgressIndicator()
                is InstallApkUiState.Error -> ErrorState(message = state.message, onDone = onBackPressed)
                is InstallApkUiState.ReadyToInstall -> ConfirmationState(state = state, onConfirm = viewModel::onInstallConfirmed)
                is InstallApkUiState.Installing -> InstallingState(state = state)
                is InstallApkUiState.Success -> SuccessState(state = state, onDone = onBackPressed)
            }
        }
    }
}

@Composable
private fun ConfirmationState(state: InstallApkUiState.ReadyToInstall, onConfirm: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Ready to Install", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        FileInfoCard(fileInfo = state.fileInfo)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
            Text("Install")
        }
    }
}

@Composable
private fun InstallingState(state: InstallApkUiState.Installing) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Installing...", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text(state.fileInfo.fileName, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))
        CircularProgressIndicator()
    }
}

@Composable
private fun SuccessState(state: InstallApkUiState.Success, onDone: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.CheckCircle, contentDescription = "Success", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Successfully Installed", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(state.fileInfo.fileName, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}

@Composable
private fun ErrorState(message: String, onDone: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.Error, contentDescription = "Error", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(24.dp))
        Text("An Error Occurred", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
private fun FileInfoCard(fileInfo: InstallableFileInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(fileInfo.fileName, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            val formattedSize = Formatter.formatShortFileSize(LocalContext.current, fileInfo.fileSizeInBytes)
            Text("Size: $formattedSize", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Text("From: ${fileInfo.filePath}", style = MaterialTheme.typography.bodySmall)
        }
    }
}