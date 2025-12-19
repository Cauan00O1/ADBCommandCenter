package com.joaomgcd.adbcommandcenter.adb.permissions.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.joaomgcd.adb.AdbPermission

@Composable
fun PermissionItem(
    permission: AdbPermission.Single,
    isGranted: Boolean,
    onGrant: () -> Unit,
    onRevoke: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = permission.displayName,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = permission.requiredFor,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row {
            OutlinedButton(
                onClick = onGrant,
                enabled = !isGranted
            ) {
                Text("Grant")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRevoke,
                enabled = isGranted
            ) {
                Text("Revoke")
            }
        }
    }
}