package com.kara.kara_general_api.infrastructure.adapter.output.link

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PublicLinkPoolLinkBuilderTest {
    @Test
    fun `should build the global share url on the frozen join path`() {
        val sut = PublicLinkPoolLinkBuilder("https://link.karapi.fr")

        assertEquals("https://link.karapi.fr/join/abc123", sut.globalShareUrl("abc123"))
    }

    @Test
    fun `should build the unique share url on the frozen p path`() {
        val sut = PublicLinkPoolLinkBuilder("https://link.karapi.fr")

        assertEquals("https://link.karapi.fr/p/abc123", sut.shareUrl("abc123"))
    }

    @Test
    fun `should drop a trailing slash from the configured base url`() {
        val sut = PublicLinkPoolLinkBuilder("https://link.karapi.fr/")

        assertEquals("https://link.karapi.fr/join/abc123", sut.globalShareUrl("abc123"))
        assertEquals("https://link.karapi.fr/p/abc123", sut.shareUrl("abc123"))
    }

    @Test
    fun `should drop repeated trailing slashes from the configured base url`() {
        val sut = PublicLinkPoolLinkBuilder("https://link.karapi.fr///")

        assertEquals("https://link.karapi.fr/join/abc123", sut.globalShareUrl("abc123"))
    }

    @Test
    fun `should support the mobile custom scheme used in development`() {
        val sut = PublicLinkPoolLinkBuilder("kara://pool")

        assertEquals("kara://pool/join/abc123", sut.globalShareUrl("abc123"))
        assertEquals("kara://pool/p/abc123", sut.shareUrl("abc123"))
    }

    @Test
    fun `should keep the custom scheme intact when the base url ends with a slash`() {
        val sut = PublicLinkPoolLinkBuilder("kara://pool/")

        assertEquals("kara://pool/join/abc123", sut.globalShareUrl("abc123"))
        assertEquals("kara://pool/p/abc123", sut.shareUrl("abc123"))
    }
}
