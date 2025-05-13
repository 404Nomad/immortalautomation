package com.cfa.immortalautomation.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Three‑button home screen.
 */
@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    val ctx: Context = LocalContext.current

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
    }
}
