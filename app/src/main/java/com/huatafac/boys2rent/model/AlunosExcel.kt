package com.huatafac.boys2rent.model

import java.text.Normalizer
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/*
 * Folha de alunos para exportar e importar (Excel).
 * Cada aluno é identificado pelo N.º na turma: na importação, um N.º que já existe atualiza esse aluno
 * e um N.º novo cria um aluno. Células vazias mantêm o valor atual (não apagam dados).
 * Foto, faltas e notas não vão na folha e ficam como estão.
 */

/** Colunas da folha, pela ordem em que são exportadas. */
enum class ColunaAluno(val cabecalho: String, val largura: Int) {
    NUMERO("N.º", 6),
    NOME("Nome", 32),
    DATA_NASCIMENTO("Data de nascimento (DD/MM/AAAA)", 16),
    PROCESSO("N.º de processo", 14),
    CARTAO_CIDADAO("Cartão de Cidadão", 16),
    NIF("NIF", 12),
    RUA("Morada", 32),
    CODIGO_POSTAL("Código postal", 12),
    LOCALIDADE("Localidade", 18),
    SAUDE("Saúde (alergias e cuidados)", 28),
    OBSERVACOES("Observações", 32),
    EE_NOME("Encarregado de educação", 28),
    EE_PARENTESCO("Parentesco (Mãe, Pai, Avó/Avô, Tutor legal, Outro)", 16),
    EE_TELEMOVEL("Telemóvel do encarregado", 16),
    EE_EMAIL("E-mail do encarregado", 28);

    /** Texto do cabeçalho sem acentos, minúsculas e sem a explicação entre parênteses. */
    internal val chave: String get() = normalizar(cabecalho)
}

/** "Data de Nascimento (DD/MM/AAAA)" → "data de nascimento". */
internal fun normalizar(texto: String): String =
    Normalizer.normalize(texto.substringBefore("(").trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("\\s+"), " ")

/** Linhas da folha (cabeçalho + um aluno por linha, por número) para exportar. */
fun folhaDeAlunos(alunos: List<Aluno>): List<List<String>> {
    val cabecalho = ColunaAluno.entries.map { it.cabecalho }
    val linhas = alunos.sortedBy { it.numero }.map { aluno ->
        val ee = aluno.encarregado
        ColunaAluno.entries.map { coluna ->
            when (coluna) {
                ColunaAluno.NUMERO -> aluno.numero.toString()
                ColunaAluno.NOME -> aluno.nome
                ColunaAluno.DATA_NASCIMENTO -> aluno.dataNascimento
                ColunaAluno.PROCESSO -> aluno.numeroProcesso
                ColunaAluno.CARTAO_CIDADAO -> aluno.cartaoCidadao
                ColunaAluno.NIF -> aluno.nif
                ColunaAluno.RUA -> aluno.morada?.rua.orEmpty()
                ColunaAluno.CODIGO_POSTAL -> aluno.morada?.codigoPostal.orEmpty()
                ColunaAluno.LOCALIDADE -> aluno.morada?.localidade.orEmpty()
                ColunaAluno.SAUDE -> aluno.saude
                ColunaAluno.OBSERVACOES -> aluno.observacoes
                ColunaAluno.EE_NOME -> ee?.nome.orEmpty()
                ColunaAluno.EE_PARENTESCO -> ee?.parentesco?.descricao.orEmpty()
                ColunaAluno.EE_TELEMOVEL -> ee?.telemovel.orEmpty()
                ColunaAluno.EE_EMAIL -> ee?.email.orEmpty()
            }
        }
    }
    return listOf(cabecalho) + linhas
}

/** Resultado de aplicar uma folha aos alunos da turma. Com [erros], nada deve ser gravado. */
data class ResultadoImportacao(
    val alunos: List<Aluno>,
    val novos: Int,
    val atualizados: Int,
    val semAlteracoes: Int,
    val erros: List<String>
)

