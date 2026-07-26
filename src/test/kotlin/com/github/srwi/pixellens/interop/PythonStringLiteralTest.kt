package com.github.srwi.pixellens.interop

import org.junit.Assert.assertEquals
import org.junit.Test

class PythonStringLiteralTest {

    @Test
    fun `decodes single quoted literal`() {
        assertEquals("3.13.0", PythonStringLiteral.decodeIfLiteral("'3.13.0'"))
    }

    @Test
    fun `decodes double quoted literal`() {
        // repr("it's") quotes with double quotes and escapes nothing
        assertEquals("it's", PythonStringLiteral.decodeIfLiteral("\"it's\""))
    }

    @Test
    fun `decodes literal containing both quote types`() {
        // repr("a'b\"c") == '\'a\\\'b"c\''
        assertEquals("a'b\"c", PythonStringLiteral.decodeIfLiteral("'a\\'b\"c'"))
    }

    @Test
    fun `decodes multi line traceback`() {
        val literal = "'Traceback (most recent call last):\\n  File \"x.py\", line 1, in <module>\\nValueError: bad'"
        val expected = "Traceback (most recent call last):\n  File \"x.py\", line 1, in <module>\nValueError: bad"
        assertEquals(expected, PythonStringLiteral.decodeIfLiteral(literal))
    }

    @Test
    fun `decodes escaped backslash`() {
        // repr("a\\nb") == "'a\\\\nb'" — a literal backslash followed by the letter n
        assertEquals("a\\nb", PythonStringLiteral.decodeIfLiteral("'a\\\\nb'"))
    }

    @Test
    fun `decodes hex and unicode escapes`() {
        assertEquals("\u0007", PythonStringLiteral.decodeIfLiteral("'\\x07'"))
        assertEquals("é", PythonStringLiteral.decodeIfLiteral("'\\xe9'"))
        assertEquals("é", PythonStringLiteral.decodeIfLiteral("'\\u00e9'"))
        assertEquals("😀", PythonStringLiteral.decodeIfLiteral("'\\U0001f600'"))
    }

    @Test
    fun `keeps raw non-ascii characters`() {
        // Python 3 repr leaves printable non-ASCII unescaped
        assertEquals("é😀", PythonStringLiteral.decodeIfLiteral("'é😀'"))
    }

    @Test
    fun `decodes base64 chunk`() {
        assertEquals("QUJDRA==", PythonStringLiteral.decodeIfLiteral("'QUJDRA=='"))
    }

    @Test
    fun `decodes empty literal`() {
        assertEquals("", PythonStringLiteral.decodeIfLiteral("''"))
    }

    @Test
    fun `keeps non-literals unchanged`() {
        assertEquals("None", PythonStringLiteral.decodeIfLiteral("None"))
        assertEquals("123", PythonStringLiteral.decodeIfLiteral("123"))
        assertEquals("(3, 4)", PythonStringLiteral.decodeIfLiteral("(3, 4)"))
        assertEquals("torch.Size([3, 4])", PythonStringLiteral.decodeIfLiteral("torch.Size([3, 4])"))
        assertEquals("", PythonStringLiteral.decodeIfLiteral(""))
        assertEquals("'", PythonStringLiteral.decodeIfLiteral("'"))
    }

    @Test
    fun `keeps malformed escapes verbatim`() {
        assertEquals("a\\x1", PythonStringLiteral.decodeIfLiteral("'a\\x1'"))
        assertEquals("a\\", PythonStringLiteral.decodeIfLiteral("'a\\'"))
        assertEquals("a\\q", PythonStringLiteral.decodeIfLiteral("'a\\q'"))
    }

    @Test
    fun `keeps mismatched quotes unchanged`() {
        assertEquals("'abc\"", PythonStringLiteral.decodeIfLiteral("'abc\""))
    }
}
