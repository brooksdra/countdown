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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daverbooks.countdown.ui.theme.MyCountDownTheme
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime
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
                items(countdowns) { countdown ->
                    CountdownCard(countdown)
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
fun CountdownCard(countdown: Countdown) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalDateTime.now()
            delay(1000)
        }
    }

    val duration = Duration.between(currentTime, countdown.targetDateTime)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
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

            if (duration.isNegative) {
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

@Composable
fun AddCountdownDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, LocalDateTime) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf(LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Countdown") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                TextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                TextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Target Date/Time (YYYY-MM-DDTHH:MM:SS)") }
                )
                Text(
                    text = "Format: 2023-12-31T23:59:59",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    val dateTime = LocalDateTime.parse(dateString)
                    onConfirm(name, description, dateTime)
                } catch (_: Exception) {
                    // In a real app, show an error message
                }
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
}
