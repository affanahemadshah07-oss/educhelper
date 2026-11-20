package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.FlashcardEntity
import com.example.data.QuestionSetEntity
import com.example.data.QuestionSetWithDetails
import com.example.domain.QuizEngine
import com.example.ui.FlashcardStudyState
import com.example.ui.QuizSessionState
import com.example.ui.StudyHubUiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FlippableFlashcard
import com.example.ui.components.MasteryIndicatorDots
import com.example.ui.components.QuestionSetCard
import com.example.ui.components.QuizHistoryStrip
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelfTestQuestionCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalStudyColors
import com.example.ui.theme.MonoFontFamily

@Composable
fun FlashcardsScreen(
    uiState: StudyHubUiState,
    studySession: FlashcardStudyState?,
    quizSession: QuizSessionState?,
    onSelectSubjectFilter: (String) -> Unit,
    onOpenQuestionSet: (Long?) -> Unit,
    onSaveQuestionSet: (QuestionSetEntity) -> Unit,
    onDeleteQuestionSet: (Long) -> Unit,
    onSaveCard: (FlashcardEntity) -> Unit,
    onDeleteCard: (Long) -> Unit,
    onStartStudySet: (QuestionSetWithDetails) -> Unit,
    onStartStudyAll: (String) -> Unit,
    onToggleFlip: () -> Unit,
    onRecordStudyResult: (Boolean) -> Unit,
    onRestartStudy: () -> Unit,
    onCloseStudy: () -> Unit,
    onStartQuizSet: (QuestionSetWithDetails) -> Unit,
    onStartQuizAll: (String) -> Unit,
    onSubmitQuizOption: (String) -> Unit,
    onAdvanceQuiz: () -> Unit,
    onRestartQuiz: () -> Unit,
    onCloseQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Full-screen interactive study & quiz overlays
    if (studySession != null) {
        FlashcardStudyOverlay(
            session = studySession,
            onToggleFlip = onToggleFlip,
            onRecordResult = onRecordStudyResult,
            onRestart = onRestartStudy,
            onClose = onCloseStudy,
            modifier = modifier
        )
        return
    }

    if (quizSession != null) {
        QuickSelfTestOverlay(
            session = quizSession,
            onSelectOption = onSubmitQuizOption,
            onNext = onAdvanceQuiz,
            onRestart = onRestartQuiz,
            onClose = onCloseQuiz,
            modifier = modifier
        )
        return
    }

    // Question Set detail sub-screen
    val activeSet = uiState.activeQuestionSet
    if (activeSet != null) {
        QuestionSetDetailView(
            setWithDetails = activeSet,
            onBack = { onOpenQuestionSet(null) },
            onSaveCard = onSaveCard,
            onDeleteCard = onDeleteCard,
            onStartStudy = { onStartStudySet(activeSet) },
            onStartQuiz = { onStartQuizSet(activeSet) },
            modifier = modifier
        )
        return
    }

    // Main Question Sets Overview
    var editingSet by remember { mutableStateOf<QuestionSetEntity?>(null) }
    var showSetDialog by remember { mutableStateOf(false) }

    val filteredSets = uiState.filteredQuestionSets
    val filteredCards = uiState.filteredFlashcards
    val masteredCount = filteredCards.count { it.masteryLevel >= 3 }

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
            item(key = "flashcards_header") {
                SectionHeader(
                    title = stringResource(R.string.flashcards_title),
                    subtitle = "${uiState.questionSets.size} question sets • ${uiState.flashcards.size} cards stored offline",
                    trailing = {
                        Button(
                            onClick = {
                                editingSet = null
                                showSetDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("add_question_set_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Set")
                        }
                    }
                )
            }

            item(key = "exam_ready_banner") {
                ExamPrepOverviewBanner(
                    totalSets = filteredSets.size,
                    totalCards = filteredCards.size,
                    masteredCards = masteredCount,
                    subjectFilter = uiState.selectedFlashcardSubject,
                    onStartStudy = { onStartStudyAll(uiState.selectedFlashcardSubject) },
                    onStartQuiz = { onStartQuizAll(uiState.selectedFlashcardSubject) }
                )
            }

            if (uiState.flashcardSubjects.size > 1) {
                item(key = "subject_filters") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.flashcardSubjects, key = { it }) { subject ->
                            FilterChip(
                                selected = uiState.selectedFlashcardSubject == subject,
                                onClick = { onSelectSubjectFilter(subject) },
                                label = { Text(subject, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
            }

            if (filteredSets.isEmpty()) {
                item(key = "empty_sets") {
                    EmptyStateCard(
                        icon = Icons.Default.Style,
                        title = "No question sets yet",
                        subtitle = "Create a Room-backed Question Set for any exam topic, add Q&A flashcards, and launch timed self-tests.",
                        primaryActionLabel = "Create Question Set",
                        onPrimaryAction = {
                            editingSet = null
                            showSetDialog = true
                        },
                        primaryTestTag = "empty_add_set_button"
                    )
                }
            } else {
                items(filteredSets, key = { it.questionSet.id }) { setWithDetails ->
                    QuestionSetCard(
                        setWithDetails = setWithDetails,
                        onOpenSet = { onOpenQuestionSet(setWithDetails.questionSet.id) },
                        onStartStudy = { onStartStudySet(setWithDetails) },
                        onStartQuiz = { onStartQuizSet(setWithDetails) },
                        onEditSet = {
                            editingSet = setWithDetails.questionSet
                            showSetDialog = true
                        },
                        onDeleteSet = { onDeleteQuestionSet(setWithDetails.questionSet.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showSetDialog) {
        QuestionSetEditorDialog(
            existing = editingSet,
            subjects = uiState.subjects,
            defaultSubject = if (uiState.selectedFlashcardSubject != "All") uiState.selectedFlashcardSubject else "",
            onDismiss = { showSetDialog = false },
            onSave = { set ->
                onSaveQuestionSet(set)
                showSetDialog = false
            }
        )
    }
}

@Composable
private fun QuestionSetDetailView(
    setWithDetails: QuestionSetWithDetails,
    onBack: () -> Unit,
    onSaveCard: (FlashcardEntity) -> Unit,
    onDeleteCard: (Long) -> Unit,
    onStartStudy: () -> Unit,
    onStartQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var editingCard by remember { mutableStateOf<FlashcardEntity?>(null) }
    var showCardDialog by remember { mutableStateOf(false) }

    val set = setWithDetails.questionSet
    val hasCards = setWithDetails.cards.isNotEmpty()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "detail_top_bar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = set.subjectName.uppercase(),
                            fontFamily = MonoFontFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = set.title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = {
                            editingCard = null
                            showCardDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("add_flashcard_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Card")
                    }
                }
            }

            item(key = "detail_actions") {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (set.description.isNotBlank()) {
                            Text(
                                text = set.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                enabled = hasCards,
                                onClick = onStartStudy,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("detail_study_button")
                            ) {
                                Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Flip Study (${setWithDetails.cards.size})")
                            }
                            FilledTonalButton(
                                enabled = hasCards,
                                onClick = onStartQuiz,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("detail_quiz_button")
                            ) {
                                Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Self-Test")
                            }
                        }

                        if (setWithDetails.attempts.isNotEmpty()) {
                            QuizHistoryStrip(attempts = setWithDetails.attempts)
                        }
                    }
                }
            }

            if (setWithDetails.cards.isEmpty()) {
                item(key = "empty_cards_in_set") {
                    EmptyStateCard(
                        icon = Icons.Default.Style,
                        title = "No cards in this set yet",
                        subtitle = "Add Q&A flashcards (with optional custom MCQ distractors) to start studying or testing yourself.",
                        primaryActionLabel = stringResource(R.string.action_add_card),
                        onPrimaryAction = {
                            editingCard = null
                            showCardDialog = true
                        },
                        primaryTestTag = "empty_add_flashcard_button"
                    )
                }
            } else {
                items(setWithDetails.cards, key = { it.id }) { card ->
                    FlashcardListItem(
                        card = card,
                        onEdit = {
                            editingCard = card
                            showCardDialog = true
                        },
                        onDelete = { onDeleteCard(card.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showCardDialog) {
        FlashcardEditorDialog(
            existing = editingCard,
            questionSetId = set.id,
            subjectName = set.subjectName,
            onDismiss = { showCardDialog = false },
            onSave = { card ->
                onSaveCard(card)
                showCardDialog = false
            }
        )
    }
}

@Composable
private fun ExamPrepOverviewBanner(
    totalSets: Int,
    totalCards: Int,
    masteredCards: Int,
    subjectFilter: String,
    onStartStudy: () -> Unit,
    onStartQuiz: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_exam_ready),
                    contentDescription = stringResource(R.string.cd_exam_ready),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 96.dp, height = 74.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (subjectFilter == "All") "ALL QUESTION SETS" else subjectFilter.uppercase(),
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$totalSets sets • $totalCards cards ($masteredCards mastered)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap any set to manage cards & view scores, or launch a mixed drill below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    enabled = totalCards > 0,
                    onClick = onStartStudy,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("start_study_deck_button")
                ) {
                    Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_study_deck))
                }
                FilledTonalButton(
                    enabled = totalCards > 0,
                    onClick = onStartQuiz,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("start_quick_quiz_button")
                ) {
                    Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_quick_quiz))
                }
            }
        }
    }
}

