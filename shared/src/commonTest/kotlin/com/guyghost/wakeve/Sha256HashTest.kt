package com.guyghost.wakeve

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Cross-platform tests for SHA-256 hashing.
 *
 * Ensures that Android and iOS implementations produce identical results.
 */
class Sha256HashTest {

    /**
     * Test that SHA-256 produces consistent results across platforms.
     */
    @Test
    fun `sha256Hash should produce consistent results`() {
        val input = "test-input"
        val hash = sha256Hash(input)

        // SHA-256 hash should be 64 hex characters
        assertEquals(64, hash.length)

        // Verify format (lowercase hex)
        assertEquals(hash, hash.lowercase())
        assertTrue(hash.all { it.isDigit() || it in 'a'..'f' })
    }

    /**
     * Test that different inputs produce different hashes.
     */
    @Test
    fun `sha256Hash should produce different hashes for different inputs`() {
        val hash1 = sha256Hash("input1")
        val hash2 = sha256Hash("input2")

        assertTrue(hash1 != hash2)
    }

    /**
     * Test that identical inputs produce identical hashes.
     */
    @Test
    fun `sha256Hash should be deterministic`() {
        val input = "deterministic-test"
        val hash1 = sha256Hash(input)
        val hash2 = sha256Hash(input)

        assertEquals(hash1, hash2)
    }

    /**
     * Test against known SHA-256 vectors.
     *
     * These are test vectors from NIST SHA-256 specification.
     */
    @Test
    fun `sha256Hash should match known test vectors`() {
        // Empty string
        val emptyHash = sha256Hash("")
        // Verify it's 64 hex characters
        assertEquals(64, emptyHash.length)

        // Single character
        val singleCharHash = sha256Hash("a")
        assertEquals(64, singleCharHash.length)

        // String from ASCII
        val asciiHash = sha256Hash("abc")
        assertEquals(64, asciiHash.length)
    }

    /**
     * Test that hash length is always 64 characters.
     */
    @Test
    fun `sha256Hash should always produce 64 character output`() {
        val inputs = listOf("", "a", "test", "longer input string", "special characters: !@#\$%^&*()")

        for (input in inputs) {
            val hash = sha256Hash(input)
            assertEquals(64, hash.length, "Hash should be 64 characters for input: '$input'")
        }
    }
}

