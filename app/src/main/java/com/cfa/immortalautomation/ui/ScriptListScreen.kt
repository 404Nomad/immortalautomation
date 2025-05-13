package com.cfa.immortalautomation.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cfa.immortalautomation.automation.AutomationAccessibilityService
import com.cfa.immortalautomation.data.ScriptRepository
import java.io.File

@Composable
fun ScriptListScreen(onClose: () -> Unit = {}) {
    val ctx: Context = LocalContext.current
    var scripts by remember { mutableStateOf(ScriptRepository.all(ctx)) }

    fun refresh() { scripts = ScriptRepository.all(ctx) }

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Scripts") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            items(items = scripts, key = File::getName) { file ->
                ScriptRow(
                    file = file,
                    onRun = {
                        AutomationAccessibilityService.instance
                            ?.playScript(file)
                            ?: onClose().also { /* prompt user to enable service */ }
                    },
                    onDelete = { ScriptRepository.delete(file); refresh() }
                )
            }
        }
    }
}

@Composable
private fun ScriptRow(
    file: File,
    onRun: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = file.nameWithoutExtension)
            Row {
                TextButton(onClick = onRun) { Text("Run") }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}
