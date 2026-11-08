package com.huatafac.boys2rent.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlunosExcelTest {

    private val ana = Aluno(
        numero = 1,
        nome = "Ana Silva",
        dataNascimento = "10/11/2014",
        faltasPeriodo = 2,
        fotoUrl = "file:///foto.jpg",
        notas = listOf(NotaAluno("Leitura", "Lê bem")),
        familiares = listOf(
            Familiar(nome = "Rui Silva", parentesco = Parentesco.PAI),
            Familiar(nome = "Maria Silva", parentesco = Parentesco.MAE, telemovel = "912345678", encarregadoEducacao = true)
        )
    )

    private val cabecalho = ColunaAluno.entries.map { it.cabecalho }

    private fun linha(vararg pares: Pair<ColunaAluno, String>): List<String> {
        val m = pares.toMap()
        return ColunaAluno.entries.map { m[it].orEmpty() }
    }

    @Test
    fun exportarEDepoisImportarNaoMudaNada() {
        val folha = folhaDeAlunos(listOf(ana))
        assertEquals(cabecalho, folha.first())
        assertEquals("Maria Silva", folha[1][ColunaAluno.EE_NOME.ordinal])
        assertEquals("Mãe", folha[1][ColunaAluno.EE_PARENTESCO.ordinal])

        val r = aplicarFolhaAlunos(listOf(ana), folha)
        assertTrue(r.erros.isEmpty())
        assertEquals(listOf(ana), r.alunos)
        assertEquals(0, r.novos)
        assertEquals(0, r.atualizados)
        assertEquals(1, r.semAlteracoes)
    }

    @Test
    fun atualizaExistenteECriaNovo() {
        val folha = listOf(
            cabecalho,
            linha(ColunaAluno.NUMERO to "1", ColunaAluno.PROCESSO to "4521", ColunaAluno.EE_TELEMOVEL to "934000111"),
            linha(ColunaAluno.NUMERO to "2", ColunaAluno.NOME to "Bruno Costa", ColunaAluno.DATA_NASCIMENTO to "3/2/2014",
                ColunaAluno.EE_NOME to "Paula Costa", ColunaAluno.EE_PARENTESCO to "mae")
        )
        val r = aplicarFolhaAlunos(listOf(ana), folha)
        assertTrue(r.erros.toString(), r.erros.isEmpty())
        assertEquals(1, r.novos)
        assertEquals(1, r.atualizados)

        val anaDepois = r.alunos.first { it.numero == 1 }
        assertEquals("4521", anaDepois.numeroProcesso)
        // Células vazias mantêm o que havia; foto, faltas e notas não são tocadas
        assertEquals("Ana Silva", anaDepois.nome)
        assertEquals(2, anaDepois.faltasPeriodo)
        assertEquals("file:///foto.jpg", anaDepois.fotoUrl)
        assertEquals(ana.notas, anaDepois.notas)
        // Só o encarregado muda; o pai fica como estava
        assertEquals("934000111", anaDepois.encarregado?.telemovel)
        assertEquals("Maria Silva", anaDepois.encarregado?.nome)
        assertEquals(2, anaDepois.familiares.size)

        val bruno = r.alunos.first { it.numero == 2 }
        assertEquals("03/02/2014", bruno.dataNascimento)
        assertEquals(Parentesco.MAE, bruno.encarregado?.parentesco)
        assertEquals(listOf(1, 2), r.alunos.map { it.numero })
    }

    @Test
    fun colunasPorOutraOrdemEComMaiusculas() {
        val folha = listOf(
            listOf("NOME", "Notas extra", "n.º"),
            listOf("Carla", "ignorado", "3")
        )
        val r = aplicarFolhaAlunos(emptyList(), folha)
        assertTrue(r.erros.isEmpty())
        assertEquals("Carla", r.alunos.single().nome)
        assertEquals(3, r.alunos.single().numero)
    }

    @Test
    fun errosNaoGravamNada() {
        val folha = listOf(
            cabecalho,
            linha(ColunaAluno.NUMERO to "abc", ColunaAluno.NOME to "X"),
            linha(ColunaAluno.NUMERO to "5"),
            linha(ColunaAluno.NUMERO to "6", ColunaAluno.NOME to "Y", ColunaAluno.DATA_NASCIMENTO to "31/02/2014"),
            linha(ColunaAluno.NUMERO to "7", ColunaAluno.NOME to "Z"),
            linha(ColunaAluno.NUMERO to "7", ColunaAluno.NOME to "Z2")
        )
        val r = aplicarFolhaAlunos(listOf(ana), folha)
        assertEquals(4, r.erros.size)
        assertTrue(r.erros[0].startsWith("Linha 2:"))
        assertTrue(r.erros[1].contains("falta o nome"))
        assertTrue(r.erros[2].contains("data de nascimento"))
        assertTrue(r.erros[3].contains("já aparece na linha 5"))
        assertEquals(listOf(ana), r.alunos)
    }

    @Test
    fun semColunaNumero() {
        val r = aplicarFolhaAlunos(emptyList(), listOf(listOf("Nome"), listOf("Ana")))
        assertEquals(1, r.erros.size)
    }

    @Test
    fun linhasVaziasEPrimeiraLinhaEmBranco() {
        val folha = listOf(emptyList(), cabecalho, emptyList(), linha(ColunaAluno.NUMERO to "4.0", ColunaAluno.NOME to "Dinis"), listOf("", " "))
        val r = aplicarFolhaAlunos(emptyList(), folha)
        assertTrue(r.erros.isEmpty())
        assertEquals(4, r.alunos.single().numero)
    }

    @Test
    fun datasEmVariosFormatos() {
        assertEquals("05/03/2014", lerData("5/3/2014"))
        assertEquals("05/03/2014", lerData("05-03-2014"))
        assertEquals("05/03/2014", lerData("2014-03-05"))
        assertEquals("05/03/2014", lerData("41703")) // série do Excel
        assertEquals("29/02/2016", lerData("29/02/2016"))
        assertNull(lerData("29/02/2015"))
        assertNull(lerData("ontem"))
    }

    @Test
    fun parentescos() {
        assertEquals(Parentesco.MAE, lerParentesco("Mãe"))
        assertEquals(Parentesco.PAI, lerParentesco("PAI"))
        assertEquals(Parentesco.AVO, lerParentesco("Avó"))
        assertEquals(Parentesco.TUTOR, lerParentesco("Tutor legal"))
        assertEquals(Parentesco.OUTRO, lerParentesco("Tia"))
    }
}
