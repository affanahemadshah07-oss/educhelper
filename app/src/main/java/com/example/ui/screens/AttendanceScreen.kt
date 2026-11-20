package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SubjectEntity
import com.example.domain.AttendanceCalculator
import com.example.domain.AttendanceHealth
import com.example.ui.StudyHubUiState
import com.example.ui.components.AttendanceProgressBarWithTarget
import com.example.ui.components.AttendanceRingGauge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SubjectColorPicker
import com.example.ui.theme.LocalStudyColors
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.SubjectColorHexes
import com.example.ui.theme.parseHexColor
import kotlin.math.roundToInt

@Composable
fun AttendanceScreen(
    uiState: StudyHubUiState,
    onUpdateDefaultTarget: (Int) -> Unit,
    onSaveSubject: (SubjectEntity) -> Unit,
    onDeleteSubject: (Long) -> Unit,
    onMarkAttendance: (SubjectEntity, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var showSubjectDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "attendance_header") {
                SectionHeader(
                    title = stringResource(R.string.attendance_title),
                    subtitle = stringResource(R.string.attendance_subtitle),
                    trailing = {
                        Button(
                            onClick = {
                                editingSubject = null
                                showSubjectDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("add_subject_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_add_subject))
                        }
                    }
                )
            }

            item(key = "overall_banner") {
                OverallAttendanceBanner(
                    uiState = uiState,
                    onUpdateDefaultTarget = onUpdateDefaultTarget
                )
            }

            if (uiState.subjects.isEmpty()) {
                item(key = "empty_subjects") {
                    EmptyStateCard(
                        icon = Icons.Default.School,
                        title = stringResource(R.string.empty_subjects_title),
                        subtitle = stringResource(R.string.empty_subjects_subtitle),
                        primaryActionLabel = stringResource(R.string.action_add_subject),
                        onPrimaryAction = {
                            editingSubject = null
                            showSubjectDialog = true
                        },
                        primaryTestTag = "empty_add_subject_button"
                    )
                }
            } else {
                items(uiState.subjects, key = { it.id }) { subject ->
                    SubjectAttendanceCard(
                        subject = subject,
                        onMarkPresent = { onMarkAttendance(subject, true) },
                        onMarkAbsent = { onMarkAttendance(subject, false) },
                        onEdit = {
                            editingSubject = subject
                            showSubjectDialog = true
                        },
                        onDelete = { onDeleteSubject(subject.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showSubjectDialog) {
        SubjectEditorDialog(
            existing = editingSubject,
            defaultTarget = uiState.defaultTargetPercent,
            onDismiss = { showSubjectDialog = false },
            onSave = { subject ->
                onSaveSubject(subject)
                showSubjectDialog = false
            }
        )
    }
}

@Composable
private fun OverallAttendanceBanner(
    uiState: StudyHubUiState,
    onUpdateDefaultTarget: (Int) -> Unit
) {
    val overall = uiState.overallAttendance
    val totalAttended = uiState.subjects.sumOf { it.attendedClasses }
    val totalConducted = uiState.subjects.sumOf { it.totalClasses }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overall_attendance_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AttendanceRingGauge(
                    percentage = overall.percentage,
                    formattedPercent = overall.formattedPercent,
                    health = overall.health,
                    size = 86.dp,
                    strokeWidth = 9.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "OVERALL SEMESTER",
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = overall.statusHeadline,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (totalConducted > 0) {
                            "$totalAttended / $totalConducted classes attended across ${uiState.subjects.size} subjects"
                        } else {
                            "Set your target % and add subjects to track"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Default Target:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(70, 75, 80, 85).forEach { pct ->
                    FilterChip(
                        selected = uiState.defaultTargetPercent == pct,
                        onClick = { onUpdateDefaultTarget(pct) },
                        label = {
                            Text(
                                text = "$pct%",
                                fontFamily = MonoFontFamily,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectAttendanceCard(
    subject: SubjectEntity,
    onMarkPresent: () -> Unit,
    onMarkAbsent: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val studyColors = LocalStudyColors.current
    val summary = remember(subject.attendedClasses, subject.totalClasses, subject.targetPercentage) {
        AttendanceCalculator.calculate(
            attended = subject.attendedClasses,
            total = subject.totalClasses,
            targetPercentage = subject.targetPercentage
        )
    }
    val accentColor = parseHexColor(subject.colorHex)
    var showSimulator by remember { mutableStateOf(false) }
    var simExtraAttend by remember { mutableIntStateOf(0) }
    var simExtraSkip by remember { mutableIntStateOf(0) }

    val statusContainer = when (summary.health) {
        AttendanceHealth.SAFE -> studyColors.safeContainer
        AttendanceHealth.BORDERLINE -> studyColors.warningContainer
        AttendanceHealth.CRITICAL -> studyColors.dangerContainer
        AttendanceHealth.EMPTY -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusContent = when (summary.health) {
        AttendanceHealth.SAFE -> studyColors.onSafeContainer
        AttendanceHealth.BORDERLINE -> studyColors.onWarningContainer
        AttendanceHealth.CRITICAL -> studyColors.onDangerContainer
        AttendanceHealth.EMPTY -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusIcon = when (summary.health) {
        AttendanceHealth.SAFE -> Icons.Outlined.CheckCircle
        AttendanceHealth.BORDERLINE, AttendanceHealth.CRITICAL -> Icons.Default.WarningAmber
        AttendanceHealth.EMPTY -> Icons.Default.School
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_card_${subject.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row: color bar + title + percentage readout + edit/delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (subject.code.isNotBlank()) {
                            Surface(
                                color = accentColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = subject.code,
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Target ${subject.targetPercentage}%",
                            fontFamily = MonoFontFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subject.instructor.isNotBlank()) {
                        Text(
                            text = subject.instructor,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = summary.formattedPercent,
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = when (summary.health) {
                            AttendanceHealth.SAFE -> studyColors.safe
                            AttendanceHealth.BORDERLINE -> studyColors.warning
                            AttendanceHealth.CRITICAL -> studyColors.danger
                            AttendanceHealth.EMPTY -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = "${subject.attendedClasses}/${subject.totalClasses} classes",
                        fontFamily = MonoFontFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress bar with vertical target marker
            AttendanceProgressBarWithTarget(
                percentage = summary.percentage,
                targetPercentage = subject.targetPercentage,
                health = summary.health
            )

            // Safe Bunk / Must Attend Insight Banner
            Surface(
                color = statusContainer,
                contentColor = statusContent,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = summary.statusHeadline,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = summary.statusDetail,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Instant + Present / + Absent Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onMarkPresent,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = studyColors.safe,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("present_btn_${subject.id}")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_mark_present))
                }
                OutlinedButton(
                    onClick = onMarkAbsent,
                    border = BorderStroke(1.2.dp, studyColors.danger.copy(alpha = 0.75f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = studyColors.danger),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("absent_btn_${subject.id}")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_mark_absent))
                }
            }

            // Secondary row: What-If Simulator toggle + Edit/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showSimulator = !showSimulator },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("What-If Simulator", style = MaterialTheme.typography.labelMedium)
                    Icon(
                        imageVector = if (showSimulator) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit ${subject.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete ${subject.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Expandable What-If Calculator Panel
            AnimatedVisibility(visible = showSimulator) {
                val simAttended = subject.attendedClasses + simExtraAttend
                val simTotal = subject.totalClasses + simExtraAttend + simExtraSkip
                val simSummary = AttendanceCalculator.calculate(simAttended, simTotal, subject.targetPercentage)

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Projected: ${simSummary.formattedPercent} ($simAttended/$simTotal)",
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (simExtraAttend > 0 || simExtraSkip > 0) {
                            TextButton(
                                onClick = {
                                    simExtraAttend = 0
                                    simExtraSkip = 0
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) { Text("Reset") }
                        }
                    }
                    Text(
                        text = simSummary.statusHeadline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { simExtraAttend++ },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("+1 Attend ($simExtraAttend)", style = MaterialTheme.typography.labelMedium)
                        }
                        FilledTonalButton(
                            onClick = { simExtraSkip++ },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("+1 Skip ($simExtraSkip)", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectEditorDialog(
    existing: SubjectEntity?,
    defaultTarget: Int,
    onDismiss: () -> Unit,
    onSave: (SubjectEntity) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var code by remember { mutableStateOf(existing?.code.orEmpty()) }
    var instructor by remember { mutableStateOf(existing?.instructor.orEmpty()) }
    var attendedText by remember { mutableStateOf((existing?.attendedClasses ?: 0).toString()) }
    var totalText by remember { mutableStateOf((existing?.totalClasses ?: 0).toString()) }
    var targetPercent by remember { mutableIntStateOf(existing?.targetPercentage ?: defaultTarget) }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: SubjectColorHexes.first()) }

    val parsedAttended = attendedText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val parsedTotal = totalText.toIntOrNull()?.coerceAtLeast(parsedAttended) ?: parsedAttended
    val preview = AttendanceCalculator.calculate(parsedAttended, parsedTotal, targetPercent)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) "Add Subject" else "Edit Subject",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name") },
                    placeholder = { Text("e.g., Operating Systems") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Code") },
                        placeholder = { Text("CS301") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = instructor,
                        onValueChange = { instructor = it },
                        label = { Text("Instructor") },
                        placeholder = { Text("Optional") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = attendedText,
                        onValueChange = { attendedText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Attended") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subject_attended_input")
                    )
                    OutlinedTextField(
                        value = totalText,
                        onValueChange = { totalText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Total Classes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subject_total_input")
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Target Attendance", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "$targetPercent%",
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = targetPercent.toFloat(),
                        onValueChange = { targetPercent = it.roundToInt() },
                        valueRange = 50f..95f,
                        steps = 8
                    )
                }

                // Live Preview inside Dialog
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Preview: ${preview.formattedPercent} • ${preview.statusHeadline}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text("Accent Color", style = MaterialTheme.typography.labelMedium)
                SubjectColorPicker(
                    selectedHex = colorHex,
                    onSelect = { colorHex = it }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.trim().isNotEmpty(),
                onClick = {
                    val safeAttended = attendedText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    val safeTotal = (totalText.toIntOrNull() ?: safeAttended).coerceAtLeast(safeAttended)
                    onSave(
                        SubjectEntity(
                            id = existing?.id ?: 0L,
                            name = name.trim(),
                            code = code.trim(),
                            instructor = instructor.trim(),
                            colorHex = colorHex,
                            attendedClasses = safeAttended,
                            totalClasses = safeTotal,
                            targetPercentage = targetPercent,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier.testTag("save_subject_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
