package com.guyghost.wakeve.productlanguage

import java.io.File

internal object ProductLanguageSourceScanner {
    fun findings(root: File, file: File): List<String> {
        val path = file.relativeTo(root).invariantSeparatorsPath
        val source = file.readText().withoutComments()
        val directFindings = directPatterns.flatMap { (type, pattern) -> pattern.findAll(source).mapNotNull { match ->
            val line = source.take(match.range.first).count { it == '\n' } + 1
            val sourceLine = source.lineSequence().drop(line - 1).first().trim()
            val occurrence = "$path:$line:$type:$sourceLine"
            if (occurrence.hashCode() in reviewedOccurrenceHashes) null else "$occurrence: direct visible literal"
        }.toList() }
        val indirectFindings = indirectLiteral.findAll(source).mapNotNull { match ->
            val value = match.groupValues[1]
            val line = source.take(match.range.first).count { it == '\n' } + 1
            val occurrence = "$path:$line:$value"
            if (!value.any(Char::isLetter) || occurrence.hashCode() in reviewedOccurrenceHashes) null
            else "$occurrence: indirect visible literal"
        }.toList()
        return directFindings + indirectFindings
    }

    private fun String.withoutComments() = replace(Regex("""(?s)/\*.*?\*/""")) { " ".repeat(it.value.length) }
        .lineSequence().joinToString("\n") { if (it.trimStart().startsWith("//")) " ".repeat(it.length) else it }

    // Covers literals hidden behind helpers, enum/map branches, defaults and tuple factories.
    private val indirectLiteral = Regex(
        """(?:->|\breturn|\?:|\b(?:Triple|Pair)\s*\(|\b(?:title|body|message|description|subtitle|label)\s*=)\s*"([^"\\]*(?:\\.[^"\\]*)*)""""
    )
    private val directPatterns = listOf(
        "Text" to Regex("""\bText\s*\(\s*(?:text\s*=\s*)?""" + "\""),
        "argument" to Regex("""\b(?:label|placeholder|supportingText|headlineContent|overlineContent|text)\s*=\s*""" + "\""),
        "semantics" to Regex("""\bcontentDescription\s*=\s*""" + "\""),
    )
    // Exact path + line + literal snapshots reviewed during the Task 5 final audit.
    // A moved or changed occurrence is intentionally rejected and must be reviewed again.
    private val reviewedOccurrenceHashes = setOf(
        861591896,
        -2081326239,
        -2072320815,
        -2067627047,
        -2061779537,
        -2031289065,
        -2029649361,
        -2025158457,
        -1996692907,
        -1893295189,
        -1841298826,
        -1705776000,
        -1659356435,
        -1657697045,
        -1646805397,
        -1617023442,
        -1559928355,
        -1550378039,
        -1511745119,
        -1412502629,
        -1298553855,
        -1297908667,
        -1257038420,
        -1194880710,
        -1194597822,
        -1166985274,
        -1119208668,
        -1118331859,
        -1086411769,
        -1081476314,
        -1048090419,
        -950707296,
        -922407765,
        -898535183,
        -882303549,
        -853001127,
        -792585626,
        -717985085,
        -710922220,
        -631753532,
        -628119723,
        -567403773,
        -557824827,
        -517401726,
        -517250987,
        -397013417,
        -324584979,
        -315583386,
        -151263098,
        -139941735,
        -111266417,
        -83680983,
        -79446583,
        -21702997,
        20754288,
        104827882,
        191011393,
        231260087,
        313659132,
        370272095,
        451845455,
        516402734,
        547379636,
        566452337,
        613252566,
        662493737,
        707206716,
        783912575,
        787783199,
        799920662,
        805000178,
        825258937,
        962958741,
        979770721,
        1015740055,
        1070843744,
        1175200357,
        1177065776,
        1210744093,
        1237193131,
        1262025301,
        1287192470,
        1407726882,
        1434584256,
        1483250916,
        1486344199,
        1524909761,
        1656865359,
        1690413310,
        1808614967,
        1839467190,
        1844429513,
        1852371111,
        1942883322,
        1978330614,
        1999485983,
        2062126506,
        2084122671,
        2099485387,
    )
}