@Composable
private fun FlashcardListItem(
    card: FlashcardEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val studyColors = LocalStudyColors.current
    var expanded by remember { mutableStateOf(false) }

    val (masteryLabel, masteryBg, masteryFg) = when (card.masteryLevel) {
        3 -> Triple("Mastered", studyColors.safeContainer, studyColors.onSafeContainer)
        2 -> Triple("Familiar", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        1 -> Triple("Learning", studyColors.warningContainer, studyColors.onWarningContainer)
        else -> Triple("New", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        onClick = { expanded = !expanded },
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flashcard_item_${card.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(
                        text = masteryLabel,
                        containerColor = masteryBg,
                        contentColor = masteryFg
                    )
                    MasteryIndicatorDots(masteryLevel = card.masteryLevel)
                }
                if (card.timesReviewed > 0) {
                    Text(
                        text = "${card.timesCorrect}/${card.timesReviewed} correct",
                        fontFamily = MonoFontFamily,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = card.question,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "ANSWER",
                        fontFamily = MonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = studyColors.safe
                    )
                    Text(
                        text = card.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (card.hint.isNotBlank()) {
                        Text(
                            text = "Hint: ${card.hint}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Tap card to hide answer" else "Tap card to reveal answer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit flashcard",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete flashcard",
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
private fun FlashcardStudyOverlay(
    session: FlashcardStudyState,
    onToggleFlip: () -> Unit,
    onRecordResult: (Boolean) -> Unit,
    onRestart: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)
    val studyColors = LocalStudyColors.current
    var showHint by remember(session.currentIndex) { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(session.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = if (session.isCompleted) {
                            "Active Recall Complete"
                        } else {
                            "Card ${session.currentIndex + 1} of ${session.cards.size}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val progress = if (session.cards.isEmpty()) 1f else {
                session.currentIndex.coerceAtMost(session.cards.size).toFloat() / session.cards.size.toFloat()
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            if (session.isCompleted) {
                SessionSummaryCard(
                    title = "Deck Completed!",
                    primaryStat = "${session.knownCount}/${session.cards.size}",
                    primaryLabel = "Cards Known",
                    subtitle = "${session.reviewAgainCount} marked for another round. Card mastery levels updated in Room.",
                    onRestart = onRestart,
                    onDone = onClose
                )
            } else {
                val card = session.cards[session.currentIndex]
                FlippableFlashcard(
                    card = card,
                    isFlipped = session.isFlipped,
                    showHint = showHint,
                    onToggleFlip = onToggleFlip,
                    onRevealHint = { showHint = true },
                    modifier = Modifier.weight(1f)
                )

                if (session.isFlipped) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRecordResult(false) },
                            border = BorderStroke(1.2.dp, studyColors.warning),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = studyColors.warning),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("study_review_again_btn")
                        ) {
                            Text("Review Again")
                        }
                        Button(
                            onClick = { onRecordResult(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = studyColors.safe,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("study_got_it_btn")
                        ) {
                            Text("Got It!")
                        }
                    }
                } else {
                    Button(
                        onClick = onToggleFlip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reveal Answer")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSelfTestOverlay(
    session: QuizSessionState,
    onSelectOption: (String) -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(session.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = if (session.isFinished) {
                            "Completed in ${QuizEngine.formatElapsedSeconds(session.elapsedSeconds)}"
                        } else {
                            "Score ${session.correctCount} • Question ${session.currentIndex + 1} of ${session.questions.size}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val progress = if (session.questions.isEmpty()) 1f else {
                session.currentIndex.coerceAtMost(session.questions.size).toFloat() / session.questions.size.toFloat()
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            if (session.isFinished) {
                val pct = if (session.questions.isEmpty()) 0 else {
                    (session.correctCount * 100) / session.questions.size
                }
                val savedNote = if (session.questionSetId != null) {
                    "Attempt saved to local Room history (${QuizEngine.formatElapsedSeconds(session.elapsedSeconds)})."
                } else {
                    "Card mastery levels updated in local storage."
                }
                SessionSummaryCard(
                    title = if (pct >= 75) "Exam Ready!" else "Keep Practicing!",
                    primaryStat = "$pct%",
                    primaryLabel = "${session.correctCount} of ${session.questions.size} correct",
                    subtitle = savedNote,
                    onRestart = onRestart,
                    onDone = onClose
                )
            } else {
                val question = session.questions[session.currentIndex]
                SelfTestQuestionCard(
                    question = question,
                    questionNumber = session.currentIndex + 1,
                    totalQuestions = session.questions.size,
                    elapsedSeconds = session.elapsedSeconds,
                    selectedOption = session.selectedOption,
                    onSelectOption = onSelectOption
                )

                if (session.selectedOption != null) {
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("quiz_next_button")
                    ) {
                        Text(
                            if (session.currentIndex + 1 >= session.questions.size) "Finish & Save Score" else "Next Question"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSummaryCard(
    title: String,
    primaryStat: String,
    primaryLabel: String,
    subtitle: String,
    onRestart: () -> Unit,
    onDone: () -> Unit
) {
    val studyColors = LocalStudyColors.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = studyColors.safe,
                modifier = Modifier.size(56.dp)
            )
            Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(
                text = primaryStat,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(primaryLabel, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Practice Again")
            }
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Done")
            }
        }
    }
}
