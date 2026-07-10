package dev.allium.tutor

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val CURRENT_LESSON_PACKAGE_VERSION = 1

@Serializable
data class LessonPackage(
    val formatVersion: Int,
    val revision: Int,
    val lesson: LessonContent,
    val evaluation: LessonEvaluationRequirements,
)

@Serializable
data class LessonEvaluationRequirements(
    val repair: EvaluationRequirements,
    val transfer: EvaluationRequirements,
)

@Serializable
data class EvaluationRequirements(
    val requiredFragments: List<RequiredFragment>,
    val failureFeedback: String,
)

@Serializable
data class RequiredFragment(
    val id: String,
    val text: String,
)

data class LessonPackageIssue(
    val path: String,
    val message: String,
)

class InvalidLessonPackageException(
    val issues: List<LessonPackageIssue>,
) : IllegalArgumentException(
    issues.joinToString(
        prefix = "Invalid lesson package:\n",
        separator = "\n",
    ) { "- ${it.path}: ${it.message}" },
)

object LessonPackageCodec {
    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
    }

    fun decode(source: String): LessonPackage =
        json.decodeFromString<LessonPackage>(source).validated()
}

fun LessonPackage.validated(): LessonPackage {
    val issues = validate()
    if (issues.isNotEmpty()) throw InvalidLessonPackageException(issues)
    return this
}

fun LessonPackage.validate(): List<LessonPackageIssue> = buildList {
    fun requireText(path: String, value: String) {
        if (value.isBlank()) add(LessonPackageIssue(path, "must not be blank"))
    }

    fun requireAuthoredText(path: String, value: AuthoredText) {
        if (value.paragraphs.isEmpty()) {
            add(LessonPackageIssue("$path.paragraphs", "must contain at least one paragraph"))
        }
        value.paragraphs.forEachIndexed { index, paragraph ->
            requireText("$path.paragraphs[$index]", paragraph)
        }
    }

    fun requireEvaluation(path: String, value: EvaluationRequirements) {
        requireText("$path.failureFeedback", value.failureFeedback)
        if (value.requiredFragments.isEmpty()) {
            add(LessonPackageIssue("$path.requiredFragments", "must contain at least one requirement"))
        }

        val ids = mutableSetOf<String>()
        value.requiredFragments.forEachIndexed { index, fragment ->
            val fragmentPath = "$path.requiredFragments[$index]"
            requireText("$fragmentPath.id", fragment.id)
            requireText("$fragmentPath.text", fragment.text)
            if (fragment.id.isNotBlank() && !ids.add(fragment.id)) {
                add(LessonPackageIssue("$fragmentPath.id", "must be unique within the evaluation"))
            }
        }
    }

    if (formatVersion != CURRENT_LESSON_PACKAGE_VERSION) {
        add(
            LessonPackageIssue(
                "formatVersion",
                "expected $CURRENT_LESSON_PACKAGE_VERSION but found $formatVersion",
            ),
        )
    }
    if (revision < 1) {
        add(LessonPackageIssue("revision", "must be at least 1"))
    }

    requireText("lesson.id", lesson.id)
    if (lesson.id.isNotBlank() && !lesson.id.matches(Regex("[a-z0-9]+(?:-[a-z0-9]+)*"))) {
        add(LessonPackageIssue("lesson.id", "must use lowercase kebab-case"))
    }
    requireText("lesson.title", lesson.title)

    requireAuthoredText("lesson.incident.body", lesson.incident.body)
    requireText("lesson.incident.actionLabel", lesson.incident.actionLabel)

    requireText("lesson.prediction.prompt", lesson.prediction.prompt)
    requireText("lesson.prediction.placeholder", lesson.prediction.placeholder)
    requireText("lesson.prediction.actionLabel", lesson.prediction.actionLabel)
    requireText("lesson.prediction.emptyFeedback", lesson.prediction.emptyFeedback)

    requireAuthoredText("lesson.evidence.body", lesson.evidence.body)
    requireText("lesson.evidence.predictionLabel", lesson.evidence.predictionLabel)
    requireText("lesson.evidence.actionLabel", lesson.evidence.actionLabel)

    requireText("lesson.concept.name", lesson.concept.name)
    requireAuthoredText("lesson.concept.explanation", lesson.concept.explanation)
    requireText("lesson.concept.actionLabel", lesson.concept.actionLabel)

    requireText("lesson.workbench.prompt", lesson.workbench.prompt)
    requireText("lesson.workbench.starter", lesson.workbench.starter)
    requireText("lesson.workbench.editorLabel", lesson.workbench.editorLabel)
    requireText("lesson.workbench.actionLabel", lesson.workbench.actionLabel)
    requireText("lesson.workbench.emptyFeedback", lesson.workbench.emptyFeedback)
    requireText("lesson.workbench.verificationMessage", lesson.workbench.verificationMessage)
    if (!lesson.workbench.starter.contains("rule ") || !lesson.workbench.starter.contains("when:")) {
        add(
            LessonPackageIssue(
                "lesson.workbench.starter",
                "must contain an Allium rule with a trigger",
            ),
        )
    }
    if (!lesson.workbench.starter.hasBalancedBraces()) {
        add(LessonPackageIssue("lesson.workbench.starter", "must have balanced braces"))
    }

    requireAuthoredText("lesson.consequence.body", lesson.consequence.body)
    requireText("lesson.consequence.actionLabel", lesson.consequence.actionLabel)

    requireAuthoredText("lesson.transfer.prompt", lesson.transfer.prompt)
    requireText("lesson.transfer.placeholder", lesson.transfer.placeholder)
    requireText("lesson.transfer.editorLabel", lesson.transfer.editorLabel)
    requireText("lesson.transfer.actionLabel", lesson.transfer.actionLabel)
    requireText("lesson.transfer.emptyFeedback", lesson.transfer.emptyFeedback)
    requireText("lesson.transfer.verificationMessage", lesson.transfer.verificationMessage)

    requireText("lesson.completion.headline", lesson.completion.headline)
    requireAuthoredText("lesson.completion.body", lesson.completion.body)
    requireText("lesson.completion.status", lesson.completion.status)

    requireEvaluation("evaluation.repair", evaluation.repair)
    requireEvaluation("evaluation.transfer", evaluation.transfer)

    if (
        evaluation.repair.requiredFragments.isNotEmpty() &&
        evaluation.repair.requiredFragments.all { lesson.workbench.starter.containsNormalized(it.text) }
    ) {
        add(
            LessonPackageIssue(
                "lesson.workbench.starter",
                "must not already satisfy every repair requirement",
            ),
        )
    }
}

private fun String.containsNormalized(fragment: String): Boolean =
    normalizedForEvaluation().contains(fragment.normalizedForEvaluation())

internal fun String.normalizedForEvaluation(): String =
    lowercase().replace(Regex("\\s+"), " ").trim()

private fun String.hasBalancedBraces(): Boolean {
    var depth = 0
    for (character in this) {
        when (character) {
            '{' -> depth += 1
            '}' -> {
                depth -= 1
                if (depth < 0) return false
            }
        }
    }
    return depth == 0
}
