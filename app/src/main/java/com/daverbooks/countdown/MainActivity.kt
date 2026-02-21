package com.daverbooks.countdown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.daverbooks.countdown.ui.theme.MyCountDownTheme
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Countdown(
    val id: Long,
    val name: String,
    val description: String,
    val targetDateTime: LocalDateTime,
    val color: Color = Color.Gray
)

val PresetColors = listOf(
    Color(0xFFE57373), // Red
    Color(0xFFF06292), // Pink
    Color(0xFFBA68C8), // Purple
    Color(0xFF9575CD), // Deep Purple
    Color(0xFF7986CB), // Indigo
    Color(0xFF64B5F6), // Blue
    Color(0xFF4FC3F7), // Light Blue
    Color(0xFF4DD0E1), // Cyan
    Color(0xFF4DB6AC), // Teal
    Color(0xFF81C784), // Green
    Color(0xFFAED581), // Light Green
    Color(0xFFFFD54F), // Amber
    Color(0xFFFFB74D), // Orange
    Color(0xFFFF8A65), // Deep Orange
    Color(0xFFA1887F), // Brown
    Color(0xFF90A4AE)  // Blue Grey
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyCountDownTheme {
                CountdownApp()
            }
        }
    }
}

@Composable
fun CountdownApp() {
    var countdowns by remember { mutableStateOf(listOf<Countdown>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCountdown by remember { mutableStateOf<Countdown?>(null) }

    LaunchedEffect(Unit) {
        val now = LocalDateTime.now()
        countdowns = listOf(
            Countdown(1, "Long Countdown", "", LocalDateTime.of(2026, 7, 10, 17, 0, 0), PresetColors[0]),
            Countdown(2, "Day Plus", "", now.plusDays(1).plusHours(1).plusMinutes(1).plusSeconds(1), PresetColors[5]),
            Countdown(3, "Hour Plus", "", now.plusHours(1).plusMinutes(1).plusSeconds(1), PresetColors[9]),
            Countdown(4, "Minute Plus", "This is a description", now.plusMinutes(1).plusSeconds(1), PresetColors[12]),
            Countdown(5, "Minute Ago", "", now.minusMinutes(1), PresetColors[15]),
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Countdown")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(countdowns, key = { it.id }) { countdown ->
                    CountdownCard(
                        countdown = countdown,
                        onEdit = { editingCountdown = countdown }
                    )
                }
            }
        }

        if (showAddDialog) {
            CountdownDialog(
                title = "Add New Countdown",
                onDismiss = { showAddDialog = false },
                onConfirm = { name, description, dateTime, color ->
                    val newCountdown = Countdown(
                        id = System.currentTimeMillis(),
                        name = name,
                        description = description,
                        targetDateTime = dateTime,
                        color = color
                    )
                    countdowns = countdowns + newCountdown
                    showAddDialog = false
                }
            )
        }

        editingCountdown?.let { countdown ->
            CountdownDialog(
                title = "Edit Countdown",
                initialCountdown = countdown,
                onDismiss = { editingCountdown = null },
                onConfirm = { name, description, dateTime, color ->
                    countdowns = countdowns.map {
                        if (it.id == countdown.id) it.copy(name = name, description = description, targetDateTime = dateTime, color = color) else it
                    }
                    editingCountdown = null
                },
                onDelete = {
                    countdowns = countdowns.filter { it.id != countdown.id }
                    editingCountdown = null
                }
            )
        }
    }
}

