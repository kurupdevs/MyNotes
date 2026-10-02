package com.kurupdevs.mynotes.export

import android.content.Context
import com.kurupdevs.mynotes.data.local.NotesDatabase
import com.kurupdevs.mynotes.data.model.BlockKind
import com.kurupdevs.mynotes.data.remote.Jsons
import com.kurupdevs.mynotes.data.repo.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportManager(
    private val context: Context,
    private val db: NotesDatabase,
    private val auth: AuthRepository
) {
    private val json = Json { prettyPrint = true }

    suspend fun exportAllJson(): File = withContext(Dispatchers.IO) {
        val uid = auth.currentUid() ?: "local"
        val notes = db.noteDao().getAllVisible(uid)
        val arr = notes.map { n ->
            buildJsonObject {
                put("id", n.id)
                put("title", n.title)
                put("type", n.type)
                put("color", n.color)
                put("pinned", n.pinned)
                put("favorite", n.favorite)
                put("archived", n.archived)
                put("createdAt", n.createdAt)
                put("updatedAt", n.updatedAt)
                putJsonArray("blocks") {
                    Jsons.blocks(n.blocksJson).forEach { b ->
                        add(buildJsonObject {
                            put("kind", b.kind.name)
                            put("text", b.text)
                            put("checked", b.checked)
                        })
                    }
                }
                putJsonArray("labels") {
                    Jsons.labels(n.labelsJson).forEach { add(JsonPrimitive(it)) }
                }
            }
        }
        val dir = File(context.cacheDir, "export").apply { mkdirs() }
        val f = File(dir, "mynotes_export_${System.currentTimeMillis()}.json")
        f.writeText(json.encodeToString(ListSerializer(kotlinx.serialization.json.JsonObject.serializer()), arr))
        f
    }

    suspend fun exportAllTxtZip(): File = withContext(Dispatchers.IO) {
        val uid = auth.currentUid() ?: "local"
        val notes = db.noteDao().getAllVisible(uid)
        val dir = File(context.cacheDir, "export").apply { mkdirs() }
        val zip = File(dir, "mynotes_export_${System.currentTimeMillis()}.zip")
        ZipOutputStream(zip.outputStream()).use { zos ->
            notes.forEachIndexed { i, n ->
                val name = (n.title.ifEmpty { "note_${i + 1}" })
                    .replace(Regex("[^a-zA-Z0-9-_ ]"), "").take(60).ifEmpty { "note_${i + 1}" } + ".txt"
                zos.putNextEntry(ZipEntry(name))
                val sb = StringBuilder()
                if (n.title.isNotBlank()) sb.append(n.title).append("\n\n")
                Jsons.blocks(n.blocksJson).forEach { b ->
                    when (b.kind) {
                        BlockKind.TODO -> sb.append(if (b.checked) "[x] " else "[ ] ").append(b.text).append('\n')
                        BlockKind.H1 -> sb.append("# ").append(b.text).append('\n')
                        BlockKind.H2 -> sb.append("## ").append(b.text).append('\n')
                        BlockKind.LI -> sb.append("• ").append(b.text).append('\n')
                        else -> sb.append(b.text).append('\n')
                    }
                }
                zos.write(sb.toString().toByteArray())
                zos.closeEntry()
            }
        }
        zip
    }

    suspend fun storageBreakdown(): StorageInfo = withContext(Dispatchers.IO) {
        val uid = auth.currentUid() ?: "local"
        val notes = db.noteDao().getAllVisible(uid)
        val trash = db.noteDao().getExpiredTrash(uid, Long.MAX_VALUE)
        var images = 0L
        var audio = 0L
        (notes + trash).forEach { n ->
            Jsons.attachments(n.attachmentsJson).forEach { a ->
                when (a.kind) {
                    "image" -> images += a.sizeBytes
                    "audio" -> audio += a.sizeBytes
                }
            }
        }
        StorageInfo(
            noteCount = notes.size,
            imageBytes = images,
            audioBytes = audio,
            trashCount = trash.size
        )
    }

    data class StorageInfo(
        val noteCount: Int,
        val imageBytes: Long,
        val audioBytes: Long,
        val trashCount: Int
    )
}

fun formatBytes(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> "%.1f KB".format(b / 1024f)
    else -> "%.1f MB".format(b / 1048576f)
}
