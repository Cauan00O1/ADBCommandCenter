package com.joaomgcd.adbcommandcenter.adb.filebrowser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSelectionScreen(
    viewModel: FileSelectionViewModel = hiltViewModel(),
    onFileSelected: (String) -> Unit,
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.currentPath) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.currentPath == "/") {
                            onBackPressed()
                        } else {
                            viewModel.onUpClicked()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Up")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.error != null) {
                Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.files) { file ->
                        RemoteFileItem(
                            file = file,
                            modifier = Modifier.clickable {
                                if (file.isDirectory) {
                                    viewModel.onFileClicked(file)
                                } else {
                                    onFileSelected(file.path)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}