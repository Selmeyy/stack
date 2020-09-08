package me.tylerbwong.markdown.compose.visitors

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.sp
import dev.chrisbanes.accompanist.coil.CoilImageWithCrossfade
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode

internal object ImageVisitor : Visitor {

    @Suppress("MagicNumber")
    override fun accept(
        node: ASTNode,
        builder: AnnotatedString.Builder,
        content: String,
        inlineTextContent: MutableMap<String, InlineTextContent>,
        linkPositions: MutableMap<IntRange, String>,
        continuation: Continuation
    ) {
        val imageUrlNode = node.children.getOrNull(node.children.size - 1) ?: return
        if (imageUrlNode.children.size > 2) {
            val imageUrl = imageUrlNode.children.getOrNull(imageUrlNode.children.size - 2)
                ?.getTextInNode(content)
                ?.toString() ?: return
            inlineTextContent[imageUrl] = InlineTextContent(
                placeholder = Placeholder(
                    width = 64.sp,
                    height = 64.sp,
                    PlaceholderVerticalAlign.TextCenter
                )
            ) {
                CoilImageWithCrossfade(
                    data = imageUrl,
                    contentScale = ContentScale.FillWidth
                )
            }
            builder.appendInlineContent(imageUrl, imageUrl)
        }
    }
}