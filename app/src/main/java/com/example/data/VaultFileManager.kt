package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.model.VaultFileItem
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class VaultFileManager(private val context: Context) {

    companion object {
        const val STREAM_BUFFER_SIZE = 128 * 1024 // 128 KB high-speed streaming buffer
        private val SAFE_SEGMENT_REGEX = Regex("[^A-Za-z0-9._\\- ()\\[\\]+]")
        private const val TEMP_PREFIX = ".decklink_part_"
    }

    private val vaultDir: File
        get() = File(context.filesDir, "shared_vault").apply {
            if (!exists()) mkdirs()
        }

    fun freeSpaceBytes(): Long = runCatching { vaultDir.usableSpace }.getOrDefault(0L)

    fun sanitizeSegment(rawName: String, fallbackPrefix: String = "file"): String {
        val base = rawName.substringAfterLast('/').substringAfterLast('\\')
            .replace("..", "_")
            .replace(SAFE_SEGMENT_REGEX, "_")
            .trim()
            .trimStart('.')
            .take(140)
        return if (base.isBlank() || base == "." || base == "..") {
            "${fallbackPrefix}_${System.currentTimeMillis()}"
        } else {
            base
        }
    }

    fun normalizeRelativePath(rawPath: String): String {
        if (rawPath.isBlank()) return ""
        val parts = rawPath.replace('\\', '/')
            .split('/')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "." && it != ".." }
            .map { sanitizeSegment(it, "dir") }
        return parts.joinToString("/")
    }

    /**
     * Resolves a directory strictly inside the secure Vault root.
     * Prevents any symlink or "../" path traversal escape.
     */
    fun resolveSafeDirectory(relativeDir: String): File? {
        val rootCanonical = vaultDir.canonicalFile
        val normalized = normalizeRelativePath(relativeDir)
        if (normalized.isEmpty()) return rootCanonical
        val candidate = File(rootCanonical, normalized).canonicalFile
        val prefix = rootCanonical.path + File.separator
        return if (candidate.path.startsWith(prefix)) candidate else null
    }

    /**
     * Resolves a file or subfolder strictly inside the secure Vault root (never the root itself).
     */
    fun resolveSafeEntry(relativePath: String): File? {
        val rootCanonical = vaultDir.canonicalFile
        val normalized = normalizeRelativePath(relativePath)
        if (normalized.isEmpty()) return null
        val candidate = File(rootCanonical, normalized).canonicalFile
        val prefix = rootCanonical.path + File.separator
        return if (candidate.path.startsWith(prefix)) candidate else null
    }

    fun resolveSafeFile(fileName: String): File? = resolveSafeEntry(fileName)

    fun listEntries(relativeDir: String = ""): List<VaultFileItem> {
        val dir = resolveSafeDirectory(relativeDir) ?: return emptyList()
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        val normDir = normalizeRelativePath(relativeDir)
        val raw = dir.listFiles() ?: return emptyList()
        return raw
            .filter { !it.name.startsWith(TEMP_PREFIX) }
            .sortedWith(
                compareByDescending<File> { it.isDirectory }
                    .thenBy { if (it.isDirectory) it.name.lowercase() else "" }
                    .thenByDescending { if (it.isDirectory) 0L else it.lastModified() }
            )
            .map { entry ->
                val rel = if (normDir.isEmpty()) entry.name else "$normDir/${entry.name}"
                VaultFileItem(
                    name = entry.name,
                    sizeBytes = if (entry.isDirectory) {
                        entry.listFiles()?.count { !it.name.startsWith(TEMP_PREFIX) }?.toLong() ?: 0L
                    } else {
                        entry.length()
                    },
                    lastModifiedMs = entry.lastModified(),
                    mimeType = if (entry.isDirectory) "inode/directory" else guessMimeType(entry.name),
                    relativePath = rel,
                    isDirectory = entry.isDirectory
                )
            }
    }

    fun listFiles(): List<VaultFileItem> {
        val results = mutableListOf<VaultFileItem>()
        collectFilesRecursive(vaultDir.canonicalFile, "", results)
        return results.sortedByDescending { it.lastModifiedMs }
    }

    private fun collectFilesRecursive(dir: File, prefix: String, out: MutableList<VaultFileItem>) {
        val children = dir.listFiles() ?: return
        for (child in children) {
            if (child.name.startsWith(TEMP_PREFIX)) continue
            val rel = if (prefix.isEmpty()) child.name else "$prefix/${child.name}"
            if (child.isDirectory) {
                collectFilesRecursive(child, rel, out)
            } else if (child.isFile) {
                out.add(
                    VaultFileItem(
                        name = child.name,
                        sizeBytes = child.length(),
                        lastModifiedMs = child.lastModified(),
                        mimeType = guessMimeType(child.name),
                        relativePath = rel,
                        isDirectory = false
                    )
                )
            }
        }
    }

    fun createFolder(relativeDir: String, rawFolderName: String): Result<VaultFileItem> {
        val parent = resolveSafeDirectory(relativeDir)
            ?: return Result.failure(SecurityException("Invalid parent folder"))
        if (!parent.exists()) parent.mkdirs()
        val safeName = sanitizeSegment(rawFolderName, "folder")
        val target = File(parent, safeName).canonicalFile
        val rootPrefix = vaultDir.canonicalFile.path + File.separator
        if (!target.path.startsWith(rootPrefix)) {
            return Result.failure(SecurityException("Invalid folder path"))
        }
        return runCatching {
            if (!target.exists() && !target.mkdirs()) {
                throw IllegalStateException("Could not create folder $safeName")
            }
            val normDir = normalizeRelativePath(relativeDir)
            val rel = if (normDir.isEmpty()) target.name else "$normDir/${target.name}"
            VaultFileItem(
                name = target.name,
                sizeBytes = 0L,
                lastModifiedMs = target.lastModified(),
                mimeType = "inode/directory",
                relativePath = rel,
                isDirectory = true
            )
        }
    }

    /**
     * Unlimited-size, ultra-fast streaming upload with 128 KB buffer and atomic temp rename.
     * Uses constant ~128 KB RAM even for multi-gigabyte files.
     */
    fun streamUpload(
        relativeDir: String,
        rawFileName: String,
        contentLength: Long,
        input: InputStream
    ): Result<VaultFileItem> {
        if (contentLength < 0L) {
            return Result.failure(IllegalArgumentException("Missing Content-Length header"))
        }
        val parent = resolveSafeDirectory(relativeDir)
            ?: return Result.failure(SecurityException("Invalid upload directory"))
        if (!parent.exists()) parent.mkdirs()

        val safeName = sanitizeSegment(rawFileName, "upload")
        val target = File(parent, safeName).canonicalFile
        val rootPrefix = vaultDir.canonicalFile.path + File.separator
        if (!target.path.startsWith(rootPrefix)) {
            return Result.failure(SecurityException("Invalid upload path"))
        }

        val tempFile = File(parent, "${TEMP_PREFIX}${System.nanoTime()}_${safeName}")
        return runCatching {
            val buffer = ByteArray(STREAM_BUFFER_SIZE)
            var remaining = contentLength
            BufferedOutputStream(FileOutputStream(tempFile), STREAM_BUFFER_SIZE).use { out ->
                while (remaining > 0L) {
                    val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                    val read = input.read(buffer, 0, toRead)
                    if (read <= 0) {
                        throw IllegalStateException("Upload interrupted before completion")
                    }
                    out.write(buffer, 0, read)
                    remaining -= read.toLong()
                }
                out.flush()
            }

            if (target.exists() && target.isFile) {
                target.delete()
            }
            if (!tempFile.renameTo(target)) {
                tempFile.copyTo(target, overwrite = true, bufferSize = STREAM_BUFFER_SIZE)
                tempFile.delete()
            }

            val normDir = normalizeRelativePath(relativeDir)
            val rel = if (normDir.isEmpty()) target.name else "$normDir/${target.name}"
            VaultFileItem(
                name = target.name,
                sizeBytes = target.length(),
                lastModifiedMs = target.lastModified(),
                mimeType = guessMimeType(target.name),
                relativePath = rel,
                isDirectory = false
            )
        }.onFailure {
            runCatching { if (tempFile.exists()) tempFile.delete() }
        }
    }

    /**
     * Unlimited-size streaming import from Android Photo Picker / Document Picker (SAF).
     */
    fun importFromUri(uri: Uri, relativeDir: String = ""): Result<VaultFileItem> {
        return runCatching {
            val resolvedName = queryDisplayName(uri) ?: "imported_${System.currentTimeMillis()}"
            val parent = resolveSafeDirectory(relativeDir)
                ?: throw SecurityException("Invalid directory")
            if (!parent.exists()) parent.mkdirs()
            val safeName = sanitizeSegment(resolvedName, "imported")
            val target = File(parent, safeName).canonicalFile
            val rootPrefix = vaultDir.canonicalFile.path + File.separator
            if (!target.path.startsWith(rootPrefix)) {
                throw SecurityException("Invalid target path")
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(STREAM_BUFFER_SIZE)
                BufferedOutputStream(FileOutputStream(target), STREAM_BUFFER_SIZE).use { out ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                    }
                    out.flush()
                }
            } ?: throw IllegalStateException("Could not open selected file")

            val normDir = normalizeRelativePath(relativeDir)
            val rel = if (normDir.isEmpty()) target.name else "$normDir/${target.name}"
            VaultFileItem(
                name = target.name,
                sizeBytes = target.length(),
                lastModifiedMs = target.lastModified(),
                mimeType = guessMimeType(target.name),
                relativePath = rel,
                isDirectory = false
            )
        }
    }

    fun deleteEntry(relativePath: String): Boolean {
        val target = resolveSafeEntry(relativePath) ?: return false
        if (!target.exists()) return false
        return if (target.isDirectory) target.deleteRecursively() else target.delete()
    }

    fun deleteFile(fileName: String): Boolean = deleteEntry(fileName)

    fun createShareIntent(relativePath: String): Intent? {
        val target = resolveSafeEntry(relativePath) ?: return null
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
