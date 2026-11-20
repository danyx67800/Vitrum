package com.vitrum.tv.ota

import com.vitrum.tv.core.SemVer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OtaParserTest {

    private val releasesJson = """
        [
          {
            "tag_name": "v1.0.1",
            "name": "Vitrum 1.0.1",
            "body": "Bug fixes\nSecond line",
            "draft": false,
            "prerelease": false,
            "published_at": "2026-11-20T10:00:00Z",
            "assets": [
              {"name": "Vitrum-v1.0.1.apk", "browser_download_url": "https://github.com/danyx67800/Vitrum/releases/download/v1.0.1/Vitrum-v1.0.1.apk", "size": 8123456},
              {"name": "Vitrum-v1.0.1.apk.sha256", "browser_download_url": "https://github.com/danyx67800/Vitrum/releases/download/v1.0.1/Vitrum-v1.0.1.apk.sha256", "size": 80}
            ]
          },
          {
            "tag_name": "v1.1.0-beta.1",
            "name": "Beta",
            "body": "Beta notes",
            "draft": false,
            "prerelease": true,
            "published_at": "2026-11-21T10:00:00Z",
            "assets": [
              {"name": "Vitrum-v1.1.0-beta.1.apk", "browser_download_url": "https://github.com/danyx67800/Vitrum/releases/download/v1.1.0-beta.1/Vitrum-v1.1.0-beta.1.apk", "size": 8200000}
            ]
          },
          {
            "tag_name": "v1.0.0",
            "name": "Vitrum 1.0.0",
            "body": "First",
            "draft": false,
            "prerelease": false,
            "published_at": "2026-11-19T10:00:00Z",
            "assets": [
              {"name": "Vitrum-v1.0.0.apk", "browser_download_url": "https://github.com/danyx67800/Vitrum/releases/download/v1.0.0/Vitrum-v1.0.0.apk", "size": 8000000}
            ]
          },
          {
            "tag_name": "v2.0.0",
            "name": "Draft",
            "draft": true,
            "prerelease": false,
            "assets": []
          }
        ]
    """.trimIndent()

    @Test
    fun stableChannelIgnoresPrereleaseAndDrafts() {
        val releases = OtaParser.parseReleases(releasesJson, includePrerelease = false)
        assertEquals(2, releases.size)
        assertEquals(SemVer(1, 0, 1), releases[0].version)
        assertFalse(releases[0].prerelease)
        assertEquals(8123456, releases[0].apk.size)
        assertEquals("Vitrum-v1.0.1.apk.sha256", releases[0].checksum?.name)
    }

    @Test
    fun betaChannelIncludesPrerelease() {
        val releases = OtaParser.parseReleases(releasesJson, includePrerelease = true)
        assertEquals(SemVer(1, 1, 0), releases.first().version)
        assertTrue(releases.first().prerelease)
        assertEquals(3, releases.size)
    }

    @Test
    fun missingApkSkipsRelease() {
        val body = """
            [{"tag_name":"v3.0.0","draft":false,"prerelease":false,"assets":[{"name":"notes.txt","browser_download_url":"https://github.com/x/y/notes.txt","size":10}]}]
        """.trimIndent()
        assertTrue(OtaParser.parseReleases(body, true).isEmpty())
    }

    @Test
    fun malformedJsonReturnsEmptyList() {
        assertTrue(OtaParser.parseReleases("not json", true).isEmpty())
        assertTrue(OtaParser.parseReleases("[]", true).isEmpty())
    }

    @Test
    fun releaseWithoutChecksumHasNullChecksum() {
        val body = """
            [{"tag_name":"v3.0.0","name":"x","draft":false,"prerelease":false,"assets":[{"name":"Vitrum-v3.0.0.apk","browser_download_url":"https://github.com/x/y/Vitrum-v3.0.0.apk","size":10}]}]
        """.trimIndent()
        val release = OtaParser.parseReleases(body, true).firstOrNull()
        assertEquals(SemVer(3, 0, 0), release?.version)
        assertNull(release?.checksum)
    }
}
