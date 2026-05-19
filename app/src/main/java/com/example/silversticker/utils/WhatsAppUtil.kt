package com.example.silversticker.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.example.silversticker.models.StickerPack

object WhatsAppUtil {
    private const val TAG = "WhatsAppUtil"
    private const val EXTRA_STICKER_PACK_ID = "sticker_pack_id"
    private const val EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority"
    private const val EXTRA_STICKER_PACK_NAME = "sticker_pack_name"

    fun createAddStickerPackIntent(context: Context, pack: StickerPack, targetPackage: String): Intent? {
        val authority = "${context.packageName}.sticker_provider"
        
        val intent = Intent().apply {
            action = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
            `package` = targetPackage
            putExtra(EXTRA_STICKER_PACK_ID, pack.identifier)
            putExtra(EXTRA_STICKER_PACK_AUTHORITY, authority)
            putExtra(EXTRA_STICKER_PACK_NAME, pack.name)
            
            // Critical for Android 11+ (API 30+)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return try {
            // Verify if the target app is actually installed
            val packageManager = context.packageManager
            if (packageManager.getPackageInfo(targetPackage, PackageManager.GET_ACTIVITIES) != null) {
                intent
            } else {
                Log.e(TAG, "Target package $targetPackage not found")
                null
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "WhatsApp not installed: $targetPackage")
            null
        }
    }
}
