package dev.allium.tutor

data class LessonEvaluation(
    val passed: Boolean,
    val feedback: String? = null,
)

interface LessonEvaluator {
    fun evaluateRepair(lesson: LessonContent, answer: String): LessonEvaluation
    fun evaluateTransfer(lesson: LessonContent, answer: String): LessonEvaluation
}

class LessonEngine(
    private val evaluator: LessonEvaluator,
) {
    fun reduce(state: TutorState, action: LessonAction): TutorState = when (action) {
        LessonAction.ContinueFromIncident ->
            state.advance(LessonStage.Incident, LessonStage.Prediction)

        is LessonAction.SubmitPrediction -> {
            if (state.stage != LessonStage.Prediction || state.prediction != null) state
            else if (action.response.isBlank()) state.copy(feedback = state.lesson.prediction.emptyFeedback)
            else state.copy(
                stage = LessonStage.Evidence,
                prediction = action.response.trim(),
                feedback = null,
            )
        }

        LessonAction.ContinueAfterEvidence -> {
            if (state.stage == LessonStage.Evidence && state.prediction != null) {
                state.copy(stage = LessonStage.Concept, feedback = null)
            } else state
        }

        LessonAction.OpenWorkbench ->
            state.advance(LessonStage.Concept, LessonStage.Workbench)

        is LessonAction.UpdateRepairDraft -> {
            if (state.stage == LessonStage.Workbench) state.copy(repairDraft = action.answer)
            else state
        }

        LessonAction.SubmitRepair -> {
            if (state.stage != LessonStage.Workbench) state
            else if (state.repairDraft.isBlank()) state.copy(feedback = state.lesson.workbench.emptyFeedback)
            else state.copy(
                stage = LessonStage.VerifyingRepair,
                submissions = state.submissions + Submission(
                    kind = SubmissionKind.Repair,
                    answer = state.repairDraft,
                ),
                feedback = null,
            )
        }

        LessonAction.ContinueAfterConsequence -> {
            if (state.stage == LessonStage.Consequence && state.hasPassingRepair) {
                state.copy(stage = LessonStage.Transfer, feedback = null)
            } else state
        }

        is LessonAction.UpdateTransferDraft -> {
            if (state.stage == LessonStage.Transfer) state.copy(transferDraft = action.answer)
            else state
        }

        LessonAction.SubmitTransfer -> {
            if (state.stage != LessonStage.Transfer) state
            else if (state.transferDraft.isBlank()) state.copy(feedback = state.lesson.transfer.emptyFeedback)
            else state.copy(
                stage = LessonStage.VerifyingTransfer,
                submissions = state.submissions + Submission(
                    kind = SubmissionKind.Transfer,
                    answer = state.transferDraft,
                ),
                feedback = null,
            )
        }

        LessonAction.EvaluatePendingSubmission -> evaluatePending(state)
    }

    private fun evaluatePending(state: TutorState): TutorState {
        val expectedKind = when (state.stage) {
            LessonStage.VerifyingRepair -> SubmissionKind.Repair
            LessonStage.VerifyingTransfer -> SubmissionKind.Transfer
            else -> return state
        }

        val index = state.submissions.indexOfLast {
            it.kind == expectedKind && it.status == EvaluationStatus.Pending
        }
        if (index < 0) return state

        val submission = state.submissions[index]
        val evaluation = when (expectedKind) {
            SubmissionKind.Repair -> evaluator.evaluateRepair(state.lesson, submission.answer)
            SubmissionKind.Transfer -> evaluator.evaluateTransfer(state.lesson, submission.answer)
        }
        val evaluated = submission.copy(
            status = if (evaluation.passed) EvaluationStatus.Passed else EvaluationStatus.Failed,
        )
        val submissions = state.submissions.toMutableList().also { it[index] = evaluated }

        return when (expectedKind) {
            SubmissionKind.Repair -> state.copy(
                stage = if (evaluation.passed) LessonStage.Consequence else LessonStage.Workbench,
                submissions = submissions,
                feedback = if (evaluation.passed) null else evaluation.feedback,
            )

            SubmissionKind.Transfer -> state.copy(
                stage = if (evaluation.passed && state.hasPassingRepair) {
                    LessonStage.Completed
                } else {
                    LessonStage.Transfer
                },
                submissions = submissions,
                feedback = if (evaluation.passed) null else evaluation.feedback,
            )
        }
    }

    private fun TutorState.advance(from: LessonStage, to: LessonStage): TutorState =
        if (stage == from) copy(stage = to, feedback = null) else this
}
