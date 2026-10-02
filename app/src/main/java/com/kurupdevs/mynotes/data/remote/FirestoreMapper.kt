package com.kurupdevs.mynotes.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.kurupdevs.mynotes.data.local.NoteEntity
import com.kurupdevs.mynotes.data.model.Attachment
import com.kurupdevs.mynotes.data.model.Block
import com.kurupdevs.mynotes.data.model.Collaborator
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object Jsons {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun blocks(s: String): List<Block> = runCatching {
        json.decodeFromString(ListSerializer(Block.serializer()), s)
    }.getOrDefault(emptyList())
    fun blocksToJson(b: List<Block>): String = json.encodeToString(ListSerializer(Block.serializer()), b)
    fun attachments(s: String): List<Attachment> = runCatching {
        json.decodeFromString(ListSerializer(Attachment.serializer()), s)
    }.getOrDefault(emptyList())
    fun attachmentsToJson(a: List<Attachment>): String =
        json.encodeToString(ListSerializer(Attachment.serializer()), a)
    fun labels(s: String): List<String> = runCatching {
        json.decodeFromString(ListSerializer(String.serializer()), s)
    }.getOrDefault(emptyList())
    fun labelsToJson(l: List<String>): String =
        json.encodeToString(ListSerializer(String.serializer()), l)
    fun collaborators(s: String): Map<String, Collaborator> = runCatching {
        json.decodeFromString(MapSerializer(String.serializer(), Collaborator.serializer()), s)
    }.getOrDefault(emptyMap())
    fun collaboratorsToJson(c: Map<String, Collaborator>): String =
        json.encodeToString(MapSerializer(String.serializer(), Collaborator.serializer()), c)
}

object FirestoreMapper {
    private fun ts(v: Any?): Long = (v as? Timestamp)?.toDate()?.time ?: (v as? Long) ?: 0L

    @Suppress("UNCHECKED_CAST")
    fun toEntity(uid: String, snap: DocumentSnapshot): NoteEntity? {
        val d = snap.data ?: return null
        val blocks = (d["blocks"] as? List<Map<String, Any?>>).orEmpty().mapIndexed { i, m ->
            mapOf(
                "id" to (m["id"] as? String ?: "b$i"),
                "kind" to (m["kind"] as? String ?: "p"),
                "text" to (m["text"] as? String ?: ""),
                "checked" to (m["checked"] as? Boolean ?: false),
                "attachmentId" to m["attachmentId"],
                "order" to ((m["order"] as? Long)?.toInt() ?: i)
            )
        }
        val atts = (d["attachments"] as? List<Map<String, Any?>>).orEmpty().mapIndexed { i, m ->
            mapOf(
                "id" to (m["id"] as? String ?: "a$i"),
                "kind" to (m["kind"] as? String ?: "image"),
                "url" to (m["url"] as? String ?: ""),
                "localPath" to "",
                "cloudinaryPublicId" to (m["cloudinaryPublicId"] as? String ?: ""),
                "mime" to (m["mime"] as? String ?: ""),
                "sizeBytes" to (m["sizeBytes"] as? Long ?: 0L),
                "width" to ((m["width"] as? Long)?.toInt() ?: 0),
                "height" to ((m["height"] as? Long)?.toInt() ?: 0),
                "durationMs" to (m["durationMs"] as? Long ?: 0L),
                "caption" to "",
                "ocrText" to ""
            )
        }
        val collabs = (d["collaborators"] as? Map<String, Map<String, Any?>>).orEmpty()
            .mapValues { (_, v) ->
                mapOf("role" to (v["role"] as? String ?: "viewer"), "addedAt" to ts(v["addedAt"]))
            }
        return NoteEntity(
            id = snap.id,
            uid = uid,
            ownerId = d["ownerId"] as? String ?: uid,
            title = d["title"] as? String ?: "",
            type = d["type"] as? String ?: "TEXT",
            blocksJson = Jsons.json.encodeToString(
                ListSerializer(com.kurupdevs.mynotes.data.model.Block.serializer()),
                blocks.map {
                    Block(
                        id = it["id"] as String, kind = runCatching {
                            com.kurupdevs.mynotes.data.model.BlockKind.valueOf((it["kind"] as String).uppercase())
                        }.getOrDefault(com.kurupdevs.mynotes.data.model.BlockKind.P),
                        text = it["text"] as String, checked = it["checked"] as Boolean,
                        attachmentId = it["attachmentId"] as? String, order = it["order"] as Int
                    )
                }
            ),
            color = d["color"] as? String ?: "default",
            pinned = d["pinned"] as? Boolean ?: false,
            pinnedAt = ts(d["pinnedAt"]),
            favorite = d["favorite"] as? Boolean ?: false,
            archived = d["archived"] as? Boolean ?: false,
            labelsJson = Jsons.labelsToJson((d["labels"] as? List<String>).orEmpty()),
            attachmentsJson = Jsons.json.encodeToString(
                ListSerializer(Attachment.serializer()),
                atts.map {
                    Attachment(
                        id = it["id"] as String, kind = it["kind"] as String, url = it["url"] as String,
                        cloudinaryPublicId = it["cloudinaryPublicId"] as String, mime = it["mime"] as String,
                        sizeBytes = it["sizeBytes"] as Long, width = it["width"] as Int,
                        height = it["height"] as Int, durationMs = it["durationMs"] as Long
                    )
                }
            ),
            reminderAt = (d["reminderAt"] as? Timestamp)?.toDate()?.time,
            collaboratorsJson = Jsons.json.encodeToString(
                MapSerializer(String.serializer(), Collaborator.serializer()),
                collabs.mapValues { (_, v) -> Collaborator(v["role"] as String, v["addedAt"] as Long) }
            ),
            createdAt = ts(d["createdAt"]),
            updatedAt = ts(d["updatedAt"]),
            updatedBy = d["updatedBy"] as? String ?: "",
            deletedAt = (d["deletedAt"] as? Timestamp)?.toDate()?.time,
            version = (d["version"] as? Long)?.toInt() ?: 1,
            syncStatus = "SYNCED"
        )
    }

