package com.tools.il2fusion.config

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mediates data access between the UI and HookConfigStore to keep logic centralized.
 */
class HookConfigRepository {

    /**
     * Loads stored RVA list and dump mode flag from the shared content provider.
     */
    suspend fun loadConfig(context: Context): HookConfigPayload = withContext(Dispatchers.IO) {
        val savedRvas = HookConfigStore.loadRvasForApp(context)
        val dumpMode = HookConfigStore.loadDumpModeForApp(context)
        HookConfigPayload(savedRvas, dumpMode)
    }

    /**
     * Persists the dump mode flag through the content provider.
     */
    suspend fun saveDumpMode(context: Context, enabled: Boolean) = withContext(Dispatchers.IO) {
        HookConfigStore.saveDumpMode(context, enabled)
    }

    /**
     * Persists the RVA list through the content provider.
     */
    suspend fun saveRvas(context: Context, rvas: List<Long>) = withContext(Dispatchers.IO) {
        HookConfigStore.saveRvas(context, rvas)
    }
}

/**
 * Represents stored configuration values used by both the UI and native hook.
 */
data class HookConfigPayload(
    val rvas: List<Long>,
    val dumpModeEnabled: Boolean
)
