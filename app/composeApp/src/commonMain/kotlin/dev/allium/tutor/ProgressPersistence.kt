package dev.allium.tutor

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val CURRENT_PROGRESS_FORMAT_VERSION = 1

interface LessonProgressStore {
    suspend fun read(lessonId: String): String?
    suspend fun write(lessonId: String, snapshot: String)
}

expect fun createPlatformLessonProgressStore(): LessonProgressStore

class InvalidProgressSnapshotException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

@Serializable
private data class ProgressSnapshot(
    val formatVersion: Int,
    val lessonId: String,
    val lessonRevision: Int,
    val stage: LessonStage,
    val prediction: String?,
    val repairDraft: String,
    val transferDraft: String,
    val submissions: List<Submission>,
    val feedback: String?,
)

object ProgressCodec {
    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
    }

    fun encode(packageContent: LessonPackage, state: TutorState): String {
        require(state.lesson.id == packageContent.lesson.id) {
            "Cannot save progress for a different lesson"
        }
        return json.encodeToString(
            ProgressSnapshot(
                formatVersion = CURRENT_PROGRESS_FORMAT_VERSION,
                lessonId = packageContent.lesson.id,
                lessonRevision = packageContent.revision,
                stage = state.stage,
                prediction = state.prediction,
                repairDraft = state.repairDraft,
                transferDraft = state.transferDraft,
                submissions = state.submissions,
                feedback = state.feedback,
            ),
        )
    }

    fun decode(source: String, packageContent: LessonPackage): TutorState {
        val snapshot = try {
            json.decodeFromString<ProgressSnapshot>(source)
        } catch (error: Exception) {
            throw InvalidProgressSnapshotException("Saved progress is not valid JSON", error)
        }

        if (snapshot.formatVersion != CURRENT_PROGRESS_FORMAT_VERSION) {
            throw InvalidProgressSnapshotException(
                "Unsupported progress format ${snapshot.formatVersion}",
            )
        }
        if (snapshot.lessonId != packageContent.lesson.id) {
            throw InvalidProgressSnapshotException(
                "Saved progress belongs to lesson ${snapshot.lessonId}",
            )
        }
        if (snapshot.lessonRevision != packageContent.revision) {
            throw InvalidProgressSnapshotException(
                "Saved progress targets lesson revision ${snapshot.lessonRevision}",
            )
        }
        if (!snapshot.isConsistent()) {
            throw InvalidProgressSnapshotException("Saved progress violates lesson lifecycle invariants")
        }

        return TutorState(
            lesson = packageContent.lesson,
            stage = snapshot.stage,
            prediction = snapshot.prediction,
            repairDraft = snapshot.repairDraft,
            transferDraft = snapshot.transferDraft,
            submissions = snapshot.submissions,
            feedback = snapshot.feedback,
        )
    }

    private fun ProgressSnapshot.isConsistent(): Boolean {
        val hasPassingRepair = submissions.any {
            it.kind == SubmissionKind.Repair && it.status == EvaluationStatus.Passed
        }
        val hasPassingTransfer = submissions.any {
            it.kind == SubmissionKind.Transfer && it.status == EvaluationStatus.Passed
        }
        val pendingRepairCount = submissions.count {
            it.kind == SubmissionKind.Repair && it.status == EvaluationStatus.Pending
        }
        val pendingTransferCount = submissions.count {
            it.kind == SubmissionKind.Transfer && it.status == EvaluationStatus.Pending
        }
        val hasTransferSubmission = submissions.any { it.kind == SubmissionKind.Transfer }
        val hasPendingSubmission = pendingRepairCount > 0 || pendingTransferCount > 0

        val predictionRequired = stage.ordinal >= LessonStage.Evidence.ordinal
        if (predictionRequired && prediction.isNullOrBlank()) return false
        if (!predictionRequired && prediction != null) return false

        return when (stage) {
            LessonStage.Incident,
            LessonStage.Prediction,
            LessonStage.Evidence,
            LessonStage.Concept,
            -> submissions.isEmpty()

            LessonStage.Workbench ->
                !hasPassingRepair && !hasTransferSubmission && !hasPendingSubmission

            LessonStage.VerifyingRepair ->
                !hasPassingRepair && !hasTransferSubmission && pendingRepairCount == 1

            LessonStage.Consequence ->
                hasPassingRepair && !hasTransferSubmission && !hasPendingSubmission

            LessonStage.Transfer ->
                hasPassingRepair && !hasPassingTransfer && !hasPendingSubmission

            LessonStage.VerifyingTransfer ->
                hasPassingRepair && !hasPassingTransfer && pendingTransferCount == 1 &&
                    pendingRepairCount == 0

            LessonStage.Completed ->
                hasPassingRepair && hasPassingTransfer && !hasPendingSubmission
        }
    }
}
