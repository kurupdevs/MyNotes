package com.kurupdevs.mynotes.data.cloud

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UploadResult(val url: String, val publicId: String, val width: Int, val height: Int, val sizeBytes: Long)

class CloudinaryUploader(private val context: Context) {
    private val cloud = "owypwmrt"
    private val preset = "mynotes"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun uploadImage(src: File, uid: String, noteId: String): UploadResult =
        withContext(Dispatchers.IO) {
            val compressed = compressImage(src)
            upload(compressed, "image", uid, noteId)
        }

    suspend fun uploadAudio(src: File, uid: String, noteId: String): UploadResult =
        withContext(Dispatchers.IO) {
            upload(src, "video", uid, noteId) // Cloudinary treats audio as video resource type
        }

    private fun upload(file: File, resourceType: String, uid: String, noteId: String): UploadResult {
        val folder = "mynotes/$uid/$noteId"
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", preset)
            .addFormDataPart("folder", folder)
            .addFormDataPart(
                "file", file.name,
                file.asRequestBody(
                    (if (resourceType == "image") "image/webp" else "audio/mp4").toMediaType()
                )
            )
            .build()
        val req = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloud/$resourceType/upload")
            .post(body)
            .build()
        client.newCall(req).execute().use { resp ->
            val json = JSONObject(resp.body!!.string())
            if (!resp.isSuccessful) throw RuntimeException(json.optString("error", "upload failed"))
            return UploadResult(
                url = json.getString("secure_url"),
                publicId = json.getString("public_id"),
                width = json.optInt("width", 0),
                height = json.optInt("height", 0),
                sizeBytes = file.length()
            )
        }
    }

    suspend fun delete(publicId: String, resourceType: String = "image") {
        // Unsigned delete is not possible; assets are removed on Cloudinary dashboard
        // or via signed API. We keep public_ids so a future signed flow can purge.
    }

    private fun compressImage(src: File): File {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(src.absolutePath, opts)
        var sample = 1
        val maxEdge = 1600
        while (opts.outWidth / sample > maxEdge || opts.outHeight / sample > maxEdge) sample *= 2
        val decode = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = BitmapFactory.decodeFile(src.absolutePath, decode) ?: return src
        val out = File(context.cacheDir, "upload_${System.currentTimeMillis()}.webp")
        FileOutputStream(out).use { fos ->
            bmp.compress(Bitmap.CompressFormat.WEBP_LOSSY, 82, fos)
        }
        if (!bmp.isRecycled) bmp.recycle()
        return out
    }
}
