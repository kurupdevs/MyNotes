package com.kurupdevs.mynotes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class, LabelEntity::class, NoteFts::class, SyncMeta::class],
    version = 1,
    exportSchema = false
)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun labelDao(): LabelDao
    abstract fun ftsDao(): FtsDao
    abstract fun syncMetaDao(): SyncMetaDao
}
