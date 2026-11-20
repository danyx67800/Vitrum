package com.vitrum.tv.ota

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecksumTest {

    @Test
    fun sha256MatchesKnownVector() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            ApkVerifier.sha256Hex("abc".toByteArray()),
        )
    }

    @Test
    fun parsesChecksumFromCommonFormats() {
        val hash = "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD"
        assertEquals(hash.lowercase(), ApkVerifier.parseChecksum(hash))
        assertEquals(hash.lowercase(), ApkVerifier.parseChecksum("$hash  Vitrum-v1.0.1.apk"))
        assertEquals(hash.lowercase(), ApkVerifier.parseChecksum("SHA256: $hash\n"))
        assertNull(ApkVerifier.parseChecksum("not a checksum"))
    }

    @Test
    fun onlyHttpsGithubHostsAreAllowed() {
        assertTrue(isAllowedGitHubUrl("https://github.com/danyx67800/Vitrum/releases/download/v1/Vitrum.apk"))
        assertTrue(isAllowedGitHubUrl("https://objects.githubusercontent.com/blob"))
        assertTrue(isAllowedGitHubUrl("https://release-assets.githubusercontent.com/blob"))
        assertFalse(isAllowedGitHubUrl("http://github.com/file.apk"))
        assertFalse(isAllowedGitHubUrl("https://evil.example.com/github.com.apk"))
        assertFalse(isAllowedGitHubUrl("https://github.com.evil.example/file.apk"))
        assertFalse(isAllowedGitHubUrl("file:///tmp/a.apk"))
    }
}
