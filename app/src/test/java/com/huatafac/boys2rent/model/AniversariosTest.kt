package com.huatafac.boys2rent.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AniversariosTest {

    private fun turma(vararg alunos: Pair<String, String>) = Turma(
        id = "t1", ano = "7.º", letra = "A",
        alunos = alunos.mapIndexed { i, (nome, nasc) -> Aluno(numero = i + 1, nome = nome, dataNascimento = nasc) }
    )

    @Test
    fun semanaComecaAoDomingo() {
        // 7 de novembro de 2026 é um sábado; 8 é domingo e já começa a semana seguinte
        assertEquals("2026-11-01" to "2026-11-07", semanaDomingoASabado(2026, 11, 7))
        assertEquals("2026-11-08" to "2026-11-14", semanaDomingoASabado(2026, 11, 8))
        assertEquals("2026-12-27" to "2027-01-02", semanaDomingoASabado(2026, 12, 31))
    }

    @Test
    fun aniversariosDaSemanaComIdade() {
        val t = turma(
            "Ana" to "10/11/2014",
            "Bruno" to "08/11/2013",
            "Carla" to "15/11/2014", // fora da semana
            "Dinis" to "",           // sem data
            "Eva" to "31/04/2014"    // data inválida
        )
        val lista = aniversariosEntre(listOf(t), "2026-11-08", "2026-11-14")
        assertEquals(listOf("Bruno", "Ana"), lista.map { it.nome })
        assertEquals(listOf(13, 12), lista.map { it.idade })
        assertEquals("2026-11-08", lista.first().data)
        assertEquals("7.º A", lista.first().turma)
    }

    @Test
    fun semanaQueAtravessaOAno() {
        val t = turma("Ana" to "30/12/2014", "Bruno" to "02/01/2015")
        val lista = aniversariosEntre(listOf(t), "2026-12-27", "2027-01-02")
        assertEquals(listOf("2026-12-30" to 12, "2027-01-02" to 12), lista.map { it.data to it.idade })
    }

    @Test
    fun nascidoA29DeFevereiro() {
        val t = turma("Ana" to "29/02/2016")
        assertEquals("2026-02-28", aniversariosEntre(listOf(t), "2026-02-22", "2026-02-28").single().data)
        assertEquals("2028-02-29", aniversariosEntre(listOf(t), "2028-02-27", "2028-03-04").single().data)
    }

    @Test
    fun antesDeNascerNaoConta() {
        val t = turma("Ana" to "10/11/2026")
        assertTrue(aniversariosEntre(listOf(t), "2026-11-08", "2026-11-14").isEmpty())
    }
}
