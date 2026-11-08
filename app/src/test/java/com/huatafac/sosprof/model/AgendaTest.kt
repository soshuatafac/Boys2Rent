package com.huatafac.sosprof.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AgendaTest {

    private val reuniao = EntradaAgenda("r", "2026-11-02", hm(17, 0), TipoAgenda.REUNIAO.name, "Conselho de turma")
    private val nota = EntradaAgenda("n", "2026-11-02", null, TipoAgenda.NOTA.name, "Levar fichas")
    private val visita = EntradaAgenda("v", "2026-11-05", hm(9, 30), TipoAgenda.ATIVIDADE.name, "Visita de estudo")
    private val agenda = listOf(visita, reuniao, nota)

    @Test
    fun dataAgenda_formatoComZeros() {
        assertEquals("2026-01-05", dataAgenda(2026, 1, 5))
    }

    @Test
    fun doDia_diaTodoPrimeiroDepoisPorHora() {
        assertEquals(listOf(nota, reuniao), agenda.doDia("2026-11-02"))
        assertEquals(emptyList<EntradaAgenda>(), agenda.doDia("2026-11-03"))
    }

    @Test
    fun entre_incluiLimitesEOrdena() {
        assertEquals(listOf(nota, reuniao, visita), agenda.entre("2026-11-02", "2026-11-08"))
        assertEquals(listOf(nota, reuniao), agenda.entre("2026-10-26", "2026-11-02"))
    }

    @Test
    fun validarEntrada_camposObrigatorios() {
        assertNull(validarEntrada(reuniao))
        assertNull(validarEntrada(nota))
        assertNotNull(validarEntrada(reuniao.copy(titulo = " ")))
        assertNotNull(validarEntrada(reuniao.copy(data = "")))
        assertNotNull(validarEntrada(reuniao.copy(horaMin = 24 * 60)))
    }

    @Test
    fun tipoDesconhecido_contaComoOutro() {
        assertEquals(TipoAgenda.OUTRO, reuniao.copy(tipo = "FERIAS").tipoAgenda)
        assertEquals(TipoAgenda.REUNIAO, reuniao.tipoAgenda)
    }

    @Test
    fun comEntrada_substituiPeloId() {
        val alterada = reuniao.copy(titulo = "Reunião de pais")
        val resultado = agenda.comEntrada(alterada)
        assertEquals(3, resultado.size)
        assertEquals("Reunião de pais", resultado.first { it.id == "r" }.titulo)
        assertEquals(4, agenda.comEntrada(alterada.copy(id = "novo")).size)
    }
}
