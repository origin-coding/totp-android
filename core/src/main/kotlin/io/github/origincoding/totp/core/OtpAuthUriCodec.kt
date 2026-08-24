package io.github.origincoding.totp.core

import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URISyntaxException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException

@Suppress("SameParameterValue", "Unused")
object OtpAuthUriCodec {
    private const val SCHEME = "otpauth"
    private const val TOTP_TYPE = "totp"
    private const val HOTP_TYPE = "hotp"
    private const val DEFAULT_PERIOD_SECONDS = 30L
    private const val HEX_DIGITS = "0123456789ABCDEF"

    fun parse(value: String): OtpAuthUri {
        require(value.isNotBlank()) { "OTP Auth URI must not be blank" }

        val uri = try {
            URI(value)
        } catch (exception: URISyntaxException) {
            throw IllegalArgumentException("Invalid OTP Auth URI", exception)
        }

        require(uri.scheme.equals(SCHEME, ignoreCase = true)) { "URI scheme must be '$SCHEME'" }
        require(uri.userInfo == null) { "OTP Auth URI must not contain user information" }
        require(uri.port == -1) { "OTP Auth URI must not contain a port" }
        require(uri.fragment == null) { "OTP Auth URI must not contain a fragment" }

        val type = requireNotNull(uri.host) { "OTP Auth URI type is missing" }.lowercase()
        require(type == TOTP_TYPE || type == HOTP_TYPE) { "Unsupported OTP Auth URI type: '$type'" }

        val rawPath = requireNotNull(uri.rawPath) { "OTP Auth URI label is missing" }
        require(rawPath.startsWith('/') && rawPath.length > 1) { "OTP Auth URI label is missing" }
        val rawLabel = rawPath.substring(1)
        require('/' !in rawLabel) { "OTP Auth URI label must be a single path segment" }

        val (labelIssuer, accountName) = parseLabel(percentDecode(rawLabel))
        val parameters = parseQuery(uri.rawQuery)
        val secret = canonicalSecret(requireParameter(parameters, "secret"))
        val parameterIssuer = parameters["issuer"]?.also {
            require(it.isNotBlank()) { "Issuer must not be blank" }
        }

        if (labelIssuer != null && parameterIssuer != null) {
            require(labelIssuer == parameterIssuer) {
                "Issuer in label must match issuer query parameter"
            }
        }

        val issuer = parameterIssuer ?: labelIssuer
        val algorithm = parseAlgorithm(parameters["algorithm"])
        val digits = parseDigits(parameters["digits"])

        return when (type) {
            TOTP_TYPE -> {
                require("counter" !in parameters) { "Counter is not valid for a TOTP URI" }
                OtpAuthUri.Totp(
                    accountName = accountName,
                    issuer = issuer,
                    secret = secret,
                    algorithm = algorithm,
                    digits = digits,
                    periodSeconds = parsePositiveLong(
                        value = parameters["period"],
                        parameterName = "period",
                        defaultValue = DEFAULT_PERIOD_SECONDS,
                    ),
                )
            }

            HOTP_TYPE -> {
                require("period" !in parameters) { "Period is not valid for an HOTP URI" }
                OtpAuthUri.Hotp(
                    accountName = accountName,
                    issuer = issuer,
                    secret = secret,
                    algorithm = algorithm,
                    digits = digits,
                    counter = parseNonNegativeLong(
                        value = requireParameter(parameters, "counter"),
                        parameterName = "counter",
                    ),
                )
            }

            else -> error("Unexpected OTP Auth URI type")
        }
    }

    fun format(value: OtpAuthUri): String {
        val secret = canonicalSecret(value.secret)
        val issuer = value.issuer
        val label = if (issuer == null) {
            percentEncode(value.accountName)
        } else {
            "${percentEncode(issuer)}:${percentEncode(value.accountName)}"
        }
        val type = when (value) {
            is OtpAuthUri.Totp -> TOTP_TYPE
            is OtpAuthUri.Hotp -> HOTP_TYPE
        }

        return buildString {
            append(SCHEME)
            append("://")
            append(type)
            append('/')
            append(label)
            append("?secret=")
            append(percentEncode(secret))

            issuer?.let {
                append("&issuer=")
                append(percentEncode(it))
            }

            append("&algorithm=")
            append(value.algorithm.name)
            append("&digits=")
            append(value.digits.value)

            when (value) {
                is OtpAuthUri.Totp -> {
                    append("&period=")
                    append(value.periodSeconds)
                }

                is OtpAuthUri.Hotp -> {
                    append("&counter=")
                    append(value.counter)
                }
            }
        }
    }

