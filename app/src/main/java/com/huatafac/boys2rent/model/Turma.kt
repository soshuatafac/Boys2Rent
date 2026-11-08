package com.huatafac.boys2rent.model

import com.google.firebase.database.Exclude

// Todos os modelos guardados no Realtime DB precisam de valores por defeito em todos os campos:
// é isso que dá à classe um construtor sem argumentos, sem o qual getValue() lança exceção.
// As propriedades calculadas levam @get:Exclude para não serem gravadas na base de dados.

/** Uma turma que o professor leciona. */
data class Turma(
    val id: String = "",
    val ano: String = "",     // ex.: "8.º"
    val letra: String = "",   // ex.: "B"
    val disciplina: String = "",
    val sala: String = "",
    val corHex: Long = 0L,       // cor usada na agenda e no avatar da turma
    val diretorTurma: Boolean = false,
    val alunos: List<Aluno> = emptyList()
) {
    @get:Exclude val nome: String get() = "$ano $letra"
    @get:Exclude val sigla: String get() = ano.filter { it.isDigit() } + letra
}

data class Aluno(
    val numero: Int = 0,
    val nome: String = "",
    val cartaoCidadao: String = "",
    val nif: String = "",
    val faltasPeriodo: Int = 0,
    val dataNascimento: String = "",       // DD/MM/AAAA
    val morada: Morada? = null,
    val observacoes: String = "",
    val familiares: List<Familiar> = emptyList(),
    val numeroProcesso: String = "",       // n.º de processo do aluno na escola
    val saude: String = "",                // alergias e cuidados de saúde (dado sensível: RGPD, art. 9.º)
    val notas: List<NotaAluno> = emptyList(),
    val fotoUrl: String = "",              // URI file:// da cópia da foto neste telemóvel ("" = sem foto)
    val fotoAtualizada: Long = 0L          // versão (millis) da foto na nuvem, em fotos/{uid}/alunos; 0 = não está na nuvem
) {
    @get:Exclude val encarregado: Familiar? get() = familiares.firstOrNull { it.encarregadoEducacao }

    /** Pai, mãe e outros familiares que não são o encarregado de educação. */
    @get:Exclude val outrosFamiliares: List<Familiar> get() = familiares.filterNot { it === encarregado }
}

/** Tema livre criado pelo professor na ficha do aluno (título + texto). */
data class NotaAluno(
    val titulo: String = "",
    val texto: String = ""
)

data class Morada(
    val rua: String = "",
    val codigoPostal: String = "",
    val localidade: String = ""
) {
    @get:Exclude val linhaCompleta: String get() = "$rua, $codigoPostal $localidade"
}

enum class Parentesco(val descricao: String) {
    MAE("Mãe"),
    PAI("Pai"),
    AVO("Avó/Avô"),
    TUTOR("Tutor legal"),
    OUTRO("Outro")
}

data class Familiar(
    val nome: String = "",
    val parentesco: Parentesco = Parentesco.OUTRO,
    val telemovel: String = "",
    val email: String = "",
    val profissao: String = "",
    val morada: Morada? = null,            // null = mesma morada do aluno
    val encarregadoEducacao: Boolean = false
)

/** Idade em anos a partir de "DD/MM/AAAA", ou null se a data for inválida. */
fun idadeEm(dataNascimento: String, hojeDia: Int, hojeMes: Int, hojeAno: Int): Int? {
    val partes = dataNascimento.split("/").mapNotNull { it.trim().toIntOrNull() }
    if (partes.size != 3) return null
    val (dia, mes, ano) = partes
    if (mes !in 1..12 || dia !in 1..31 || ano > hojeAno) return null
    val fezAnos = hojeMes > mes || (hojeMes == mes && hojeDia >= dia)
    return hojeAno - ano - if (fezAnos) 0 else 1
}

/**
 * Um bloco do horário semanal.
 * [diaSemana] vai de 1 (segunda) a 5 (sexta); as horas são minutos desde a meia-noite,
 * para não depender de java.time (minSdk 24).
 */
data class Aula(
    val diaSemana: Int = 1,
    val inicioMin: Int = 0,
    val fimMin: Int = 0,
    val titulo: String = "",
    val local: String = "",
    val turmaId: String? = null,   // null = reunião, apoio, etc.
    val nota: String? = null,      // ex.: "Teste"
    val disciplina: String = ""    // escolhida no horário da turma ("" = aulas antigas, sem disciplina)
) {
    /** Linha secundária para listas: disciplina, local e nota, o que existir. */
    @get:Exclude val detalhe: String
        get() = listOf(disciplina, local, nota.orEmpty()).filter { it.isNotBlank() }.joinToString(" · ")
}

/** Nomes dos dias do horário da turma (segunda a sexta), indexados por [Aula.diaSemana] - 1. */
val diasSemana: List<String> = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta")

fun Int.comoHora(): String = "%02d:%02d".format(this / 60, this % 60)

fun hm(horas: Int, minutos: Int): Int = horas * 60 + minutos

