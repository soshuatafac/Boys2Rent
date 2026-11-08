package com.huatafac.sosprof.data

import com.google.firebase.database.FirebaseDatabase
import com.huatafac.sosprof.model.EntradaAgenda
import kotlinx.coroutines.tasks.await

/** Agenda pessoal do professor em users/{uid}/agenda/{id}, separada do horário das turmas. */
class AgendaRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private fun agendaRef(uid: String) = database.getReference("users").child(uid).child("agenda")

    suspend fun getAgenda(uid: String): Result<List<EntradaAgenda>> = try {
        val snapshot = agendaRef(uid).get().await()
        Result.success(snapshot.children.mapNotNull { child ->
            child.getValue(EntradaAgenda::class.java)?.let { it.copy(id = it.id.ifBlank { child.key.orEmpty() }) }
        })
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Cria (id em branco) ou altera uma entrada. Grava só o nó dessa entrada,
     * para não pisar alterações feitas noutras entradas noutro dispositivo. Devolve a entrada gravada (com id).
     */
    suspend fun guardar(uid: String, entrada: EntradaAgenda): Result<EntradaAgenda> = try {
        val id = entrada.id.ifBlank {
            agendaRef(uid).push().key ?: throw IllegalStateException("Não foi possível gerar o id")
        }
        val gravada = entrada.copy(id = id)
        agendaRef(uid).child(id).setValue(gravada).await()
        Result.success(gravada)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun apagar(uid: String, id: String): Result<Unit> = try {
        require(id.isNotBlank()) { "Entrada sem id" }
        agendaRef(uid).child(id).removeValue().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
