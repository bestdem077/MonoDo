package com.example

import com.example.data.model.NoteEntity
import com.example.ui.theme.CategoryTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun categoryTheme_resolvesCoreCategoriesAccurately() {
        val workStyle = CategoryTheme.getStyle("Work")
        assertEquals("Work", workStyle.name)

        val healthStyle = CategoryTheme.getStyle("Health")
        assertEquals("Health", healthStyle.name)

        val personalStyle = CategoryTheme.getStyle("Personal")
        assertEquals("Personal", personalStyle.name)

        // Custom categories deterministically assign styles
        val customStyle = CategoryTheme.getStyle("Finance")
        assertNotNull(customStyle)
    }

    @Test
    fun noteEntity_holdsFieldsCorrectly() {
        val note = NoteEntity(
            id = 1L,
            title = "Test Note",
            content = "Detailed note content",
            category = "Health",
            isPinned = true
        )
        assertEquals(1L, note.id)
        assertEquals("Test Note", note.title)
        assertEquals("Detailed note content", note.content)
        assertEquals("Health", note.category)
        assertTrue(note.isPinned)
    }
}
