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
    val remaining = if (duration.isNegative) "Finished" else {
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60
        String.format(Locale.getDefault(), "%02d : %02d : %02d : %02d", days, hours, minutes, seconds)
    }

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
                fontWeight = FontWeight.Bold
            )
            Text(
                text = countdown.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = remaining,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
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
