package dev.retrotv.app.data.scanner

import java.io.InputStream

object DatParser {

    private val CRC_REGEX   = Regex("""crc\s+([0-9a-fA-F]{8})""")
    private val TAG_STRIP   = Regex("""[\(\[][^\)\]]*[\)\]]""")  // (tags) and [tags]
    private val PUNCT_STRIP = Regex("""[-.,!?'_]""")                  // - at start = literal hyphen
    private val LANG_SUFFIX = Regex("""\b(esp|usa|eur|jap|jpn|fra|ger|ita|por|ned|swe|chn|kor|bra|aus)\s*$""", RegexOption.IGNORE_CASE)
    private val SPACE_NORM  = Regex("""\s+""")
    private val PREF_ORDER  = listOf("world", "usa", "europe")

    data class ParseResult(
        val byCrc:  Map<String, String>,
        val byName: Map<String, String>,
    )

    fun parse(input: InputStream): ParseResult {
        val byCrc  = mutableMapOf<String, String>()
        val byName = mutableMapOf<String, MutableList<String>>()
        var inGame = false
        var current: String? = null

        input.bufferedReader().forEachLine { raw ->
            val line = raw.trim()
            when {
                line.startsWith("game (") -> { inGame = true; current = null }
                inGame && current == null && line.startsWith("name ") -> {
                    current = line.removePrefix("name ").trim().removeSurrounding("\"")
                }
                inGame && line.startsWith("rom (") -> {
                    CRC_REGEX.find(line)?.let { m ->
                        current?.let { name ->
                            byCrc[m.groupValues[1].lowercase()] = name
                            val key = normalizeTitle(name)
                            byName.getOrPut(key) { mutableListOf() }.add(name)
                        }
                    }
                }
                line == ")" -> { inGame = false; current = null }
            }
        }

        val bestByName = byName.mapValues { (_, candidates) ->
            // Pick the candidate with the best priority (lowest index in PREF_ORDER)
            candidates.minByOrNull { c ->
                PREF_ORDER.indexOfFirst { p -> c.contains(p, ignoreCase = true) }
                    .let { if (it == -1) PREF_ORDER.size else it }
            } ?: candidates.first()
        }

        return ParseResult(byCrc, bestByName)
    }

    fun normalizeTitle(raw: String): String =
        raw.replace(TAG_STRIP, "")
           .replace(PUNCT_STRIP, " ")
           .replace(LANG_SUFFIX, "")
           .replace(SPACE_NORM, " ")
           .trim()
           .lowercase()
}
