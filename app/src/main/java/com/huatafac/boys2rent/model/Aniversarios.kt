package com.huatafac.boys2rent.model

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/** Aniversário de um aluno num dia ([data] em "aaaa-MM-dd"), com a idade que faz. */
data class Aniversario(
    val data: String,
    val nome: String,
    val turmaId: String,
    val turma: String,
    val idade: Int,
    val numero: Int = 0          // n.º do aluno na turma, para abrir a ficha
)

internal fun bissexto(ano: Int) = (ano % 4 == 0 && ano % 100 != 0) || ano % 400 == 0

internal fun diasDoMes(ano: Int, mes: Int): Int = when (mes) {
    2 -> if (bissexto(ano)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

/**
 * Aniversários dos alunos de [turmas] entre [de] e [ate] ("aaaa-MM-dd", inclusive), por data e nome.
 * A data de nascimento é "DD/MM/AAAA"; datas inválidas são ignoradas.
 * Quem nasceu a 29 de fevereiro faz anos a 28 nos anos que não são bissextos.
 */
fun aniversariosEntre(turmas: List<Turma>, de: String, ate: String): List<Aniversario> {
    val anoDe = de.take(4).toIntOrNull() ?: return emptyList()
    val anoAte = ate.take(4).toIntOrNull() ?: return emptyList()
    if (anoAte < anoDe) return emptyList()
    val lista = mutableListOf<Aniversario>()
    for (turma in turmas) {
        for (aluno in turma.alunos) {
            val partes = aluno.dataNascimento.split("/").mapNotNull { it.trim().toIntOrNull() }
            if (partes.size != 3) continue
            val (dia, mes, anoNasc) = partes
            if (mes !in 1..12 || dia !in 1..diasDoMes(anoNasc, mes)) continue
            for (ano in anoDe..anoAte) {
                if (ano <= anoNasc) continue
                val diaNoAno = if (mes == 2 && dia == 29 && !bissexto(ano)) 28 else dia
                val data = dataAgenda(ano, mes, diaNoAno)
                if (data in de..ate) {
                    lista += Aniversario(data, aluno.nome, turma.id, turma.nome, ano - anoNasc, aluno.numero)
                }
            }
        }
    }
    return lista.sortedWith(compareBy({ it.data }, { it.nome.lowercase() }))
}

/** Domingo e sábado ("aaaa-MM-dd") da semana do dia indicado (mês 1–12). */
fun semanaDomingoASabado(ano: Int, mes: Int, dia: Int): Pair<String, String> {
    val c = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(ano, mes - 1, dia)
        add(Calendar.DAY_OF_MONTH, Calendar.SUNDAY - get(Calendar.DAY_OF_WEEK))
    }
    val domingo = dataAgenda(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    c.add(Calendar.DAY_OF_MONTH, 6)
    val sabado = dataAgenda(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    return domingo to sabado
}
