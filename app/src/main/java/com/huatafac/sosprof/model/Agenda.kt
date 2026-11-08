package com.huatafac.sosprof.model

import com.google.firebase.database.Exclude

/** Tipos de entrada da agenda pessoal (guardados pelo nome, para o DB não depender da ordem). */
enum class TipoAgenda(val rotulo: String, val corHex: Long) {
    REUNIAO("Reunião", 0xFF1A5FD0),
    ATIVIDADE("Atividade", 0xFF0E7C86),
    NOTA("Nota", 0xFFC2570C),
    OUTRO("Outro", 0xFF55627D);

    companion object {
        /** Tipos desconhecidos (ex.: vindos de uma versão mais nova da app) contam como [OUTRO]. */
        fun de(nome: String): TipoAgenda = entries.firstOrNull { it.name == nome } ?: OUTRO
    }
}

/**
 * Uma entrada da agenda pessoal do professor (reunião, atividade, nota…), separada do horário das turmas.
 * Fica em users/{uid}/agenda/{id}.
 * [data] é "aaaa-MM-dd" (ordena como texto e não depende do fuso horário);
 * [horaMin] são minutos desde a meia-noite, ou null para o dia todo.
 */
data class EntradaAgenda(
    val id: String = "",
    val data: String = "",
    val horaMin: Int? = null,
    val tipo: String = TipoAgenda.NOTA.name,
    val titulo: String = "",
    val texto: String = ""
) {
    @get:Exclude val tipoAgenda: TipoAgenda
        get() = TipoAgenda.de(tipo)
}

/** "aaaa-MM-dd" a partir de ano, mês (1–12) e dia. */
fun dataAgenda(ano: Int, mes: Int, dia: Int): String = "%04d-%02d-%02d".format(ano, mes, dia)

/** Entradas do dia [data], primeiro as do dia todo e depois por hora. */
fun List<EntradaAgenda>.doDia(data: String): List<EntradaAgenda> =
    filter { it.data == data }.sortedWith(compareBy({ it.horaMin ?: -1 }, { it.titulo.lowercase() }))

/** Entradas entre [de] e [ate] (inclusive), por data e hora. */
fun List<EntradaAgenda>.entre(de: String, ate: String): List<EntradaAgenda> =
    filter { it.data in de..ate }.sortedWith(compareBy({ it.data }, { it.horaMin ?: -1 }, { it.titulo.lowercase() }))

private val formatoData = Regex("""\d{4}-\d{2}-\d{2}""")

/** Mensagem de erro, ou null se a entrada pode ser gravada. */
fun validarEntrada(entrada: EntradaAgenda): String? = when {
    entrada.titulo.isBlank() -> "Indica um título."
    !formatoData.matches(entrada.data) -> "Escolhe a data."
    entrada.horaMin != null && entrada.horaMin !in 0 until 24 * 60 -> "Hora inválida."
    else -> null
}

/** Substitui (mesmo id) ou acrescenta a entrada. */
fun List<EntradaAgenda>.comEntrada(entrada: EntradaAgenda): List<EntradaAgenda> =
    filterNot { it.id == entrada.id } + entrada
