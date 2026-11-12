package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.CodeSyntaxHighlighter
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAiThemeTokens
import kotlinx.coroutines.delay

private sealed interface MarkdownBlock {
    data class Prose(val text: String) : MarkdownBlock
    data class Code(val language: String, val code: String) : MarkdownBlock
}

private fun parseMarkdownBlocks(raw: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val regex = Regex("```([a-zA-Z0-9_+#.-]*)\\s*\\n([\\s\\S]*?)```")
    var lastIndex = 0
    for (match in regex.findAll(raw)) {
        if (match.range.first > lastIndex) {
            val prose = raw.substring(lastIndex, match.range.first).trim()
            if (prose.isNotEmpty()) blocks.add(MarkdownBlock.Prose(prose))
        }
        val lang = match.groupValues[1].ifBlank { "code" }
        val code = match.groupValues[2].trimEnd()
        blocks.add(MarkdownBlock.Code(lang, code))
        lastIndex = match.range.last + 1
    }
    if (lastIndex < raw.length) {
        val tail = raw.substring(lastIndex).trim()
        if (tail.isNotEmpty()) blocks.add(MarkdownBlock.Prose(tail))
    }
    if (blocks.isEmpty() && raw.isNotBlank()) {
        blocks.add(MarkdownBlock.Prose(raw))
    }
    return blocks
}

@Composable
fun MarkdownMessageView(
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    SelectionContainer {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.Prose -> ProseBlockView(block.text, textColor)
                    is MarkdownBlock.Code -> SyntaxCodeCard(language = block.language, code = block.code)
                }
            }
        }
    }
}

@Composable
private fun ProseBlockView(text: String, textColor: Color) {
    val tokens = LocalAiThemeTokens.current
    val lines = remember(text) { text.lines() }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("### ") -> Text(
                    text = parseInlineStyles(trimmed.removePrefix("### "), tokens.glowSecondary),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = tokens.glowSecondary
                )
                trimmed.startsWith("## ") -> Text(
                    text = parseInlineStyles(trimmed.removePrefix("## "), tokens.glowSecondary),
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor
                )
                trimmed.startsWith("# ") -> Text(
                    text = parseInlineStyles(trimmed.removePrefix("# "), tokens.glowSecondary),
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor
                )
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "•",
                            color = tokens.glowSecondary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = parseInlineStyles(trimmed.drop(2), tokens.glowSecondary),
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                    }
                }
                trimmed.isEmpty() -> Spacer(modifier = Modifier.height(2.dp))
                else -> Text(
                    text = parseInlineStyles(line, tokens.glowSecondary),
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
            }
        }
    }
}

private fun parseInlineStyles(line: String, accentColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < line.length) {
            when {
                line.startsWith("**", i) -> {
                    val end = line.indexOf("**", i + 2)
                    if (end > i + 2) {
                        val startIdx = length
                        append(line.substring(i + 2, end))
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), startIdx, length)
                        i = end + 2
                    } else {
                        append("**")
                        i += 2
                    }
                }
                line[i] == '`' -> {
                    val end = line.indexOf('`', i + 1)
                    if (end > i + 1) {
                        val startIdx = length
                        append(line.substring(i + 1, end))
                        addStyle(
                            SpanStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = accentColor,
                                background = accentColor.copy(alpha = 0.14f)
                            ),
                            startIdx,
                            length
                        )
                        i = end + 1
                    } else {
                        append('`')
                        i += 1
                    }
                }
                line[i] == '*' -> {
                    val end = line.indexOf('*', i + 1)
                    if (end > i + 1) {
                        val startIdx = length
                        append(line.substring(i + 1, end))
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), startIdx, length)
                        i = end + 1
                    } else {
                        append('*')
                        i += 1
                    }
                }
                else -> {
                    append(line[i])
                    i += 1
                }
            }
        }
    }
}

@Composable
fun SyntaxCodeCard(
    language: String,
    code: String,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAiThemeTokens.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }

    val highlighted = remember(code, language) {
        CodeSyntaxHighlighter.highlight(code, language)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = tokens.codeBackground,
        border = BorderStroke(1.dp, tokens.glowPrimary.copy(alpha = 0.35f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tokens.glowPrimary.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = language.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.glowSecondary
                )
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (copied) Color(0xFF34D399) else tokens.glowSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = highlighted,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            )
        }
    }
}
