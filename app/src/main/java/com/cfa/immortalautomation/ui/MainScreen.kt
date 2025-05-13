package com.cfa.immortalautomation.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Home screen with three main actions + a navigation
 * toggle to the script list.
 */
@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    val ctx: Context = LocalContext.current
    var showList by remember { mutableStateOf(false) }

    if (showList) {
        ScriptListScreen { showList = false }
        return   // early‑return so we don’t also draw the buttons
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = { vm.requestOverlay(ctx) }) {
            Text("Grant overlay & start widget")
        }
        Button(onClick = { vm.requestAccessibility(ctx) }) {
            Text("Enable accessibility service")
        }
        Button(onClick = { vm.runScript(ctx) }) {
            Text("Run recorded script")
        }
        Button(onClick = { showList = true }) {
            Text("View scripts")
        }
        Button(onClick = { vm.saveCurrent(ctx) }) {
            Text("Save current recording")
        }
    }
}
