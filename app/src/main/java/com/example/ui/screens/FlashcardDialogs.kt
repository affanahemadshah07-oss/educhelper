package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.FlashcardEntity
import com.example.data.QuestionSetEntity
import com.example.data.SubjectEntity
import com.example.ui.components.SubjectColorPicker
import com.example.ui.theme.SubjectColorHexes

@Composable
fun QuestionSetEditorDialog(
    existing: QuestionSetEntity?,
    subjects: List<SubjectEntity>,
    defaultSubject: String,
    onDismiss: () -> Unit,
    onSave: (QuestionSetEntity) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var subjectName by remember {
        mutableStateOf(
            existing?.subjectName
                ?: defaultSubject.ifBlank { subjects.firstOrNull()?.name.orEmpty() }
        )
    }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var colorHex by remember {
        mutableStateOf(
            existing?.colorHex
                ?: subjects.firstOrNull { it.name.equals(subjectName, ignoreCase = true) }?.colorHex
                ?: SubjectColorHexes.first()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Question Set" else "Edit Question Set") },
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
                    label = { Text("Set Title") },
                    placeholder = { Text("e.g., Midterm: Deadlocks & Memory") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("question_set_title_input")
                )

                if (subjects.isNotEmpty()) {
                    Text("Subject", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects, key = { it.id }) { sub ->
                            FilterChip(
                                selected = subjectName.equals(sub.name, ignoreCase = true),
                                onClick = {
                                    subjectName = sub.name
                                    colorHex = sub.colorHex
                                },
                                label = { Text(sub.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject Name") },
                    placeholder = { Text("e.g., Operating Systems") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("question_set_subject_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Topics Covered (optional)") },
                    placeholder = { Text("Chapters, formulas, or concepts") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Accent Color", style = MaterialTheme.typography.labelMedium)
                SubjectColorPicker(
                    selectedHex = colorHex,
                    onSelect = { colorHex = it }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.trim().isNotEmpty() && subjectName.trim().isNotEmpty(),
                onClick = {
                    onSave(
                        QuestionSetEntity(
                            id = existing?.id ?: 0L,
                            title = title.trim(),
                            subjectName = subjectName.trim(),
                            description = description.trim(),
                            colorHex = colorHex,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier.testTag("save_question_set_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
fun FlashcardEditorDialog(
    existing: FlashcardEntity?,
    questionSetId: Long,
    subjectName: String,
    onDismiss: () -> Unit,
    onSave: (FlashcardEntity) -> Unit
) {
    var question by remember { mutableStateOf(existing?.question.orEmpty()) }
    var answer by remember { mutableStateOf(existing?.answer.orEmpty()) }
    var hint by remember { mutableStateOf(existing?.hint.orEmpty()) }
    var distractorOne by remember { mutableStateOf(existing?.distractorOne.orEmpty()) }
    var distractorTwo by remember { mutableStateOf(existing?.distractorTwo.orEmpty()) }
    var distractorThree by remember { mutableStateOf(existing?.distractorThree.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Question Card" else "Edit Question Card") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Question / Prompt") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("flashcard_question_input")
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Correct Answer") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("flashcard_answer_input")
                )
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Hint / Memory Cue (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Custom Quiz Distractors (optional — auto-filled from deck if blank)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = distractorOne,
                    onValueChange = { distractorOne = it },
                    label = { Text("Wrong Option 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = distractorTwo,
                    onValueChange = { distractorTwo = it },
                    label = { Text("Wrong Option 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = distractorThree,
                    onValueChange = { distractorThree = it },
                    label = { Text("Wrong Option 3") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = question.trim().isNotEmpty() && answer.trim().isNotEmpty(),
                onClick = {
                    onSave(
                        FlashcardEntity(
                            id = existing?.id ?: 0L,
                            questionSetId = existing?.questionSetId ?: questionSetId,
                            subjectName = existing?.subjectName ?: subjectName,
                            question = question.trim(),
                            answer = answer.trim(),
                            distractorOne = distractorOne.trim(),
                            distractorTwo = distractorTwo.trim(),
                            distractorThree = distractorThree.trim(),
                            hint = hint.trim(),
                            masteryLevel = existing?.masteryLevel ?: 0,
                            timesReviewed = existing?.timesReviewed ?: 0,
                            timesCorrect = existing?.timesCorrect ?: 0,
                            lastReviewedAt = existing?.lastReviewedAt ?: 0L,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier.testTag("save_flashcard_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
