package com.huatafac.boys2rent.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HorarioTurmaTest {

    private val mat = Aula(1, hm(8, 30), hm(9, 20), "8.º B", "Sala 14", turmaId = "8B", disciplina = "Matemática")
    private val cn = Aula(1, hm(9, 30), hm(10, 20), "8.º B", "Lab", turmaId = "8B", disciplina = "Ciências")
    private val outraTurma = Aula(1, hm(8, 30), hm(9, 20), "7.º A", "Sala 9", turmaId = "7A", disciplina = "Matemática")
    private val horario = listOf(cn, outraTurma, mat)

    @Test
    fun daTurma_filtraEOrdena() {
        val doisDias = horario + mat.copy(diaSemana = 3, inicioMin = hm(8, 0), fimMin = hm(8, 50))
        assertEquals(listOf(mat, cn, doisDias.last()), doisDias.daTurma("8B"))
    }

    @Test
    fun validarAula_camposObrigatorios() {
        val nova = mat.copy(diaSemana = 2)
        assertNull(validarAula(nova, horario))
        // A disciplina é opcional
        assertNull(validarAula(nova.copy(disciplina = " "), horario))
        assertNotNull(validarAula(nova.copy(fimMin = nova.inicioMin), horario))
        assertNotNull(validarAula(nova.copy(diaSemana = 6), horario))
    }

    @Test
    fun validarAula_sobreposicaoSoNaMesmaTurma() {
        val sobreposta = mat.copy(inicioMin = hm(9, 0), fimMin = hm(9, 50), disciplina = "Inglês")
        assertNotNull(validarAula(sobreposta, horario))
        // Encostada à anterior (começa quando a outra acaba) é permitida
        assertNull(validarAula(sobreposta.copy(inicioMin = hm(10, 20), fimMin = hm(11, 10)), horario))
        // Outra turma à mesma hora não conta
        assertNull(validarAula(outraTurma.copy(turmaId = "9C"), listOf(outraTurma)))
    }

    @Test
    fun validarAula_aEditarIgnoraAPropria() {
        val editada = mat.copy(fimMin = hm(9, 25))
        assertNull(validarAula(editada, horario, antiga = mat))
    }

    @Test
    fun comAula_substituiOuAcrescenta() {
        val editada = mat.copy(disciplina = "Inglês")
        val r = horario.comAula(mat, editada)
        assertEquals(3, r.size)
        assertEquals(editada, r.first { it.turmaId == "8B" && it.inicioMin == hm(8, 30) })

        val nova = mat.copy(diaSemana = 2)
        assertEquals(4, horario.comAula(null, nova).size)
    }

    @Test
    fun semAula_removeSoUma() {
        assertEquals(listOf(cn, outraTurma), horario.semAula(mat))
        assertEquals(horario, horario.semAula(mat.copy(diaSemana = 5)))
    }

    @Test
    fun detalhe_juntaOQueExiste() {
        assertEquals("Matemática · Sala 14", mat.detalhe)
        assertEquals("Sala 14 · Teste", mat.copy(disciplina = "", nota = "Teste").detalhe)
    }
}
