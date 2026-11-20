package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.DeadlineEntity
import com.example.data.SubjectEntity
import com.example.domain.TimeUtils
import com.example.ui.StudyHubUiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalStudyColors
import com.example.ui.theme.MonoFontFamily

private val DeadlineFilters = listOf(
    "ALL" to "Upcoming",
    "EXAM" to "Exams & Quizzes",
    "ASSIGNMENT" to "Assignments & Labs",
    "COMPLETED" to "Completed"
)

private val DeadlineTypes = listOf("EXAM", "ASSIGNMENT", "LAB", "QUIZ")

@Composable
fun DeadlinesScreen(
    uiState: StudyHubUiState,
    onSelectFilter: (String) -> Unit,
    onSaveDeadline: (DeadlineEntity) -> Unit,
    onToggleComplete: (DeadlineEntity) -> Unit,
    onDeleteDeadline: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingDeadline by remember { mutableStateOf<DeadlineEntity?>(null) }
    var showDeadlineDialog by remember { mutableStateOf(false) }

    val filtered = uiState.filteredDeadlines

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "deadlines_header") {
                SectionHeader(
                    title = stringResource(R.string.deadlines_title),
                    subtitle = stringResource(R.string.deadlines_subtitle),
                    trailing = {
                        Button(
                            onClick = {
                                editingDeadline = null
                                showDeadlineDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("add_deadline_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_add_deadline))
                        }
                    }
                )
            }

            item(key = "deadline_filters") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DeadlineFilters, key = { it.first }) { (key, label) ->
                        FilterChip(
                            selected = uiState.selectedDeadlineFilter == key,
                            onClick = { onSelectFilter(key) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item(key = "empty_deadlines") {
                    EmptyStateCard(
                        icon = Icons.Default.EventAvailable,
                        title = stringResource(R.string.empty_deadlines_title),
                        subtitle = stringResource(R.string.empty_deadlines_subtitle),
                        primaryActionLabel = stringResource(R.string.action_add_deadline),
                        onPrimaryAction = {
                            editingDeadline = null
                            showDeadlineDialog = true
                        },
                        primaryTestTag = "empty_add_deadline_button"
                    )
                }
            } else {
                items(filtered, key = { it.id }) { deadline ->
                    DeadlineCard(
                        deadline = deadline,
                        nowMillis = uiState.nowMillis,
                        onToggleComplete = { onToggleComplete(deadline) },
                        onEdit = {
                            editingDeadline = deadline
                            showDeadlineDialog = true
                        },
                        onDelete = { onDeleteDeadline(deadline.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showDeadlineDialog) {
        DeadlineEditorDialog(
            existing = editingDeadline,
            subjects = uiState.subjects,
            onDismiss = { showDeadlineDialog = false },
            onSave = { deadline ->
                onSaveDeadline(deadline)
                showDeadlineDialog = false
            }
        )
    }
}

@Composable
private fun DeadlineCard(
    deadline: DeadlineEntity,
    nowMillis: Long,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val studyColors = LocalStudyColors.current
    val (countdownLabel, isOverdue) = TimeUtils.formatDueCountdown(deadline.dueEpochMillis, nowMillis)

    val (typeBg, typeFg) = when (deadline.type) {
        "EXAM" -> studyColors.dangerContainer to studyColors.onDangerContainer
        "QUIZ" -> studyColors.warningContainer to studyColors.onWarningContainer
        "LAB" -> studyColors.safeContainer to studyColors.onSafeContainer
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = if (!deadline.isCompleted && isOverdue) 1.5.dp else 1.dp,
            color = if (!deadline.isCompleted && isOverdue) studyColors.danger else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("deadline_card_${deadline.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = deadline.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    modifier = Modifier.testTag("deadline_check_${deadline.id}")
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            text = deadline.type,
                            containerColor = typeBg,
                            contentColor = typeFg
                        )
                        if (!deadline.isCompleted) {
                            StatusPill(
                                text = countdownLabel,
                                containerColor = if (isOverdue) studyColors.dangerContainer else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isOverdue) studyColors.onDangerContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = deadline.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (deadline.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (deadline.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )

                    if (deadline.subjectName.isNotBlank()) {
                        Text(
                            text = deadline.subjectName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = TimeUtils.formatDueDateTime(deadline.dueEpochMillis),
                                fontFamily = MonoFontFamily,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (deadline.reminderEnabled && !deadline.isCompleted) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Reminder active",
                                    tint = studyColors.safe,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${deadline.reminderMinutesBefore}m prior",
                                    fontFamily = MonoFontFamily,
                                    fontSize = 11.sp,
                                    color = studyColors.safe
                                )
                            }
                        }
                    }

                    if (deadline.notes.isNotBlank()) {
                        Text(
                            text = deadline.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column {
                    IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit deadline",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete deadline",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeadlineEditorDialog(
    existing: DeadlineEntity?,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (DeadlineEntity) -> Unit
) {
    val context = LocalContext.current
    val now = remember { System.currentTimeMillis() }
    val dayMs = 24 * 3_600_000L
    val hourMs = 3_600_000L

    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var subjectName by remember { mutableStateOf(existing?.subjectName ?: subjects.firstOrNull()?.name.orEmpty()) }
    var type by remember { mutableStateOf(existing?.type ?: "ASSIGNMENT") }
    var dueMillis by remember { mutableLongStateOf(existing?.dueEpochMillis ?: (now + dayMs)) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var reminderEnabled by remember { mutableStateOf(existing?.reminderEnabled ?: false) }
    var reminderMinutesBefore by remember { mutableIntStateOf(existing?.reminderMinutesBefore ?: 60) }
    var notificationPermissionDenied by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            reminderEnabled = true
            notificationPermissionDenied = false
        } else {
            reminderEnabled = false
            notificationPermissionDenied = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Deadline" else "Edit Deadline") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g., OS Midterm Exam") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deadline_title_input")
                )

                if (subjects.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects, key = { it.id }) { sub ->
                            FilterChip(
                                selected = subjectName.equals(sub.name, ignoreCase = true),
                                onClick = { subjectName = sub.name },
                                label = { Text(sub.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DeadlineTypes, key = { it }) { itemType ->
                        FilterChip(
                            selected = type == itemType,
                            onClick = { type = itemType },
                            label = { Text(itemType) }
                        )
                    }
                }

                Text("Due Date & Time", style = MaterialTheme.typography.labelMedium)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = TimeUtils.formatDueDateTime(dueMillis),
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val presets = listOf(
                        "+6h" to (now + 6 * hourMs),
                        "Tomorrow" to (now + dayMs),
                        "+3 Days" to (now + 3 * dayMs),
                        "+1 Week" to (now + 7 * dayMs)
                    )
                    items(presets, key = { it.first }) { (label, targetMillis) ->
                        OutlinedButton(
                            onClick = { dueMillis = targetMillis },
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) { Text(label, style = MaterialTheme.typography.labelMedium) }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { dueMillis = (dueMillis - dayMs).coerceAtLeast(now + hourMs) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) { Text("-1 Day") }
                    OutlinedButton(
                        onClick = { dueMillis += dayMs },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) { Text("+1 Day") }
                    OutlinedButton(
                        onClick = { dueMillis += 2 * hourMs },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) { Text("+2 Hr") }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Syllabus / Notes (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (reminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("Local Reminder Alert", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "Notify before deadline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { wantEnable ->
                            if (!wantEnable) {
                                reminderEnabled = false
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    reminderEnabled = true
                                } else {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                reminderEnabled = true
                            }
                        }
                    )
                }

                if (notificationPermissionDenied) {
                    Text(
                        text = "Notification permission was denied. Enable notifications in system settings to receive alerts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (reminderEnabled) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val offsets = listOf(30 to "30m before", 60 to "1h before", 120 to "2h before", 1440 to "1d before")
                        items(offsets, key = { it.first }) { (mins, label) ->
                            FilterChip(
                                selected = reminderMinutesBefore == mins,
                                onClick = { reminderMinutesBefore = mins },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.trim().isNotEmpty(),
                onClick = {
                    onSave(
                        DeadlineEntity(
                            id = existing?.id ?: 0L,
                            title = title.trim(),
                            subjectName = subjectName.trim(),
                            type = type,
                            dueEpochMillis = dueMillis,
                            notes = notes.trim(),
                            isCompleted = existing?.isCompleted ?: false,
                            reminderEnabled = reminderEnabled,
                            reminderMinutesBefore = reminderMinutesBefore,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier.testTag("save_deadline_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
