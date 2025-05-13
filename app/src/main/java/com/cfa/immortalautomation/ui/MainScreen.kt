package com.cfa.immortalautomation.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Home screen with record / run / save / list actions.
 *
 * Ajout : trois compteurs indépendants sous les boutons Test A, B, C
 * pour vérifier qu’un replay d’enregistrement émet bien 1:1 tap events.
 */
@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    val ctx: Context = LocalContext.current
    var showList by remember { mutableStateOf(false) }

    /* ---------- compteurs test ---------- */
    var countA by remember { mutableStateOf(0) }
    var countB by remember { mutableStateOf(0) }
    var countC by remember { mutableStateOf(0) }

    if (showList) {
        ScriptListScreen { showList = false }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        /* ── actions principales ────────────────────── */
        Button(onClick = { vm.requestOverlay(ctx) })   { Text("Grant overlay & start widget") }
        Button(onClick = { vm.requestAccessibility(ctx) }) { Text("Enable accessibility service") }
        Button(onClick = { vm.runScript(ctx) })        { Text("Run recorded script") }
        Button(onClick = { vm.saveCurrent(ctx) })      { Text("Save current recording") }
        Button(onClick = { showList = true })          { Text("View scripts") }

        Spacer(Modifier.height(20.dp))

        /* ── zone de test des clics ─────────────────── */
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            /* ---------- Bouton A ---------- */
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { countA++ }) { Text("Test A") }
                Text("Count : $countA")
            }

            /* ---------- Bouton B ---------- */
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { countB++ }) { Text("Test B") }
                Text("Count : $countB")
            }

            /* ---------- Bouton C ---------- */
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { countC++ }) { Text("Test C") }
                Text("Count : $countC")
            }
        }
    }
}
