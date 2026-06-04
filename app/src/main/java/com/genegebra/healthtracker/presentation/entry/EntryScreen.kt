package com.genegebra.healthtracker.presentation.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EntryScreen(
    onNavigateToHistory: () -> Unit,
    viewModel: EntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val existing = uiState.existingEntry

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Log Health Data", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Session · ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        if (existing != null) {
            EntryReadOnlyCard(existing)
            Spacer(Modifier.height(16.dp))
            Text(
                "You have already logged data for this session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Hold Log for 3 s to log again.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            EntryForm(
                isSaved = uiState.isSaved,
                error = uiState.error,
                onSave = { s, d, p, a -> viewModel.saveEntry(s, d, p, a) }
            )
        }

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = onNavigateToHistory,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View History")
        }
    }
}

@Composable
private fun EntryForm(
    isSaved: Boolean,
    error: String?,
    onSave: (String, String, String, String) -> Unit
) {
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }
    var anxietyLevel by remember { mutableStateOf("") }

    if (isSaved) {
        Text("Entry saved successfully!", color = MaterialTheme.colorScheme.primary)
        return
    }

    HealthIntField(value = systolic, onValueChange = { systolic = it }, label = "Systolic pressure (mmHg)", range = 60..250)
    Spacer(Modifier.height(12.dp))
    HealthIntField(value = diastolic, onValueChange = { diastolic = it }, label = "Diastolic pressure (mmHg)", range = 40..150)
    Spacer(Modifier.height(12.dp))
    HealthIntField(value = pulse, onValueChange = { pulse = it }, label = "Pulse (bpm)", range = 30..220)
    Spacer(Modifier.height(12.dp))
    HealthIntField(value = anxietyLevel, onValueChange = { anxietyLevel = it }, label = "Anxiety level (0–10)", range = 0..10)

    error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }

    Spacer(Modifier.height(24.dp))

    val hasAnyValue = listOf(systolic, diastolic, pulse, anxietyLevel).any { it.isNotBlank() }

    Button(
        onClick = { onSave(systolic, diastolic, pulse, anxietyLevel) },
        enabled = hasAnyValue,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Save Entry")
    }

    Spacer(Modifier.height(8.dp))
    Text(
        "All fields are optional. Leave blank to skip.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun HealthIntField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    range: IntRange
) {
    val intValue = value.toIntOrNull()
    val isError = intValue != null && intValue !in range
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            if (new.isEmpty() || new.all { it.isDigit() }) onValueChange(new)
        },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = if (isError) { { Text("Must be ${range.first}–${range.last}") } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EntryReadOnlyCard(entry: com.genegebra.healthtracker.domain.model.HealthEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("This session's entry", style = MaterialTheme.typography.titleMedium)
            ReadOnlyRow("Systolic", entry.systolic, "mmHg")
            ReadOnlyRow("Diastolic", entry.diastolic, "mmHg")
            ReadOnlyRow("Pulse", entry.pulse, "bpm")
            ReadOnlyRow("Anxiety", entry.anxietyLevel, "/ 10")
        }
    }
}

@Composable
private fun ReadOnlyRow(label: String, value: Int?, unit: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(if (value != null) "$value $unit" else "—", style = MaterialTheme.typography.bodyMedium)
    }
}
