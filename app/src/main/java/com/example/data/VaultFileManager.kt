package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.model.VaultFileItem
import java.io.File
import java.io.FileOutputStream

class VaultFileManager(private val context: Context) {

    companion object {
        const val MAX_UPLOAD_BYTES = 25L * 1024L * 1024L // 25 MB per file
        private val SAFE_NAME_REGEX = Regex("[^A-Za-z0-9._\\- ]")
    }

    private val vaultDir: File
        get() = File(context.filesDir, "shared_vault").apply {
            if (!exists()) mkdirs()
        }

    fun sanitizeFileName(rawName: String): String {
        val base = rawName.substringAfterLast('/').substringAfterLast('\\')
            .replace("..", "_")
            .replace(SAFE_NAME_REGEX, "_")
            .trim()
            .take(120)
        return if (base.isBlank() || base == "." || base == "..") {
            "file_${System.currentTimeMillis()}.bin"
        } else {
            base
        }
    }

    fun resolveSafeFile(fileName: String): File? {
        val safeName = sanitizeFileName(fileName)
        val baseCanonical = vaultDir.canonicalFile
        val target = File(baseCanonical, safeName).canonicalFile
        val expectedPrefix = baseCanonical.path + File.separator
        return if (target.path.startsWith(expectedPrefix)) target else null
    }

    fun listFiles(): List<VaultFileItem> {
        val dir = vaultDir
        val files = dir.listFiles()?.filter { it.isFile } ?: return emptyList()
        return files
            .sortedByDescending { it.lastModified() }
            .map { file ->
                VaultFileItem(
                    name = file.name,
                    sizeBytes = file.length(),
                    lastModifiedMs = file.lastModified(),
                    mimeType = guessMimeType(file.name)
                )
            }
    }

    fun saveBytes(rawName: String, bytes: ByteArray): Result<VaultFileItem> {
        if (bytes.size.toLong() > MAX_UPLOAD_BYTES) {
            return Result.failure(IllegalArgumentException("File exceeds 25 MB limit"))
        }
        val target = resolveSafeFile(rawName)
            ?: return Result.failure(SecurityException("Invalid target file path"))
        return runCatching {
            FileOutputStream(target).use { out ->
                out.write(bytes)
                out.flush()
            }
            VaultFileItem(
                name = target.name,
                sizeBytes = target.length(),
                lastModifiedMs = target.lastModified(),
                mimeType = guessMimeType(target.name)
            )
        }
    }

    fun importFromUri(uri: Uri): Result<VaultFileItem> {
        return runCatching {
            val resolvedName = queryDisplayName(uri) ?: "imported_${System.currentTimeMillis()}"
            val target = resolveSafeFile(resolvedName)
                ?: throw SecurityException("Invalid file name")
            context.contentResolver.openInputStream(uri)?.use { input ->
                var totalRead = 0L
                val buffer = ByteArray(16 * 1024)
                FileOutputStream(target).use { out ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        totalRead += read
                        if (totalRead > MAX_UPLOAD_BYTES) {
                            target.delete()
                            throw IllegalArgumentException("Selected file exceeds 25 MB vault limit")
                        }
                        out.write(buffer, 0, read)
                    }
                    out.flush()
                }
            } ?: throw IllegalStateException("Could not open selected file")

            VaultFileItem(
                name = target.name,
                sizeBytes = target.length(),
                lastModifiedMs = target.lastModified(),
                mimeType = guessMimeType(target.name)
            )
        }
    }

    fun deleteFile(fileName: String): Boolean {
        val target = resolveSafeFile(fileName) ?: return false
        return target.exists() && target.isFile && target.delete()
    }

    fun createShareIntent(fileName: String): Intent? {
        val target = resolveSafeFile(fileName) ?: return null
        if (!target.exists() || !target.isFile) return null
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            target
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = guessMimeType(target.name)
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return "application/octet-stream"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: "application/octet-stream"
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull()
    }
}
