package com.huatafac.sosprof.model

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/*
 * Feriados nacionais e dias festivos em Portugal, calculados na app (sem internet).
 * Só entram dias com data fixa ou calculável com exatidão: as datas fixas e as que dependem
 * da Páscoa (Sexta-feira Santa, Corpo de Deus, Carnaval…) ou de uma regra (Dia da Mãe).
 * Feriados municipais e regionais (Açores, Madeira) não entram: dependem de onde se vive.
 */

enum class TipoDiaEspecial(val rotulo: String) {
    FERIADO("Feriado nacional"),
    FESTIVO("Dia festivo")
}

/** Um feriado ou dia festivo. [data] é "aaaa-MM-dd", como na agenda. */
data class DiaEspecial(val data: String, val nome: String, val tipo: TipoDiaEspecial)

private val utc: TimeZone = TimeZone.getTimeZone("UTC")

private fun calendario(ano: Int, mes: Int, dia: Int): Calendar =
    GregorianCalendar(utc).apply {
        clear()
        set(ano, mes - 1, dia)
    }

private fun Calendar.comoData(): String =
    dataAgenda(get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH))

/**
 * Domingo de Páscoa (calendário gregoriano), pelo algoritmo de Meeus/Jones/Butcher.
 * Devolve o calendário em UTC, à meia-noite.
 */
private fun calendarioPascoa(ano: Int): Calendar {
    val a = ano % 19
    val b = ano / 100
    val c = ano % 100
    val d = b / 4
    val e = b % 4
    val f = (b + 8) / 25
    val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4
    val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val mes = (h + l - 7 * m + 114) / 31
    val dia = (h + l - 7 * m + 114) % 31 + 1
    return calendario(ano, mes, dia)
}

/** Domingo de Páscoa do [ano], em "aaaa-MM-dd". */
fun pascoa(ano: Int): String = calendarioPascoa(ano).comoData()

/** Data a [dias] da Páscoa do [ano] (negativo = antes). */
private fun relativaAPascoa(ano: Int, dias: Int): String =
    calendarioPascoa(ano).apply { add(Calendar.DAY_OF_MONTH, dias) }.comoData()

/** Primeiro domingo do [mes] (1–12) do [ano]. */
private fun primeiroDomingo(ano: Int, mes: Int): String {
    val c = calendario(ano, mes, 1)
    val avanco = (Calendar.SUNDAY - c.get(Calendar.DAY_OF_WEEK) + 7) % 7
    c.add(Calendar.DAY_OF_MONTH, avanco)
    return c.comoData()
}

private fun calcularAno(ano: Int): List<DiaEspecial> {
    val fixo = { mes: Int, dia: Int -> dataAgenda(ano, mes, dia) }
    val feriado = { data: String, nome: String -> DiaEspecial(data, nome, TipoDiaEspecial.FERIADO) }
    val festivo = { data: String, nome: String -> DiaEspecial(data, nome, TipoDiaEspecial.FESTIVO) }
    val lista = listOf(
        // Feriados nacionais obrigatórios (Código do Trabalho, art. 234.º)
        feriado(fixo(1, 1), "Ano Novo"),
        feriado(relativaAPascoa(ano, -2), "Sexta-feira Santa"),
        feriado(pascoa(ano), "Páscoa"),
        feriado(fixo(4, 25), "Dia da Liberdade"),
        feriado(fixo(5, 1), "Dia do Trabalhador"),
        feriado(relativaAPascoa(ano, 60), "Corpo de Deus"),
        feriado(fixo(6, 10), "Dia de Portugal, de Camões e das Comunidades Portuguesas"),
        feriado(fixo(8, 15), "Assunção de Nossa Senhora"),
        feriado(fixo(10, 5), "Implantação da República"),
        feriado(fixo(11, 1), "Dia de Todos os Santos"),
        feriado(fixo(12, 1), "Restauração da Independência"),
        feriado(fixo(12, 8), "Imaculada Conceição"),
        feriado(fixo(12, 25), "Natal"),

        // Dias festivos (não são feriado obrigatório)
        festivo(fixo(1, 6), "Dia de Reis"),
        festivo(fixo(2, 14), "Dia dos Namorados"),
        festivo(relativaAPascoa(ano, -47), "Carnaval"),
        festivo(relativaAPascoa(ano, -46), "Quarta-feira de Cinzas"),
        festivo(fixo(3, 8), "Dia Internacional da Mulher"),
        festivo(fixo(3, 19), "Dia do Pai"),
        festivo(relativaAPascoa(ano, -7), "Domingo de Ramos"),
        festivo(primeiroDomingo(ano, 5), "Dia da Mãe"),
        festivo(fixo(6, 1), "Dia Mundial da Criança"),
        festivo(fixo(6, 13), "Santo António"),
        festivo(fixo(6, 24), "São João"),
        festivo(fixo(6, 29), "São Pedro"),
        festivo(fixo(7, 26), "Dia dos Avós"),
        festivo(fixo(10, 5), "Dia Mundial do Professor"),
        festivo(fixo(11, 2), "Dia de Fiéis Defuntos"),
        festivo(fixo(11, 11), "São Martinho"),
        festivo(fixo(12, 24), "Véspera de Natal"),
        festivo(fixo(12, 31), "Véspera de Ano Novo")
    )
    // Por data; no mesmo dia, o feriado primeiro
    return lista.sortedWith(compareBy({ it.data }, { it.tipo.ordinal }))
}

private val cache = mutableMapOf<Int, List<DiaEspecial>>()

/** Feriados nacionais e dias festivos do [ano], por data. */
fun diasEspeciais(ano: Int): List<DiaEspecial> = synchronized(cache) { cache.getOrPut(ano) { calcularAno(ano) } }

/** Feriados e dias festivos entre [de] e [ate] ("aaaa-MM-dd", inclusive), por data. */
fun diasEspeciaisEntre(de: String, ate: String): List<DiaEspecial> {
    val anoDe = de.take(4).toIntOrNull() ?: return emptyList()
    val anoAte = ate.take(4).toIntOrNull() ?: return emptyList()
    if (anoAte < anoDe) return emptyList()
    return (anoDe..anoAte).flatMap { diasEspeciais(it) }.filter { it.data in de..ate }
}

/** Feriados e dias festivos do dia [data] ("aaaa-MM-dd"). */
fun diasEspeciaisDoDia(data: String): List<DiaEspecial> = diasEspeciaisEntre(data, data)

/** true se [data] ("aaaa-MM-dd") é feriado nacional. */
fun eFeriado(data: String): Boolean = diasEspeciaisDoDia(data).any { it.tipo == TipoDiaEspecial.FERIADO }
