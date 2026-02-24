package com.daverbooks.countdown

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.app.NotificationCompat
import com.daverbooks.countdown.ui.theme.MyCountDownTheme
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

enum class PatternType {
    NONE, BIRTHDAY, RETIREMENT, VALENTINE, ST_PATRICK, SOLSTICE, CHRISTMAS
}

data class Countdown(
    val id: Long,
    val name: String,
    val description: String,
    val targetDateTime: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val color: Color = Color.Gray,
    val isNotificationEnabled: Boolean = false,
    val hasNotified: Boolean = false,
    val patternType: PatternType = PatternType.NONE,
    val isFavorite: Boolean = false
)

enum class SortOption(val displayName: String) {
    MANUAL("Manual"),
    TARGET_DATE("Target Date"),
    NAME("Name"),
    CREATED_AT("Created At")
}

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

class CountdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.daverbooks.countdown.ACTION_COUNTDOWN_FINISHED" -> {
                val name = intent.getStringExtra("countdown_name") ?: "A countdown"
                val id = intent.getLongExtra("countdown_id", 0L)
                triggerNotification(context, name, id)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Rescheduling logic would go here in a production app with a database.
            }
        }
    }

    private fun triggerNotification(context: Context, name: String, id: Long) {
        val builder = NotificationCompat.Builder(context, "COUNTDOWN_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Countdown Finished!")
            .setContentText("$name has reached zero.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id.toInt(), builder.build())
    }
}

private fun scheduleAlarm(context: Context, countdown: Countdown) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "Please grant permission to schedule exact alarms.", Toast.LENGTH_LONG).show()
            try {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            } catch (e: Exception) {
                // Activity not found
            }
            return
        }
    }

    val intent = Intent(context, CountdownReceiver::class.java).apply {
        action = "com.daverbooks.countdown.ACTION_COUNTDOWN_FINISHED"
        putExtra("countdown_name", countdown.name)
        putExtra("countdown_id", countdown.id)
    }
    
    val pendingIntent = PendingIntent.getBroadcast(
        context, 
        countdown.id.toInt(), 
        intent, 
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val triggerTime = countdown.targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    
    try {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    } catch (e: SecurityException) {
        Toast.makeText(context, "Exact alarm permission denied.", Toast.LENGTH_SHORT).show()
    }
}

private fun cancelAlarm(context: Context, countdown: Countdown) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, CountdownReceiver::class.java).apply {
        action = "com.daverbooks.countdown.ACTION_COUNTDOWN_FINISHED"
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, 
        countdown.id.toInt(), 
        intent, 
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (pendingIntent != null) {
        alarmManager.cancel(pendingIntent)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)
        enableEdgeToEdge()
        setContent {
            var appTitle by remember { mutableStateOf("My Countdowns") }
            var isDarkMode by remember { mutableStateOf(false) }
            
            MyCountDownTheme(darkTheme = isDarkMode) {
                CountdownApp(
                    appTitle = appTitle,
                    onTitleChange = { appTitle = it },
                    isDarkMode = isDarkMode,
                    onDarkModeChange = { isDarkMode = it }
                )
            }
        }
    }
}

