package com.huatafac.sosprof.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.huatafac.sosprof.model.Aluno
import com.huatafac.sosprof.model.Aula
import com.huatafac.sosprof.model.EntradaAgenda
import com.huatafac.sosprof.model.Turma
import com.huatafac.sosprof.ui.agenda.AgendaScreen
import com.huatafac.sosprof.ui.agenda.EntradaAgendaDialog
import com.huatafac.sosprof.ui.home.HomeScreen
import com.huatafac.sosprof.ui.theme.SOSProfTheme
import com.huatafac.sosprof.ui.theme.SosBlueDark
import com.huatafac.sosprof.ui.theme.SosBlueLight
import com.huatafac.sosprof.ui.theme.SosMuted
import com.huatafac.sosprof.ui.turmas.AlunoScreen
import com.huatafac.sosprof.ui.turmas.ApagarTurmaDialog
import com.huatafac.sosprof.ui.turmas.TurmaFormDialog
import com.huatafac.sosprof.ui.turmas.TurmaDetailScreen
import com.huatafac.sosprof.ui.turmas.TurmasScreen
import kotlinx.coroutines.launch

enum class MainTab(val label: String, val icon: ImageVector) {
    HOJE("Hoje", Icons.Rounded.Home),
    AGENDA("Agenda", Icons.Rounded.CalendarMonth),
    TURMAS("Turmas", Icons.Rounded.Groups)
}

