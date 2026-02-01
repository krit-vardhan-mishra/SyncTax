package com.just_for_fun.synctax.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

@Composable
fun MarkdownText(
    text: String,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val annotatedString = remember(text) {
        val builder = AnnotatedString.Builder()
        
        // Simple regex-based parsing for bold and bullet points
        // Note: This is a basic implementation.
        
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            // Bullet points
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ")) {
                builder.append("• " + trimmedLine.substring(2))
                // Process inline formatting for the rest of the bullet point
                // Note: We need to process the substring after the bullet
                // Ideally we'd use a more robust recursive parser but for now we just append plain or basic bold
                // For simplicity in this lightweight version, let's just append the text. 
                // To support bold INSIDE bullet points, we'd need to refactor.
                // Let's just do a simple pass:
            } else if (trimmedLine.startsWith("# ")) {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = style.fontSize * 1.5)) {
                    append(trimmedLine.substring(2))
                }
            } else if (trimmedLine.startsWith("## ")) {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = style.fontSize * 1.25)) {
                    append(trimmedLine.substring(3))
                }
            } else if (trimmedLine.startsWith("### ")) {
                builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(trimmedLine.substring(4))
                }
            } else {
                parseInlineFormatting(builder, line)
            }
            
            if (index < lines.size - 1) {
                builder.append("\n")
            }
        }
        builder.toAnnotatedString()
    }

    Text(
        text = annotatedString,
        style = style,
        color = color,
        modifier = modifier
    )
}

private fun parseInlineFormatting(builder: AnnotatedString.Builder, text: String) {
    // Regex for **bold**
    val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
    
    var lastIndex = 0
    val matches = boldRegex.findAll(text)
    
    for (match in matches) {
        // Append text before match
        if (match.range.first > lastIndex) {
            builder.append(text.substring(lastIndex, match.range.first))
        }
        
        // Append bold text
        builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(match.groupValues[1])
        }
        
        lastIndex = match.range.last + 1
    }
    
    // Append remaining text
    if (lastIndex < text.length) {
        builder.append(text.substring(lastIndex))
    }
}
