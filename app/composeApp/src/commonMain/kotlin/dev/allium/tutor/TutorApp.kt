package dev.allium.tutor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel

private val Ink = Color(0xFFE9E6DF)
private val MutedInk = Color(0xFFAAA69D)
private val Canvas = Color(0xFF151614)
private val Panel = Color(0xFF20221F)
private val Rule = Color(0xFF3A3D37)
private val Evidence = Color(0xFF8EB7C7)
private val Action = Color(0xFFE0B86A)
private val Success = Color(0xFF91B98B)
private val Failure = Color(0xFFD58A7D)

private val ProseStyle = TextStyle(
    color = Ink,
    fontFamily = FontFamily.SansSerif,
    fontSize = 18.sp,
    lineHeight = 28.sp,
)

private val SmallStyle = TextStyle(
    color = MutedInk,
    fontFamily = FontFamily.SansSerif,
    fontSize = 13.sp,
    lineHeight = 18.sp,
)

private val CodeStyle = TextStyle(
    color = Ink,
    fontFamily = FontFamily.Monospace,
    fontSize = 15.sp,
    lineHeight = 22.sp,
)

private sealed interface TutorApplicationLoadState {
    data object Loading : TutorApplicationLoadState
    data class Ready(
        val application: TutorApplication,
        val initialState: TutorState,
    ) : TutorApplicationLoadState

    data class Failed(val message: String) : TutorApplicationLoadState
}

@Composable
fun TutorApp() {
    var loadState by remember { mutableStateOf<TutorApplicationLoadState>(TutorApplicationLoadState.Loading) }

    LaunchedEffect(Unit) {
        loadState = try {
            val application = createTutorApplication(loadTemporalOverduePackage())
            TutorApplicationLoadState.Ready(
                application = application,
                initialState = application.restoreState(),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            TutorApplicationLoadState.Failed(error.message ?: "The lesson package could not be loaded.")
        }
    }

    when (val current = loadState) {
        TutorApplicationLoadState.Loading -> PackageStatus("Opening the lesson…")
        is TutorApplicationLoadState.Ready -> TutorSession(
            application = current.application,
            initialState = current.initialState,
            persistChanges = true,
        )
        is TutorApplicationLoadState.Failed -> PackageStatus(current.message, Failure)
    }
}

@Composable
private fun PackageStatus(message: String, color: Color = Evidence) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(message, style = ProseStyle.copy(color = color))
    }
}

@Composable
fun TutorApp(application: TutorApplication) {
    TutorSession(
        application = application,
        initialState = application.initialState(),
        persistChanges = false,
    )
}

@Composable
private fun TutorSession(
    application: TutorApplication,
    initialState: TutorState,
    persistChanges: Boolean,
) {
    var state by remember(application, initialState) { mutableStateOf(initialState) }
    var durabilityFailure by remember(application) { mutableStateOf<String?>(null) }
    val actions = remember(application) { Channel<LessonAction>(Channel.UNLIMITED) }
    val dispatch: (LessonAction) -> Unit = { action ->
        check(actions.trySend(action).isSuccess) { "Lesson action queue is closed" }
    }

    DisposableEffect(actions) {
        onDispose { actions.close() }
    }

    LaunchedEffect(actions, application, persistChanges) {
        for (action in actions) {
            try {
                state = if (persistChanges) {
                    application.reduceDurably(state, action)
                } else {
                    application.engine.reduce(state, action)
                }
                durabilityFailure = null
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                durabilityFailure = error.message ?: "Progress could not be saved."
            }
        }
    }

    LaunchedEffect(state.stage, state.submissions.size) {
        if (
            state.stage == LessonStage.VerifyingRepair ||
            state.stage == LessonStage.VerifyingTransfer
        ) {
            dispatch(LessonAction.EvaluatePendingSubmission)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas),
    ) {
        val compact = maxWidth < 640.dp
        val horizontalPadding = if (compact) 22.dp else 48.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 780.dp),
            ) {
                LessonHeader(state)
                if (durabilityFailure != null) {
                    Spacer(Modifier.height(18.dp))
                    Feedback("Progress was not applied: $durabilityFailure")
                }
                Spacer(Modifier.height(if (compact) 34.dp else 52.dp))
                Stage(state = state, dispatch = dispatch)
            }
        }
    }
}

