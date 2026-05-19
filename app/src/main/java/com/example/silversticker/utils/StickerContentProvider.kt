package com.example.silversticker.utils

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileNotFoundException

class StickerContentProvider : ContentProvider() {
    companion object {
        private const val TAG = "StickerProvider"
        
        private const val METADATA = 1
        private const val METADATA_ID = 2
        private const val STICKERS = 3
        private const val STICKERS_ASSET = 4

        private val MATCHER = UriMatcher(UriMatcher.NO_MATCH)

        // WhatsApp Standard Column Names - Exact match required
        private const val STICKER_PACK_IDENTIFIER = "sticker_pack_identifier"
        private const val STICKER_PACK_NAME = "sticker_pack_name"
        private const val STICKER_PACK_PUBLISHER = "sticker_pack_publisher"
        private const val STICKER_PACK_ICON = "sticker_pack_icon"
        private const val ANDROID_PLAY_STORE_LINK = "android_play_store_link"
        private const val IOS_APP_STORE_LINK = "ios_app_store_link"
        private const val PUBLISHER_EMAIL = "sticker_pack_publisher_email"
        private const val PUBLISHER_WEBSITE = "sticker_pack_publisher_website"
        private const val PRIVACY_POLICY_WEBSITE = "sticker_pack_privacy_policy_website"
        private const val LICENSE_AGREEMENT_WEBSITE = "sticker_pack_license_agreement_website"
        private const val IMAGE_DATA_VERSION = "image_data_version"
        private const val AVOID_CACHE = "avoid_cache"
        private const val ANIMATED_STICKER_PACK = "animated_sticker_pack"

        private const val STICKER_FILE_NAME = "sticker_file_name"
        private const val STICKER_EMOJI = "sticker_emoji"
    }

    override fun onCreate(): Boolean {
        val authority = "${context?.packageName}.sticker_provider"
        
        MATCHER.addURI(authority, "metadata", METADATA)
        MATCHER.addURI(authority, "metadata/*", METADATA_ID)
        MATCHER.addURI(authority, "stickers/*", STICKERS)
        MATCHER.addURI(authority, "*", STICKERS_ASSET)
        
        context?.let { StickerStorage.init(it) }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val match = MATCHER.match(uri)
        Log.d(TAG, "Query received. URI: $uri, Match: $match")
        
        return when (match) {
            METADATA -> getAllPacksCursor()
            METADATA_ID -> getPackCursor(uri.lastPathSegment ?: "")
            STICKERS -> getStickersCursor(uri.lastPathSegment ?: "")
            else -> {
                Log.w(TAG, "No match for query URI: $uri")
                MatrixCursor(arrayOf("_id"))
            }
        }
    }

    private fun getAllPacksCursor(): Cursor {
        val cursor = MatrixCursor(arrayOf(
            STICKER_PACK_IDENTIFIER, STICKER_PACK_NAME, STICKER_PACK_PUBLISHER, STICKER_PACK_ICON,
            ANDROID_PLAY_STORE_LINK, IOS_APP_STORE_LINK, PUBLISHER_EMAIL, PUBLISHER_WEBSITE,
            PRIVACY_POLICY_WEBSITE, LICENSE_AGREEMENT_WEBSITE, IMAGE_DATA_VERSION, AVOID_CACHE, ANIMATED_STICKER_PACK
        ))
        
        StickerStorage.getAllPacks().forEach { pack ->
            val isAnimated = if (pack.stickers.any { it.isAnimated }) 1 else 0
            cursor.addRow(arrayOf<Any?>(
                pack.identifier,
                pack.name,
                pack.publisher,
                pack.trayImageFileName,
                "", "", "", "", "", "", "1", 0, isAnimated
            ))
        }
        return cursor
    }

    private fun getPackCursor(identifier: String): Cursor {
        val cursor = MatrixCursor(arrayOf(
            STICKER_PACK_IDENTIFIER, STICKER_PACK_NAME, STICKER_PACK_PUBLISHER, STICKER_PACK_ICON,
            ANDROID_PLAY_STORE_LINK, IOS_APP_STORE_LINK, PUBLISHER_EMAIL, PUBLISHER_WEBSITE,
            PRIVACY_POLICY_WEBSITE, LICENSE_AGREEMENT_WEBSITE, IMAGE_DATA_VERSION, AVOID_CACHE, ANIMATED_STICKER_PACK
        ))
        
        StickerStorage.getPack(identifier)?.let { pack ->
            val isAnimated = if (pack.stickers.any { it.isAnimated }) 1 else 0
            cursor.addRow(arrayOf<Any?>(
                pack.identifier,
                pack.name,
                pack.publisher,
                pack.trayImageFileName,
                "", "", "", "", "", "", "1", 0, isAnimated
            ))
        }
        return cursor
    }

    private fun getStickersCursor(identifier: String): Cursor {
        val cursor = MatrixCursor(arrayOf(STICKER_FILE_NAME, STICKER_EMOJI))
        val pack = StickerStorage.getPack(identifier)
        
        Log.d(TAG, "Getting stickers for pack: $identifier. Found: ${pack?.stickers?.size ?: 0}")
        
        pack?.stickers?.forEach { sticker ->
            val emojis = if (sticker.emojis.isNotEmpty()) sticker.emojis.joinToString(",") else "😊,🔥"
            cursor.addRow(arrayOf(sticker.imageFileName, emojis))
        }
        return cursor
    }

    override fun getType(uri: Uri): String? {
        val match = MATCHER.match(uri)
        return when (match) {
            METADATA -> "vnd.android.cursor.dir/vnd.com.whatsapp.sticker.pack"
            METADATA_ID -> "vnd.android.cursor.item/vnd.com.whatsapp.sticker.pack"
            STICKERS -> "vnd.android.cursor.dir/vnd.com.whatsapp.sticker.stickers"
            STICKERS_ASSET -> "image/webp"
            else -> null
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val fileName = uri.lastPathSegment ?: throw FileNotFoundException("No filename in URI")
        val file = File(StickerStorage.getStickersDir(context!!), fileName)
        
        Log.d(TAG, "openFile: Requested: $fileName, Path: ${file.absolutePath}, Exists: ${file.exists()}")
        
        if (!file.exists()) {
            throw FileNotFoundException("Sticker file not found: ${file.absolutePath}")
        }
        
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
