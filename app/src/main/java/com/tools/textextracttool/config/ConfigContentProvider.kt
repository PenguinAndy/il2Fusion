package com.tools.textextracttool.config

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log

class ConfigContentProvider : ContentProvider() {

    companion object {
        private const val TAG = "[TextExtractTool]"
        private const val AUTHORITY = "com.tools.textextracttool.provider"
        private const val PATH_CONFIG = "config"
        const val KEY_RVAS = "rvas"
        const val KEY_DUMP_MODE = "dump_mode"
        private const val CODE_CONFIG = 1
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_CONFIG")

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, PATH_CONFIG, CODE_CONFIG)
        }
    }

    private val columns = arrayOf("key", "value")

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        if (uriMatcher.match(uri) != CODE_CONFIG) return null
        val prefs = context?.getSharedPreferences(PATH_CONFIG, android.content.Context.MODE_PRIVATE) ?: return null
        val all = prefs.getAll()
        val cursor = MatrixCursor(columns)
        all.forEach { (k, v) ->
            cursor.addRow(arrayOf(k, v?.toString() ?: ""))
        }
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (uriMatcher.match(uri) != CODE_CONFIG) return null
        val prefs = context?.getSharedPreferences(PATH_CONFIG, android.content.Context.MODE_PRIVATE) ?: return null
        val key = values?.getAsString("key") ?: KEY_RVAS
        val value = values?.getAsString("value") ?: ""
        prefs.edit().putString(key, value).apply()
        Log.i(TAG, "ConfigContentProvider insert key=$key")
        return Uri.withAppendedPath(CONTENT_URI, key)
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        if (uriMatcher.match(uri) != CODE_CONFIG) return 0
        val prefs = context?.getSharedPreferences(PATH_CONFIG, android.content.Context.MODE_PRIVATE) ?: return 0
        val key = values?.getAsString("key") ?: KEY_RVAS
        val value = values?.getAsString("value") ?: ""
        prefs.edit().putString(key, value).apply()
        Log.i(TAG, "ConfigContentProvider update key=$key")
        return 1
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun getType(uri: Uri): String? = null
}
