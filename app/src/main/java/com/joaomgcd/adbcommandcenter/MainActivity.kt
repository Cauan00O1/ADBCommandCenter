package com.joaomgcd.adbcommandcenter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.joaomgcd.adbcommandcenter.ui.AdbControlCenterApp
import com.joaomgcd.adbcommandcenter.ui.theme.ADBCommandCenterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ADBCommandCenterTheme {
                AdbControlCenterApp()
            }
        }
    }
}