@Composable
private fun LessonHeader(state: TutorState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = "ALLIUM · ${state.lesson.title.uppercase()}",
            style = SmallStyle.copy(color = Evidence, fontWeight = FontWeight.SemiBold),
        )
        BasicText(
            text = "${state.stage.ordinal + 1} / ${LessonStage.entries.size}",
            style = SmallStyle,
        )
    }
}

@Composable
private fun Stage(
    state: TutorState,
    dispatch: (LessonAction) -> Unit,
) {
    when (state.stage) {
        LessonStage.Incident -> IncidentStage(state, dispatch)
        LessonStage.Prediction -> PredictionStage(state, dispatch)
        LessonStage.Evidence -> EvidenceStage(state, dispatch)
        LessonStage.Concept -> ConceptStage(state, dispatch)
        LessonStage.Workbench -> WorkbenchStage(state, dispatch)
        LessonStage.VerifyingRepair -> VerifyingStage(state.lesson.workbench.verificationMessage)
        LessonStage.Consequence -> ConsequenceStage(state, dispatch)
        LessonStage.Transfer -> TransferStage(state, dispatch)
        LessonStage.VerifyingTransfer -> VerifyingStage(state.lesson.transfer.verificationMessage)
        LessonStage.Completed -> CompletedStage(state)
    }
}

@Composable
private fun IncidentStage(state: TutorState, dispatch: (LessonAction) -> Unit) {
    StageLabel("Incident")
    Story(state.lesson.incident.body.render())
    Spacer(Modifier.height(34.dp))
    TutorButton(state.lesson.incident.actionLabel) {
        dispatch(LessonAction.ContinueFromIncident)
    }
}

@Composable
private fun PredictionStage(state: TutorState, dispatch: (LessonAction) -> Unit) {
    var prediction by remember { mutableStateOf("") }

    StageLabel("Your prediction")
    Story(state.lesson.prediction.prompt)
    Spacer(Modifier.height(24.dp))
    Editor(
        value = prediction,
        onValueChange = { prediction = it },
        minHeight = 130.dp,
        placeholder = state.lesson.prediction.placeholder,
        contentDescription = "Prediction",
    )
    Feedback(state.feedback)
    Spacer(Modifier.height(24.dp))
    TutorButton(state.lesson.prediction.actionLabel) {
        dispatch(LessonAction.SubmitPrediction(prediction))
    }
}

@Composable
private fun EvidenceStage(state: TutorState, dispatch: (LessonAction) -> Unit) {
    StageLabel("Evidence")
    EvidencePanel(state.lesson.evidence.body.render())
    Spacer(Modifier.height(22.dp))
    BasicText(
        state.lesson.evidence.predictionLabel,
        style = SmallStyle.copy(fontWeight = FontWeight.SemiBold),
    )
    Spacer(Modifier.height(8.dp))
    Story(state.prediction.orEmpty(), color = MutedInk, italic = true)
    Spacer(Modifier.height(30.dp))
    TutorButton(state.lesson.evidence.actionLabel) {
        dispatch(LessonAction.ContinueAfterEvidence)
    }
}

