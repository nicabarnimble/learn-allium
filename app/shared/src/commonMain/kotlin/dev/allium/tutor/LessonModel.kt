package dev.allium.tutor

import kotlinx.serialization.Serializable

@Serializable
data class AuthoredText(
    val paragraphs: List<String>,
) {
    fun render(): String = paragraphs.joinToString("\n\n")
}

@Serializable
data class IncidentContent(
    val body: AuthoredText,
    val actionLabel: String,
)

@Serializable
data class PredictionContent(
    val prompt: String,
    val placeholder: String,
    val actionLabel: String,
    val emptyFeedback: String,
)

@Serializable
data class EvidenceContent(
    val body: AuthoredText,
    val predictionLabel: String,
    val actionLabel: String,
)

@Serializable
data class ConceptContent(
    val name: String,
    val explanation: AuthoredText,
    val actionLabel: String,
)

@Serializable
data class WorkbenchContent(
    val prompt: String,
    val starter: String,
    val editorLabel: String,
    val actionLabel: String,
    val emptyFeedback: String,
    val verificationMessage: String,
)

@Serializable
data class ConsequenceContent(
    val body: AuthoredText,
    val actionLabel: String,
)

@Serializable
data class TransferContent(
    val prompt: AuthoredText,
    val starter: String = "",
    val placeholder: String,
    val editorLabel: String,
    val actionLabel: String,
    val emptyFeedback: String,
    val verificationMessage: String,
)

@Serializable
data class CompletionContent(
    val headline: String,
    val body: AuthoredText,
    val status: String,
)

@Serializable
data class LessonContent(
    val id: String,
    val title: String,
    val incident: IncidentContent,
    val prediction: PredictionContent,
    val evidence: EvidenceContent,
    val concept: ConceptContent,
    val workbench: WorkbenchContent,
    val consequence: ConsequenceContent,
    val transfer: TransferContent,
    val completion: CompletionContent,
)

@Serializable
enum class LessonStage {
    Incident,
    Prediction,
    Evidence,
    Concept,
    Workbench,
    VerifyingRepair,
    Consequence,
    Transfer,
    VerifyingTransfer,
    Completed,
}

@Serializable
enum class SubmissionKind { Repair, Transfer }

@Serializable
enum class EvaluationStatus { Pending, Passed, Failed }

@Serializable
data class Submission(
    val kind: SubmissionKind,
    val answer: String,
    val status: EvaluationStatus = EvaluationStatus.Pending,
)

data class TutorState(
    val lesson: LessonContent,
    val stage: LessonStage = LessonStage.Incident,
    val prediction: String? = null,
    val repairDraft: String = lesson.workbench.starter,
    val transferDraft: String = lesson.transfer.starter,
    val submissions: List<Submission> = emptyList(),
    val feedback: String? = null,
) {
    val hasPassingRepair: Boolean
        get() = submissions.any {
            it.kind == SubmissionKind.Repair && it.status == EvaluationStatus.Passed
        }

    val hasPassingTransfer: Boolean
        get() = submissions.any {
            it.kind == SubmissionKind.Transfer && it.status == EvaluationStatus.Passed
        }
}

sealed interface LessonAction {
    data object ContinueFromIncident : LessonAction
    data class SubmitPrediction(val response: String) : LessonAction
    data object ContinueAfterEvidence : LessonAction
    data object OpenWorkbench : LessonAction
    data class UpdateRepairDraft(val answer: String) : LessonAction
    data object SubmitRepair : LessonAction
    data class UpdateTransferDraft(val answer: String) : LessonAction
    data object ContinueAfterConsequence : LessonAction
    data object SubmitTransfer : LessonAction
    data object EvaluatePendingSubmission : LessonAction
}