    fun toMap(e: NoteEntity): Map<String, Any?> {
        val blocks = Jsons.blocks(e.blocksJson).map {
            mapOf(
                "id" to it.id, "kind" to it.kind.name.lowercase(), "text" to it.text,
                "checked" to it.checked, "attachmentId" to it.attachmentId, "order" to it.order
            )
        }
        val atts = Jsons.attachments(e.attachmentsJson).map {
            mapOf(
                "id" to it.id, "kind" to it.kind, "url" to it.url,
                "cloudinaryPublicId" to it.cloudinaryPublicId, "mime" to it.mime,
                "sizeBytes" to it.sizeBytes, "width" to it.width, "height" to it.height,
                "durationMs" to it.durationMs
            )
        }
        val collabs = Jsons.collaborators(e.collaboratorsJson).mapValues { (_, c) ->
            mapOf("role" to c.role, "addedAt" to Timestamp(c.addedAt / 1000, ((c.addedAt % 1000) * 1_000_000).toInt()))
        }
        fun t(ms: Long) = Timestamp(ms / 1000, ((ms % 1000) * 1_000_000).toInt())
        return mutableMapOf<String, Any?>(
            "title" to e.title,
            "type" to e.type,
            "blocks" to blocks,
            "color" to e.color,
            "pinned" to e.pinned,
            "pinnedAt" to t(e.pinnedAt),
            "favorite" to e.favorite,
            "archived" to e.archived,
            "labels" to Jsons.labels(e.labelsJson),
            "attachments" to atts,
            "collaborators" to collabs,
            "ownerId" to e.ownerId,
            "createdAt" to t(e.createdAt),
            "updatedAt" to t(e.updatedAt),
            "updatedBy" to e.updatedBy,
            "version" to e.version
        ).apply {
            e.reminderAt?.let { put("reminderAt", t(it)) }
            e.deletedAt?.let { put("deletedAt", t(it)) }
        }
    }
}
