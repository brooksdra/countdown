package com.daverbooks.countdown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Countdown(
    val id: Long,
    val name: String,
    val description: String,
    val targetDateTime: LocalDateTime
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
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val now = LocalDateTime.now()
        countdowns = listOf(
            Countdown(1, "Long Countdown", "", LocalDateTime.of(2026, 7, 10, 17, 0, 0)),
            Countdown(2, "Day Plus", "", now.plusDays(1).plusHours(1).plusMinutes(1).plusSeconds(1)),
            Countdown(3, "Hour Plus", "", now.plusHours(1).plusMinutes(1).plusSeconds(1)),
            Countdown(4, "Minute Plus", "", now.plusMinutes(1).plusSeconds(1)),
            Countdown(5, "Minute Ago", "", now.minusMinutes(1)),
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
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
                        onDelete = {
                            countdowns = countdowns.filter { it.id != countdown.id }
                        }
                    )
                }
            }
        }

        if (showDialog) {
            AddCountdownDialog(
                onDismiss = { showDialog = false },
                onConfirm = { name, description, dateTime ->
                    val newCountdown = Countdown(
                        id = System.currentTimeMillis(),
                        name = name,
                        description = description,
                        targetDateTime = dateTime
                    )
                    countdowns = countdowns + newCountdown
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun CountdownCard(countdown: Countdown, onDelete: () -> Unit) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalDateTime.now()
            delay(1000)
        }
    }

    val duration = Duration.between(currentTime, countdown.targetDateTime)
    val isRunning = !duration.isNegative

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = {
                    if (isRunning) {
                        showDeleteConfirm = true
                    } else {
                        onDelete()
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Countdown")
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
                Text(
                    text = countdown.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))

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
                            CountdownUnit(days, "Day", "Days")
                            showPrevious = true
                        }
                        if (showPrevious || hours > 0) {
                            if (showPrevious) CountdownSeparator()
                            CountdownUnit(hours.toLong(), "Hour", "Hours")
                            showPrevious = true
                        }
                        if (showPrevious || minutes > 0) {
                            if (showPrevious) CountdownSeparator()
                            CountdownUnit(minutes.toLong(), "Minute", "Minutes")
                            showPrevious = true
                        }
                        if (showPrevious) CountdownSeparator()
                        CountdownUnit(seconds.toLong(), "Second", "Seconds")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Countdown") },
            text = { Text("Are you sure you want to delete the running countdown \"${countdown.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
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
}

@Composable
fun CountdownUnit(value: Long, singular: String, plural: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (singular == "Day") value.toString() else String.format(Locale.getDefault(), "%02d", value),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            lineHeight = 28.sp
        )
        Text(
            text = if (value == 1L) singular else plural,
            fontSize = 10.sp,
            style = MaterialTheme.typography.labelSmall,
            lineHeight = 10.sp
        )
    }
}

@Composable
fun CountdownSeparator() {
    Text(
        text = ":",
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(horizontal = 4.dp),
        lineHeight = 28.sp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCountdownDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, LocalDateTime) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    val now = LocalDateTime.now()
    var selectedDate by remember { mutableStateOf(now.toLocalDate()) }
    var selectedTime by remember { mutableStateOf(now.toLocalTime()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Countdown") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
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
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(name, description, LocalDateTime.of(selectedDate, selectedTime))
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

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