/**
 * Aplica as [linhas] da folha (a primeira linha com conteúdo é o cabeçalho) aos alunos [existentes].
 * As colunas são reconhecidas pelo nome do cabeçalho, em qualquer ordem; colunas desconhecidas são ignoradas.
 * Devolve a lista final de alunos (por número) e os contadores, ou os erros encontrados (tudo ou nada).
 */
fun aplicarFolhaAlunos(existentes: List<Aluno>, linhas: List<List<String>>): ResultadoImportacao {
    val erros = mutableListOf<String>()
    val iCabecalho = linhas.indexOfFirst { linha -> linha.any { it.isNotBlank() } }
    if (iCabecalho < 0) return ResultadoImportacao(existentes, 0, 0, 0, listOf("A folha está vazia."))

    val porChave = ColunaAluno.entries.associateBy { it.chave }
    val colunas = mutableMapOf<ColunaAluno, Int>()
    linhas[iCabecalho].forEachIndexed { i, texto ->
        val coluna = porChave[normalizar(texto)] ?: return@forEachIndexed
        if (coluna !in colunas) colunas[coluna] = i
    }
    if (ColunaAluno.NUMERO !in colunas) {
        return ResultadoImportacao(
            existentes, 0, 0, 0,
            listOf("A folha não tem a coluna \"${ColunaAluno.NUMERO.cabecalho}\". Exporta a folha da turma e preenche essa.")
        )
    }

    val atuais = existentes.associateBy { it.numero }.toMutableMap()
    val linhaDoNumero = mutableMapOf<Int, Int>()
    var novos = 0
    var atualizados = 0
    var semAlteracoes = 0

    for (i in iCabecalho + 1 until linhas.size) {
        val linha = linhas[i]
        if (linha.all { it.isBlank() }) continue
        val n = i + 1 // número da linha no Excel
        /** Valor da coluna sem espaços à volta, ou null se a coluna não existe ou está vazia. */
        fun valor(coluna: ColunaAluno): String? =
            colunas[coluna]?.let { linha.getOrNull(it) }?.trim()?.takeIf { it.isNotEmpty() }

        val textoNumero = valor(ColunaAluno.NUMERO)
        val numero = textoNumero?.let { lerInteiro(it) }
        if (numero == null || numero <= 0) {
            erros += if (textoNumero == null) "Linha $n: falta o N.º do aluno."
            else "Linha $n: o N.º tem de ser um número inteiro (está \"$textoNumero\")."
            continue
        }
        val anterior = linhaDoNumero[numero]
        if (anterior != null) {
            erros += "Linha $n: o N.º $numero já aparece na linha $anterior."
            continue
        }
        linhaDoNumero[numero] = n

        val base = atuais[numero]
        if (base == null && valor(ColunaAluno.NOME) == null) {
            erros += "Linha $n: falta o nome do aluno n.º $numero."
            continue
        }
        val textoNascimento = valor(ColunaAluno.DATA_NASCIMENTO)
        val nascimento = textoNascimento?.let { lerData(it) }
        if (textoNascimento != null && nascimento == null) {
            erros += "Linha $n: data de nascimento inválida (\"$textoNascimento\"). Usa DD/MM/AAAA."
            continue
        }

        val antes = base ?: Aluno(numero = numero)
        val moradaAntes = antes.morada ?: Morada()
        val rua = valor(ColunaAluno.RUA)
        val codigoPostal = valor(ColunaAluno.CODIGO_POSTAL)
        val localidade = valor(ColunaAluno.LOCALIDADE)
        val morada = if (rua == null && codigoPostal == null && localidade == null) antes.morada
        else moradaAntes.copy(
            rua = rua ?: moradaAntes.rua,
            codigoPostal = codigoPostal ?: moradaAntes.codigoPostal,
            localidade = localidade ?: moradaAntes.localidade
        )

        val eeNome = valor(ColunaAluno.EE_NOME)
        val eeParentesco = valor(ColunaAluno.EE_PARENTESCO)
        val eeTelemovel = valor(ColunaAluno.EE_TELEMOVEL)
        val eeEmail = valor(ColunaAluno.EE_EMAIL)
        val familiares = if (eeNome == null && eeParentesco == null && eeTelemovel == null && eeEmail == null) {
            antes.familiares
        } else {
            val atual = antes.encarregado
            val ee = (atual ?: Familiar(encarregadoEducacao = true)).let {
                it.copy(
                    nome = eeNome ?: it.nome,
                    parentesco = eeParentesco?.let { p -> lerParentesco(p) } ?: it.parentesco,
                    telemovel = eeTelemovel ?: it.telemovel,
                    email = eeEmail ?: it.email
                )
            }
            if (atual == null) antes.familiares + ee
            else antes.familiares.map { if (it === atual) ee else it }
        }

        val depois = antes.copy(
            nome = valor(ColunaAluno.NOME) ?: antes.nome,
            dataNascimento = nascimento ?: antes.dataNascimento,
            numeroProcesso = valor(ColunaAluno.PROCESSO) ?: antes.numeroProcesso,
            cartaoCidadao = valor(ColunaAluno.CARTAO_CIDADAO) ?: antes.cartaoCidadao,
            nif = valor(ColunaAluno.NIF) ?: antes.nif,
            morada = morada,
            saude = valor(ColunaAluno.SAUDE) ?: antes.saude,
            observacoes = valor(ColunaAluno.OBSERVACOES) ?: antes.observacoes,
            familiares = familiares
        )
        when {
            base == null -> novos++
            depois != base -> atualizados++
            else -> semAlteracoes++
        }
        atuais[numero] = depois
    }

    if (erros.isNotEmpty()) return ResultadoImportacao(existentes, 0, 0, 0, erros)
    return ResultadoImportacao(atuais.values.sortedBy { it.numero }, novos, atualizados, semAlteracoes, emptyList())
}

