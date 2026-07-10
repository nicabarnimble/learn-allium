package dev.allium.tutor

import platform.Foundation.NSUserDefaults

internal class IosLessonProgressStore : LessonProgressStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun read(lessonId: String): String? =
        defaults.stringForKey(lessonId.storageKey())

    override suspend fun write(lessonId: String, snapshot: String) {
        defaults.setObject(snapshot, forKey = lessonId.storageKey())
    }

    private fun String.storageKey(): String = "lesson-progress.$this"
}