@Composable
private fun ConceptStage(state: TutorState, dispatch: (LessonAction) -> Unit) {
    StageLabel("Concept")
    BasicText(
        text = state.lesson.concept.name,
        style = ProseStyle.copy(
            color = Action,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
    Spacer(Modifier.height(20.dp))
    Story(state.lesson.concept.explanation.render())
    Spacer(Modifier.height(34.dp))
    TutorButton(state.lesson.concept.actionLabel) {
        dispatch(LessonAction.OpenWorkbench)
    }
}

@Composable
private fun WorkbenchStage(state: TutorState, dispatch: (LessonAction) -> Unit) {
    StageLabel("Workbench")
    Story(state.lesson.workbench.prompt)
    Spacer(Modifier.height(20.dp))
    Editor(
        value = state.repairDraft,
        onValueChange = { dispatch(LessonAction.UpdateRepairDraft(it)) },
        minHeight = 260.dp,
        contentDescription = state.lesson.workbench.editorLabel,
        monospace = true,
    )
    Feedback(state.feedback)
    Spacer(Modifier.height(24.dp))
    TutorButton(state.lesson.workbench.actionLabel) {
        dispatch(LessonAction.SubmitRepair)
    }
}

@Composable
private fun VerifyingStage(message: String) {
    StageLabel("Verification")
    Story(message, color = Evidence)
}

@Composable
private fun ConsequenceStage(state: TutorState, dispatch: (LessonAction) -> Unit) {
    StageLabel("Consequence")
    Story(state.lesson.consequence.body.render())
    Spacer(Modifier.height(34.dp))
    TutorButton(state.lesson.consequence.actionLabel) {
        dispatch(LessonAction.ContinueAfterConsequence)
    }
}

@Composable
private fun TransferStage(state: TutorState, dispatch: (LessonAction) -> Unit) {
    StageLabel("Transfer")
    Story(state.lesson.transfer.prompt.render())
    Spacer(Modifier.height(20.dp))
    Editor(
        value = state.transferDraft,
        onValueChange = { dispatch(LessonAction.UpdateTransferDraft(it)) },
        minHeight = 220.dp,
        placeholder = state.lesson.transfer.placeholder,
        contentDescription = state.lesson.transfer.editorLabel,
        monospace = true,
    )
    Feedback(state.feedback)
    Spacer(Modifier.height(24.dp))
    TutorButton(state.lesson.transfer.actionLabel) {
        dispatch(LessonAction.SubmitTransfer)
    }
}

@Composable
private fun CompletedStage(state: TutorState) {
    StageLabel("Complete")
    BasicText(
        text = state.lesson.completion.headline,
        style = ProseStyle.copy(
            color = Success,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
    Spacer(Modifier.height(18.dp))
    Story(state.lesson.completion.body.render())
    Spacer(Modifier.height(28.dp))
    BasicText(
        text = state.lesson.completion.status,
        style = SmallStyle.copy(color = Success),
    )
}

@Composable
private fun StageLabel(text: String) {
    BasicText(
        text = text.uppercase(),
        style = SmallStyle.copy(color = Action, fontWeight = FontWeight.Bold),
    )
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun Story(
    text: String,
    color: Color = Ink,
    italic: Boolean = false,
) {
    SelectionContainer {
        BasicText(
            text = text,
            style = ProseStyle.copy(
                color = color,
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            ),
        )
    }
}

@Composable
private fun EvidencePanel(text: String) {
    SelectionContainer {
        BasicText(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Panel)
                .border(1.dp, Rule, RoundedCornerShape(8.dp))
                .padding(20.dp),
            style = CodeStyle.copy(color = Evidence),
        )
    }
}

@Composable
private fun Editor(
    value: String,
    onValueChange: (String) -> Unit,
    minHeight: androidx.compose.ui.unit.Dp,
    contentDescription: String,
    placeholder: String = "",
    monospace: Boolean = false,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .semantics { this.contentDescription = contentDescription }
            .clip(RoundedCornerShape(8.dp))
            .background(Panel)
            .border(1.dp, Rule, RoundedCornerShape(8.dp)),
        textStyle = if (monospace) CodeStyle else ProseStyle,
        cursorBrush = SolidColor(Action),
        decorationBox = { innerTextField ->
            Box(Modifier.padding(18.dp)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    BasicText(placeholder, style = SmallStyle)
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun Feedback(message: String?) {
    if (message == null) return
    Spacer(Modifier.height(12.dp))
    BasicText(message, style = SmallStyle.copy(color = Failure))
}

@Composable
private fun TutorButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Action)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = SmallStyle.copy(
                color = Canvas,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
