package com.tools.textextracttool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tools.textextracttool.ui.HookConfigApp

/**
 * Hosts the Compose entry point for configuring hook parameters.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HookConfigApp()
        }
    }
}
