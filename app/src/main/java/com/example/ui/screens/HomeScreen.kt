package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SubjectEntity
import com.example.data.TimetableSlotEntity
import com.example.domain.AttendanceHealth
import com.example.domain.LiveClassState
import com.example.domain.TimeUtils
import com.example.domain.WeekDays
import com.example.ui.MainTab
import com.example.ui.StudyHubUiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.SubjectColorPicker
import com.example.ui.theme.LocalStudyColors
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.OxfordIndigo
import com.example.ui.theme.SubjectColorHexes
import com.example.ui.theme.parseHexColor

@Composable
fun HomeScreen(
    uiState: StudyHubUiState,
    onSelectDay: (Int) -> Unit,
    onNavigateTab: (MainTab) -> Unit,
    onSaveSlot: (TimetableSlotEntity) -> Unit,
    onDeleteSlot: (Long) -> Unit,
    onMarkAttendance: (SubjectEntity, Boolean) -> Unit,
    onLoadSampleSemester: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddSlotDialog by remember { mutableStateOf(false) }

    val hour = TimeUtils.currentHour(uiState.nowMillis)
    val greeting = when {
        hour < 12 -> stringResource(R.string.greeting_morning)
        hour < 17 -> stringResource(R.string.greeting_afternoon)
        else -> stringResource(R.string.greeting_evening)
    }

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
            // 1. Editorial Hero Card with Live Current / Next Class Indicator
            item(key = "hero_banner") {
                TodayHeroCard(
                    greeting = greeting,
                    dateLabel = TimeUtils.formatTodayHeader(uiState.nowMillis),
                    liveClassState = uiState.liveClassState
                )
            }

            // 2. Quick Stats Bento Row
            item(key = "quick_stats") {
                QuickStatsRow(
                    uiState = uiState,
                    onNavigateTab = onNavigateTab
                )
            }

            // 3. Schedule Header + Day-of-Week Strip
            item(key = "schedule_header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.section_todays_classes),
                        subtitle = WeekDays.firstOrNull { it.dayOfWeek == uiState.selectedDayOfWeek }?.fullName,
                        trailing = {
                            FilledTonalButton(
                                onClick = { showAddSlotDialog = true },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("home_add_class_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.action_add_class))
                            }
                        }
                    )

                    DaySelectorStrip(
                        selectedDay = uiState.selectedDayOfWeek,
                        todayDay = uiState.todayDayOfWeek,
                        allSlots = uiState.allSlots,
                        onSelectDay = onSelectDay
                    )
                }
            }

            // 4. Class Timeline or Empty State
            val daySlots = uiState.selectedDaySlots
            if (daySlots.isEmpty()) {
                item(key = "empty_schedule") {
                    EmptyStateCard(
                        icon = Icons.Outlined.CalendarToday,
                        title = stringResource(R.string.empty_schedule_title),
                        subtitle = stringResource(R.string.empty_schedule_subtitle),
                        primaryActionLabel = stringResource(R.string.action_add_class),
                        onPrimaryAction = { showAddSlotDialog = true },
                        primaryTestTag = "empty_add_class_button",
                        secondaryActionLabel = if (uiState.isWorkspaceEmpty) stringResource(R.string.action_load_sample) else null,
                        onSecondaryAction = if (uiState.isWorkspaceEmpty) onLoadSampleSemester else null,
                        secondaryTestTag = "load_sample_semester_button"
                    )
                }
            } else {
                val nowMins = TimeUtils.currentMinutesFromMidnight(uiState.nowMillis)
                val isViewingToday = uiState.selectedDayOfWeek == uiState.todayDayOfWeek

                items(daySlots, key = { it.id }) { slot ->
                    val linkedSubject = uiState.subjects.firstOrNull { sub ->
                        (slot.subjectId != null && sub.id == slot.subjectId) ||
                            sub.name.equals(slot.subjectName, ignoreCase = true)
                    }
                    val isOngoing = isViewingToday && nowMins in slot.startMinutes until slot.endMinutes
                    val isNext = isViewingToday &&
                        (uiState.liveClassState as? LiveClassState.UpNext)?.slot?.id == slot.id

                    TimetableSlotCard(
                        slot = slot,
                        isOngoing = isOngoing,
                        isNext = isNext,
                        linkedSubject = linkedSubject,
                        onMarkPresent = linkedSubject?.let { sub -> { onMarkAttendance(sub, true) } },
                        onMarkAbsent = linkedSubject?.let { sub -> { onMarkAttendance(sub, false) } },
                        onDelete = { onDeleteSlot(slot.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showAddSlotDialog) {
        AddTimetableSlotDialog(
            initialDayOfWeek = uiState.selectedDayOfWeek,
            subjects = uiState.subjects,
            onDismiss = { showAddSlotDialog = false },
            onSave = { newSlot ->
                onSaveSlot(newSlot)
                showAddSlotDialog = false
            }
        )
    }
}

@Composable
private fun TodayHeroCard(
    greeting: String,
    dateLabel: String,
    liveClassState: LiveClassState
) {
    val studyColors = LocalStudyColors.current

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = OxfordIndigo,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_hero_card")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_study_hero),
                contentDescription = stringResource(R.string.cd_study_hero),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xEC10253D),
                                Color(0xCC163252),
                                Color(0x881B3A5C)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = dateLabel.uppercase(),
                            fontFamily = MonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFBFD7F5)
                        )
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.app_full_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6E4F5)
                        )
                    }

                    val badgeText = when (liveClassState) {
                        is LiveClassState.Ongoing -> stringResource(R.string.status_live_now)
                        is LiveClassState.UpNext -> stringResource(R.string.status_up_next)
                        LiveClassState.CompletedForToday -> stringResource(R.string.status_all_done)
                        LiveClassState.NoClassesToday -> "FREE DAY"
                    }
                    val badgeColor = when (liveClassState) {
                        is LiveClassState.Ongoing -> studyColors.safe
                        is LiveClassState.UpNext -> Color(0xFFF59E0B)
                        else -> Color.White.copy(alpha = 0.2f)
                    }
                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontFamily = MonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (liveClassState is LiveClassState.UpNext) Color(0xFF2B1600) else Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Live Class Spotlight Strip
                Surface(
                    color = Color.White.copy(alpha = 0.13f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (liveClassState) {
                            is LiveClassState.Ongoing -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = liveClassState.slot.subjectName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${TimeUtils.formatDurationMinutes(liveClassState.minutesRemaining)} left",
                                        fontFamily = MonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6EE7B7)
                                    )
                                }
                                Text(
                                    text = buildString {
                                        append(TimeUtils.formatMinutes(liveClassState.slot.startMinutes))
                                        append(" – ")
                                        append(TimeUtils.formatMinutes(liveClassState.slot.endMinutes))
                                        if (liveClassState.slot.room.isNotBlank()) {
                                            append("  •  ")
                                            append(liveClassState.slot.room)
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE0ECFA)
                                )
                                LinearProgressIndicator(
                                    progress = { liveClassState.progress },
                                    color = Color(0xFF34D399),
                                    trackColor = Color.White.copy(alpha = 0.22f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                            }

                            is LiveClassState.UpNext -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = liveClassState.slot.subjectName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "In ${TimeUtils.formatDurationMinutes(liveClassState.minutesUntilStart)}",
                                        fontFamily = MonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFCD34D)
                                    )
                                }
                                Text(
                                    text = buildString {
                                        append(TimeUtils.formatMinutes(liveClassState.slot.startMinutes))
                                        append(" – ")
                                        append(TimeUtils.formatMinutes(liveClassState.slot.endMinutes))
                                        if (liveClassState.slot.room.isNotBlank()) {
                                            append("  •  ")
                                            append(liveClassState.slot.room)
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE0ECFA)
                                )
                            }

                            LiveClassState.CompletedForToday -> {
                                Text(
                                    text = "All scheduled classes finished for today",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Text(
                                    text = "Great time to run a quick flashcard self-test or review deadlines.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD6E4F5)
                                )
                            }

                            LiveClassState.NoClassesToday -> {
                                Text(
                                    text = "No classes on today's timetable yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Text(
                                    text = "Add your classes below to unlock live Current & Next class tracking.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD6E4F5)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatsRow(
    uiState: StudyHubUiState,
    onNavigateTab: (MainTab) -> Unit
) {
    val studyColors = LocalStudyColors.current
    val overall = uiState.overallAttendance
    val attendanceAccent = when (overall.health) {
        AttendanceHealth.SAFE -> studyColors.safe
        AttendanceHealth.BORDERLINE -> studyColors.warning
        AttendanceHealth.CRITICAL -> studyColors.danger
        AttendanceHealth.EMPTY -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickStatTile(
            label = "Attendance",
            value = overall.formattedPercent,
            caption = if (uiState.subjects.isEmpty()) "Add subjects" else "${uiState.subjects.size} courses",
            accentColor = attendanceAccent,
            icon = Icons.Default.School,
            onClick = { onNavigateTab(MainTab.ATTENDANCE) },
            modifier = Modifier
                .weight(1f)
                .testTag("stat_attendance_card")
        )

        val nextDueCaption = uiState.nextPendingDeadline?.let {
            TimeUtils.formatDueCountdown(it.dueEpochMillis, uiState.nowMillis).first
        } ?: "All clear"
        QuickStatTile(
            label = "Deadlines",
            value = uiState.pendingDeadlinesCount.toString(),
            caption = nextDueCaption,
            accentColor = studyColors.warning,
            icon = Icons.Default.EventAvailable,
            onClick = { onNavigateTab(MainTab.DEADLINES) },
            modifier = Modifier
                .weight(1f)
                .testTag("stat_deadlines_card")
        )

        val mastered = uiState.flashcards.count { it.masteryLevel >= 3 }
        QuickStatTile(
            label = "Flashcards",
            value = uiState.flashcards.size.toString(),
            caption = if (uiState.flashcards.isEmpty()) "Create deck" else "$mastered mastered",
            accentColor = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.Style,
            onClick = { onNavigateTab(MainTab.FLASHCARDS) },
            modifier = Modifier
                .weight(1f)
                .testTag("stat_flashcards_card")
        )
    }
}

@Composable
private fun QuickStatTile(
    label: String,
    value: String,
    caption: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.14f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DaySelectorStrip(
    selectedDay: Int,
    todayDay: Int,
    allSlots: List<TimetableSlotEntity>,
    onSelectDay: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(WeekDays, key = { it.dayOfWeek }) { day ->
            val isSelected = day.dayOfWeek == selectedDay
            val isToday = day.dayOfWeek == todayDay
            val count = allSlots.count { it.dayOfWeek == day.dayOfWeek }

            Surface(
                onClick = { onSelectDay(day.dayOfWeek) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(14.dp),
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                modifier = Modifier
                    .width(56.dp)
                    .testTag("day_chip_${day.shortLabel.lowercase()}")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                ) {
                    Text(
                        text = day.shortLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium
                    )
                    Text(
                        text = if (count > 0) "$count" else "–",
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isToday && isSelected -> Color(0xFF6EE7B7)
                                    isToday -> MaterialTheme.colorScheme.secondary
                                    else -> Color.Transparent
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun TimetableSlotCard(
    slot: TimetableSlotEntity,
    isOngoing: Boolean,
    isNext: Boolean,
    linkedSubject: SubjectEntity?,
    onMarkPresent: (() -> Unit)?,
    onMarkAbsent: (() -> Unit)?,
    onDelete: () -> Unit
) {
    val studyColors = LocalStudyColors.current
    val accentColor = parseHexColor(slot.colorHex)
    val borderColor = when {
        isOngoing -> studyColors.safe
        isNext -> studyColors.warning
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(if (isOngoing || isNext) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timetable_slot_${slot.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Time Rail
                Column(
                    modifier = Modifier.width(78.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = TimeUtils.formatMinutes(slot.startMinutes),
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = TimeUtils.formatMinutes(slot.endMinutes),
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Vertical Subject Accent Bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Subject details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = slot.subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isOngoing) {
                            StatusPill(
                                text = "NOW",
                                containerColor = studyColors.safeContainer,
                                contentColor = studyColors.onSafeContainer
                            )
                        } else if (isNext) {
                            StatusPill(
                                text = "NEXT",
                                containerColor = studyColors.warningContainer,
                                contentColor = studyColors.onWarningContainer
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (slot.room.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = slot.room,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = TimeUtils.formatDurationMinutes(slot.endMinutes - slot.startMinutes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete class ${slot.subjectName}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Quick Attendance check-in directly from today's class card if linked to a tracked subject
            if (linkedSubject != null && onMarkPresent != null && onMarkAbsent != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onMarkPresent,
                        border = BorderStroke(1.dp, studyColors.safe.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = studyColors.safe),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Present", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onMarkAbsent,
                        border = BorderStroke(1.dp, studyColors.danger.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = studyColors.danger),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Absent", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTimetableSlotDialog(
    initialDayOfWeek: Int,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (TimetableSlotEntity) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf<Long?>(subjects.firstOrNull()?.id) }
    var subjectName by remember { mutableStateOf(subjects.firstOrNull()?.name.orEmpty()) }
    var room by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableIntStateOf(initialDayOfWeek) }
    var startHour by remember { mutableIntStateOf(9) }
    var startMinute by remember { mutableIntStateOf(0) }
    var durationMinutes by remember { mutableIntStateOf(60) }
    var selectedColorHex by remember {
        mutableStateOf(subjects.firstOrNull()?.colorHex ?: SubjectColorHexes.first())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Class to Timetable", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (subjects.isNotEmpty()) {
                    Text("Pick Tracked Subject", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subjects, key = { it.id }) { sub ->
                            FilterChip(
                                selected = selectedSubjectId == sub.id,
                                onClick = {
                                    selectedSubjectId = sub.id
                                    subjectName = sub.name
                                    selectedColorHex = sub.colorHex
                                },
                                label = { Text(sub.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = subjectName,
                    onValueChange = {
                        subjectName = it
                        selectedSubjectId = subjects.firstOrNull { s -> s.name.equals(it.trim(), ignoreCase = true) }?.id
                    },
                    label = { Text("Subject Name") },
                    placeholder = { Text("e.g., Data Structures") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("slot_subject_input")
                )

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room / Building (optional)") },
                    placeholder = { Text("e.g., Hall B-204") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Day of Week", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(WeekDays, key = { it.dayOfWeek }) { day ->
                        FilterChip(
                            selected = dayOfWeek == day.dayOfWeek,
                            onClick = { dayOfWeek = day.dayOfWeek },
                            label = { Text(day.shortLabel) }
                        )
                    }
                }

                Text("Start Time", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val startTotal = startHour * 60 + startMinute
                    Text(
                        text = TimeUtils.formatMinutes(startTotal),
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                val next = (startTotal - 30).coerceAtLeast(6 * 60)
                                startHour = next / 60
                                startMinute = next % 60
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) { Text("-30m") }
                        OutlinedButton(
                            onClick = {
                                val next = (startTotal + 30).coerceAtMost(22 * 60)
                                startHour = next / 60
                                startMinute = next % 60
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) { Text("+30m") }
                    }
                }

                Text("Duration", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(45, 60, 90, 120).forEach { mins ->
                        FilterChip(
                            selected = durationMinutes == mins,
                            onClick = { durationMinutes = mins },
                            label = { Text("${mins}m") }
                        )
                    }
                }

                Text("Color Accent", style = MaterialTheme.typography.labelMedium)
                SubjectColorPicker(
                    selectedHex = selectedColorHex,
                    onSelect = { selectedColorHex = it }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = subjectName.trim().isNotEmpty(),
                onClick = {
                    val startMins = startHour * 60 + startMinute
                    val endMins = (startMins + durationMinutes).coerceAtMost(23 * 60 + 55)
                    onSave(
                        TimetableSlotEntity(
                            subjectId = selectedSubjectId,
                            subjectName = subjectName.trim(),
                            room = room.trim(),
                            dayOfWeek = dayOfWeek,
                            startMinutes = startMins,
                            endMinutes = endMins,
                            colorHex = selectedColorHex
                        )
                    )
                },
                modifier = Modifier.testTag("save_slot_button")
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
