package com.genegebra.healthtracker.presentation.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.genegebra.healthtracker.domain.model.HealthEntry
import java.text.SimpleDateFormat
import java.util.*

private val dateFormat = SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault())
private val shortDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

@Composable
fun HistoryScreen(
    onNavigateToEntry: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }

    if (showFilterDialog) {
        DateFilterDialog(
            currentFilter = uiState.filter,
            onApply = { from, to ->
                viewModel.setFilter(from, to)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false },
            onClear = {
                viewModel.clearFilter()
                showFilterDialog = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("History", style = MaterialTheme.typography.headlineMedium)
            Row {
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter by date")
                }
            }
        }

        uiState.filter.from?.let { from ->
            val to = uiState.filter.to
            val label = if (to != null) {
                "${shortDateFormat.format(from)} → ${shortDateFormat.format(to)}"
            } else {
                "From ${shortDateFormat.format(from)}"
            }
            Spacer(Modifier.height(4.dp))
            FilterChip(
                selected = true,
                onClick = { viewModel.clearFilter() },
                label = { Text(label) }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        if (uiState.entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No entries found.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.entries, key = { it.id }) { entry ->
                    HealthEntryCard(
                        entry = entry,
                        showUserEmail = uiState.currentUser?.isAdmin == true
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthEntryCard(entry: HealthEntry, showUserEmail: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(dateFormat.format(entry.createdAt), style = MaterialTheme.typography.labelSmall)
                if (showUserEmail) {
                    Text(entry.userEmail, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricChip("Sys", entry.systolic, "mmHg")
                MetricChip("Dia", entry.diastolic, "mmHg")
                MetricChip("Pulse", entry.pulse, "bpm")
                MetricChip("Anxiety", entry.anxietyLevel, "/10")
            }
            entry.kerdoIndex?.let { index ->
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                KerdoIndexRow(kerdoIndex = index)
            }
        }
    }
}

@Composable
private fun KerdoIndexRow(kerdoIndex: Double) {
    val (label, color) = when {
        kerdoIndex > 0 -> "Sympathicotonia · Fight or Flight" to MaterialTheme.colorScheme.error
        kerdoIndex < 0 -> "Vagotonia · Rest & Digest" to Color(0xFF2E7D32)
        else -> "Eutonia · Balanced" to MaterialTheme.colorScheme.primary
    }
    val sign = if (kerdoIndex > 0) "+" else ""
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Kerdo Index", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.bodySmall, color = color)
        }
        Text(
            "$sign${"%.1f".format(kerdoIndex)}",
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MetricChip(label: String, value: Int?, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (value != null) "$value" else "—",
            style = MaterialTheme.typography.titleMedium
        )
        if (value != null) {
            Text(unit, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DateFilterDialog(
    currentFilter: HistoryFilter,
    onApply: (Date?, Date?) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    var fromText by remember { mutableStateOf(currentFilter.from?.let { shortDateFormat.format(it) } ?: "") }
    var toText by remember { mutableStateOf(currentFilter.to?.let { shortDateFormat.format(it) } ?: "") }
    var parseError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
        title = { Text("Filter by date") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Format: dd MMM yyyy (e.g. 01 Jan 2025)", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = fromText,
                    onValueChange = { fromText = it },
                    label = { Text("From") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = toText,
                    onValueChange = { toText = it },
                    label = { Text("To") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (parseError.isNotEmpty()) {
                    Text(parseError, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val from = fromText.takeIf { it.isNotBlank() }?.let {
                    runCatching { shortDateFormat.parse(it) }.getOrNull()
                }
                val to = toText.takeIf { it.isNotBlank() }?.let {
                    runCatching { shortDateFormat.parse(it) }.getOrNull()
                }
                if ((fromText.isNotBlank() && from == null) || (toText.isNotBlank() && to == null)) {
                    parseError = "Invalid date format."
                } else {
                    onApply(from, to)
                }
            }) { Text("Apply") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
