package com.kurupdevs.mynotes.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE uid = :uid AND deletedAt IS NULL AND archived = 0 ORDER BY pinned DESC, pinnedAt DESC, updatedAt DESC")
    fun observeHome(uid: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE uid = :uid AND deletedAt IS NULL AND archived = 0 AND favorite = 1 ORDER BY pinned DESC, updatedAt DESC")
    fun observeFavorites(uid: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE uid = :uid AND deletedAt IS NULL AND archived = 1 ORDER BY updatedAt DESC")
    fun observeArchived(uid: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE uid = :uid AND deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(uid: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE uid = :uid AND deletedAt IS NULL AND reminderAt IS NOT NULL AND reminderDone = 0 ORDER BY reminderAt ASC")
    fun observeReminders(uid: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE uid = :uid AND deletedAt IS NULL AND reminderAt IS NOT NULL ORDER BY reminderAt ASC")
    fun observeAllReminders(uid: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id AND uid = :uid LIMIT 1")
    fun observeById(id: String, uid: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id AND uid = :uid LIMIT 1")
    suspend fun getById(id: String, uid: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE uid = :uid AND syncStatus != 'SYNCED'")
    suspend fun getPending(uid: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE uid = :uid AND deletedAt IS NOT NULL AND deletedAt < :olderThan")
    suspend fun getExpiredTrash(uid: String, olderThan: Long): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE uid = :uid AND deletedAt IS NULL AND archived = 0")
    suspend fun getLive(uid: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE uid = :uid AND deletedAt IS NULL")
    suspend fun getAllVisible(uid: String): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id AND uid = :uid")
    suspend fun deleteHard(id: String, uid: String)

    @Query("SELECT COUNT(*) FROM notes WHERE uid = :uid AND deletedAt IS NULL AND archived = 0")
    suspend fun countLive(uid: String): Int
}

@Dao
interface LabelDao {
    @Query("SELECT * FROM labels WHERE uid = :uid ORDER BY name ASC")
    fun observe(uid: String): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels WHERE uid = :uid ORDER BY name ASC")
    suspend fun getAll(uid: String): List<LabelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(label: LabelEntity)

    @Query("DELETE FROM labels WHERE id = :id AND uid = :uid")
    suspend fun delete(id: String, uid: String)

    @Query("SELECT COUNT(*) FROM labels WHERE uid = :uid")
    suspend fun count(uid: String): Int
}

@Dao
interface FtsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: NoteFts)

    @Query("DELETE FROM note_fts WHERE noteId = :noteId")
    suspend fun delete(noteId: String)

    @Query("SELECT noteId FROM note_fts WHERE note_fts MATCH :query LIMIT 200")
    suspend fun searchNoteIds(query: String): List<String>
}

@Dao
interface SyncMetaDao {
    @Query("SELECT value FROM sync_meta WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(meta: SyncMeta)
}
