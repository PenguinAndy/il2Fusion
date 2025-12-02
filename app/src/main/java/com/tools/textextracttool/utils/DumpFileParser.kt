package com.tools.textextracttool.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Handles extracting RVA values from Il2Cpp dumper output files.
 */
class DumpFileParser {
    /**
     * Reads a C# dump file and returns RVA strings found above set_Text methods.
     */
    suspend fun extractRvas(context: Context, uri: Uri, maxCount: Int): List<String> = withContext(Dispatchers.IO) {
        val setTextPattern = Regex("""set_Text\s*\(""", RegexOption.IGNORE_CASE)
        val rvaPattern = Regex("""RVA:\s*(0x[0-9a-fA-F]+|\d+)""")
        val resolver = context.contentResolver
        val input = resolver.openInputStream(uri) ?: return@withContext emptyList<String>()
        input.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                val lines = reader.readLines()
                val results = LinkedHashSet<String>()
                for (i in 0 until lines.size - 1) {
                    val comment = lines[i]
                    val next = lines[i + 1]
                    if (!setTextPattern.containsMatchIn(next)) continue
                    val match = rvaPattern.find(comment) ?: continue
                    val raw = match.groupValues.getOrNull(1) ?: continue
                    val parsed = RvaUtils.parseRva(raw) ?: continue
                    results.add(RvaUtils.formatRva(parsed))
                    if (results.size >= maxCount) break
                }
                return@withContext results.toList()
            }
        }
    }
}
