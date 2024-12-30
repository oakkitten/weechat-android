// Copyright (C) 2011 George Yunaev @ Ulduzsoft
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
package androidx.preference

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import com.ubergeek42.WeechatAndroid.R
import com.ubergeek42.WeechatAndroid.upload.Suri
import com.ubergeek42.WeechatAndroid.upload.resolver
import com.ubergeek42.WeechatAndroid.utils.getUris
import com.ubergeek42.WeechatAndroid.utils.saveUriToFile
import com.ubergeek42.WeechatAndroid.views.snackbar.showSnackbar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File


internal object FontManager {
    fun getFontSearchDirectories(context: Context): List<String> {
        val internalFontFolder = context.getExternalFilesDir(CUSTOM_FONTS_DIRECTORY)
        return if (internalFontFolder != null) {
            listOf("/system/fonts", internalFontFolder.toString())
        } else {
            listOf("/system/fonts")
        }
    }

    fun enumerateFonts(context: Activity): List<FontInfo> {
        val fonts = mutableListOf<FontInfo>()

        getFontSearchDirectories(context).forEach { directoryName ->
            val directory = File(directoryName)
            if (directory.exists()) {
                directory.listFiles()?.forEach { file ->
                    try {
                        val typeface = Typeface.createFromFile(file.absolutePath)
                        fonts.add(FontInfo(file.name, file.absolutePath, typeface))
                    } catch (e: Exception) {
                        context.showSnackbar(e)
                    }
                }
            }
        }

        return fonts
    }

    fun requestFontImport(activity: Activity) {
        activity.startActivityForResult(importFontsIntent, IMPORT_FONTS_REQUEST_CODE, null)
    }

    @JvmStatic fun importFontsFromResultIntent(context: Activity, intent: Intent?) {
        val imported = mutableListOf<String>()

        intent?.getUris()?.forEach { uri ->
            try {
                val mediaType = resolver.getType(uri)?.toMediaTypeOrNull()
                val fileName = Suri.makeFileNameWithExtension(uri, mediaType)
                val folder = context.getExternalFilesDir(CUSTOM_FONTS_DIRECTORY)
                val file = File(folder, fileName)

                try {
                    context.saveUriToFile(uri, file)
                } catch (e: Exception) {
                    file.delete()
                    throw e
                }

                // Typeface.createFromFile() does not validate the font,
                // so we are not doing any validation

                imported.add(fileName)
            } catch (e: Exception) {
                context.showSnackbar(e)
            }
        }

        if (imported.isNotEmpty()) {
            context.showSnackbar(context.getString(R.string.pref__FontPreference__imported,
                                 imported.joinToString(", ")))
        }
    }
}


internal class FontInfo (
    var name: String,
    var path: String,
    var typeface: Typeface
)


const val CUSTOM_FONTS_DIRECTORY = "fonts"
const val IMPORT_FONTS_REQUEST_CODE = 1234


private val importFontsIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
    type = "font/*"
    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
    addCategory(Intent.CATEGORY_OPENABLE)
    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
}