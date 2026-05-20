package com.example.silversticker.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.silversticker.models.StickerPack
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object StickerStorage {
    private const val TAG = "StickerStorage"
    private const val STORAGE_FILE = "sticker_packs.json"
    private const val STICKERS_DIR = "stickers"
    private val gson = Gson()
    private var packs = mutableMapOf<String, StickerPack>()
    private var isInitialized = false

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        
        val file = File(context.filesDir, STORAGE_FILE)
        if (file.exists()) {
            try {
                val json = file.readText()
                Log.d(TAG, "Loading JSON: $json")
                val type = object : TypeToken<Map<String, StickerPack>>() {}.type
                val loaded: Map<String, StickerPack>? = gson.fromJson(json, type)
                if (loaded != null) {
                    packs.clear()
                    packs.putAll(loaded)
                    Log.d(TAG, "Successfully loaded ${packs.size} packs")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load sticker packs", e)
                // If corrupted, we might want to start fresh or keep empty
            }
        } else {
            Log.d(TAG, "Storage file not found, starting with empty list")
        }
        
        getStickersDir(context).mkdirs()
        isInitialized = true
    }

    fun getStickersDir(context: Context): File {
        return File(context.filesDir, STICKERS_DIR)
    }

    @Synchronized
    private fun save(context: Context) {
        try {
            val file = File(context.filesDir, STORAGE_FILE)
            val json = gson.toJson(packs)
            file.writeText(json)
            Log.d(TAG, "Saved ${packs.size} packs to ${file.absolutePath}")
            
            // Notify WhatsApp and the system that sticker metadata has been updated
            val authority = "${context.packageName}.sticker_provider"
            context.contentResolver.notifyChange(Uri.parse("content://$authority/metadata"), null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save sticker packs", e)
        }
    }

    @Synchronized
    fun addPack(context: Context, pack: StickerPack) {
        init(context) // Ensure loaded
        packs[pack.identifier] = pack
        save(context)
    }

    @Synchronized
    fun removePack(context: Context, identifier: String) {
        init(context)
        val pack = packs[identifier]
        if (pack != null) {
            val dir = getStickersDir(context)
            File(dir, pack.trayImageFileName).delete()
            pack.stickers.forEach { 
                File(dir, it.imageFileName).delete() 
                File(dir, it.imageFileName.replace(".webp", ".gif")).delete()
            }
            packs.remove(identifier)
            save(context)
        }
    }

    @Synchronized
    fun getPack(identifier: String): StickerPack? {
        // Note: This assumes init was called. If not, it returns from empty map.
        return packs[identifier]
    }

    @Synchronized
    fun getAllPacks(): List<StickerPack> {
        return packs.values.toList().sortedByDescending { it.name }
    }
}
