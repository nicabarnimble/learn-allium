package dev.allium.tutor

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProgressPersistenceTest {
    private val packageContent = testLessonPackage()

    @Test
    fun progressRoundTripPreservesLearnerWorkAndEvaluations() {
        val state = TutorState(
            lesson = packageContent.lesson,
            stage = LessonStage.Consequence,
            prediction = "The condition stays true.",
            repairDraft = "complete repair",
            transferDraft = "transfer in progress",
            submissions = listOf(
                Submission(
                    kind = SubmissionKind.Repair,
                    answer = "complete repair",
                    status = EvaluationStatus.Passed,
                ),
            ),
            feedback = null,
        )

        val restored = ProgressCodec.decode(
            ProgressCodec.encode(packageContent, state),
            packageContent,
        )

        assertEquals(state, restored)
    }

    @Test
    fun progressFromAnotherLessonRevisionIsIgnored() {
        val state = TutorState(packageContent.lesson)
        val encoded = ProgressCodec.encode(packageContent, state)
        val nextRevision = packageContent.copy(revision = packageContent.revision + 1)

        assertFailsWith<InvalidProgressSnapshotException> {
            ProgressCodec.decode(encoded, nextRevision)
        }
    }

    @Test
    fun impossibleCompletedStateIsIgnored() {
        val impossible = TutorState(
            lesson = packageContent.lesson,
            stage = LessonStage.Completed,
            prediction = "A prediction",
        )

        assertFailsWith<InvalidProgressSnapshotException> {
            ProgressCodec.decode(
                ProgressCodec.encode(packageContent, impossible),
                packageContent,
            )
        }
    }

    @Test
    fun earlyLifecycleStateCannotCarryEvaluationResults() {
        val impossible = TutorState(
            lesson = packageContent.lesson,
            stage = LessonStage.Incident,
            submissions = listOf(
                Submission(SubmissionKind.Repair, "repair", EvaluationStatus.Passed),
            ),
        )

        assertFailsWith<InvalidProgressSnapshotException> {
            ProgressCodec.decode(
                ProgressCodec.encode(packageContent, impossible),
                packageContent,
            )
        }
    }

    @Test
    fun pendingEvaluationCanResumeAfterTermination() {
        val pending = TutorState(
            lesson = packageContent.lesson,
            stage = LessonStage.VerifyingRepair,
            prediction = "A prediction",
            repairDraft = "repair",
            submissions = listOf(
                Submission(SubmissionKind.Repair, "repair", EvaluationStatus.Pending),
            ),
        )

        val restored = ProgressCodec.decode(
            ProgressCodec.encode(packageContent, pending),
            packageContent,
        )

        assertEquals(LessonStage.VerifyingRepair, restored.stage)
        assertEquals(EvaluationStatus.Pending, restored.submissions.last().status)
    }

    @Test
    fun applicationSavesAndRestoresThroughAnInjectedStore() = runTest {
        val store = InMemoryProgressStore()
        val application = createTutorApplication(packageContent, store)
        val state = TutorState(
            lesson = packageContent.lesson,
            stage = LessonStage.Evidence,
            prediction = "Inspect the temporal condition.",
        )

        application.saveState(state)
        val restored = application.restoreState()

        assertEquals(state, restored)
    }

    @Test
    fun applicationDoesNotDiscardDurabilityFailures() = runTest {
        val application = createTutorApplication(packageContent, FailingProgressStore())

        assertFailsWith<IllegalStateException> {
            application.saveState(application.initialState())
        }
        assertFailsWith<IllegalStateException> {
            application.restoreState()
        }
    }

    @Test
    fun durableReductionDoesNotReturnAnUnwrittenTransition() = runTest {
        val application = createTutorApplication(packageContent, FailingProgressStore())
        val original = application.initialState()

        assertFailsWith<IllegalStateException> {
            application.reduceDurably(original, LessonAction.ContinueFromIncident)
        }
        assertEquals(LessonStage.Incident, original.stage)
    }

    @Test
    fun durableReductionWritesTheCandidateBeforeReturningIt() = runTest {
        val store = InMemoryProgressStore()
        val application = createTutorApplication(packageContent, store)

        val next = application.reduceDurably(
            application.initialState(),
            LessonAction.ContinueFromIncident,
        )
        val persisted = application.restoreState()

        assertEquals(LessonStage.Prediction, next.stage)
        assertEquals(next, persisted)
    }
}

private class FailingProgressStore : LessonProgressStore {
    override suspend fun read(lessonId: String): String? = error("read failed")

    override suspend fun write(lessonId: String, snapshot: String) {
        error("write failed")
    }
}

private class InMemoryProgressStore : LessonProgressStore {
    private val snapshots = mutableMapOf<String, String>()

    override suspend fun read(lessonId: String): String? = snapshots[lessonId]

    override suspend fun write(lessonId: String, snapshot: String) {
        snapshots[lessonId] = snapshot
    }
}
