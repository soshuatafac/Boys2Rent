package com.huatafac.sosprof.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AulaTest {

    private val horario = listOf(
        Aula(4, hm(10, 20), hm(11, 10), "8.º B", "Sala 14", turmaId = "8B"),
        Aula(4, hm(14, 0), hm(14, 50), "9.º C", "Sala 14", turmaId = "9C"),
        Aula(4, hm(11, 30), hm(12, 20), "7.º A", "Sala 9", turmaId = "7A"),
        Aula(5, hm(8, 30), hm(9, 20), "7.º A", "Sala 9", turmaId = "7A")
    )

    @Test
    fun aulaAtual_devolveAAulaADecorrer() {
        assertEquals("8B", horario.aulaAtual(dia = 4, agoraMin = hm(10, 48))?.turmaId)
    }

    @Test
    fun aulaAtual_noFimDaAulaJaNaoConta() {
        assertNull(horario.aulaAtual(dia = 4, agoraMin = hm(11, 10)))
    }

    @Test
    fun proximasAulas_soDoDiaEOrdenadas() {
        val proximas = horario.proximasAulas(dia = 4, agoraMin = hm(10, 48))
        assertEquals(listOf("7A", "9C"), proximas.map { it.turmaId })
    }

    @Test
    fun comoHora_formataComZeros() {
        assertEquals("08:05", hm(8, 5).comoHora())
    }

    @Test
    fun sigla_juntaAnoELetra() {
        val turma = Turma("8B", "8.º", "B", "Matemática", "Sala 14", 0xFF1A5FD0)
        assertEquals("8B", turma.sigla)
        assertEquals("8.º B", turma.nome)
    }
}
