package com.example

import com.example.ui.components.ContentBlock
import com.example.ui.components.parseInlineMarkdown
import com.example.ui.components.parseMarkdownBlocks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun markdownBlockParser_parsesCodeFenceCorrectly() {
        val input = "Here is some code:\n```kotlin\nval x = 10\n```\nDone."
        val blocks = parseMarkdownBlocks(input)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is ContentBlock.Text)
        assertTrue(blocks[1] is ContentBlock.Code)
        val codeBlock = blocks[1] as ContentBlock.Code
        assertEquals("kotlin", codeBlock.language)
        assertEquals("val x = 10", codeBlock.code)
    }

    @Test
    fun markdownInlineParser_handlesBoldText() {
        val input = "Hello **world**"
        val annotated = parseInlineMarkdown(input)
        assertEquals("Hello world", annotated.text)
    }
}
