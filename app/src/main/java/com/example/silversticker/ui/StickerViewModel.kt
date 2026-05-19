package com.example.silversticker.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.silversticker.models.Sticker
import com.example.silversticker.models.StickerPack
import com.example.silversticker.utils.StickerStorage
import android.content.Context
import android.net.Uri
import com.example.silversticker.utils.ZipUtils
import com.example.silversticker.utils.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StickerViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "StickerViewModel"
    
    private val _stickerPacks = MutableStateFlow<List<StickerPack>>(emptyList())
    val stickerPacks: StateFlow<List<StickerPack>> = _stickerPacks.asStateFlow()

    private val _targetPackage = MutableStateFlow("com.whatsapp")
    val targetPackage: StateFlow<String> = _targetPackage.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized")
        StickerStorage.init(application)
        refreshPacks()
    }

    fun setTargetPackage(packageName: String) {
        _targetPackage.value = packageName
    }

    fun refreshPacks() {
        val packs = StickerStorage.getAllPacks()
        Log.d(TAG, "Refreshing packs, found: ${packs.size}")
        _stickerPacks.value = packs
    }

    fun addStickerPack(pack: StickerPack) {
        Log.d(TAG, "Adding pack: ${pack.name} (${pack.identifier})")
        StickerStorage.addPack(getApplication<Application>().applicationContext, pack)
        refreshPacks()
    }

    fun removeStickerPack(identifier: String) {
        Log.d(TAG, "Removing pack: $identifier")
        StickerStorage.removePack(getApplication<Application>().applicationContext, identifier)
        refreshPacks()
    }

    fun removeStickerPacks(identifiers: List<String>) {
        Log.d(TAG, "Removing multiple packs: $identifiers")
        val context = getApplication<Application>().applicationContext
        identifiers.forEach { identifier ->
            StickerStorage.removePack(context, identifier)
        }
        refreshPacks()
    }

    fun addStickersToPack(identifier: String, newStickers: List<Sticker>) {
        val pack = StickerStorage.getPack(identifier)
        if (pack != null) {
            val updatedPack = pack.copy(stickers = pack.stickers + newStickers)
            StickerStorage.addPack(getApplication<Application>().applicationContext, updatedPack)
            refreshPacks()
        }
    }

    fun removeStickersFromPack(packIdentifier: String, stickerFileNames: List<String>) {
        val pack = StickerStorage.getPack(packIdentifier)
        if (pack != null) {
            val updatedStickers = pack.stickers.filter { it.imageFileName !in stickerFileNames }
            val updatedPack = pack.copy(stickers = updatedStickers)
            
            val stickerDir = StickerStorage.getStickersDir(getApplication())
            stickerFileNames.forEach { stickerFileName ->
                val stickerFile = java.io.File(stickerDir, stickerFileName)
                if (stickerFile.exists()) stickerFile.delete()
                val gifFile = java.io.File(stickerDir, stickerFileName.replace(".webp", ".gif"))
                if (gifFile.exists()) gifFile.delete()
            }
            
            StickerStorage.addPack(getApplication<Application>().applicationContext, updatedPack)
            refreshPacks()
        }
    }

    fun importStickerPack(context: Context, uri: Uri): StickerPack? {
        val pack = ZipUtils.importPack(context, uri)
        if (pack != null) {
            refreshPacks()
        }
        return pack
    }

    fun exportStickerPack(context: Context, pack: StickerPack): Uri? {
        return ZipUtils.exportPack(context, pack)
    }

    fun updatePackDetails(packIdentifier: String, name: String, publisher: String, newTrayUri: Uri?) {
        val pack = StickerStorage.getPack(packIdentifier)
        if (pack != null) {
            val updatedPack = pack.copy(name = name, publisher = publisher)
            
            if (newTrayUri != null) {
                val context = getApplication<Application>().applicationContext
                val trayFile = pack.getTrayInternalFile(context)
                val tempFile = ImageUtils.convertToWebPFile(context, newTrayUri, 96, "tray_temp_${System.currentTimeMillis()}")
                if (tempFile != null && tempFile.exists()) {
                    try {
                        tempFile.inputStream().use { input ->
                            trayFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to copy new tray file: ${e.message}", e)
                    } finally {
                        tempFile.delete()
                    }
                }
            }
            
            StickerStorage.addPack(getApplication<Application>().applicationContext, updatedPack)
            refreshPacks()
        }
    }

    fun updateStickerEmojis(packIdentifier: String, stickerFileName: String, emojis: List<String>) {
        val pack = StickerStorage.getPack(packIdentifier)
        if (pack != null) {
            val updatedStickers = pack.stickers.map {
                if (it.imageFileName == stickerFileName) it.copy(emojis = emojis) else it
            }
            val updatedPack = pack.copy(stickers = updatedStickers)
            StickerStorage.addPack(getApplication<Application>().applicationContext, updatedPack)
            refreshPacks()
        }
    }
}
