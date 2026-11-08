package com.huatafac.boys2rent.model

import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * O Firebase Realtime DB só consegue ler (getValue) classes com construtor sem argumentos.
 * Sem ele, a leitura lança DatabaseException e a app fica sem turmas.
 */
class FirebaseModelsTest {
    @Test
    fun modelsSavedInRealtimeDb_haveNoArgConstructor() {
        listOf(
            Turma::class.java,
            Aluno::class.java,
            Morada::class.java,
            Familiar::class.java,
            NotaAluno::class.java,
            Aula::class.java,
            EntradaAgenda::class.java,
            UserProfile::class.java
        ).forEach { classe ->
            assertNotNull("${classe.simpleName} precisa de construtor sem argumentos", classe.getDeclaredConstructor())
        }
    }
}
