package com.huatafac.sosprof.ui.turmas

import android.app.Application
import java.io.File
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.huatafac.sosprof.data.FotosRepository
import com.huatafac.sosprof.data.TurmaRepository
import com.huatafac.sosprof.model.Aluno
import com.huatafac.sosprof.model.Aula
import com.huatafac.sosprof.model.Turma
import com.huatafac.sosprof.model.validarAula
import com.huatafac.sosprof.model.validarTurma
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface TurmasState {
    data object Loading : TurmasState
    data class Success(
        val turmas: List<Turma> = emptyList(),
        val horario: List<Aula> = emptyList(),
        val message: String? = null
    ) : TurmasState
    data class Error(val message: String) : TurmasState
}

class TurmaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TurmaRepository = TurmaRepository()

    /** Pasta privada da app com as cópias locais das fotos dos alunos ({uid}/{turmaId}/{numero}_{versão}.jpg). */
    private val pastaFotos = File(application.filesDir, "fotos_alunos")

    private val _uiState = MutableStateFlow<TurmasState>(TurmasState.Loading)
    val uiState: StateFlow<TurmasState> = _uiState.asStateFlow()

    fun loadData(uid: String) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            _uiState.value = TurmasState.Loading
            _uiState.value = carregar(uid)
        }
    }

    /** Lê turmas e horário do Realtime DB. Não passa por Loading, para o ecrã não piscar depois de gravar. */
    private suspend fun carregar(uid: String): TurmasState {
        val turmasResult = repository.getTurmas(uid)
        val horarioResult = repository.getHorario(uid)
        return if (turmasResult.isSuccess && horarioResult.isSuccess) {
            val turmas = turmasResult.getOrDefault(emptyList())
            // Mostra já o que há no telemóvel; as fotos que faltam vêm da nuvem a seguir
            sincronizarFotos(uid, turmas)
            TurmasState.Success(
                turmas = repository.prepararFotos(uid, turmas, pastaFotos, rede = false),
                horario = horarioResult.getOrDefault(emptyList())
            )
        } else {
            val errorMsg = turmasResult.exceptionOrNull()?.localizedMessage
                ?: horarioResult.exceptionOrNull()?.localizedMessage
                ?: "Erro ao carregar dados"
            TurmasState.Error("Não foi possível carregar as turmas: $errorMsg")
        }
    }

    /**
     * Descarrega da nuvem as fotos que faltam neste telemóvel e envia as que só existem nele;
     * se alguma coisa mudou, atualiza o ecrã (só se entretanto não tiver mudado de estado).
     */
    private fun sincronizarFotos(uid: String, turmas: List<Turma>) {
        viewModelScope.launch {
            // Espera que o ecrã já tenha as turmas (o carregamento que lançou isto ainda está a terminar)
            withTimeoutOrNull(10_000) { _uiState.first { it is TurmasState.Success } }
            val antes = repository.prepararFotos(uid, turmas, pastaFotos, rede = false)
            val depois = repository.prepararFotos(uid, turmas, pastaFotos, rede = true)
            if (depois == antes) return@launch
            val atual = _uiState.value as? TurmasState.Success ?: return@launch
            if (atual.turmas == antes) _uiState.value = atual.copy(turmas = depois)
        }
    }

    /**
     * Acrescenta ([antiga] = null) ou altera uma aula do horário da turma. Valida antes de gravar
     * (disciplina, horas e sobreposição com outra aula da mesma turma).
     * [onComplete] recebe a mensagem de erro, ou null se gravou.
     */
    fun guardarAula(uid: String, antiga: Aula?, nova: Aula, onComplete: (String?) -> Unit) {
        if (uid.isBlank()) return
        val limpa = nova.copy(disciplina = nova.disciplina.trim(), local = nova.local.trim())
        val horario = (_uiState.value as? TurmasState.Success)?.horario.orEmpty()
        validarAula(limpa, horario, antiga)?.let { erro ->
            onComplete(erro)
            return
        }
        viewModelScope.launch {
            val result = repository.guardarAula(uid, antiga, limpa)
            val horarioNovo = result.getOrNull()
            val estado = _uiState.value
            if (horarioNovo != null) {
                if (estado is TurmasState.Success) _uiState.value = estado.copy(horario = horarioNovo)
                onComplete(null)
            } else {
                onComplete("Erro ao guardar a aula: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun apagarAula(uid: String, aula: Aula, onComplete: (String) -> Unit) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            val result = repository.apagarAula(uid, aula)
            val horarioNovo = result.getOrNull()
            val estado = _uiState.value
            if (horarioNovo != null) {
                if (estado is TurmasState.Success) _uiState.value = estado.copy(horario = horarioNovo)
                onComplete("Aula removida do horário.")
            } else {
                onComplete("Erro ao remover a aula: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    /**
     * Cria (id em branco) ou edita uma turma. Valida antes de gravar (campos obrigatórios e turma repetida).
     * [onComplete] recebe a mensagem final e, em caso de sucesso, o id da turma (null em erro).
     */
    fun guardarTurma(uid: String, turma: Turma, onComplete: (String, String?) -> Unit) {
        if (uid.isBlank()) return
        val limpa = turma.copy(
            ano = turma.ano.trim(),
            letra = turma.letra.trim().uppercase(),
            disciplina = turma.disciplina.trim(),
            sala = turma.sala.trim()
        )
        val existentes = (_uiState.value as? TurmasState.Success)?.turmas.orEmpty()
        validarTurma(limpa, existentes)?.let { erro ->
            onComplete(erro, null)
            return
        }
        val nova = limpa.id.isBlank()
        viewModelScope.launch {
            val result = repository.guardarTurma(uid, limpa)
            if (result.isSuccess) {
                _uiState.value = carregar(uid)
                onComplete(if (nova) "Turma ${limpa.nome} criada." else "Turma ${limpa.nome} guardada.", result.getOrNull())
            } else {
                onComplete("Erro ao guardar a turma: ${result.exceptionOrNull()?.localizedMessage}", null)
            }
        }
    }

    /**
     * Apaga a turma com os alunos, as faltas e as aulas dela no horário; depois apaga as fotos dos alunos
     * (na nuvem e no telemóvel). A mensagem chega logo que os dados são apagados; as fotos vêm a seguir.
     */
    fun deleteTurma(uid: String, turmaId: String, onComplete: (String) -> Unit) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            val result = repository.deleteTurma(uid, turmaId)
            val turma = result.getOrNull()
            if (turma != null) {
                _uiState.value = carregar(uid)
                onComplete("Turma ${turma.nome} apagada.")
                repository.apagarFotosTurma(uid, turma, pastaFotos)
            } else {
                onComplete("Erro ao apagar a turma: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun saveAluno(uid: String, turmaId: String, aluno: Aluno, onComplete: (String) -> Unit) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            val result = repository.saveAluno(uid, turmaId, aluno)
            if (result.isSuccess) {
                _uiState.value = carregar(uid)
                onComplete("Aluno guardado com sucesso!")
            } else {
                onComplete("Erro ao guardar aluno: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun uploadFotoAluno(
        uid: String,
        turmaId: String,
        alunoNumero: Int,
        jpeg: ByteArray,
        onComplete: (String) -> Unit
    ) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            val result = repository.uploadFotoAluno(uid, turmaId, alunoNumero, jpeg, pastaFotos)
            val envio = result.getOrNull()
            if (envio != null) {
                _uiState.value = carregar(uid)
                onComplete(
                    when (envio) {
                        FotosRepository.Envio.ENVIADA -> "Foto guardada."
                        FotosRepository.Envio.PENDENTE -> "Foto guardada. Vai para a nuvem quando houver rede."
                        FotosRepository.Envio.FALHOU ->
                            "Foto guardada só neste telemóvel (a nuvem recusou: ${repository.ultimoErroFotos ?: "erro desconhecido"})."
                    }
                )
            } else {
                onComplete("Erro ao guardar a foto: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    /** Importa a folha de alunos (Excel) para a turma: cria os números novos e atualiza os existentes. */
    fun importarAlunos(uid: String, turmaId: String, linhas: List<List<String>>, onComplete: (String) -> Unit) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            val result = repository.importarAlunos(uid, turmaId, linhas)
            val r = result.getOrNull()
            if (r != null) {
                _uiState.value = carregar(uid)
                val partes = listOfNotNull(
                    r.novos.takeIf { it > 0 }?.let { if (it == 1) "1 aluno novo" else "$it alunos novos" },
                    r.atualizados.takeIf { it > 0 }?.let { if (it == 1) "1 atualizado" else "$it atualizados" }
                )
                onComplete(if (partes.isEmpty()) "Importação feita: nada mudou." else "Importação feita: ${partes.joinToString(", ")}.")
            } else {
                onComplete("Erro ao importar: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun deleteAluno(uid: String, turmaId: String, alunoNumero: Int, onComplete: (String) -> Unit) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            val result = repository.deleteAluno(uid, turmaId, alunoNumero)
            if (result.isSuccess) {
                _uiState.value = carregar(uid)
                onComplete("Aluno removido com sucesso!")
            } else {
                onComplete("Erro ao remover aluno: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun clearMessage() {
        val currentState = _uiState.value
        if (currentState is TurmasState.Success && currentState.message != null) {
            _uiState.value = currentState.copy(message = null)
        }
    }
}
