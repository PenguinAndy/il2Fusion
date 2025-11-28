package com.tools.textextracttool.config

import android.content.Context
import android.content.SharedPreferences
import com.tools.textextracttool.BuildConfig

object HookConfigStore {
    private const val PREF_NAME = "hook_config"
    private const val KEY_RVAS = "rvas"
    private const val KEY_ENABLED_PKGS = "enabled_packages"

    private fun prefs(ctx: Context): SharedPreferences {
        val moduleCtx = if (ctx.packageName == BuildConfig.APPLICATION_ID) {
            ctx.applicationContext
        } else {
            try {
                ctx.createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY)
            } catch (e: Throwable) {
                ctx.applicationContext
            }
        }
        return moduleCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveRvas(ctx: Context, rvas: List<Long>) {
        val text = rvas.joinToString(separator = ",")
        prefs(ctx).edit().putString(KEY_RVAS, text).apply()
    }

    fun loadRvas(ctx: Context): List<Long> {
        val raw = prefs(ctx).getString(KEY_RVAS, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(',')
            .mapNotNull {
                val trimmed = it.trim()
                if (trimmed.isEmpty()) null else trimmed.toLongOrNull()
            }
    }

    fun markHookedPackage(ctx: Context, pkg: String): Set<String> {
        val current = enabledPackages(ctx).toMutableSet()
        current.add(pkg)
        prefs(ctx).edit().putStringSet(KEY_ENABLED_PKGS, current).apply()
        return current
    }

    fun enabledPackages(ctx: Context): Set<String> {
        return prefs(ctx).getStringSet(KEY_ENABLED_PKGS, emptySet()) ?: emptySet()
    }
}
