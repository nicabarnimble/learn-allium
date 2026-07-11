package dev.allium.tutor

import java.util.prefs.Preferences

internal class DesktopLessonProgressStore : LessonProgressStore {
    private val preferences = Preferences.userRoot().node("dev/allium/tutor/progress")

    override suspend fun read(lessonId: String): String? =
        preferences.get(lessonId, null)

    override suspend fun write(lessonId: String, snapshot: String) {
        preferences.put(lessonId, snapshot)
        preferences.flush()
    }
}
