package dev.allium.tutor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LessonEngineTest {
    private val packageContent = testLessonPackage()
    private val lesson = packageContent.lesson
    private val evaluator = PackageLessonEvaluator(packageContent.evaluation)
    private val engine = LessonEngine(evaluator)

    @Test
    fun evidenceCannotBeReachedWithoutPrediction() {
        var state = TutorState(lesson)
        state = engine.reduce(state, LessonAction.ContinueFromIncident)
        state = engine.reduce(state, LessonAction.SubmitPrediction("   "))

        assertEquals(LessonStage.Prediction, state.stage)
        assertFalse(state.prediction != null)
        assertEquals(lesson.prediction.emptyFeedback, state.feedback)
    }

    @Test
    fun conceptFollowsRecordedPredictionAndEvidence() {
        var state = TutorState(lesson)
        state = engine.reduce(state, LessonAction.ContinueFromIncident)
        state = engine.reduce(state, LessonAction.SubmitPrediction("The date stays true."))

        assertEquals(LessonStage.Evidence, state.stage)
        assertEquals("The date stays true.", state.prediction)

        state = engine.reduce(state, LessonAction.ContinueAfterEvidence)
        assertEquals(LessonStage.Concept, state.stage)
    }

    @Test
    fun failedRepairReturnsToWorkbenchWithoutReplacingDraft() {
        var state = stateAtWorkbench().copy(repairDraft = "rule LooksFine { }")
        state = engine.reduce(state, LessonAction.SubmitRepair)
        assertEquals(LessonStage.VerifyingRepair, state.stage)

        state = engine.reduce(state, LessonAction.EvaluatePendingSubmission)
        assertEquals(LessonStage.Workbench, state.stage)
        assertEquals("rule LooksFine { }", state.repairDraft)
        assertEquals(EvaluationStatus.Failed, state.submissions.last().status)
        assertEquals(packageContent.evaluation.repair.failureFeedback, state.feedback)
    }

    @Test
    fun passingRepairRevealsConsequence() {
        var state = stateAtWorkbench().copy(repairDraft = passingRepair)
        state = engine.reduce(state, LessonAction.SubmitRepair)
        state = engine.reduce(state, LessonAction.EvaluatePendingSubmission)

        assertEquals(LessonStage.Consequence, state.stage)
        assertTrue(state.hasPassingRepair)
    }

    @Test
    fun completionRequiresPassingRepairAndTransfer() {
        var state = passingRepairState()
        state = engine.reduce(state, LessonAction.ContinueAfterConsequence)
        state = engine.reduce(state, LessonAction.UpdateTransferDraft(passingTransfer))
        state = engine.reduce(state, LessonAction.SubmitTransfer)
        state = engine.reduce(state, LessonAction.EvaluatePendingSubmission)

        assertEquals(LessonStage.Completed, state.stage)
        assertTrue(state.hasPassingRepair)
        assertTrue(state.hasPassingTransfer)
    }

    @Test
    fun evaluationIsDeterministicForTheSameAnswer() {
        assertEquals(
            evaluator.evaluateRepair(lesson, passingRepair),
            evaluator.evaluateRepair(lesson, passingRepair),
        )
    }

    private fun stateAtWorkbench(): TutorState {
        var state = TutorState(lesson)
        state = engine.reduce(state, LessonAction.ContinueFromIncident)
        state = engine.reduce(state, LessonAction.SubmitPrediction("The condition remains true."))
        state = engine.reduce(state, LessonAction.ContinueAfterEvidence)
        return engine.reduce(state, LessonAction.OpenWorkbench)
    }

    private fun passingRepairState(): TutorState {
        var state = stateAtWorkbench().copy(repairDraft = passingRepair)
        state = engine.reduce(state, LessonAction.SubmitRepair)
        return engine.reduce(state, LessonAction.EvaluatePendingSubmission)
    }

    private val passingRepair = """
        rule MarkBookOverdue {
            when: _: Book.due_at <= now
            requires: book.status = borrowed
            ensures: book.status = overdue
        }
    """.trimIndent()

    private val passingTransfer = """
        rule ExpireGrant {
            when: _: AccessGrant.expires_at <= now
            requires: grant.status = active
            ensures: grant.status = expired
        }
    """.trimIndent()
}

internal fun testLessonPackage(): LessonPackage = LessonPackage(
    formatVersion = CURRENT_LESSON_PACKAGE_VERSION,
    revision = 1,
    lesson = LessonContent(
        id = "test-lesson",
        title = "A test lesson",
        incident = IncidentContent(AuthoredText(listOf("Something happened.")), "Investigate"),
        prediction = PredictionContent(
            prompt = "What happened?",
            placeholder = "Commit to a prediction.",
            actionLabel = "Open evidence",
            emptyFeedback = "Make a prediction first.",
        ),
        evidence = EvidenceContent(
            body = AuthoredText(listOf("when: _: Book.due_at <= now")),
            predictionLabel = "Your prediction",
            actionLabel = "Name the concept",
        ),
        concept = ConceptContent(
            name = "Temporal guards",
            explanation = AuthoredText(listOf("Time and state work together.")),
            actionLabel = "Repair",
        ),
        workbench = WorkbenchContent(
            prompt = "Repair the rule.",
            starter = "rule Repair {\n    when: _: Book.due_at <= now\n}",
            editorLabel = "Repair editor",
            actionLabel = "Verify",
            emptyFeedback = "The repair cannot be empty.",
            verificationMessage = "Checking…",
        ),
        consequence = ConsequenceContent(
            body = AuthoredText(listOf("The incident is resolved.")),
            actionLabel = "Transfer",
        ),
        transfer = TransferContent(
            prompt = AuthoredText(listOf("Transfer the idea.")),
            placeholder = "Write a rule.",
            editorLabel = "Transfer editor",
            actionLabel = "Test transfer",
            emptyFeedback = "The transfer cannot be empty.",
            verificationMessage = "Testing…",
        ),
        completion = CompletionContent(
            headline = "Complete",
            body = AuthoredText(listOf("The concept transferred.")),
            status = "Both checks passed.",
        ),
    ),
    evaluation = LessonEvaluationRequirements(
        repair = EvaluationRequirements(
            requiredFragments = listOf(
                RequiredFragment("trigger", "book.due_at <= now"),
                RequiredFragment("guard", "requires: book.status = borrowed"),
                RequiredFragment("change", "ensures: book.status = overdue"),
            ),
            failureFeedback = "The repair is incomplete.",
        ),
        transfer = EvaluationRequirements(
            requiredFragments = listOf(
                RequiredFragment("trigger", "grant.expires_at <= now"),
                RequiredFragment("guard", "requires: grant.status = active"),
                RequiredFragment("change", "ensures: grant.status = expired"),
            ),
            failureFeedback = "The transfer is incomplete.",
        ),
    ),
).validated()
