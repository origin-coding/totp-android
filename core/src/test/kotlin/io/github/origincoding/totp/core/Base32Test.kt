package io.github.origincoding.totp.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class Base32Test {
    @Test
    fun `encode produces RFC 4648 vectors with padding`() {
        for (vector in RFC_4648_VECTORS) {
            assertEquals(
                vector.padded,
                Base32.encode(vector.plainText.toByteArray(Charsets.US_ASCII), withPadding = true),
            )
        }
    }

    @Test
    fun `encode produces RFC 4648 vectors without padding by default`() {
        for (vector in RFC_4648_VECTORS) {
            assertEquals(
                vector.padded.trimEnd('='),
                Base32.encode(vector.plainText.toByteArray(Charsets.US_ASCII)),
            )
        }
    }

    @Test
    fun `decode accepts padded and unpadded RFC 4648 vectors`() {
        for (vector in RFC_4648_VECTORS) {
            val expected = vector.plainText.toByteArray(Charsets.US_ASCII)

            assertArrayEquals(expected, Base32.decode(vector.padded))
            assertArrayEquals(expected, Base32.decode(vector.padded.trimEnd('=')))
        }
    }

    @Test
    fun `decode accepts lowercase symbols`() {
        assertArrayEquals(
            "foobar".toByteArray(Charsets.US_ASCII),
            Base32.decode("mzxw6ytboi======"),
        )
    }

    @Test
    fun `encode and decode preserve arbitrary bytes`() {
        val bytes = ByteArray(256) { it.toByte() }

        assertArrayEquals(bytes, Base32.decode(Base32.encode(bytes)))
        assertArrayEquals(bytes, Base32.decode(Base32.encode(bytes, withPadding = true)))
    }

    @Test
    fun `empty input maps to empty output`() {
        assertEquals("", Base32.encode(byteArrayOf()))
        assertArrayEquals(byteArrayOf(), Base32.decode(""))
    }

    @Test
    fun `decode rejects invalid lengths`() {
        for (encoded in listOf("A", "AAA", "AAAAAA")) {
            assertThrows(IllegalArgumentException::class.java) {
                Base32.decode(encoded)
            }
        }
    }

    @Test
    fun `decode rejects invalid characters`() {
        assertThrows(IllegalArgumentException::class.java) {
            Base32.decode("MZXW6YQ!")
        }
    }

    @Test
    fun `decode rejects misplaced or incorrect padding`() {
        for (encoded in listOf("MY=====", "MY====A=", "MZXW6YQ==")) {
            assertThrows(IllegalArgumentException::class.java) {
                Base32.decode(encoded)
            }
        }
    }

    @Test
    fun `decode rejects non-zero trailing bits`() {
        assertThrows(IllegalArgumentException::class.java) {
            Base32.decode("MZ")
        }
    }

    private data class Rfc4648Vector(
        val plainText: String,
        val padded: String,
    )

    private companion object {
        val RFC_4648_VECTORS = listOf(
            Rfc4648Vector("", ""),
            Rfc4648Vector("f", "MY======"),
            Rfc4648Vector("fo", "MZXQ===="),
            Rfc4648Vector("foo", "MZXW6==="),
            Rfc4648Vector("foob", "MZXW6YQ="),
            Rfc4648Vector("fooba", "MZXW6YTB"),
            Rfc4648Vector("foobar", "MZXW6YTBOI======"),
        )
    }
}
