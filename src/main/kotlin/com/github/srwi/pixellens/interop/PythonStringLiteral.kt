package com.github.srwi.pixellens.interop

/**
 * Decodes Python string literals as produced by repr(). The debugpy backend returns
 * string-valued evaluate results as their repr (quoted and escaped) instead of raw text.
 */
object PythonStringLiteral {

    fun decodeIfLiteral(text: String): String {
        if (text.length < 2) return text
        val quote = text.first()
        if (quote != '\'' && quote != '"') return text
        if (text.last() != quote) return text
        return unescape(text.substring(1, text.length - 1))
    }

    private fun unescape(text: String): String {
        val result = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c != '\\' || i == text.length - 1) {
                result.append(c)
                i++
                continue
            }
            when (val escaped = text[i + 1]) {
                '\\' -> { result.append('\\'); i += 2 }
                '\'' -> { result.append('\''); i += 2 }
                '"' -> { result.append('"'); i += 2 }
                'n' -> { result.append('\n'); i += 2 }
                'r' -> { result.append('\r'); i += 2 }
                't' -> { result.append('\t'); i += 2 }
                'a' -> { result.append('\u0007'); i += 2 }
                'b' -> { result.append('\b'); i += 2 }
                'f' -> { result.append('\u000C'); i += 2 }
                'v' -> { result.append('\u000B'); i += 2 }
                'x' -> i += appendCodePoint(result, text, i, digits = 2)
                'u' -> i += appendCodePoint(result, text, i, digits = 4)
                'U' -> i += appendCodePoint(result, text, i, digits = 8)
                else -> { result.append('\\').append(escaped); i += 2 }
            }
        }
        return result.toString()
    }

    private fun appendCodePoint(result: StringBuilder, text: String, start: Int, digits: Int): Int {
        val hexStart = start + 2
        val hexEnd = hexStart + digits
        val codePoint = if (hexEnd <= text.length) text.substring(hexStart, hexEnd).toIntOrNull(16) else null
        if (codePoint == null || !Character.isValidCodePoint(codePoint)) {
            // Malformed escape; keep it verbatim
            result.append(text[start]).append(text[start + 1])
            return 2
        }
        result.appendCodePoint(codePoint)
        return 2 + digits
    }
}
