package com.genegebra.healthtracker.presentation.entry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

    val s = systolic.toIntOrNull()
    val d = diastolic.toIntOrNull()
    val p = pulse.toIntOrNull()
    if (d != null && p != null && p > 0) {
        Spacer(Modifier.height(16.dp))
        KerdoIndexCard(kerdoIndex = (1.0 - d.toDouble() / p.toDouble()) * 100.0)
    }
    if (s != null && p != null) {
        Spacer(Modifier.height(8.dp))
        RobinsonIndexCard(robinsonIndex = s.toDouble() * p.toDouble() / 100.0)
    }

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
    entry.kerdoIndex?.let { index ->
        Spacer(Modifier.height(8.dp))
        KerdoIndexCard(kerdoIndex = index)
    }
    entry.robinsonIndex?.let { index ->
        Spacer(Modifier.height(8.dp))
        RobinsonIndexCard(robinsonIndex = index)
    }
}

@Composable
private fun KerdoIndexCard(kerdoIndex: Double) {
    val (label, description, color) = when {
        kerdoIndex > 0 -> Triple("Sympathicotonia", "Fight or Flight", MaterialTheme.colorScheme.error)
        kerdoIndex < 0 -> Triple("Vagotonia", "Rest & Digest", Color(0xFF2E7D32))
        else -> Triple("Eutonia", "Balanced", MaterialTheme.colorScheme.primary)
    }
    val sign = if (kerdoIndex > 0) "+" else ""
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Kerdo Index", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(label, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "$sign${"%.1f".format(kerdoIndex)}",
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RobinsonIndexCard(robinsonIndex: Double) {
    val (label, description, color) = when {
        robinsonIndex < 76 -> Triple("Excellent", "Low myocardial demand", Color(0xFF2E7D32))
        robinsonIndex < 90 -> Triple("Good", "Moderate demand", MaterialTheme.colorScheme.primary)
        robinsonIndex <= 110 -> Triple("Normal", "Average demand", Color(0xFFE65100))
        else -> Triple("Elevated", "High myocardial demand", MaterialTheme.colorScheme.error)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Robinson Index (RPP)", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(label, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${"%.1f".format(robinsonIndex)}",
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
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