    private fun parseLabel(label: String): Pair<String?, String> {
        require(label.isNotBlank()) { "OTP Auth URI label must not be blank" }

        val separatorIndex = label.indexOf(':')
        if (separatorIndex < 0) return null to label

        val issuer = label.substring(0, separatorIndex)
        val accountName = label.substring(separatorIndex + 1).trimStart(' ')

        require(issuer.isNotBlank()) { "Issuer in label must not be blank" }
        require(accountName.isNotBlank()) { "Account name in label must not be blank" }
        require(':' !in accountName) { "Account name must not contain ':'" }

        return issuer to accountName
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrEmpty()) return emptyMap()

        val parameters = linkedMapOf<String, String>()
        for (part in rawQuery.split('&')) {
            require(part.isNotEmpty()) { "OTP Auth URI contains an empty query parameter" }

            val separatorIndex = part.indexOf('=')
            require(separatorIndex > 0) { "OTP Auth URI query parameter must have a name and value" }

            val name = percentDecode(part.substring(0, separatorIndex))
            val parameterValue = percentDecode(part.substring(separatorIndex + 1))
            require(parameters.put(name, parameterValue) == null) {
                "Duplicate OTP Auth URI query parameter: '$name'"
            }
        }

        return parameters
    }

    private fun requireParameter(
        parameters: Map<String, String>,
        name: String,
    ): String =
        requireNotNull(parameters[name]) { "Required OTP Auth URI parameter is missing: '$name'" }

    private fun canonicalSecret(secret: String): String {
        require(secret.isNotEmpty()) { "OTP secret must not be empty" }
        val decoded = Base32.decode(secret)
        require(decoded.isNotEmpty()) { "OTP secret must contain at least one byte" }
        return Base32.encode(decoded)
    }

    private fun parseAlgorithm(value: String?): OtpAlgorithm {
        if (value == null) return OtpAlgorithm.SHA1

        return OtpAlgorithm.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unsupported OTP algorithm: '$value'")
    }

    private fun parseDigits(value: String?): OtpDigits =
        when (value) {
            null, "6" -> OtpDigits.SIX
            "8" -> OtpDigits.EIGHT
            else -> throw IllegalArgumentException("Unsupported OTP digit count: '$value'")
        }

    private fun parsePositiveLong(
        value: String?,
        parameterName: String,
        defaultValue: Long,
    ): Long {
        if (value == null) return defaultValue
        val parsed = value.toLongOrNull()
            ?: throw IllegalArgumentException("OTP Auth URI parameter '$parameterName' must be an integer")
        require(parsed > 0) { "OTP Auth URI parameter '$parameterName' must be positive" }
        return parsed
    }

    private fun parseNonNegativeLong(
        value: String,
        parameterName: String,
    ): Long {
        val parsed = value.toLongOrNull()
            ?: throw IllegalArgumentException("OTP Auth URI parameter '$parameterName' must be an integer")
        require(parsed >= 0) { "OTP Auth URI parameter '$parameterName' must not be negative" }
        return parsed
    }

    private fun percentDecode(value: String): String {
        val bytes = ByteArrayOutputStream(value.length)
        var index = 0

        while (index < value.length) {
            if (value[index] == '%') {
                require(index + 2 < value.length) { "Incomplete percent-encoded sequence" }
                val high = hexValue(value[index + 1])
                val low = hexValue(value[index + 2])
                require(high >= 0 && low >= 0) { "Invalid percent-encoded sequence" }
                bytes.write((high shl 4) or low)
                index += 3
            } else {
                val nextPercentIndex = value.indexOf('%', startIndex = index).let {
                    if (it < 0) value.length else it
                }
                val unescapedBytes =
                    value.substring(index, nextPercentIndex).toByteArray(Charsets.UTF_8)
                bytes.write(unescapedBytes, 0, unescapedBytes.size)
                index = nextPercentIndex
            }
        }

        return try {
            Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes.toByteArray())).toString()
        } catch (exception: CharacterCodingException) {
            throw IllegalArgumentException("Percent-encoded value is not valid UTF-8", exception)
        }
    }

    private fun percentEncode(value: String): String =
        buildString {
            for (byte in value.toByteArray(Charsets.UTF_8)) {
                val unsigned = byte.toInt() and 0xff
                if (isUnreserved(unsigned)) {
                    append(unsigned.toChar())
                } else {
                    append('%')
                    append(HEX_DIGITS[unsigned ushr 4])
                    append(HEX_DIGITS[unsigned and 0x0f])
                }
            }
        }

    private fun isUnreserved(value: Int): Boolean =
        value in 'A'.code..'Z'.code ||
                value in 'a'.code..'z'.code ||
                value in '0'.code..'9'.code ||
                value == '-'.code ||
                value == '.'.code ||
                value == '_'.code ||
                value == '~'.code

    private fun hexValue(value: Char): Int =
        when (value) {
            in '0'..'9' -> value - '0'
            in 'A'..'F' -> value - 'A' + 10
            in 'a'..'f' -> value - 'a' + 10
            else -> -1
        }
}
