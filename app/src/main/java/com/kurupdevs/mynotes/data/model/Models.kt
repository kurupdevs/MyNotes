package com.kurupdevs.mynotes.data.model

import kotlinx.serialization.Serializable

enum class NoteType { TEXT, CHECKLIST, IMAGE, VOICE }
enum class SyncStatus { SYNCED, PENDING_UPLOAD, PENDING_DELETE, CONFLICT }
enum class SortOrder { EDITED, CREATED, TITLE, COLOR }
enum class BlockKind { P, H1, H2, LI, TODO, IMG, AUDIO, DRAW }

@Serializable
data class Block(
    val id: String,
    val kind: BlockKind,
    val text: String = "",
    val checked: Boolean = false,
    val attachmentId: String? = null,
    val order: Int = 0,
    val marks: List<String> = emptyList() // "bold" | "italic" | "underline" | "strike"
)

@Serializable
data class Attachment(
    val id: String,
    val kind: String, // "image" | "audio"
    val url: String = "",
    val localPath: String = "",
    val cloudinaryPublicId: String = "",
    val mime: String = "",
    val sizeBytes: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0,
    val caption: String = "",
    val ocrText: String = ""
)

@Serializable
data class Collaborator(val role: String, val addedAt: Long)

enum class NoteColor(val key: String) {
    DEFAULT("default"), CORAL("coral"), YELLOW("yellow"), CREAM("cream"),
    GREEN("green"), BLUE("blue"), PURPLE("purple");

    companion object {
        fun fromKey(key: String?): NoteColor = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
