package io.github.origincoding.totp.core

object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val PADDING = '='

    fun encode(
        bytes: ByteArray,
        withPadding: Boolean = false,
    ): String {
        if (bytes.isEmpty()) return ""

        val encoded = StringBuilder((bytes.size * 8 + 4) / 5)
        var buffer = 0
        var bitCount = 0

        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xff)
            bitCount += 8

            while (bitCount >= 5) {
                bitCount -= 5
                encoded.append(ALPHABET[(buffer shr bitCount) and 0x1f])
                buffer = buffer and bitMask(bitCount)
            }
        }

        if (bitCount > 0) {
            encoded.append(ALPHABET[(buffer shl (5 - bitCount)) and 0x1f])
        }

        if (withPadding) {
            while (encoded.length % 8 != 0) {
                encoded.append(PADDING)
            }
        }

        return encoded.toString()
    }

    fun decode(encoded: String): ByteArray {
        if (encoded.isEmpty()) return byteArrayOf()

        val firstPaddingIndex = encoded.indexOf(PADDING)
        val symbolCount = if (firstPaddingIndex >= 0) firstPaddingIndex else encoded.length
        validatePadding(encoded, symbolCount)

        val decoded = ByteArray(symbolCount * 5 / 8)
        var outputIndex = 0
        var buffer = 0
        var bitCount = 0

        for (index in 0 until symbolCount) {
            buffer = (buffer shl 5) or decodeSymbol(encoded[index])
            bitCount += 5

            if (bitCount >= 8) {
                bitCount -= 8
                decoded[outputIndex++] = ((buffer shr bitCount) and 0xff).toByte()
                buffer = buffer and bitMask(bitCount)
            }
        }

        require(buffer == 0) { "Base32 input contains non-zero trailing bits" }

        return decoded
    }

    private fun validatePadding(
        encoded: String,
        symbolCount: Int,
    ) {
        val remainder = symbolCount % 8
        require(remainder == 0 || remainder == 2 || remainder == 4 || remainder == 5 || remainder == 7) {
            "Invalid Base32 input length"
        }

        if (symbolCount == encoded.length) return

        require(encoded.length % 8 == 0) { "Padded Base32 input length must be a multiple of 8" }
        require(encoded.substring(symbolCount).all { it == PADDING }) {
            "Base32 padding must appear only at the end"
        }

        val expectedPaddingCount = when (remainder) {
            2 -> 6
            4 -> 4
            5 -> 3
            7 -> 1
            else -> 0
        }
        val actualPaddingCount = encoded.length - symbolCount

        require(actualPaddingCount == expectedPaddingCount) { "Invalid Base32 padding" }
    }

    private fun decodeSymbol(symbol: Char): Int =
        when (symbol) {
            in 'A'..'Z' -> symbol - 'A'
            in 'a'..'z' -> symbol - 'a'
            in '2'..'7' -> symbol - '2' + 26
            else -> throw IllegalArgumentException("Invalid Base32 character: '$symbol'")
        }

    private fun bitMask(bitCount: Int): Int = (1 shl bitCount) - 1
}