/** A aula a decorrer em [agoraMin], ou null. */
fun List<Aula>.aulaAtual(dia: Int, agoraMin: Int): Aula? =
    firstOrNull { it.diaSemana == dia && agoraMin >= it.inicioMin && agoraMin < it.fimMin }

/** As aulas do [dia] que ainda não começaram, por ordem. */
fun List<Aula>.proximasAulas(dia: Int, agoraMin: Int): List<Aula> =
    filter { it.diaSemana == dia && it.inicioMin > agoraMin }.sortedBy { it.inicioMin }

// ---- Gestão de turmas (criar, editar, apagar) ----

/** Cores à escolha para uma turma nova (as mesmas tonalidades da paleta da app). */
val coresTurma: List<Long> = listOf(
    0xFF1A5FD0, 0xFF0E7C86, 0xFFC2570C, 0xFF7B3FC4, 0xFFB3261E, 0xFF2E7D32, 0xFF8D6E63, 0xFF455A64
)

/**
 * Valida os dados de uma turma antes de gravar. Devolve a mensagem de erro, ou null se estiver tudo bem.
 * [existentes] são as outras turmas do professor; a própria turma (mesmo id) é ignorada ao editar.
 */
fun validarTurma(turma: Turma, existentes: List<Turma>): String? {
    if (turma.ano.isBlank()) return "Indique o ano (ex.: 8.º)."
    if (turma.letra.isBlank()) return "Indique a letra da turma (ex.: B)."
    val repetida = existentes.any {
        it.id != turma.id &&
            it.ano.trim().equals(turma.ano.trim(), ignoreCase = true) &&
            it.letra.trim().equals(turma.letra.trim(), ignoreCase = true) &&
            it.disciplina.trim().equals(turma.disciplina.trim(), ignoreCase = true)
    }
    val de = turma.disciplina.trim().takeIf { it.isNotEmpty() }?.let { " de $it" }.orEmpty()
    return if (repetida) "Já existe a turma ${turma.nome.trim()}$de." else null
}

/** O horário sem as aulas da turma [turmaId] (usado ao apagar a turma). */
fun List<Aula>.semTurma(turmaId: String): List<Aula> = filterNot { it.turmaId == turmaId }

/**
 * Atualiza as aulas de uma turma editada: o título e o local acompanham o nome e a sala da turma,
 * mas só quando ainda eram os valores antigos (não estraga um título ou local escrito à mão).
 */
fun List<Aula>.comTurmaAtualizada(antiga: Turma, nova: Turma): List<Aula> = map { aula ->
    if (aula.turmaId != nova.id) aula
    else aula.copy(
        titulo = if (aula.titulo == antiga.nome) nova.nome else aula.titulo,
        local = if (aula.local == antiga.sala) nova.sala else aula.local
    )
}

// ---- Horário da turma (segunda a sexta) ----
// O horário não tem ids: cada aula identifica-se pelos próprios valores (igualdade da data class).
// Duas aulas iguais em tudo não podem existir na mesma turma, porque se sobreporiam.

/** As aulas da turma [turmaId], de segunda a sexta, por ordem de dia e hora. */
fun List<Aula>.daTurma(turmaId: String): List<Aula> =
    filter { it.turmaId == turmaId }.sortedWith(compareBy({ it.diaSemana }, { it.inicioMin }))

/**
 * Valida uma aula do horário da turma antes de gravar. Devolve a mensagem de erro, ou null.
 * [antiga] é a aula que está a ser editada (ignorada na verificação de sobreposição).
 */
fun validarAula(aula: Aula, horario: List<Aula>, antiga: Aula? = null): String? {
    if (aula.diaSemana !in 1..5) return "Escolha um dia de segunda a sexta."
    if (aula.fimMin <= aula.inicioMin) return "A hora de fim tem de ser depois da hora de início."
    val sobreposta = horario.filter { it != antiga }.firstOrNull {
        it.turmaId == aula.turmaId && it.diaSemana == aula.diaSemana &&
            aula.inicioMin < it.fimMin && it.inicioMin < aula.fimMin
    }
    return sobreposta?.let {
        "Já há ${it.disciplina.ifBlank { "uma aula" }} das ${it.inicioMin.comoHora()} às ${it.fimMin.comoHora()} " +
            "nesse dia."
    }
}

/** O horário com [nova] no lugar de [antiga] (ou acrescentada, se [antiga] for null ou já não existir). */
fun List<Aula>.comAula(antiga: Aula?, nova: Aula): List<Aula> {
    val i = if (antiga == null) -1 else indexOf(antiga)
    val lista = if (i >= 0) toMutableList().apply { set(i, nova) } else this + nova
    return lista.sortedWith(compareBy({ it.diaSemana }, { it.inicioMin }))
}

/** O horário sem [aula] (só a primeira igual). */
fun List<Aula>.semAula(aula: Aula): List<Aula> {
    val i = indexOf(aula)
    return if (i < 0) this else toMutableList().apply { removeAt(i) }
}
