package com.huatafac.sosprof.data

import com.google.firebase.database.FirebaseDatabase
import android.net.Uri
import java.io.File
import com.huatafac.sosprof.model.Aluno
import com.huatafac.sosprof.model.Aula
import com.huatafac.sosprof.model.ResultadoImportacao
import com.huatafac.sosprof.model.Turma
import com.huatafac.sosprof.model.aplicarFolhaAlunos
import com.huatafac.sosprof.model.comAula
import com.huatafac.sosprof.model.comTurmaAtualizada
import com.huatafac.sosprof.model.semAula
import com.huatafac.sosprof.model.semTurma
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TurmaRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val fotos: FotosRepository = FotosRepository(database)
) {
    /** Fotos que já se tentou descarregar ou enviar nesta sessão (para não repetir a cada carregamento). */
    /** Motivo do último envio de foto recusado. */
    val ultimoErroFotos: String? get() = fotos.ultimoErro

    private val fotosTentadas: MutableSet<String> = java.util.Collections.synchronizedSet(mutableSetOf())

    private fun getUserTurmasRef(uid: String) = database.getReference("users").child(uid).child("turmas")
    private fun getUserHorarioRef(uid: String) = database.getReference("users").child(uid).child("horario")

    private fun getUserRef(uid: String) = database.getReference("users").child(uid)

    /**
     * Versões antigas da app gravavam turmas e horário de exemplo na primeira utilização e marcavam a conta
     * com [MARCA_EXEMPLOS]. Nessas contas apaga, uma única vez, o que ainda é exemplo:
     * - as turmas de exemplo (ids fixos 7A, 8B, 9C) cujos alunos ainda são todos os de exemplo;
     *   se o professor lá pôs alunos reais, a turma fica;
     * - as aulas dessas turmas apagadas e as duas aulas de exemplo sem turma ("Apoio" e "Reunião").
     * Tudo numa só escrita, que também remove a marca, para não voltar a correr.
     */
    private suspend fun limparExemplosAntigos(uid: String) {
        val userRef = getUserRef(uid)
        if (!userRef.child(MARCA_EXEMPLOS).get().await().exists()) return
        val updates = mutableMapOf<String, Any?>(MARCA_EXEMPLOS to null)
        val apagadas = mutableSetOf<String>()
        for ((id, nomes) in TURMAS_EXEMPLO) {
            val turma = getTurma(uid, id) ?: continue
            if (turma.alunos.all { it.nome in nomes }) {
                updates["turmas/$id"] = null
                apagadas += id
            }
        }
        val horario = lerHorario(uid)
        val novoHorario = horario.filterNot { aula ->
            aula.turmaId in apagadas ||
                (aula.turmaId == null && AULAS_EXEMPLO_SEM_TURMA.any { (dia, inicio, titulo, local) ->
                    aula.diaSemana == dia && aula.inicioMin == inicio && aula.titulo == titulo && aula.local == local
                })
        }
        if (novoHorario != horario) updates["horario"] = novoHorario
        userRef.updateChildren(updates).await()
    }

    suspend fun getTurmas(uid: String): Result<List<Turma>> {
        return try {
            // Se a limpeza falhar (ex.: sem rede), as turmas carregam na mesma e tenta-se da próxima vez
            try {
                limparExemplosAntigos(uid)
            } catch (_: Exception) {
            }
            val snapshot = getUserTurmasRef(uid).get().await()
            val list = mutableListOf<Turma>()
            for (child in snapshot.children) {
                child.getValue(Turma::class.java)?.let { list.add(it.copy(id = it.id.ifBlank { child.key.orEmpty() })) }
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cria uma turma nova (id em branco) ou altera os dados de uma existente.
     * Ao editar, só mudam os dados da turma: os alunos ficam como estão na base de dados,
     * e as aulas do horário dessa turma acompanham o novo nome e a nova sala.
     * Turma e horário são gravados numa só escrita (tudo ou nada). Devolve o id da turma.
     */
    suspend fun guardarTurma(uid: String, turma: Turma): Result<String> {
        return try {
            val userRef = getUserRef(uid)
            if (turma.id.isBlank()) {
                val id = getUserTurmasRef(uid).push().key
                    ?: return Result.failure(IllegalStateException("Não foi possível gerar o id da turma"))
                getUserTurmasRef(uid).child(id).setValue(turma.copy(id = id, alunos = emptyList())).await()
                Result.success(id)
            } else {
                val antiga = getTurma(uid, turma.id)
                    ?: return Result.failure(IllegalStateException("Turma não encontrada"))
                val nova = antiga.copy(
                    ano = turma.ano,
                    letra = turma.letra,
                    disciplina = turma.disciplina,
                    sala = turma.sala,
                    corHex = turma.corHex,
                    diretorTurma = turma.diretorTurma,
                    id = turma.id
                )
                val horario = lerHorario(uid)
                val updates = mutableMapOf<String, Any?>("turmas/${turma.id}" to nova)
                val horarioNovo = horario.comTurmaAtualizada(antiga, nova)
                if (horarioNovo != horario) updates["horario"] = horarioNovo
                userRef.updateChildren(updates).await()
                Result.success(turma.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Apaga a turma e tudo o que é dela: os alunos (com as fichas e as faltas, que estão dentro da turma)
     * e as aulas dessa turma no horário. As duas coisas vão numa só escrita, para não ficar meio apagado.
     * Devolve a turma apagada, para o chamador poder apagar as fotos depois ([apagarFotosTurma]).
     */
    suspend fun deleteTurma(uid: String, turmaId: String): Result<Turma> {
        if (turmaId.isBlank()) return Result.failure(IllegalArgumentException("Turma sem id"))
        return try {
            val turma = getTurma(uid, turmaId)
                ?: return Result.failure(IllegalStateException("Turma não encontrada"))
            val horario = lerHorario(uid)
            val updates = mutableMapOf<String, Any?>(
                "turmas/$turmaId" to null,
                "horario" to horario.semTurma(turmaId)
            )
            getUserRef(uid).updateChildren(updates).await()
            Result.success(turma)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Apaga as fotos dos alunos de uma turma já apagada: na nuvem (fotos/{uid}/alunos/{turmaId})
     * e no dispositivo (pasta [pastaLocal]/{uid}/{turmaId}).
     * Falhas são ignoradas, porque os dados da turma já foram apagados.
     */
    suspend fun apagarFotosTurma(uid: String, turma: Turma, pastaLocal: File) {
        fotos.apagarTurma(uid, turma.id)
        turma.alunos.map { it.fotoUrl }.filter { it.isNotBlank() }.forEach { apagarFoto(it) }
        try {
            File(pastaLocal, "$uid/${turma.id}").deleteRecursively()
        } catch (_: Exception) {
        }
    }

    private suspend fun lerHorario(uid: String): List<Aula> =
        getUserHorarioRef(uid).get().await().children.mapNotNull { it.getValue(Aula::class.java) }

    suspend fun saveTurma(uid: String, turma: Turma): Result<Unit> {
        return try {
            val id = turma.id.ifBlank { java.util.UUID.randomUUID().toString() }
            getUserTurmasRef(uid).child(id).setValue(turma).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getTurma(uid: String, turmaId: String): Turma? =
        getUserTurmasRef(uid).child(turmaId).get().await().getValue(Turma::class.java)

    /**
     * Lê a turma, aplica [alteracao] e grava só essa turma.
     * Gravar só o nó da turma evita que uma edição apague alterações feitas noutras turmas
     * (por exemplo, noutro dispositivo) entre a leitura e a escrita.
     */
    private suspend fun alterarTurma(uid: String, turmaId: String, alteracao: (Turma) -> Turma): Result<Unit> {
        return try {
            val turma = getTurma(uid, turmaId)
                ?: return Result.failure(IllegalStateException("Turma não encontrada"))
            saveTurma(uid, alteracao(turma).copy(id = turmaId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveAluno(uid: String, turmaId: String, aluno: Aluno): Result<Unit> =
        alterarTurma(uid, turmaId) { turma ->
            turma.copy(alunos = (turma.alunos.filterNot { it.numero == aluno.numero } + aluno).sortedBy { it.numero })
        }

    /**
     * Aplica a folha de alunos (Excel) à turma e grava só essa turma. A folha é aplicada aos alunos
     * lidos agora da base de dados, para não pisar alterações feitas entretanto.
     * Se a folha tiver erros, não grava nada e devolve falha com os erros.
     */
    suspend fun importarAlunos(uid: String, turmaId: String, linhas: List<List<String>>): Result<ResultadoImportacao> {
        var resultado: ResultadoImportacao? = null
        val gravado = alterarTurma(uid, turmaId) { turma ->
            val r = aplicarFolhaAlunos(turma.alunos, linhas)
            resultado = r
            if (r.erros.isEmpty()) turma.copy(alunos = r.alunos) else turma
        }
        val r = resultado
        return when {
            gravado.isFailure -> Result.failure(gravado.exceptionOrNull() ?: IllegalStateException("Erro ao gravar"))
            r == null -> Result.failure(IllegalStateException("Turma não encontrada"))
            r.erros.isNotEmpty() -> Result.failure(IllegalArgumentException(r.erros.joinToString("\n")))
            else -> Result.success(r)
        }
    }

    suspend fun deleteAluno(uid: String, turmaId: String, alunoNumero: Int): Result<Unit> {
        val fotoUrl = try {
            getTurma(uid, turmaId)?.alunos?.firstOrNull { it.numero == alunoNumero }?.fotoUrl
        } catch (_: Exception) {
            null
        }
        val result = alterarTurma(uid, turmaId) { turma ->
            turma.copy(alunos = turma.alunos.filterNot { it.numero == alunoNumero })
        }
        if (result.isSuccess) {
            if (!fotoUrl.isNullOrBlank()) apagarFoto(fotoUrl)
            fotos.apagarAluno(uid, turmaId, alunoNumero)
        }
        return result
    }

    /**
     * Guarda a foto do aluno (JPEG já reduzido) no telemóvel, em [pastaLocal] (filesDir), e envia-a para a
     * nuvem (Realtime Database, fotos/{uid}/alunos/{turmaId}/{numero}). Grava em Aluno.fotoUrl o URI local
     * e em Aluno.fotoAtualizada a versão (millis) que está na nuvem — 0 se o envio foi recusado, para se
     * tentar outra vez mais tarde. Cada foto usa um nome novo ("{numero}_{versão}.jpg"), para a app não
     * mostrar a antiga em cache; a anterior é apagada no fim.
     * Devolve como correu o envio para a nuvem.
     */
    suspend fun uploadFotoAluno(
        uid: String,
        turmaId: String,
        alunoNumero: Int,
        jpeg: ByteArray,
        pastaLocal: File
    ): Result<FotosRepository.Envio> {
        val versao = System.currentTimeMillis()
        val nome = "${alunoNumero}_$versao.jpg"
        val url = try {
            guardarLocal(pastaLocal, uid, turmaId, nome, jpeg)
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val envio = fotos.enviarAluno(uid, turmaId, alunoNumero, jpeg, versao)
        val versaoNaNuvem = if (envio == FotosRepository.Envio.FALHOU) 0L else versao
        return try {
            var anterior = ""
            val result = alterarTurma(uid, turmaId) { turma ->
                turma.copy(alunos = turma.alunos.map { aluno ->
                    if (aluno.numero == alunoNumero) {
                        anterior = aluno.fotoUrl
                        aluno.copy(fotoUrl = url, fotoAtualizada = versaoNaNuvem)
                    } else aluno
                })
            }
            if (result.isSuccess) {
                if (anterior.isNotBlank() && anterior != url) apagarFoto(anterior)
            } else {
                // Não ficou associada a nenhum aluno: não deixa o ficheiro órfão
                apagarFoto(url)
            }
            result.map { envio }
        } catch (e: Exception) {
            apagarFoto(url)
            Result.failure(e)
        }
    }

    /**
     * Prepara as fotos dos alunos para mostrar neste telemóvel: Aluno.fotoUrl passa a apontar para a cópia
     * local da versão [Aluno.fotoAtualizada] ("" se não houver cópia).
     * Com [rede]:
     * - descarrega da nuvem as fotos que faltam no telemóvel (ex.: tiradas noutro telemóvel);
     * - envia para a nuvem as fotos que só existiam no telemóvel (versões antigas da app, ou envio recusado).
     * Cada foto só é tentada uma vez por sessão. Nunca lança exceção.
     */
    suspend fun prepararFotos(uid: String, turmas: List<Turma>, pastaLocal: File, rede: Boolean): List<Turma> =
        withContext(Dispatchers.IO) {
            turmas.map { turma ->
                turma.copy(alunos = turma.alunos.map { aluno ->
                    try {
                        prepararFoto(uid, turma.id, aluno, pastaLocal, rede)
                    } catch (_: Exception) {
                        aluno
                    }
                })
            }
        }

    private suspend fun prepararFoto(uid: String, turmaId: String, aluno: Aluno, pastaLocal: File, rede: Boolean): Aluno {
        val pasta = File(pastaLocal, "$uid/$turmaId")
        val chave = "$uid/$turmaId/${aluno.numero}"
        if (aluno.fotoAtualizada > 0) {
            val local = File(pasta, "${aluno.numero}_${aluno.fotoAtualizada}.jpg")
            if (local.exists()) return aluno.copy(fotoUrl = Uri.fromFile(local).toString())
            if (!rede || !fotosTentadas.add("baixar/$chave")) return aluno.copy(fotoUrl = "")
            val nuvem = fotos.descarregarAluno(uid, turmaId, aluno.numero) ?: return aluno.copy(fotoUrl = "")
            val nome = "${aluno.numero}_${aluno.fotoAtualizada}.jpg"
            apagarFotosLocaisDoAluno(pasta, aluno.numero)
            return aluno.copy(fotoUrl = guardarLocal(pastaLocal, uid, turmaId, nome, nuvem.jpeg))
        }
        // Foto que só existe neste telemóvel: envia-a para a nuvem
        val ficheiro = aluno.fotoUrl.takeIf { it.startsWith("file:") }?.let { Uri.parse(it).path }?.let { File(it) }
        if (ficheiro == null || !ficheiro.exists()) return aluno.copy(fotoUrl = "")
        if (!rede || !fotosTentadas.add("enviar/$chave")) return aluno
        val versao = ficheiro.nameWithoutExtension.substringAfter('_', "").toLongOrNull()?.takeIf { it > 0 }
            ?: System.currentTimeMillis()
        val envio = fotos.enviarAluno(uid, turmaId, aluno.numero, ficheiro.readBytes(), versao)
        if (envio == FotosRepository.Envio.FALHOU) return aluno
        val esperado = File(pasta, "${aluno.numero}_$versao.jpg")
        if (ficheiro.absolutePath != esperado.absolutePath) ficheiro.copyTo(esperado, overwrite = true)
        val url = Uri.fromFile(esperado).toString()
        alterarTurma(uid, turmaId) { t ->
            t.copy(alunos = t.alunos.map {
                if (it.numero == aluno.numero && it.fotoAtualizada == 0L) it.copy(fotoUrl = url, fotoAtualizada = versao) else it
            })
        }
        return aluno.copy(fotoUrl = url, fotoAtualizada = versao)
    }

    /** Apaga as cópias locais antigas da foto do aluno n.º [numero] (ficheiros "{numero}_*.jpg"). */
    private fun apagarFotosLocaisDoAluno(pasta: File, numero: Int) {
        try {
            pasta.listFiles { f -> f.name.startsWith("${numero}_") }?.forEach { it.delete() }
        } catch (_: Exception) {
        }
    }

    private fun guardarLocal(pastaLocal: File, uid: String, turmaId: String, nome: String, jpeg: ByteArray): String {
        // Pasta privada da app (filesDir): não vai para a galeria e não é apagada como a cache
        val pasta = File(pastaLocal, "$uid/$turmaId").apply { mkdirs() }
        val ficheiro = File(pasta, nome)
        ficheiro.writeBytes(jpeg)
        return Uri.fromFile(ficheiro).toString()
    }

    /**
     * Apaga uma foto guardada no dispositivo pelo URI file://; falhas são ignoradas (ex.: já apagada).
     * Endereços antigos do Firebase Storage (já sem acesso no plano Spark) não são ficheiros locais e ficam como estão.
     */
    private fun apagarFoto(url: String) {
        try {
            if (url.startsWith("file:")) {
                Uri.parse(url).path?.let { File(it).delete() }
            }
        } catch (_: Exception) {
        }
    }

    suspend fun getHorario(uid: String): Result<List<Aula>> {
        return try {
            Result.success(lerHorario(uid))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveHorario(uid: String, horario: List<Aula>): Result<Unit> {
        return try {
            getUserHorarioRef(uid).setValue(horario).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Acrescenta ([antiga] = null) ou altera uma aula do horário. Lê o horário atual da base de dados
     * antes de gravar, para não perder alterações feitas noutro dispositivo. Devolve o horário gravado.
     */
    suspend fun guardarAula(uid: String, antiga: Aula?, nova: Aula): Result<List<Aula>> {
        return try {
            val horario = lerHorario(uid).comAula(antiga, nova)
            getUserHorarioRef(uid).setValue(horario).await()
            Result.success(horario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Remove uma aula do horário. Devolve o horário gravado. */
    suspend fun apagarAula(uid: String, aula: Aula): Result<List<Aula>> {
        return try {
            val horario = lerHorario(uid).semAula(aula)
            getUserHorarioRef(uid).setValue(horario).await()
            Result.success(horario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class AulaExemplo(val dia: Int, val inicio: Int, val titulo: String, val local: String)

    private companion object {
        /** users/{uid}/exemplosCriados: marca deixada pelas versões que gravavam dados de exemplo. */
        const val MARCA_EXEMPLOS = "exemplosCriados"

        /** Turmas de exemplo dessas versões (id fixo) e os nomes dos alunos de exemplo de cada uma. */
        val TURMAS_EXEMPLO = mapOf(
            "7A" to setOf("Ana Rodrigues", "Bruno Costa", "Inês Almeida", "João Pereira", "Marta Sousa"),
            "8B" to setOf("Carolina Silva", "Diogo Ferreira", "Eva Martins", "Francisco Lopes", "Gonçalo Ribeiro", "Helena Duarte"),
            "9C" to setOf("Leonor Gomes", "Miguel Santos", "Rita Carvalho", "Tiago Oliveira")
        )

        /** Aulas de exemplo sem turma: dia, início (minutos), título e local. */
        val AULAS_EXEMPLO_SEM_TURMA = listOf(
            AulaExemplo(2, 16 * 60 + 30, "Apoio", "Biblioteca"),
            AulaExemplo(4, 16 * 60 + 30, "Reunião", "Departamento")
        )
    }
}