/** "3", "3.0" ou "3,0" → 3. */
private fun lerInteiro(texto: String): Int? {
    val limpo = texto.replace(',', '.')
    limpo.toIntOrNull()?.let { return it }
    val d = limpo.toDoubleOrNull() ?: return null
    return if (d == Math.floor(d) && d in 1.0..1_000_000.0) d.toInt() else null
}

/**
 * Data de nascimento em "DD/MM/AAAA". Aceita D/M/AAAA, DD-MM-AAAA, DD.MM.AAAA, AAAA-MM-DD
 * e o número de série de datas do Excel (quando a célula foi formatada como data).
 */
internal fun lerData(texto: String): String? {
    val t = texto.trim()
    val (dia, mes, ano) = when {
        Regex("""\d{1,2}[/.\-]\d{1,2}[/.\-]\d{4}""").matches(t) ->
            t.split('/', '.', '-').map { it.toInt() }.let { Triple(it[0], it[1], it[2]) }
        Regex("""\d{4}-\d{1,2}-\d{1,2}""").matches(t) ->
            t.split('-').map { it.toInt() }.let { Triple(it[2], it[1], it[0]) }
        Regex("""\d{1,6}(\.0+)?""").matches(t) -> {
            // Série do Excel: dias desde 30/12/1899
            val serie = t.substringBefore('.').toInt()
            if (serie !in 1..2_958_465) return null
            val c = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(1899, Calendar.DECEMBER, 30)
                add(Calendar.DAY_OF_MONTH, serie)
            }
            Triple(c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR))
        }
        else -> return null
    }
    if (ano !in 1900..2100 || mes !in 1..12 || dia !in 1..diasDoMes(ano, mes)) return null
    return "%02d/%02d/%04d".format(dia, mes, ano)
}

/** "Mãe", "mae", "MÃE" → MAE; "Avô" e "Avó" → AVO; desconhecido → OUTRO. */
internal fun lerParentesco(texto: String): Parentesco {
    val t = normalizar(texto)
    return Parentesco.entries.firstOrNull { normalizar(it.descricao) == t || it.name.lowercase() == t }
        ?: when {
            t.startsWith("av") -> Parentesco.AVO
            t.startsWith("tutor") -> Parentesco.TUTOR
            else -> Parentesco.OUTRO
        }
}
