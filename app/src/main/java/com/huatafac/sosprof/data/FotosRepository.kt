package com.huatafac.sosprof.data

import android.util.Base64
import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Fotos na nuvem, na Realtime Database (funciona no plano gratuito Spark; o Firebase Storage exige o Blaze).
 *
 * Ficam em fotos/{uid}/…, FORA de users/{uid}: o perfil é lido do nó users/{uid} inteiro e, se as fotos
 * estivessem lá dentro, seriam descarregadas sempre que se abre a app.
 * - fotos/{uid}/alunos/{turmaId}/{numero} = { jpeg: base64, atualizada: millis }
 * - fotos/{uid}/perfil = { jpeg: base64, atualizada: millis }
 * As fotos já chegam reduzidas (≈ 600 px, poucas dezenas de KB). O telemóvel guarda sempre uma cópia
 * local e só descarrega da nuvem quando não a tem (ex.: noutro telemóvel ou depois de reinstalar).
 */
class FotosRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    /** Foto descarregada: bytes do JPEG e a versão ([atualizada], em millis). */
    class FotoNuvem(val jpeg: ByteArray, val atualizada: Long)

    /** Como correu o envio. */
    enum class Envio {
        /** Gravada na nuvem. */
        ENVIADA,

        /** Sem resposta a tempo (ex.: sem rede): o Firebase envia sozinho quando houver ligação. */
        PENDENTE,

        /** Recusada (ex.: regras por publicar): fica só no telemóvel e tenta-se outra vez mais tarde. */
        FALHOU
    }

    /** Motivo do último envio recusado (para mostrar ao utilizador e no Logcat, etiqueta "SOSProf"). */
    @Volatile
    var ultimoErro: String? = null
        private set

    private fun raiz(uid: String) = database.getReference("fotos").child(uid)
    private fun refAluno(uid: String, turmaId: String, numero: Int) =
        raiz(uid).child("alunos").child(turmaId).child(numero.toString())
    private fun refPerfil(uid: String) = raiz(uid).child("perfil")

    suspend fun enviarAluno(uid: String, turmaId: String, numero: Int, jpeg: ByteArray, atualizada: Long): Envio =
        enviar(refAluno(uid, turmaId, numero), jpeg, atualizada)

    suspend fun descarregarAluno(uid: String, turmaId: String, numero: Int): FotoNuvem? =
        descarregar(refAluno(uid, turmaId, numero))

    suspend fun apagarAluno(uid: String, turmaId: String, numero: Int) = apagar(refAluno(uid, turmaId, numero))

    suspend fun apagarTurma(uid: String, turmaId: String) = apagar(raiz(uid).child("alunos").child(turmaId))

    suspend fun enviarPerfil(uid: String, jpeg: ByteArray, atualizada: Long): Envio = enviar(refPerfil(uid), jpeg, atualizada)

    suspend fun descarregarPerfil(uid: String): FotoNuvem? = descarregar(refPerfil(uid))

    private suspend fun enviar(ref: DatabaseReference, jpeg: ByteArray, atualizada: Long): Envio = try {
        val dados = mapOf("jpeg" to Base64.encodeToString(jpeg, Base64.NO_WRAP), "atualizada" to atualizada)
        // Sem rede a escrita fica em fila no Firebase e só termina mais tarde: não se espera por ela
        val r = withTimeoutOrNull(TEMPO_ENVIO_MS) { ref.setValue(dados).await() }?.let { Envio.ENVIADA } ?: Envio.PENDENTE
        Log.i(TAG, "Foto ${ref.path} (${jpeg.size} bytes): $r")
        r
    } catch (e: Exception) {
        ultimoErro = e.localizedMessage ?: e.javaClass.simpleName
        Log.w(TAG, "Foto ${ref.path} recusada", e)
        Envio.FALHOU
    }

    private suspend fun descarregar(ref: DatabaseReference): FotoNuvem? = try {
        withTimeoutOrNull(TEMPO_LEITURA_MS) {
            val snapshot = ref.get().await()
            val texto = snapshot.child("jpeg").getValue(String::class.java)
            if (texto.isNullOrBlank()) null
            else FotoNuvem(Base64.decode(texto, Base64.NO_WRAP), snapshot.child("atualizada").getValue(Long::class.java) ?: 0L)
        }
    } catch (e: Exception) {
        Log.w(TAG, "Foto ${ref.path} não descarregada", e)
        null
    }

    /** Apagar é "melhor esforço": se falhar, a foto fica órfã na nuvem mas não aparece em lado nenhum. */
    private suspend fun apagar(ref: DatabaseReference) {
        try {
            withTimeoutOrNull(TEMPO_ENVIO_MS) { ref.removeValue().await() }
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val TEMPO_ENVIO_MS = 20_000L
        const val TEMPO_LEITURA_MS = 15_000L
        const val TAG = "SOSProf"
    }
}
