package com.kurupdevs.mynotes.ui.nav

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val EDITOR = "editor/{noteId}"
    const val SEARCH = "search"
    const val LABELS = "labels"
    const val REMINDERS = "reminders"
    const val TRASH = "trash"
    const val ARCHIVE = "archive"
    const val SETTINGS = "settings"
    const val SHARE = "share/{noteId}"

    fun editor(noteId: String) = "editor/$noteId"
    fun share(noteId: String) = "share/$noteId"
}
