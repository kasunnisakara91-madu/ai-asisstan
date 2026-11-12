package com.example.ai

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

object CodeSyntaxHighlighter {

    private val keywordColor = Color(0xFFC77DFF)
    private val stringColor = Color(0xFF34D399)
    private val commentColor = Color(0xFF7C7294)
    private val numberColor = Color(0xFFFBBF24)
    private val typeOrTagColor = Color(0xFF38BDF8)
    private val defaultColor = Color(0xFFF3EEFF)

    private val commonKeywords = setOf(
        "fun", "val", "var", "class", "interface", "object", "data", "sealed", "enum",
        "return", "if", "else", "when", "for", "while", "do", "try", "catch", "finally",
        "throw", "import", "package", "private", "public", "protected", "internal",
        "override", "suspend", "async", "await", "function", "const", "let", "export",
        "default", "from", "def", "lambda", "with", "as", "pass", "yield", "elif",
        "extends", "implements", "new", "static", "void", "final", "switch", "case",
        "break", "continue", "struct", "namespace", "using", "template", "typename",
        "include", "echo", "foreach", "true", "false", "null", "None", "True", "False"
    )

    fun highlight(code: String, language: String): AnnotatedString {
        val lang = language.lowercase().trim()
        return buildAnnotatedString {
            append(code)
            addStyle(SpanStyle(color = defaultColor), 0, code.length)

            // Comments (//..., #..., /*...*/, <!--...-->)
            val commentPattern = when (lang) {
                "python", "py" -> Regex("""#[^\n]*""")
                "html", "xml" -> Regex("""<!--[\s\S]*?-->""")
                else -> Regex("""//[^\n]*|/\*[\s\S]*?\*/|#[^\n]*""")
            }
            commentPattern.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = commentColor), match.range.first, match.range.last + 1)
            }

            // Strings
            val stringPattern = Regex("""\"([^\"\\]|\\.)*\"|'([^'\\]|\\.)*'|`([^`\\]|\\.)*`""")
            stringPattern.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
            }

            // HTML/XML tags
            if (lang == "html" || lang == "xml" || lang == "php") {
                val tagPattern = Regex("""</?[a-zA-Z0-9_-]+""")
                tagPattern.findAll(code).forEach { match ->
                    addStyle(
                        SpanStyle(color = typeOrTagColor, fontWeight = FontWeight.SemiBold),
                        match.range.first,
                        match.range.last + 1
                    )
                }
            }

            // Numbers
            val numberPattern = Regex("""\b\d+(\.\d+)?\b""")
            numberPattern.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = numberColor), match.range.first, match.range.last + 1)
            }

            // PascalCase types & annotations
            val typePattern = Regex("""@[A-Za-z_][A-Za-z0-9_]*|\b[A-Z][A-Za-z0-9_]*\b""")
            typePattern.findAll(code).forEach { match ->
                addStyle(SpanStyle(color = typeOrTagColor), match.range.first, match.range.last + 1)
            }

            // Keywords
            val wordPattern = Regex("""\b[A-Za-z_][A-Za-z0-9_]*\b""")
            wordPattern.findAll(code).forEach { match ->
                if (match.value in commonKeywords) {
                    addStyle(
                        SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold),
                        match.range.first,
                        match.range.last + 1
                    )
                }
            }
        }
    }
}
