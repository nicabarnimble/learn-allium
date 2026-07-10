package dev.allium.tutor

import dev.allium.tutor.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

private const val TEMPORAL_OVERDUE_RESOURCE = "files/lessons/temporal-overdue.json"

class PackageLessonEvaluator(
    private val requirements: LessonEvaluationRequirements,
) : LessonEvaluator {
    override fun evaluateRepair(lesson: LessonContent, answer: String): LessonEvaluation =
        evaluate(answer, requirements.repair)

    override fun evaluateTransfer(lesson: LessonContent, answer: String): LessonEvaluation =
        evaluate(answer, requirements.transfer)

    private fun evaluate(answer: String, evaluation: EvaluationRequirements): LessonEvaluation {
        val normalized = answer.normalizedForEvaluation()
        val passed = evaluation.requiredFragments.all { requirement ->
            normalized.contains(requirement.text.normalizedForEvaluation())
        }
        return LessonEvaluation(
            passed = passed,
            feedback = if (passed) null else evaluation.failureFeedback,
        )
    }
}

data class TutorApplication(
    val packageContent: LessonPackage,
    val engine: LessonEngine,
    val progressStore: LessonProgressStore,
) {
    val lesson: LessonContent
        get() = packageContent.lesson

    fun initialState(): TutorState = TutorState(lesson = lesson)

    suspend fun restoreState(): TutorState {
        val snapshot = progressStore.read(lesson.id) ?: return initialState()
        return ProgressCodec.decode(snapshot, packageContent)
    }

    suspend fun saveState(state: TutorState) {
        progressStore.write(
            lessonId = lesson.id,
            snapshot = ProgressCodec.encode(packageContent, state),
        )
    }

    suspend fun reduceDurably(state: TutorState, action: LessonAction): TutorState {
        val candidate = engine.reduce(state, action)
        if (candidate == state) return state
        saveState(candidate)
        return candidate
    }
}

@OptIn(ExperimentalResourceApi::class)
suspend fun loadTemporalOverduePackage(): LessonPackage =
    LessonPackageCodec.decode(
        Res.readBytes(TEMPORAL_OVERDUE_RESOURCE).decodeToString(),
    )

fun createTutorApplication(
    packageContent: LessonPackage,
    progressStore: LessonProgressStore,
): TutorApplication = TutorApplication(
    packageContent = packageContent,
    engine = LessonEngine(PackageLessonEvaluator(packageContent.evaluation)),
    progressStore = progressStore,
)
