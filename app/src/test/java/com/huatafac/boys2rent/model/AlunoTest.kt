package com.huatafac.boys2rent.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlunoTest {

    @Test
    fun idadeEm_antesDoAniversario() {
        assertEquals(12, idadeEm("15/11/2013", hojeDia = 30, hojeMes = 10, hojeAno = 2026))
    }

    @Test
    fun idadeEm_noDiaDoAniversario() {
        assertEquals(13, idadeEm("30/10/2013", hojeDia = 30, hojeMes = 10, hojeAno = 2026))
    }

    @Test
    fun idadeEm_dataInvalida() {
        assertNull(idadeEm("", 30, 10, 2026))
        assertNull(idadeEm("31/13/2013", 30, 10, 2026))
        assertNull(idadeEm("abc", 30, 10, 2026))
    }

    @Test
    fun encarregado_separadoDosOutrosFamiliares() {
        val mae = Familiar("Sofia Silva", Parentesco.MAE, encarregadoEducacao = true)
        val pai = Familiar("Rui Silva", Parentesco.PAI)
        val aluno = Aluno(1, "Carolina Silva", familiares = listOf(mae, pai))
        assertEquals(mae, aluno.encarregado)
        assertEquals(listOf(pai), aluno.outrosFamiliares)
    }

    @Test
    fun morada_linhaCompleta() {
        assertEquals(
            "Rua das Flores, 12, 2670-123 Loures",
            Morada("Rua das Flores, 12", "2670-123", "Loures").linhaCompleta
        )
    }
}
