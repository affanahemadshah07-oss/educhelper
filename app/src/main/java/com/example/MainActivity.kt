package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainTab
import com.example.ui.StudyHubViewModel
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.DeadlinesScreen
import com.example.ui.screens.FlashcardsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.StudyHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyHubTheme {
                StudyHubApp()
            }
        }
    }
}

private data class NavDestination(
    val tab: MainTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val NavDestinations = listOf(
    NavDestination(
        tab = MainTab.TODAY,
        labelRes = R.string.nav_today,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth,
        testTag = "nav_today"
    ),
    NavDestination(
        tab = MainTab.ATTENDANCE,
        labelRes = R.string.nav_attendance,
        selectedIcon = Icons.Filled.School,
        unselectedIcon = Icons.Outlined.School,
        testTag = "nav_attendance"
    ),
    NavDestination(
        tab = MainTab.FLASHCARDS,
        labelRes = R.string.nav_flashcards,
        selectedIcon = Icons.Filled.Style,
        unselectedIcon = Icons.Outlined.Style,
        testTag = "nav_flashcards"
    ),
    NavDestination(
        tab = MainTab.DEADLINES,
        labelRes = R.string.nav_deadlines,
        selectedIcon = Icons.Filled.EventAvailable,
        unselectedIcon = Icons.Outlined.EventAvailable,
        testTag = "nav_deadlines"
    )
)

@Composable
fun StudyHubApp(
    viewModel: StudyHubViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val studySession by viewModel.studySession.collectAsStateWithLifecycle()
    val quizSession by viewModel.quizSession.collectAsStateWithLifecycle()
    val lastUndo by viewModel.lastAttendanceUndo.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val undoActionLabel = stringResource(R.string.action_undo)

    LaunchedEffect(lastUndo) {
        val snapshot = lastUndo ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = snapshot.actionLabel,
            actionLabel = undoActionLabel,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoLastAttendance()
        } else {
            viewModel.clearUndoSnackbar()
        }
    }

    val isFullScreenOverlay = activeTab == MainTab.FLASHCARDS && (studySession != null || quizSession != null)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail && !isFullScreenOverlay) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        NavDestinations.forEach { dest ->
                            val selected = activeTab == dest.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail && !isFullScreenOverlay) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        NavDestinations.forEach { dest ->
                            val selected = activeTab == dest.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (activeTab) {
                        MainTab.TODAY -> HomeScreen(
                            uiState = uiState,
                            onSelectDay = viewModel::selectDay,
                            onNavigateTab = viewModel::selectTab,
                            onSaveSlot = viewModel::saveTimetableSlot,
                            onDeleteSlot = viewModel::deleteTimetableSlot,
                            onMarkAttendance = viewModel::markAttendance,
                            onLoadSampleSemester = viewModel::loadSampleSemester
                        )

                        MainTab.ATTENDANCE -> AttendanceScreen(
                            uiState = uiState,
                            onUpdateDefaultTarget = viewModel::updateDefaultTargetPercent,
                            onSaveSubject = viewModel::saveSubject,
                            onDeleteSubject = viewModel::deleteSubject,
                            onMarkAttendance = viewModel::markAttendance
                        )

                        MainTab.FLASHCARDS -> FlashcardsScreen(
                            uiState = uiState,
                            studySession = studySession,
                            quizSession = quizSession,
                            onSelectSubjectFilter = viewModel::selectFlashcardSubject,
                            onOpenQuestionSet = viewModel::openQuestionSet,
                            onSaveQuestionSet = viewModel::saveQuestionSet,
                            onDeleteQuestionSet = viewModel::deleteQuestionSet,
                            onSaveCard = viewModel::saveFlashcard,
                            onDeleteCard = viewModel::deleteFlashcard,
                            onStartStudySet = viewModel::startStudyForSet,
                            onStartStudyAll = viewModel::startStudySession,
                            onToggleFlip = viewModel::toggleStudyCardFlip,
                            onRecordStudyResult = viewModel::recordStudyCardResult,
                            onRestartStudy = viewModel::restartStudySession,
                            onCloseStudy = viewModel::closeStudySession,
                            onStartQuizSet = viewModel::startQuizForSet,
                            onStartQuizAll = viewModel::startQuickSelfTest,
                            onSubmitQuizOption = viewModel::submitQuizOption,
                            onAdvanceQuiz = viewModel::advanceQuizQuestion,
                            onRestartQuiz = viewModel::restartQuizSession,
                            onCloseQuiz = viewModel::closeQuizSession
                        )

                        MainTab.DEADLINES -> DeadlinesScreen(
                            uiState = uiState,
                            onSelectFilter = viewModel::selectDeadlineFilter,
                            onSaveDeadline = viewModel::saveDeadline,
                            onToggleComplete = viewModel::toggleDeadlineComplete,
                            onDeleteDeadline = viewModel::deleteDeadline
                        )
                    }
                }
            }
        }
    }
}
