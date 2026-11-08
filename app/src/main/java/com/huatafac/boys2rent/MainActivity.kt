package com.huatafac.boys2rent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huatafac.boys2rent.auth.AuthState
import com.huatafac.boys2rent.auth.AuthViewModel
import com.huatafac.boys2rent.model.Aula
import com.huatafac.boys2rent.model.Turma
import com.huatafac.boys2rent.ui.MainShell
import com.huatafac.boys2rent.ui.agenda.AgendaViewModel
import com.huatafac.boys2rent.ui.login.LoginScreen
import com.huatafac.boys2rent.ui.profile.UserProfileScreen
import com.huatafac.boys2rent.ui.profile.UserProfileViewModel
import com.huatafac.boys2rent.ui.theme.Boys2RentTheme
import com.huatafac.boys2rent.ui.turmas.TurmaViewModel
import com.huatafac.boys2rent.ui.turmas.TurmasState

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val userProfileViewModel: UserProfileViewModel by viewModels()
    private val turmaViewModel: TurmaViewModel by viewModels()
    private val agendaViewModel: AgendaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Boys2RentTheme {
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()
                val email by authViewModel.email.collectAsStateWithLifecycle()
                val password by authViewModel.password.collectAsStateWithLifecycle()
                val isSignUpMode by authViewModel.isSignUpMode.collectAsStateWithLifecycle()
                val webClientId = getString(R.string.default_web_client_id)

                when (val state = authState) {
                    is AuthState.Unauthenticated,
                    is AuthState.Error,
                    is AuthState.Loading -> {
                        LoginScreen(
                            authState = state,
                            email = email,
                            password = password,
                            isSignUpMode = isSignUpMode,
                            onEmailChanged = authViewModel::onEmailChanged,
                            onPasswordChanged = authViewModel::onPasswordChanged,
                            onToggleAuthMode = authViewModel::toggleAuthMode,
                            onSignInWithEmail = authViewModel::signInWithEmail,
                            onSignUpWithEmail = authViewModel::signUpWithEmail,
                            onResendVerificationEmail = authViewModel::resendVerificationEmail,
                            onRecuperarPassword = authViewModel::sendPasswordReset,
                            onSignInWithGoogle = { context ->
                                authViewModel.signInWithGoogle(context, webClientId)
                            },
                            onErrorDismiss = {
                                authViewModel.clearError()
                            }
                        )
                    }
                    is AuthState.Authenticated -> {
                        var currentScreen by rememberSaveable { mutableStateOf("dashboard") }

                        LaunchedEffect(state.user.uid) {
                            userProfileViewModel.loadProfile(state.user.uid, state.user.email ?: "")
                            turmaViewModel.loadData(state.user.uid)
                            agendaViewModel.loadData(state.user.uid)
                        }

                        val fullName by userProfileViewModel.fullName.collectAsStateWithLifecycle()
                        val fotoPerfil by userProfileViewModel.profileImageUri.collectAsStateWithLifecycle()
                        val userName = when {
                            fullName.isNotBlank() -> fullName
                            !state.user.displayName.isNullOrBlank() -> state.user.displayName!!
                            !state.user.email.isNullOrBlank() -> state.user.email!!.substringBefore("@")
                            else -> "Professor"
                        }

                        val turmasState by turmaViewModel.uiState.collectAsStateWithLifecycle()
                        // Em erro mostra-se a lista vazia e a mensagem de erro.
                        val (turmas, horario) = when (val ts = turmasState) {
                            is TurmasState.Success -> ts.turmas to ts.horario
                            else -> emptyList<Turma>() to emptyList<Aula>()
                        }
                        val erroTurmas = (turmasState as? TurmasState.Error)?.message
                        val agendaState by agendaViewModel.uiState.collectAsStateWithLifecycle()

                        BackHandler(enabled = currentScreen == "profile") {
                            currentScreen = "dashboard"
                        }

                        if (currentScreen == "profile") {
                            UserProfileScreen(
                                uid = state.user.uid,
                                userEmail = state.user.email ?: "",
                                viewModel = userProfileViewModel,
                                onBack = { currentScreen = "dashboard" }
                            )
                        } else {
                            MainShell(
                                userName = userName,
                                turmas = turmas,
                                horario = horario,
                                erroCarregamento = erroTurmas ?: agendaState.erro,
                                agenda = agendaState.entradas,
                                onGuardarEntrada = { entrada, terminou ->
                                    agendaViewModel.guardar(state.user.uid, entrada, terminou)
                                },
                                onApagarEntrada = { entrada, avisar ->
                                    agendaViewModel.apagar(state.user.uid, entrada, avisar)
                                },
                                onGuardarAula = { antiga, nova, terminou ->
                                    turmaViewModel.guardarAula(state.user.uid, antiga, nova, terminou)
                                },
                                onApagarAula = { aula, avisar ->
                                    turmaViewModel.apagarAula(state.user.uid, aula, avisar)
                                },
                                onSaveAluno = { turmaId, aluno, avisar ->
                                    turmaViewModel.saveAluno(state.user.uid, turmaId, aluno, avisar)
                                },
                                onDeleteAluno = { turmaId, alunoNumero, avisar ->
                                    turmaViewModel.deleteAluno(state.user.uid, turmaId, alunoNumero, avisar)
                                },
                                onUploadFotoAluno = { turmaId, alunoNumero, jpeg, avisar ->
                                    turmaViewModel.uploadFotoAluno(
                                        state.user.uid, turmaId, alunoNumero, jpeg,
                                        onComplete = avisar
                                    )
                                },
                                onGuardarTurma = { turma, terminou ->
                                    turmaViewModel.guardarTurma(state.user.uid, turma, terminou)
                                },
                                onDeleteTurma = { turmaId, avisar ->
                                    turmaViewModel.deleteTurma(
                                        state.user.uid, turmaId,
                                        onComplete = avisar
                                    )
                                },
                                onImportarAlunos = { turmaId, linhas, avisar ->
                                    turmaViewModel.importarAlunos(state.user.uid, turmaId, linhas, avisar)
                                },
                                fotoPerfil = fotoPerfil,
                                onOpenProfile = { currentScreen = "profile" },
                                onSignOut = { authViewModel.signOut() }
                            )
                        }
                    }
                }
            }
        }
    }
}
