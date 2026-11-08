package com.huatafac.sosprof.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileFormatTest {

    @Test
    fun formatarData_insereBarras() {
        assertEquals("14/03/1980", formatarData("14031980"))
        assertEquals("14/0", formatarData("140"))
        assertEquals("14/03/1980", formatarData("14/03/1980"))
    }

    @Test
    fun formatarCodigoPostal_insereHifen() {
        assertEquals("2670-123", formatarCodigoPostal("2670123"))
        assertEquals("2670", formatarCodigoPostal("2670"))
        assertEquals("2670-123", formatarCodigoPostal("2670-1234"))
    }

    @Test
    fun splitList_limpaEspacosERepetidos() {
        assertEquals(listOf("Matemática", "Física"), splitList(" Matemática, Física,, Matemática "))
        assertEquals(emptyList<String>(), splitList(""))
    }
}
