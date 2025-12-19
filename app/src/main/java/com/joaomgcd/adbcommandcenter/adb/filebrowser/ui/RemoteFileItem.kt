package com.joaomgcd.adbcommandcenter.adb.filebrowser.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.joaomgcd.adbcommandcenter.adb.filebrowser.domain.RemoteFile

@Composable
fun RemoteFileItem(file: RemoteFile, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile
        Icon(
            imageVector = icon,
            contentDescription = if (file.isDirectory) "Directory" else "File",
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = file.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}