private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Countdown Alarms"
        val descriptionText = "Notifications when timers finish"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("COUNTDOWN_CHANNEL", name, importance).apply {
            description = descriptionText
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownApp(
    appTitle: String,
    onTitleChange: (String) -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var countdowns by remember { mutableStateOf(listOf<Countdown>()) }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var editingCountdown by remember { mutableStateOf<Countdown?>(null) }
    var swipedCountdownToDelete by remember { mutableStateOf<Countdown?>(null) }
    
    var currentSortOption by remember { mutableStateOf(SortOption.TARGET_DATE) }
    var isAscending by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val displayCountdowns = remember(countdowns, currentSortOption, isAscending) {
        val baseSorted = when (currentSortOption) {
            SortOption.TARGET_DATE -> countdowns.sortedBy { it.targetDateTime }
            SortOption.NAME -> countdowns.sortedBy { it.name.lowercase() }
            SortOption.CREATED_AT -> countdowns.sortedBy { it.createdAt }
            SortOption.MANUAL -> countdowns
        }
        val finalSorted = if (isAscending || currentSortOption == SortOption.MANUAL) baseSorted else baseSorted.reversed()
        
        if (currentSortOption == SortOption.MANUAL) {
            finalSorted
        } else {
            finalSorted.sortedWith(compareByDescending { it.isFavorite })
        }
    }

    val currentTime by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            delay(1000)
            value = LocalDateTime.now()
        }
    }

    LaunchedEffect(Unit) {
        val now = LocalDateTime.now()
        val initialCountdowns = listOf(
            Countdown(1, "Long Countdown", "", LocalDateTime.of(2026, 7, 10, 17, 0, 0), now, PresetColors[0], isNotificationEnabled = true),
            Countdown(2, "Day Plus", "", now.plusDays(1).plusHours(1).plusMinutes(1).plusSeconds(1), now, PresetColors[5], isFavorite = true),
            Countdown(3, "Hour Plus", "", now.plusHours(1).plusMinutes(1).plusSeconds(1), now, PresetColors[9], isNotificationEnabled = true),
            Countdown(4, "Minute Plus", "This is a description that will be longer than one line to test the expansion logic.", now.plusMinutes(1).plusSeconds(1), now, PresetColors[12]),
            Countdown(5, "Minute Ago", "", now.minusMinutes(1), now, PresetColors[15]),
            Countdown(6, "Dad's Birthday", "Celebrating another great year!", now.plusMonths(2), now, PresetColors[1], patternType = PatternType.BIRTHDAY),
            Countdown(7, "Retirement Party", "Enjoy your free time!", now.plusYears(1), now, PresetColors[14], patternType = PatternType.RETIREMENT),
            Countdown(8, "Valentine's Day", "Lots of love", LocalDateTime.of(now.year + if (now.monthValue > 2 || (now.monthValue == 2 && now.dayOfMonth >= 14)) 1 else 0, 2, 14, 0, 0), now, PresetColors[1], patternType = PatternType.VALENTINE, isFavorite = true),
            Countdown(9, "St. Patrick's Day", "Luck of the Irish", LocalDateTime.of(now.year + if (now.monthValue > 3 || (now.monthValue == 3 && now.dayOfMonth >= 17)) 1 else 0, 3, 17, 0, 0), now, PresetColors[9], patternType = PatternType.ST_PATRICK),
            Countdown(10, "Summer Solstice", "Longest day of the year", LocalDateTime.of(now.year + if (now.monthValue > 6 || (now.monthValue == 6 && now.dayOfMonth >= 21)) 1 else 0, 6, 21, 0, 0), now, PresetColors[11], patternType = PatternType.SOLSTICE),
            Countdown(11, "Christmas", "Merry Christmas!", LocalDateTime.of(now.year + if (now.monthValue == 12 && now.dayOfMonth >= 25) 1 else 0, 12, 25, 0, 0), now, PresetColors[0], patternType = PatternType.CHRISTMAS)
        )
        countdowns = initialCountdowns
        initialCountdowns.forEach { 
            if (it.isNotificationEnabled && it.targetDateTime.isAfter(now)) {
                scheduleAlarm(context, it)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(appTitle) },
                navigationIcon = {
                    Row {
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                        IconButton(onClick = { showHelpDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
                        }
                    }
                },
                actions = {
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Sort Options")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            if (currentSortOption != SortOption.MANUAL) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Ascending")
                                            Switch(
                                                checked = isAscending,
                                                onCheckedChange = { isAscending = it },
                                                modifier = Modifier.scale(0.75f)
                                            )
                                        }
                                    },
                                    onClick = { isAscending = !isAscending }
                                )
                                HorizontalDivider()
                            }
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName) },
                                    onClick = {
                                        currentSortOption = option
                                        showSortMenu = false
                                    },
                                    trailingIcon = {
                                        if (currentSortOption == option) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Countdown")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (displayCountdowns.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No countdowns yet.",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap + to start one!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                val listState = rememberLazyListState()
                val dragDropState = rememberDragDropState(listState) { fromIndex, toIndex ->
                    val newList = countdowns.toMutableList()
                    Collections.swap(newList, fromIndex, toIndex)
                    countdowns = newList
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .dragContainer(dragDropState, currentSortOption == SortOption.MANUAL),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(displayCountdowns, key = { _, item -> item.id }) { index, countdown ->
                        DraggableItem(dragDropState, index) {
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        val isRunning = !Duration.between(LocalDateTime.now(), countdown.targetDateTime).isNegative
                                        if (isRunning) {
                                            swipedCountdownToDelete = countdown
                                            false
                                        } else {
                                            cancelAlarm(context, countdown)
                                            countdowns = countdowns.filter { it.id != countdown.id }
                                            true
                                        }
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    val direction = dismissState.dismissDirection
                                    if (direction == SwipeToDismissBoxValue.EndToStart) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Red, MaterialTheme.shapes.medium)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            ) {
                                CountdownCard(
                                    countdown = countdown,
                                    currentTime = currentTime,
                                    onEdit = { editingCountdown = countdown },
                                    onFavoriteToggle = {
                                        countdowns = countdowns.map { 
                                            if (it.id == countdown.id) it.copy(isFavorite = !it.isFavorite) else it
                                        }
                                    },
                                    showDragHandle = currentSortOption == SortOption.MANUAL,
                                    isDarkMode = isDarkMode
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            CountdownDialog(
                title = "Add New Countdown",
                onDismiss = { showAddDialog = false },
                onConfirm = { name, description, dateTime, color, isNotificationEnabled, patternType, isFavorite ->
                    val newCountdown = Countdown(
                        id = System.currentTimeMillis(),
                        name = name,
                        description = description,
                        targetDateTime = dateTime,
                        color = color,
                        isNotificationEnabled = isNotificationEnabled,
                        patternType = patternType,
                        isFavorite = isFavorite
                    )
                    countdowns = countdowns + newCountdown
                    if (isNotificationEnabled) {
                        scheduleAlarm(context, newCountdown)
                    }
                    showAddDialog = false
                }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                currentTitle = appTitle,
                isDarkMode = isDarkMode,
                onDismiss = { showSettingsDialog = false },
                onSave = { newTitle, newDarkMode ->
                    onTitleChange(newTitle)
                    onDarkModeChange(newDarkMode)
                    showSettingsDialog = false
                }
            )
        }

        if (showHelpDialog) {
            HelpDialog(onDismiss = { showHelpDialog = false })
        }

        editingCountdown?.let { countdown ->
            CountdownDialog(
                title = "Edit Countdown",
                initialCountdown = countdown,
                onDismiss = { editingCountdown = null },
                onConfirm = { name, description, dateTime, color, isNotificationEnabled, patternType, isFavorite ->
                    val updatedCountdown = countdown.copy(
                        name = name, 
                        description = description, 
                        targetDateTime = dateTime, 
                        color = color,
                        isNotificationEnabled = isNotificationEnabled,
                        patternType = patternType,
                        isFavorite = isFavorite,
                        hasNotified = if (countdown.targetDateTime != dateTime) false else countdown.hasNotified
                    )
                    countdowns = countdowns.map {
                        if (it.id == countdown.id) updatedCountdown else it
                    }
                    if (isNotificationEnabled) {
                        scheduleAlarm(context, updatedCountdown)
                    } else {
                        cancelAlarm(context, countdown)
                    }
                    editingCountdown = null
                },
                onDelete = {
                    cancelAlarm(context, countdown)
                    countdowns = countdowns.filter { it.id != countdown.id }
                    editingCountdown = null
                }
            )
        }

        swipedCountdownToDelete?.let { countdown ->
            AlertDialog(
                onDismissRequest = { swipedCountdownToDelete = null },
                title = { Text("Delete Countdown") },
                text = { Text("Are you sure you want to delete the running countdown \"${countdown.name}\"?") },
                confirmButton = {
                    TextButton(onClick = {
                        cancelAlarm(context, countdown)
                        countdowns = countdowns.filter { it.id != countdown.id }
                        swipedCountdownToDelete = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { swipedCountdownToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsDialog(
    currentTitle: String,
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }
    var darkMode by remember { mutableStateOf(isDarkMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("App Title", style = MaterialTheme.typography.labelLarge)
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dark Mode")
                    Switch(
                        checked = darkMode,
                        onCheckedChange = { darkMode = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, darkMode) },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How to Use") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Welcome to MyCountDown!", fontWeight = FontWeight.Bold)
                
                Text("1. Adding Timers", fontWeight = FontWeight.SemiBold)
                Text("Tap the '+' button at the bottom right to create a new countdown. You can set a name, description, date, time, and choose a color or special theme.")
                
                Text("2. Favorites", fontWeight = FontWeight.SemiBold)
                Text("Tap the Star icon on any card to make it a favorite. Favorites will always float to the top of the list. Note: Favorites are hidden when using Manual sorting.")
                
                Text("3. Editing", fontWeight = FontWeight.SemiBold)
                Text("Tap the 'Edit' pencil icon on any card to change its details or delete it permanently.")
                
                Text("4. Deleting", fontWeight = FontWeight.SemiBold)
                Text("Swipe any card from right-to-left to delete it. Running timers will ask for confirmation first.")
                
                Text("5. Reordering", fontWeight = FontWeight.SemiBold)
                Text("Select 'Manual' from the Sort Options menu (top right), then long-press and drag any card by its handle (on the left) to reorder the list.")
                
                Text("6. Settings", fontWeight = FontWeight.SemiBold)
                Text("Use the sprocket icon (top left) to change the app title or toggle Dark Mode.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it!")
            }
        }
    )
}

@Composable
fun CountdownCard(
    countdown: Countdown, 
    currentTime: LocalDateTime, 
    onEdit: () -> Unit, 
    onFavoriteToggle: () -> Unit,
    showDragHandle: Boolean = false, 
    isDarkMode: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val duration = Duration.between(currentTime, countdown.targetDateTime)
    val isRunning = !duration.isNegative
    
    val backgroundColor = if (isRunning) {
        countdown.color
    } else {
        countdown.color.copy(alpha = 1f).compositeOver(if (isDarkMode) Color.Black else Color.White).copy(alpha = 0.4f).compositeOver(if (isDarkMode) Color.Black else Color.White)
    }
    
    val contentColor = if (isRunning) {
        Color.White
    } else {
        if (isDarkMode) Color.LightGray.copy(alpha = 0.7f) else Color.DarkGray.copy(alpha = 0.7f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRunning) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        border = if (!isRunning) BorderStroke(1.dp, if (isDarkMode) Color.Gray else Color.DarkGray) else null
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            if (isRunning && countdown.patternType != PatternType.NONE) {
                HolidayPattern(countdown.patternType)
            }
            
            if (showDragHandle) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Drag to reorder",
                    tint = contentColor,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(24.dp)
                )
            } else {
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        if (countdown.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (countdown.isFavorite) Color.Yellow else contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (countdown.isNotificationEnabled) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notification enabled",
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = contentColor)
                }
            }

            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
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
                        maxLines = if (expanded) 10 else 1,
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
fun HolidayPattern(type: PatternType) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val patternColor = Color.White.copy(alpha = 0.35f)
        when (type) {
            PatternType.BIRTHDAY -> {
                for (i in 0..40) {
                    val x = (i * 137f) % size.width
                    val y = (i * 83f) % size.height
                    if (i % 2 == 0) {
                        drawRect(patternColor, Offset(x, y), size = Size(12f, 12f))
                    } else {
                        drawCircle(patternColor, radius = 6f, center = Offset(x, y))
                    }
                }
            }
            PatternType.VALENTINE -> {
                for (i in 0..20) {
                    val x = (i * 173f + (i % 5) * 20f) % size.width
                    val y = (i * 113f + (i % 3) * 30f) % size.height
                    val heartSize = 40f
                    val path = Path().apply {
                        moveTo(x, y + heartSize / 4)
                        cubicTo(x - heartSize / 2, y - heartSize / 4, x - heartSize, y + heartSize / 2, x, y + heartSize)
                        cubicTo(x + heartSize, y + heartSize / 2, x + heartSize / 2, y - heartSize / 4, x, y + heartSize / 4)
                        close()
                    }
                    drawPath(path, patternColor)
                }
            }
            PatternType.ST_PATRICK -> {
                for (i in 0..20) {
                    val x = (i * 130f) % size.width
                    val y = (i * 90f) % size.height
                    // Draw a simple four-leaf clover
                    val leafSize = 15f
                    drawCircle(patternColor, radius = leafSize, center = Offset(x - leafSize, y - leafSize))
                    drawCircle(patternColor, radius = leafSize, center = Offset(x + leafSize, y - leafSize))
                    drawCircle(patternColor, radius = leafSize, center = Offset(x - leafSize, y + leafSize))
                    drawCircle(patternColor, radius = leafSize, center = Offset(x + leafSize, y + leafSize))
                    // Stem
                    drawLine(patternColor, start = Offset(x, y), end = Offset(x, y + leafSize * 2.5f), strokeWidth = 4f)
                }
            }
            PatternType.CHRISTMAS -> {
                for (i in -10..20) {
                    val startX = i * 60f
                    drawLine(
                        patternColor,
                        start = Offset(startX, 0f),
                        end = Offset(startX + size.height, size.height),
                        strokeWidth = 10f
                    )
                }
            }
            PatternType.SOLSTICE -> {
                drawCircle(patternColor, radius = 60f, center = Offset(size.width / 2, size.height / 2))
                for (i in 0..8) {
                    val angle = (i * 45f) * (Math.PI / 180f).toFloat()
                    val startX = size.width / 2 + 70f * cos(angle.toDouble()).toFloat()
                    val startY = size.height / 2 + 70f * sin(angle.toDouble()).toFloat()
                    val endX = size.width / 2 + 100f * cos(angle.toDouble()).toFloat()
                    val endY = size.height / 2 + 100f * sin(angle.toDouble()).toFloat()
                    drawLine(patternColor, Offset(startX, startY), Offset(endX, endY), strokeWidth = 8f)
                }
            }
            PatternType.RETIREMENT -> {
                for (i in 0..15) {
                    val x = (i * 180f) % size.width
                    val y = (i * 120f) % size.height
                    drawLine(patternColor, Offset(x, y), Offset(x + 30f, y + 30f), strokeWidth = 5f)
                    drawLine(patternColor, Offset(x + 30f, y), Offset(x, y + 30f), strokeWidth = 5f)
                }
            }
            else -> {}
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
    onConfirm: (String, String, LocalDateTime, Color, Boolean, PatternType, Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialCountdown?.name ?: "") }
    var description by remember { mutableStateOf(initialCountdown?.description ?: "") }
    var selectedColor by remember { mutableStateOf(initialCountdown?.color ?: PresetColors[0]) }
    var isNotificationEnabled by remember { mutableStateOf(initialCountdown?.isNotificationEnabled ?: false) }
    var patternType by remember { mutableStateOf(initialCountdown?.patternType ?: PatternType.NONE) }
    var isFavorite by remember { mutableStateOf(initialCountdown?.isFavorite ?: false) }
    
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (isFavorite) Color.Yellow else LocalContentColor.current,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Mark as Favorite")
                    }
                    Switch(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isNotificationEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Notify when finished")
                    }
                    Switch(
                        checked = isNotificationEnabled,
                        onCheckedChange = { isNotificationEnabled = it }
                    )
                }

                Text("Special Theme", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = patternType == PatternType.NONE,
                            onClick = { patternType = PatternType.NONE },
                            label = { Text("None") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = patternType == PatternType.BIRTHDAY,
                            onClick = { patternType = PatternType.BIRTHDAY },
                            label = { Text("Birthday") },
                            leadingIcon = { Icon(Icons.Default.Cake, null, Modifier.size(18.dp)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = patternType == PatternType.RETIREMENT,
                            onClick = { patternType = PatternType.RETIREMENT },
                            label = { Text("Retirement") },
                            leadingIcon = { Icon(Icons.Default.Work, null, Modifier.size(18.dp)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = patternType == PatternType.VALENTINE,
                            onClick = { patternType = PatternType.VALENTINE },
                            label = { Text("Valentine's") },
                            leadingIcon = { Icon(Icons.Default.Favorite, null, Modifier.size(18.dp)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = patternType == PatternType.ST_PATRICK,
                            onClick = { patternType = PatternType.ST_PATRICK },
                            label = { Text("St. Paddy's") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = patternType == PatternType.SOLSTICE,
                            onClick = { patternType = PatternType.SOLSTICE },
                            label = { Text("Solstice") },
                            leadingIcon = { Icon(Icons.Default.WbSunny, null, Modifier.size(18.dp)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = patternType == PatternType.CHRISTMAS,
                            onClick = { patternType = PatternType.CHRISTMAS },
                            label = { Text("Christmas") },
                            leadingIcon = { Icon(Icons.Default.Redeem, null, Modifier.size(18.dp)) }
                        )
                    }
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
                    onConfirm(name, description, LocalDateTime.of(selectedDate, selectedTime), selectedColor, isNotificationEnabled, patternType, isFavorite)
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

// Drag and Drop implementation helpers
@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit
): DragDropState {
    val state = remember(lazyListState) {
        DragDropState(lazyListState, onMove)
    }
    return state
}

class DragDropState(
    val lazyListState: LazyListState,
    private val onMove: (Int, Int) -> Unit
) {
    var draggedDistance by mutableFloatStateOf(0f)
    var draggingItemIndex by mutableStateOf<Int?>(null)
    var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)

    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                offset.y.toInt() in item.offset..(item.offset + item.size)
            }?.also {
                draggingItemIndex = it.index
                initiallyDraggedElement = it
            }
    }

    fun onDragInterrupted() {
        draggingItemIndex = null
        initiallyDraggedElement = null
        draggedDistance = 0f
    }

    fun onDrag(offset: Offset) {
        draggedDistance += offset.y

        val initialElement = initiallyDraggedElement ?: return
        val startOffset = (initialElement.offset + draggedDistance).toInt()
        val endOffset = (initialElement.offset + initialElement.size + draggedDistance).toInt()

        val currentElement = lazyListState.layoutInfo.visibleItemsInfo.find { item ->
            draggingItemIndex != item.index &&
                    (startOffset in item.offset..item.offset + item.size ||
                            endOffset in item.offset..item.offset + item.size)
        }

        if (currentElement != null) {
            onMove(draggingItemIndex!!, currentElement.index)
            draggingItemIndex = currentElement.index
        }
    }
}

fun Modifier.dragContainer(dragDropState: DragDropState, enabled: Boolean): Modifier {
    if (!enabled) return this
    return this.pointerInput(dragDropState) {
        detectDragGesturesAfterLongPress(
            onDrag = { change, offset ->
                change.consume()
                dragDropState.onDrag(offset)
            },
            onDragStart = { offset -> dragDropState.onDragStart(offset) },
            onDragEnd = { dragDropState.onDragInterrupted() },
            onDragCancel = { dragDropState.onDragInterrupted() }
        )
    }
}

@Composable
fun DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    content: @Composable (isDragging: Boolean) -> Unit
) {
    val dragging = index == dragDropState.draggingItemIndex

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = if (dragging) dragDropState.draggedDistance else 0f
                scaleX = if (dragging) 1.05f else 1f
                scaleY = if (dragging) 1.05f else 1f
                alpha = if (dragging) 0.8f else 1f
            }
            .zIndex(if (dragging) 1f else 0f)
    ) {
        content(dragging)
    }
}