@Composable
fun CountdownCard(countdown: Countdown, onEdit: () -> Unit) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalDateTime.now()
            delay(1000)
        }
    }

    val duration = Duration.between(currentTime, countdown.targetDateTime)
    val isRunning = !duration.isNegative
    
    val backgroundColor = if (isRunning) {
        countdown.color
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    
    val contentColor = if (isRunning) {
        Color.White // Assume white text looks good on these preset colors
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Countdown", tint = contentColor)
            }

            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = countdown.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (countdown.description.isNotBlank()) {
                    Text(
                        text = countdown.description,
                        fontSize = 14.sp,
                        color = contentColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (!isRunning) {
                    Text(
                        text = "Finished",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    val days = duration.toDays()
                    val hours = (duration.toHours() % 24).toInt()
                    val minutes = (duration.toMinutes() % 60).toInt()
                    val seconds = (duration.seconds % 60).toInt()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Top
                    ) {
                        var showPrevious = false

                        if (days > 0) {
                            CountdownUnit(days, "Day", "Days", contentColor)
                            showPrevious = true
                        }
                        if (showPrevious || hours > 0) {
                            if (showPrevious) CountdownSeparator(contentColor)
                            CountdownUnit(hours.toLong(), "Hour", "Hours", contentColor)
                            showPrevious = true
                        }
                        if (showPrevious || minutes > 0) {
                            if (showPrevious) CountdownSeparator(contentColor)
                            CountdownUnit(minutes.toLong(), "Minute", "Minutes", contentColor)
                            showPrevious = true
                        }
                        if (showPrevious) CountdownSeparator(contentColor)
                        CountdownUnit(seconds.toLong(), "Second", "Seconds", contentColor)
                    }
                }
            }
        }
    }
}

@Composable
fun CountdownUnit(value: Long, singular: String, plural: String, contentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (singular == "Day") value.toString() else String.format(Locale.getDefault(), "%02d", value),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            lineHeight = 28.sp,
            color = contentColor
        )
        Text(
            text = if (value == 1L) singular else plural,
            fontSize = 10.sp,
            style = MaterialTheme.typography.labelSmall,
            lineHeight = 10.sp,
            color = contentColor.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun CountdownSeparator(contentColor: Color) {
    Text(
        text = ":",
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(horizontal = 4.dp),
        lineHeight = 28.sp,
        color = contentColor
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownDialog(
    title: String,
    initialCountdown: Countdown? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, LocalDateTime, Color) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialCountdown?.name ?: "") }
    var description by remember { mutableStateOf(initialCountdown?.description ?: "") }
    var selectedColor by remember { mutableStateOf(initialCountdown?.color ?: PresetColors[0]) }
    
    val nowDateTime = LocalDateTime.now()
    val initialDateTime = initialCountdown?.targetDateTime ?: nowDateTime
    var selectedDate by remember { mutableStateOf(initialDateTime.toLocalDate()) }
    var selectedTime by remember { mutableStateOf(initialDateTime.toLocalTime()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(selectedDate.format(dateFormatter))
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(selectedTime.format(timeFormatter))
                    }
                }

                Text("Quick Add", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            val current = LocalDateTime.of(selectedDate, selectedTime)
                            val newDt = current.plusHours(1)
                            selectedDate = newDt.toLocalDate()
                            selectedTime = newDt.toLocalTime()
                        },
                        label = { Text("+1 Hour") }
                    )
                    AssistChip(
                        onClick = {
                            val current = LocalDateTime.of(selectedDate, selectedTime)
                            val newDt = current.plusDays(1)
                            selectedDate = newDt.toLocalDate()
                            selectedTime = newDt.toLocalTime()
                        },
                        label = { Text("+1 Day") }
                    )
                    AssistChip(
                        onClick = {
                            val current = LocalDateTime.of(selectedDate, selectedTime)
                            val newDt = current.plusWeeks(1)
                            selectedDate = newDt.toLocalDate()
                            selectedTime = newDt.toLocalTime()
                        },
                        label = { Text("+1 Week") }
                    )
                }

                Text("Pick a Color", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(PresetColors) { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(name, description, LocalDateTime.of(selectedDate, selectedTime), selectedColor)
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (initialCountdown == null) "Add" else "Save")
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDelete != null) {
                    TextButton(onClick = {
                        val duration = Duration.between(LocalDateTime.now(), initialDateTime)
                        if (!duration.isNegative) {
                            showDeleteConfirm = true
                        } else {
                            onDelete()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Spacer(Modifier.width(1.dp)) // Placeholder
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Countdown") },
            text = { Text("Are you sure you want to delete this running countdown?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete?.invoke()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Time",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}
