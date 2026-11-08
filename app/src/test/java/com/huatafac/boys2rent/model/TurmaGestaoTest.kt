package com.huatafac.boys2rent.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TurmaGestaoTest {

    private val t8b = Turma(id = "8B", ano = "8.º", letra = "B", disciplina = "Matemática", sala = "Sala 14")
    private val t7a = Turma(id = "7A", ano = "7.º", letra = "A", disciplina = "Matemática", sala = "Sala 9")

    @Test
    fun validarTurma_camposObrigatorios() {
        assertNotNull(validarTurma(t8b.copy(ano = " "), emptyList()))
        assertNotNull(validarTurma(t8b.copy(letra = ""), emptyList()))
        // A disciplina é opcional (ex.: 1.º ciclo)
        assertNull(validarTurma(t8b.copy(disciplina = ""), emptyList()))
        assertNull(validarTurma(t8b.copy(sala = ""), emptyList()))
    }

    @Test
    fun validarTurma_repetidaNaoPassa() {
        val nova = Turma(ano = "8.º", letra = "b", disciplina = " matemática ")
        assertNotNull(validarTurma(nova, listOf(t7a, t8b)))
        // Outra disciplina na mesma turma é permitida
        assertNull(validarTurma(nova.copy(disciplina = "Físico-Química"), listOf(t7a, t8b)))
    }

    @Test
    fun validarTurma_aEditarIgnoraAPropria() {
        assertNull(validarTurma(t8b.copy(sala = "Sala 2"), listOf(t7a, t8b)))
    }

    @Test
    fun semTurma_tiraSoAsAulasDaTurma() {
        val horario = listOf(
            Aula(1, 0, 50, "8.º B", "Sala 14", turmaId = "8B"),
            Aula(1, 60, 110, "7.º A", "Sala 9", turmaId = "7A"),
            Aula(2, 0, 50, "Reunião", "Departamento")
        )
        val resto = horario.semTurma("8B")
        assertEquals(2, resto.size)
        assertEquals(listOf("7A", null), resto.map { it.turmaId })
    }

    @Test
    fun comTurmaAtualizada_acompanhaNomeESala() {
        val horario = listOf(
            Aula(1, 0, 50, "8.º B", "Sala 14", turmaId = "8B"),
            Aula(2, 0, 50, "Apoio 8B", "Biblioteca", turmaId = "8B"),
            Aula(3, 0, 50, "7.º A", "Sala 9", turmaId = "7A")
        )
        val nova = t8b.copy(letra = "C", sala = "Sala 3")
        val r = horario.comTurmaAtualizada(t8b, nova)
        assertEquals(Aula(1, 0, 50, "8.º C", "Sala 3", turmaId = "8B"), r[0])
        // Título e local escritos à mão não mudam
        assertEquals(horario[1], r[1])
        assertEquals(horario[2], r[2])
    }
}
