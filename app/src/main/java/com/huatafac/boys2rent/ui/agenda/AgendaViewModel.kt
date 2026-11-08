package com.huatafac.boys2rent.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huatafac.boys2rent.data.AgendaRepository
import com.huatafac.boys2rent.model.EntradaAgenda
import com.huatafac.boys2rent.model.comEntrada
import com.huatafac.boys2rent.model.validarEntrada
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Entradas da agenda pessoal e o erro de carregamento, se houver. */
data class AgendaState(
    val entradas: List<EntradaAgenda> = emptyList(),
    val erro: String? = null
)

class AgendaViewModel(
    private val repository: AgendaRepository = AgendaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgendaState())
    val uiState: StateFlow<AgendaState> = _uiState.asStateFlow()

    fun loadData(uid: String) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            val result = repository.getAgenda(uid)
            _uiState.value = result.fold(
                onSuccess = { AgendaState(entradas = it) },
                onFailure = { AgendaState(erro = "Não foi possível carregar a agenda: ${it.localizedMessage}") }
            )
        }
    }

    /** Cria ou altera uma entrada. [onComplete] recebe a mensagem de erro, ou null se gravou. */
    fun guardar(uid: String, entrada: EntradaAgenda, onComplete: (String?) -> Unit) {
        if (uid.isBlank()) return
        val limpa = entrada.copy(titulo = entrada.titulo.trim(), texto = entrada.texto.trim())
        validarEntrada(limpa)?.let { erro ->
            onComplete(erro)
            return
        }
        viewModelScope.launch {
            val result = repository.guardar(uid, limpa)
            val gravada = result.getOrNull()
            if (gravada != null) {
                _uiState.value = _uiState.value.copy(entradas = _uiState.value.entradas.comEntrada(gravada))
                onComplete(null)
            } else {
                onComplete("Erro ao guardar: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun apagar(uid: String, entrada: EntradaAgenda, onComplete: (String) -> Unit) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            val result = repository.apagar(uid, entrada.id)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(entradas = _uiState.value.entradas.filterNot { it.id == entrada.id })
                onComplete("Entrada apagada da agenda.")
            } else {
                onComplete("Erro ao apagar: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }
}
