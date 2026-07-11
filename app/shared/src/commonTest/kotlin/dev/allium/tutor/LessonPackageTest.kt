package dev.allium.tutor

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LessonPackageTest {
    @Test
    fun bundledTemporalLessonDecodesAndCoversTheLifecycle() = runTest {
        val packageContent = loadTemporalOverduePackage()

        assertEquals(1, packageContent.revision)
        assertEquals("temporal-overdue", packageContent.lesson.id)
        assertTrue(packageContent.lesson.incident.body.render().contains("240"))
        assertEquals("Temporal guards", packageContent.lesson.concept.name)
        assertTrue(packageContent.lesson.workbench.starter.contains("when:"))
        assertTrue(packageContent.lesson.consequence.body.paragraphs.isNotEmpty())
        assertTrue(packageContent.lesson.transfer.prompt.paragraphs.isNotEmpty())
        assertTrue(packageContent.lesson.completion.headline.isNotBlank())
        assertEquals(4, packageContent.evaluation.repair.requiredFragments.size)
        assertTrue(packageContent.lesson.workbench.prompt.contains("notice event"))
        assertTrue(
            packageContent.evaluation.repair.requiredFragments.any { it.id == "notice-event" },
        )
        assertEquals(3, packageContent.evaluation.transfer.requiredFragments.size)
    }

    @Test
    fun temporalRepairRequiresTheNoticeEventPromisedByTheStory() = runTest {
        val packageContent = loadTemporalOverduePackage()
        val evaluator = PackageLessonEvaluator(packageContent.evaluation)
        val stateOnlyRepair = """
            rule MarkBookOverdue {
                when: _: Book.due_at <= now
                requires: book.status = borrowed
                ensures: book.status = overdue
            }
        """.trimIndent()
        val completeRepair = """
            rule MarkBookOverdue {
                when: _: Book.due_at <= now
                requires: book.status = borrowed
                ensures: book.status = overdue
                ensures: BookOverdue(book: book, borrower: book.borrowed_by)
            }
        """.trimIndent()

        assertFalse(evaluator.evaluateRepair(packageContent.lesson, stateOnlyRepair).passed)
        assertTrue(evaluator.evaluateRepair(packageContent.lesson, completeRepair).passed)
    }

    @Test
    fun packageDrivenEvaluatorUsesAuthoredRequirementsAndFeedback() {
        val packageContent = testLessonPackage()
        val evaluator = PackageLessonEvaluator(packageContent.evaluation)

        val failed = evaluator.evaluateRepair(packageContent.lesson, "rule Incomplete { }")
        assertFalse(failed.passed)
        assertEquals(packageContent.evaluation.repair.failureFeedback, failed.feedback)

        val passed = evaluator.evaluateRepair(
            packageContent.lesson,
            """
                when: _: Book.due_at <= now
                requires: book.status = borrowed
                ensures: book.status = overdue
            """.trimIndent(),
        )
        assertTrue(passed.passed)
        assertEquals(null, passed.feedback)
    }

    @Test
    fun validationRejectsMissingLifecycleCopy() {
        val packageContent = testLessonPackage().copy(
            lesson = testLessonPackage().lesson.copy(
                completion = testLessonPackage().lesson.completion.copy(headline = " "),
            ),
        )

        val error = assertFailsWith<InvalidLessonPackageException> {
            packageContent.validated()
        }

        assertTrue(error.issues.any { it.path == "lesson.completion.headline" })
    }

    @Test
    fun validationRejectsDuplicateRequirementIds() {
        val packageContent = testLessonPackage()
        val duplicate = packageContent.evaluation.repair.requiredFragments.first().copy()
        val invalid = packageContent.copy(
            evaluation = packageContent.evaluation.copy(
                repair = packageContent.evaluation.repair.copy(
                    requiredFragments = packageContent.evaluation.repair.requiredFragments + duplicate,
                ),
            ),
        )

        val error = assertFailsWith<InvalidLessonPackageException> {
            invalid.validated()
        }

        assertTrue(error.issues.any { it.message.contains("unique") })
    }

    @Test
    fun validationRejectsSolvedOrStructurallyBrokenStarter() {
        val packageContent = testLessonPackage()
        val solvedStarter = """
            rule AlreadySolved {
                when: _: Book.due_at <= now
                requires: book.status = borrowed
                ensures: book.status = overdue
            }
        """.trimIndent()
        val solved = packageContent.copy(
            lesson = packageContent.lesson.copy(
                workbench = packageContent.lesson.workbench.copy(starter = solvedStarter),
            ),
        )
        val broken = packageContent.copy(
            lesson = packageContent.lesson.copy(
                workbench = packageContent.lesson.workbench.copy(
                    starter = "rule Broken {\n    when: _: Book.due_at <= now",
                ),
            ),
        )

        val solvedError = assertFailsWith<InvalidLessonPackageException> { solved.validated() }
        val brokenError = assertFailsWith<InvalidLessonPackageException> { broken.validated() }

        assertTrue(solvedError.issues.any { it.message.contains("must not already satisfy") })
        assertTrue(brokenError.issues.any { it.message.contains("balanced braces") })
    }
}