/** Estrutura principal depois do login: barra de navegação em baixo + ecrãs. */
@Composable
fun MainShell(
    userName: String,
    turmas: List<Turma>,
    horario: List<Aula>,
    // O último parâmetro de cada callback recebe a mensagem final (sucesso ou erro) para mostrar ao utilizador.
    onSaveAluno: (String, Aluno, (String) -> Unit) -> Unit,
    onDeleteAluno: (String, Int, (String) -> Unit) -> Unit,
    onOpenProfile: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    erroCarregamento: String? = null,
    // turmaId, número do aluno, JPEG, callback com a mensagem final
    onUploadFotoAluno: (String, Int, ByteArray, (String) -> Unit) -> Unit = { _, _, _, _ -> },
    // Turma a criar (id em branco) ou editar; callback com a mensagem e o id da turma (null se falhou)
    onGuardarTurma: (Turma, (String, String?) -> Unit) -> Unit = { _, cb -> cb("Indisponível", null) },
    // turmaId, callback com a mensagem final
    onDeleteTurma: (String, (String) -> Unit) -> Unit = { _, _ -> },
    // Aula antiga (null = nova), aula nova, callback com a mensagem de erro (null se gravou)
    onGuardarAula: (Aula?, Aula, (String?) -> Unit) -> Unit = { _, _, cb -> cb("Indisponível") },
    onApagarAula: (Aula, (String) -> Unit) -> Unit = { _, _ -> },
    // Agenda pessoal (reuniões, atividades, notas), separada do horário das turmas
    agenda: List<EntradaAgenda> = emptyList(),
    // Entrada a criar (id em branco) ou editar; callback com a mensagem de erro (null se gravou)
    onGuardarEntrada: (EntradaAgenda, (String?) -> Unit) -> Unit = { _, cb -> cb("Indisponível") },
    onApagarEntrada: (EntradaAgenda, (String) -> Unit) -> Unit = { _, _ -> },
    // turmaId, linhas da folha Excel, callback com a mensagem final
    onImportarAlunos: (String, List<List<String>>, (String) -> Unit) -> Unit = { _, _, cb -> cb("Indisponível") },
    // URI da foto do perfil, para o ecrã Hoje
    fotoPerfil: String = ""
) {
    var tab by rememberSaveable { mutableStateOf(MainTab.HOJE) }
    var turmaAbertaId by rememberSaveable { mutableStateOf<String?>(null) }
    var alunoAbertoNumero by rememberSaveable { mutableStateOf<Int?>(null) }
    // Separador da turma aberta (0 = Horário, 1 = Alunos); fica guardado enquanto se vê a ficha de um aluno
    var separadorTurma by rememberSaveable { mutableIntStateOf(0) }
    // null = fechado; Turma() = nova turma; turma existente = editar
    var turmaEmEdicao by remember { mutableStateOf<Turma?>(null) }
    var turmaAApagar by remember { mutableStateOf<Turma?>(null) }
    // null = fechado; id em branco = nova entrada da agenda
    var entradaEmEdicao by remember { mutableStateOf<EntradaAgenda?>(null) }
    val novaEntrada: (String) -> Unit = { data -> entradaEmEdicao = EntradaAgenda(data = data) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val avisar: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }

    LaunchedEffect(erroCarregamento) {
        erroCarregamento?.let { snackbar.showSnackbar(it) }
    }

    val abrirTurma: (String) -> Unit = { id ->
        tab = MainTab.TURMAS
        turmaAbertaId = id
        alunoAbertoNumero = null
        separadorTurma = 0
    }
    // Abre logo a ficha do aluno (ex.: ao tocar num aniversário)
    val abrirAluno: (String, Int) -> Unit = { turmaId, numero ->
        tab = MainTab.TURMAS
        turmaAbertaId = turmaId
        alunoAbertoNumero = numero
        // Ao voltar da ficha, fica na lista de alunos da turma
        separadorTurma = 1
    }

    BackHandler(enabled = turmaAbertaId != null || tab != MainTab.HOJE) {
        when {
            alunoAbertoNumero != null -> alunoAbertoNumero = null
            turmaAbertaId != null -> turmaAbertaId = null
            else -> tab = MainTab.HOJE
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                MainTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = {
                            // Tocar outra vez em Turmas volta à lista
                            if (item == MainTab.TURMAS || item != tab) {
                                turmaAbertaId = null
                                alunoAbertoNumero = null
                            }
                            tab = item
                        },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label, fontWeight = if (tab == item) FontWeight.SemiBold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SosBlueDark,
                            selectedTextColor = SosBlueDark,
                            indicatorColor = SosBlueLight,
                            unselectedIconColor = SosMuted,
                            unselectedTextColor = SosMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            val turmaAberta = turmas.firstOrNull { it.id == turmaAbertaId }
            val alunoAberto = turmaAberta?.alunos?.firstOrNull { it.numero == alunoAbertoNumero }
            when {
                tab == MainTab.TURMAS && turmaAberta != null && alunoAberto != null -> AlunoScreen(
                    aluno = alunoAberto,
                    turma = turmaAberta,
                    onBack = { alunoAbertoNumero = null },
                    onSaveAluno = { updatedAluno ->
                        onSaveAluno(turmaAberta.id, updatedAluno, avisar)
                    },
                    onDeleteAluno = {
                        onDeleteAluno(turmaAberta.id, alunoAberto.numero, avisar)
                        alunoAbertoNumero = null
                    },
                    onUploadFoto = { jpeg, terminou ->
                        onUploadFotoAluno(turmaAberta.id, alunoAberto.numero, jpeg) { msg ->
                            terminou()
                            avisar(msg)
                        }
                    }
                )
                tab == MainTab.TURMAS && turmaAberta != null -> TurmaDetailScreen(
                    turma = turmaAberta,
                    horario = horario,
                    onBack = { turmaAbertaId = null },
                    onOpenAluno = { alunoAbertoNumero = it },
                    separador = separadorTurma,
                    onSeparadorChange = { separadorTurma = it },
                    onGuardarAula = { antiga, nova, terminou ->
                        onGuardarAula(antiga, nova) { erro ->
                            terminou(erro)
                            if (erro == null) avisar(if (antiga == null) "Aula acrescentada ao horário." else "Aula guardada.")
                        }
                    },
                    onApagarAula = { aula -> onApagarAula(aula, avisar) },
                    onAddAluno = { novoAluno ->
                        onSaveAluno(turmaAberta.id, novoAluno, avisar)
                    },
                    onEditarTurma = { turmaEmEdicao = turmaAberta },
                    onApagarTurma = { turmaAApagar = turmaAberta },
                    onImportarAlunos = { linhas -> onImportarAlunos(turmaAberta.id, linhas, avisar) },
                    onAviso = avisar
                )
                tab == MainTab.TURMAS -> TurmasScreen(
                    turmas = turmas,
                    horario = horario,
                    onOpenTurma = abrirTurma,
                    onNovaTurma = { turmaEmEdicao = Turma() }
                )
                tab == MainTab.AGENDA -> AgendaScreen(
                    turmas = turmas,
                    agenda = agenda,
                    onAddEntrada = novaEntrada,
                    onOpenEntrada = { entradaEmEdicao = it },
                    onOpenTurma = abrirTurma,
                    onOpenAluno = abrirAluno
                )
                else -> HomeScreen(
                    userName = userName,
                    horario = horario,
                    turmas = turmas,
                    onOpenProfile = onOpenProfile,
                    onSignOut = onSignOut,
                    onOpenAgenda = { tab = MainTab.AGENDA },
                    onOpenTurma = abrirTurma,
                    agenda = agenda,
                    onAddEntrada = novaEntrada,
                    onOpenEntrada = { entradaEmEdicao = it },
                    fotoPerfil = fotoPerfil,
                    onOpenAluno = abrirAluno
                )
            }
        }
    }

    turmaEmEdicao?.let { emEdicao ->
        TurmaFormDialog(
            turma = emEdicao.takeIf { it.id.isNotBlank() },
            onDismiss = { turmaEmEdicao = null },
            onSave = { dados, terminou ->
                onGuardarTurma(dados) { msg, id ->
                    if (id != null) {
                        terminou(null)
                        turmaEmEdicao = null
                        avisar(msg)
                        // Turma nova: abre-a logo, para se poderem acrescentar alunos
                        if (emEdicao.id.isBlank()) abrirTurma(id)
                    } else {
                        terminou(msg)
                    }
                }
            }
        )
    }

    entradaEmEdicao?.let { emEdicao ->
        EntradaAgendaDialog(
            inicial = emEdicao,
            onDismiss = { entradaEmEdicao = null },
            onSave = { entrada, terminou ->
                onGuardarEntrada(entrada) { erro ->
                    terminou(erro)
                    if (erro == null) {
                        entradaEmEdicao = null
                        avisar(if (emEdicao.id.isBlank()) "Adicionado à agenda." else "Agenda atualizada.")
                    }
                }
            },
            onApagar = if (emEdicao.id.isBlank()) null else {
                {
                    entradaEmEdicao = null
                    onApagarEntrada(emEdicao, avisar)
                }
            }
        )
    }

    turmaAApagar?.let { aApagar ->
        ApagarTurmaDialog(
            turma = aApagar,
            aulasNoHorario = horario.count { it.turmaId == aApagar.id },
            onDismiss = { turmaAApagar = null },
            onConfirm = {
                turmaAApagar = null
                if (turmaAbertaId == aApagar.id) {
                    turmaAbertaId = null
                    alunoAbertoNumero = null
                }
                onDeleteTurma(aApagar.id, avisar)
            }
        )
    }
}

@Preview(showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun MainShellPreview() {
    SOSProfTheme {
        MainShell(
            userName = "Professor",
            turmas = emptyList(),
            horario = emptyList(),
            onSaveAluno = { _, _, _ -> },
            onDeleteAluno = { _, _, _ -> },
            onOpenProfile = {},
            onSignOut = {}
        )
    }
}
