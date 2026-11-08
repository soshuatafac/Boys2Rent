package com.huatafac.sosprof.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeriadosTest {

    @Test
    fun pascoaEmAnosConhecidos() {
        assertEquals("2024-03-31", pascoa(2024))
        assertEquals("2025-04-20", pascoa(2025))
        assertEquals("2026-04-05", pascoa(2026))
        assertEquals("2027-03-28", pascoa(2027))
        assertEquals("2038-04-25", pascoa(2038)) // data mais tardia possível
        assertEquals("2285-03-22", pascoa(2285)) // data mais cedo possível
    }

    @Test
    fun feriadosMoveisDe2026() {
        val dias = diasEspeciais(2026).associate { it.nome to it.data }
        assertEquals("2026-04-03", dias["Sexta-feira Santa"])
        assertEquals("2026-06-04", dias["Corpo de Deus"])
        assertEquals("2026-02-17", dias["Carnaval"])
        assertEquals("2026-02-18", dias["Quarta-feira de Cinzas"])
        assertEquals("2026-03-29", dias["Domingo de Ramos"])
        assertEquals("2026-05-03", dias["Dia da Mãe"])
    }

    @Test
    fun trezeFeriadosNacionaisPorAno() {
        for (ano in 2020..2040) {
            assertEquals(13, diasEspeciais(ano).count { it.tipo == TipoDiaEspecial.FERIADO })
        }
    }

    @Test
    fun diaDaMaeEPrimeiroDomingoDeMaio() {
        val dias = mapOf(2024 to "2024-05-05", 2025 to "2025-05-04", 2027 to "2027-05-02", 2032 to "2032-05-02")
        for ((ano, esperado) in dias) {
            assertEquals(esperado, diasEspeciais(ano).first { it.nome == "Dia da Mãe" }.data)
        }
        // 1 de maio a um domingo: é esse o Dia da Mãe
        assertEquals("2033-05-01", diasEspeciais(2033).first { it.nome == "Dia da Mãe" }.data)
    }

    @Test
    fun cincoDeOutubroTemFeriadoPrimeiroEDepoisFestivo() {
        val dia = diasEspeciaisDoDia("2026-10-05")
        assertEquals(listOf("Implantação da República", "Dia Mundial do Professor"), dia.map { it.nome })
        assertTrue(eFeriado("2026-10-05"))
        assertFalse(eFeriado("2026-10-06"))
        assertFalse(eFeriado("2026-03-19")) // Dia do Pai é festivo, não feriado
    }

    @Test
    fun intervaloQueAtravessaOAno() {
        val dias = diasEspeciaisEntre("2026-12-24", "2027-01-06")
        assertEquals(
            listOf("Véspera de Natal", "Natal", "Véspera de Ano Novo", "Ano Novo", "Dia de Reis"),
            dias.map { it.nome }
        )
    }

    @Test
    fun datasInvalidasNaoRebentam() {
        assertTrue(diasEspeciaisEntre("", "2026-01-01").isEmpty())
        assertTrue(diasEspeciaisEntre("2027-01-01", "2026-01-01").isEmpty())
    }
}
