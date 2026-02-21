package com.daverbooks.countdown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
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

data class Countdown(
    val id: Long,
    val name: String,
    val description: String,
    val targetDateTime: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val color: Color = Color.Gray
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownApp() {
    var countdowns by remember { mutableStateOf(listOf<Countdown>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCountdown by remember { mutableStateOf<Countdown?>(null) }
    var swipedCountdownToDelete by remember { mutableStateOf<Countdown?>(null) }
    
    var currentSortOption by remember { mutableStateOf(SortOption.TARGET_DATE) }
    var isAscending by remember { mutableStateOf(true) }

    val displayCountdowns = remember(countdowns, currentSortOption, isAscending) {
        if (currentSortOption == SortOption.MANUAL) {
            countdowns
        } else {
            val sorted = when (currentSortOption) {
                SortOption.TARGET_DATE -> countdowns.sortedBy { it.targetDateTime }
                SortOption.NAME -> countdowns.sortedBy { it.name.lowercase() }
                SortOption.CREATED_AT -> countdowns.sortedBy { it.createdAt }
                SortOption.MANUAL -> countdowns
            }
            if (isAscending) sorted else sorted.reversed()
        }
    }

    // Single source of time for all countdowns
    val currentTime by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            delay(1000)
            value = LocalDateTime.now()
        }
    }

    LaunchedEffect(Unit) {
        val now = LocalDateTime.now()
        countdowns = listOf(
            Countdown(1, "Long Countdown", "", LocalDateTime.of(2026, 7, 10, 17, 0, 0), now, PresetColors[0]),
            Countdown(2, "Day Plus", "", now.plusDays(1).plusHours(1).plusMinutes(1).plusSeconds(1), now, PresetColors[5]),
            Countdown(3, "Hour Plus", "", now.plusHours(1).plusMinutes(1).plusSeconds(1), now, PresetColors[9]),
            Countdown(4, "Minute Plus", "This is a description that will be longer than one line to test the expansion logic. Tap the card to see more or less of it!", now.plusMinutes(1).plusSeconds(1), now, PresetColors[12]),
            Countdown(5, "Minute Ago", "", now.minusMinutes(1), now, PresetColors[15]),
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Countdowns") },
                actions = {
                    if (currentSortOption != SortOption.MANUAL) {
                        IconButton(onClick = { isAscending = !isAscending }) {
                            Icon(
                                if (isAscending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Sort Direction"
                            )
                        }
                    }
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Sort Options")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
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
                                    val color = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.EndToStart -> Color.Red
                                        else -> Color.Transparent
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color, MaterialTheme.shapes.medium)
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
                            ) {
                                CountdownCard(
                                    countdown = countdown,
                                    currentTime = currentTime,
                                    onEdit = { editingCountdown = countdown },
                                    showDragHandle = currentSortOption == SortOption.MANUAL
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

        swipedCountdownToDelete?.let { countdown ->
            AlertDialog(
                onDismissRequest = { swipedCountdownToDelete = null },
                title = { Text("Delete Countdown") },
                text = { Text("Are you sure you want to delete the running countdown \"${countdown.name}\"?") },
                confirmButton = {
                    TextButton(onClick = {
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
fun CountdownCard(countdown: Countdown, currentTime: LocalDateTime, onEdit: () -> Unit, showDragHandle: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    val duration = Duration.between(currentTime, countdown.targetDateTime)
    val isRunning = !duration.isNegative
    
    val backgroundColor = if (isRunning) {
        countdown.color
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    
    val contentColor = if (isRunning) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = contentColor)
                }
                if (showDragHandle) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Drag to reorder",
                        tint = contentColor,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
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
    var draggedDistance by mutableStateOf(0f)
    var draggingItemIndex by mutableStateOf<Int?>(null)
    var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)

    fun onDragStart(offset: androidx.compose.ui.geometry.Offset) {
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

    fun onDrag(offset: androidx.compose.ui.geometry.Offset) {
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
