package com.example.silversticker.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.silversticker.models.Sticker
import com.example.silversticker.models.StickerPack
import com.google.gson.Gson
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {
    private const val TAG = "ZipUtils"
    private const val PACK_JSON_ENTRY = "pack.json"
    private val gson = Gson()

    /**
     * Zip a sticker pack and return a shareable URI.
     */
    fun exportPack(context: Context, pack: StickerPack): Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "shared_packs")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            // Safe name for file
            val zipFile = File(cacheDir, "${pack.name.replace("\\s+".toRegex(), "_")}_sticker_pack.zip")
            if (zipFile.exists()) zipFile.delete()

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                val stickersDir = StickerStorage.getStickersDir(context)

                // 1. Write the metadata pack.json entry
                val metadataJson = gson.toJson(pack)
                val packEntry = ZipEntry(PACK_JSON_ENTRY)
                zos.putNextEntry(packEntry)
                zos.write(metadataJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 2. Write the tray image
                val trayFile = File(stickersDir, pack.trayImageFileName)
                if (trayFile.exists()) {
                    writeEntry(zos, trayFile, pack.trayImageFileName)
                }

                // 3. Write each sticker file
                for (sticker in pack.stickers) {
                    val stickerFile = File(stickersDir, sticker.imageFileName)
                    if (stickerFile.exists()) {
                        writeEntry(zos, stickerFile, sticker.imageFileName)
                    }
                }
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export pack: ${e.message}", e)
            null
        }
    }

    private fun writeEntry(zos: ZipOutputStream, file: File, entryName: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(1024 * 4)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                zos.write(buffer, 0, bytesRead)
            }
        }
        zos.closeEntry()
    }

    /**
     * Imports a zip file URI from file picker and registers it as a sticker pack.
     * Returns the imported StickerPack or null on failure.
     */
    fun importPack(context: Context, zipUri: Uri): StickerPack? {
        val stickersDir = StickerStorage.getStickersDir(context)
        if (!stickersDir.exists()) stickersDir.mkdirs()

        var importedPack: StickerPack? = null
        val tempFiles = mutableMapOf<String, File>()

        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(zipUri) ?: return null
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    if (entryName == PACK_JSON_ENTRY) {
                        val jsonBytes = zis.readBytes()
                        val jsonString = String(jsonBytes, Charsets.UTF_8)
                        importedPack = gson.fromJson(jsonString, StickerPack::class.java)
                    } else if (!entry.isDirectory) {
                        // Extract images to temporary location first, then move if JSON matches
                        val tempFile = File(context.cacheDir, "imported_${System.currentTimeMillis()}_$entryName")
                        FileOutputStream(tempFile).use { fos ->
                            val buffer = ByteArray(1024 * 4)
                            var bytesRead: Int
                            while (zis.read(buffer).also { bytesRead = it } != -1) {
                                fos.write(buffer, 0, bytesRead)
                            }
                        }
                        tempFiles[entryName] = tempFile
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val pack = importedPack ?: return null
            
            // To prevent identifier collision, we generate a fresh unique UUID and clean file names
            val newIdentifier = UUID.randomUUID().toString().replace("-", "").lowercase()
            val fileMap = mutableMapOf<String, String>() // old name to new name map

            // Copy tray file
            val oldTrayName = pack.trayImageFileName
            val newTrayName = "tray_${System.currentTimeMillis()}.webp"
            val tempTrayFile = tempFiles[oldTrayName] ?: return null
            val destTrayFile = File(stickersDir, newTrayName)
            tempTrayFile.copyTo(destTrayFile, overwrite = true)
            tempTrayFile.delete()
            fileMap[oldTrayName] = newTrayName

            // Copy sticker files
            val newStickers = mutableListOf<Sticker>()
            for (sticker in pack.stickers) {
                val oldStickerName = sticker.imageFileName
                val newStickerName = "sticker_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.webp"
                val tempStickerFile = tempFiles[oldStickerName]
                if (tempStickerFile != null && tempStickerFile.exists()) {
                    val destStickerFile = File(stickersDir, newStickerName)
                    tempStickerFile.copyTo(destStickerFile, overwrite = true)
                    tempStickerFile.delete()
                    newStickers.add(sticker.copy(imageFileName = newStickerName))
                }
            }

            // Clean up other temp files if any
            tempFiles.values.forEach { if (it.exists()) it.delete() }

            val finalPack = StickerPack(
                identifier = newIdentifier,
                name = pack.name,
                publisher = pack.publisher,
                trayImageFileName = newTrayName,
                stickers = newStickers
            )

            // Save to sticker storage
            StickerStorage.addPack(context, finalPack)
            Log.d(TAG, "Successfully imported pack: ${finalPack.name}")
            return finalPack
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import pack: ${e.message}", e)
            // Cleanup on error
            tempFiles.values.forEach { if (it.exists()) it.delete() }
            return null
        }
    }
}
