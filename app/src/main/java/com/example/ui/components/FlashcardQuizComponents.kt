package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FlashcardEntity
import com.example.data.QuestionSetWithDetails
import com.example.data.QuizAttemptEntity
import com.example.domain.GeneratedQuizQuestion
import com.example.domain.QuizEngine
import com.example.ui.theme.LocalStudyColors
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.parseHexColor

@Composable
fun QuestionSetCard(
    setWithDetails: QuestionSetWithDetails,
    onOpenSet: () -> Unit,
    onStartStudy: () -> Unit,
    onStartQuiz: () -> Unit,
    onEditSet: () -> Unit,
    onDeleteSet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val set = setWithDetails.questionSet
    val accent = parseHexColor(set.colorHex)
    val studyColors = LocalStudyColors.current
    val hasCards = setWithDetails.cards.isNotEmpty()

    Surface(
        onClick = onOpenSet,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("question_set_card_${set.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            text = set.subjectName,
                            containerColor = accent.copy(alpha = 0.14f),
                            contentColor = accent
                        )
                        setWithDetails.bestScorePercent?.let { best ->
                            StatusPill(
                                text = "Best $best%",
                                containerColor = studyColors.safeContainer,
                                contentColor = studyColors.onSafeContainer,
                                icon = Icons.Default.EmojiEvents
                            )
                        }
                    }

                    Text(
                        text = set.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (set.description.isNotBlank()) {
                        Text(
                            text = set.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column {
                    IconButton(onClick = onEditSet, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit question set ${set.title}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(onClick = onDeleteSet, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete question set ${set.title}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // Mastery Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${setWithDetails.cards.size} cards • ${setWithDetails.masteredCount} mastered",
                        fontFamily = MonoFontFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(setWithDetails.masteryFraction * 100).toInt()}%",
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = accent
                    )
                }
                LinearProgressIndicator(
                    progress = { setWithDetails.masteryFraction },
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            // Quick launch actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    enabled = hasCards,
                    onClick = onStartStudy,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("set_study_btn_${set.id}")
                ) {
                    Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Flip Study", style = MaterialTheme.typography.labelLarge)
                }
                FilledTonalButton(
                    enabled = hasCards,
                    onClick = onStartQuiz,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("set_quiz_btn_${set.id}")
                ) {
                    Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Self-Test", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun MasteryIndicatorDots(
    masteryLevel: Int,
    modifier: Modifier = Modifier
) {
    val studyColors = LocalStudyColors.current
    val activeColor = when (masteryLevel) {
        3 -> studyColors.safe
        2 -> MaterialTheme.colorScheme.primary
        1 -> studyColors.warning
        else -> MaterialTheme.colorScheme.outline
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        repeat(3) { idx ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        if (idx < masteryLevel) activeColor else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
fun FlippableFlashcard(
    card: FlashcardEntity,
    isFlipped: Boolean,
    showHint: Boolean,
    onToggleFlip: () -> Unit,
    onRevealHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val studyColors = LocalStudyColors.current
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 240f),
        label = "flippable_card_rotation"
    )
    val showingBack = rotation > 90f

    Surface(
        color = if (showingBack) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = 1.5.dp,
            color = if (showingBack) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
        ),
        tonalElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clickable { onToggleFlip() }
            .testTag("study_flip_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = if (showingBack) 180f else 0f
                }
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusPill(
                    text = if (showingBack) "ANSWER" else card.subjectName.uppercase(),
                    containerColor = if (showingBack) studyColors.safe else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (showingBack) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                )
                MasteryIndicatorDots(masteryLevel = card.masteryLevel)
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = if (showingBack) card.answer else card.question,
                style = if (showingBack) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (!showingBack && card.hint.isNotBlank()) {
                AnimatedVisibility(visible = showHint) {
                    Surface(
                        color = studyColors.warningContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "Hint: ${card.hint}",
                            style = MaterialTheme.typography.bodySmall,
                            color = studyColors.onWarningContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                if (!showHint) {
                    TextButton(
                        onClick = onRevealHint,
                        modifier = Modifier.testTag("flashcard_hint_btn")
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Show Hint")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (showingBack) "Rate your recall below" else "Tap anywhere on card to flip",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SelfTestQuestionCard(
    question: GeneratedQuizQuestion,
    questionNumber: Int,
    totalQuestions: Int,
    elapsedSeconds: Int,
    selectedOption: String?,
    onSelectOption: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val studyColors = LocalStudyColors.current
    val hasAnswered = selectedOption != null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        text = "Q$questionNumber/$totalQuestions • ${question.card.subjectName}",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Elapsed quiz time",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = QuizEngine.formatElapsedSeconds(elapsedSeconds),
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = question.card.question,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            question.options.forEachIndexed { index, option ->
                val isSelected = selectedOption == option
                val isCorrect = option == question.correctOption

                val containerColor = when {
                    !hasAnswered -> MaterialTheme.colorScheme.surface
                    isCorrect -> studyColors.safeContainer
                    isSelected -> studyColors.dangerContainer
                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                }
                val borderColor = when {
                    !hasAnswered -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    isCorrect -> studyColors.safe
                    isSelected -> studyColors.danger
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                }
                val textColor = when {
                    !hasAnswered -> MaterialTheme.colorScheme.onSurface
                    isCorrect -> studyColors.onSafeContainer
                    isSelected -> studyColors.onDangerContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    onClick = { if (!hasAnswered) onSelectOption(option) },
                    color = containerColor,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(
                        width = if (hasAnswered && (isCorrect || isSelected)) 1.5.dp else 1.dp,
                        color = borderColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quiz_option_$index")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(borderColor.copy(alpha = 0.18f))
                        ) {
                            if (hasAnswered && isCorrect) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Correct option",
                                    tint = studyColors.safe,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = ('A' + index).toString(),
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = textColor
                                )
                            }
                        }
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = hasAnswered && question.card.hint.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = studyColors.warning,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Key takeaway: ${question.card.hint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun QuizHistoryStrip(
    attempts: List<QuizAttemptEntity>,
    modifier: Modifier = Modifier
) {
    if (attempts.isEmpty()) return
    val studyColors = LocalStudyColors.current
    val ordered = attempts.sortedByDescending { it.completedAt }.take(6)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "RECENT SELF-TEST SCORES",
            fontFamily = MonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ordered, key = { it.id }) { attempt ->
                val pct = attempt.percentage
                val pillBg = when {
                    pct >= 80 -> studyColors.safeContainer
                    pct >= 60 -> studyColors.warningContainer
                    else -> studyColors.dangerContainer
                }
                val pillFg = when {
                    pct >= 80 -> studyColors.onSafeContainer
                    pct >= 60 -> studyColors.onWarningContainer
                    else -> studyColors.onDangerContainer
                }
                Surface(
                    color = pillBg,
                    contentColor = pillFg,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "$pct%",
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${attempt.score}/${attempt.totalQuestions} • ${QuizEngine.formatElapsedSeconds(attempt.durationSeconds)}",
                            fontFamily = MonoFontFamily,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
