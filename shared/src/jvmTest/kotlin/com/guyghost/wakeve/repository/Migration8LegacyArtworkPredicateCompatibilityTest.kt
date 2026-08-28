package com.guyghost.wakeve.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Migration8LegacyArtworkPredicateCompatibilityTest {

    @Test
    fun `migration 8 De Morgan predicate is SQLite equivalent and SQLDelight parser friendly`() {
        val cases = linkedMapOf(
            "https://cdn.wakeve.app/art.jpg" to false,
            "https://api.wakeve.app/art.jpg" to false,
            "https://example.org/art.jpg" to true,
            "https://cdn.wakeve.app/user@art.jpg" to true,
            "https://cdn.wakeve.app/art.jpg?size=2" to true,
            "https://cdn.wakeve.app/art.jpg#crop" to true
        )
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            null,
            "CREATE TABLE predicate_case(url TEXT NOT NULL, expected_rejected INTEGER NOT NULL)",
            0
        ).value
        cases.forEach { (url, expectedRejected) ->
            driver.execute(
                null,
                "INSERT INTO predicate_case(url, expected_rejected) VALUES (?, ?)",
                2
            ) {
                bindString(0, url)
                bindLong(1, if (expectedRejected) 1 else 0)
            }.value
        }

        val rows = driver.executeQuery(
            identifier = null,
            sql = """
                SELECT
                    url,
                    expected_rejected,
                    NOT (
                        (url LIKE 'https://cdn.wakeve.app/%' OR url LIKE 'https://api.wakeve.app/%')
                        AND instr(url, '@') = 0
                        AND instr(url, '?') = 0
                        AND instr(url, '#') = 0
                    ) AS original_rejected,
                    (
                        (url NOT LIKE 'https://cdn.wakeve.app/%' AND url NOT LIKE 'https://api.wakeve.app/%')
                        OR instr(url, '@') != 0
                        OR instr(url, '?') != 0
                        OR instr(url, '#') != 0
                    ) AS demorgan_rejected
                FROM predicate_case
                ORDER BY url
            """.trimIndent(),
            mapper = { cursor ->
                val values = mutableListOf<PredicateResult>()
                while (cursor.next().value) {
                    values += PredicateResult(
                        url = cursor.getString(0)!!,
                        expectedRejected = cursor.getLong(1) == 1L,
                        originalRejected = cursor.getLong(2) == 1L,
                        demorganRejected = cursor.getLong(3) == 1L
                    )
                }
                app.cash.sqldelight.db.QueryResult.Value(values)
            },
            parameters = 0
        ).value

        rows.forEach { row ->
            assertEquals(row.expectedRejected, row.originalRejected, row.url)
            assertEquals(row.originalRejected, row.demorganRejected, row.url)
        }

        val migration = migration8Source()
        val issueInsert = migration.substringAfter("INSERT INTO event_artwork_migration_issue")
            .substringBefore("DROP TABLE invitation_event_artwork_source")
        assertFalse(
            issueInsert.contains("AND NOT ("),
            "SQLDelight rejects unary NOT after AND in migration 8 even though SQLite accepts it."
        )
        assertTrue(issueInsert.contains("heroImageUrl NOT LIKE 'https://cdn.wakeve.app/%'"))
        assertTrue(issueInsert.contains("heroImageUrl NOT LIKE 'https://api.wakeve.app/%'"))
        for (reserved in listOf("@", "?", "#")) {
            assertTrue(issueInsert.contains("instr(heroImageUrl, '$reserved') != 0"))
        }
    }

    private fun migration8Source(): String {
        val candidates = listOf(
            File("src/commonMain/sqldelight/com/guyghost/wakeve/migrations/8.sqm"),
            File("shared/src/commonMain/sqldelight/com/guyghost/wakeve/migrations/8.sqm")
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("migrations/8.sqm not found")
    }

    private data class PredicateResult(
        val url: String,
        val expectedRejected: Boolean,
        val originalRejected: Boolean,
        val demorganRejected: Boolean
    )
}
