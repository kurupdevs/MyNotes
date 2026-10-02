package com.kurupdevs.mynotes.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val uid: String,              // local account uid (owner of this device copy)
    val ownerId: String,          // Firestore owner uid (same as uid unless shared)
    val title: String = "",
    val type: String = "TEXT",
    val blocksJson: String = "[]",
    val color: String = "default",
    val pinned: Boolean = false,
    val pinnedAt: Long = 0,
    val favorite: Boolean = false,
    val archived: Boolean = false,
    val labelsJson: String = "[]",
    val attachmentsJson: String = "[]",
    val reminderAt: Long? = null,
    val reminderRepeat: String? = null, // null | "daily" | "weekly"
    val reminderDone: Boolean = false,
    val collaboratorsJson: String = "{}",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val updatedBy: String = "",
    val deletedAt: Long? = null,
    val version: Int = 1,
    val syncStatus: String = "SYNCED",
    val conflictJson: String? = null,   // remote blocks snapshot awaiting pick-one
    val locked: Boolean = false,
    val trashedWasPinned: Boolean = false,
    val trashedWasArchived: Boolean = false
)

@Entity(tableName = "labels")
data class LabelEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val name: String,
    val color: String = "coral",
    val createdAt: Long = 0,
    val syncStatus: String = "SYNCED"
)

@Fts4
@Entity(tableName = "note_fts")
data class NoteFts(
    @ColumnInfo(name = "noteId") val noteId: String,
    @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "content") val content: String
)

@Entity(tableName = "sync_meta")
data class SyncMeta(
    @PrimaryKey val key: String,
    val value: String = ""
)
