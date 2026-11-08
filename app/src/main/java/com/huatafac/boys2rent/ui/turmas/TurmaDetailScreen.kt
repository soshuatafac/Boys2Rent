package com.huatafac.boys2rent.ui.turmas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huatafac.boys2rent.model.Aluno
import com.huatafac.boys2rent.model.Aula
import com.huatafac.boys2rent.model.Turma
import com.huatafac.boys2rent.model.idadeEm
import com.huatafac.boys2rent.ui.components.B2rHeader
import com.huatafac.boys2rent.ui.theme.*
import java.util.Calendar

private val separadores = listOf("Horário", "Alunos")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurmaDetailScreen(
    turma: Turma,
    horario: List<Aula>,
    onBack: () -> Unit,
    onOpenAluno: (Int) -> Unit,
    onAddAluno: (Aluno) -> Unit,
    modifier: Modifier = Modifier,
    onEditarTurma: () -> Unit = {},
    onApagarTurma: () -> Unit = {},
    // Aula antiga (null = nova), aula nova, callback com a mensagem de erro (null se gravou)
    onGuardarAula: (Aula?, Aula, (String?) -> Unit) -> Unit = { _, _, cb -> cb("Indisponível") },
    onApagarAula: (Aula) -> Unit = {},
    // Linhas da folha Excel importada (já confirmada pelo professor)
    onImportarAlunos: (List<List<String>>) -> Unit = {},
    onAviso: (String) -> Unit = {},
    // Separador aberto (0 = Horário, 1 = Alunos…). Vem de fora para não se perder ao abrir a ficha
    // de um aluno: este ecrã sai de cena e, ao voltar, abria sempre no Horário.
    separador: Int = 0,
    onSeparadorChange: (Int) -> Unit = {}
) {
    var menuAberto by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().background(B2rBackground)) {
        Column(Modifier.background(Color.White)) {
            B2rHeader(bottomPadding = 30.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar às turmas", tint = Color.White)
                    }
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(turma.sigla, color = B2rBlueDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(listOf(turma.nome, turma.disciplina).filter { it.isNotBlank() }.joinToString(" · "), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        val detalhe = listOfNotNull(
                            "${turma.alunos.size} alunos",
                            turma.sala,
                            "Diretor de turma".takeIf { turma.diretorTurma }
                        ).joinToString(" · ")
                        Text(detalhe, color = B2rBlueOnDark, fontSize = 13.sp)
                    }
                    Box {
                        IconButton(onClick = { menuAberto = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Opções da turma", tint = Color.White)
                        }
                        DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                            DropdownMenuItem(
                                text = { Text("Editar turma") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    menuAberto = false
                                    onEditarTurma()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Apagar turma", color = B2rWarning) },
                                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = B2rWarning) },
                                onClick = {
                                    menuAberto = false
                                    onApagarTurma()
                                }
                            )
                        }
                    }
                }
            }
            PrimaryTabRow(
                selectedTabIndex = separador.coerceIn(0, separadores.lastIndex),
                containerColor = Color.White,
                contentColor = B2rBlueDark
            ) {
                separadores.forEachIndexed { i, nome ->
                    Tab(
                        selected = separador == i,
                        onClick = { onSeparadorChange(i) },
                        text = { Text(nome, fontWeight = if (separador == i) FontWeight.SemiBold else FontWeight.Medium, fontSize = 14.sp) },
                        unselectedContentColor = B2rMuted
                    )
                }
            }
        }

        Box(Modifier.weight(1f)) {
            when (separador) {
                1 -> AlunosTab(turma, onOpenAluno, onAddAluno, onImportarAlunos, onAviso)
                else -> HorarioTurmaTab(turma, horario, onGuardarAula, onApagarAula)
            }
        }
    }
}

@Composable
private fun AlunosTab(
    turma: Turma,
    onOpenAluno: (Int) -> Unit,
    onAddAluno: (Aluno) -> Unit,
    onImportarAlunos: (List<List<String>>) -> Unit,
    onAviso: (String) -> Unit
) {
    val alunos = turma.alunos
    var showAddDialog by remember { mutableStateOf(false) }
    val hoje = remember { Calendar.getInstance() }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = B2rBlue)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Adicionar aluno", fontSize = 14.sp)
                }
            }
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column {
                    if (alunos.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Sem alunos registados.", color = B2rMuted, fontSize = 14.sp)
                        }
                    } else {
                        alunos.forEachIndexed { i, aluno ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(onClickLabel = "Abrir ficha de ${aluno.nome}") { onOpenAluno(aluno.numero) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape).background(B2rBlueLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(aluno.numero.toString(), color = B2rBlueDark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                Text(aluno.nome, fontSize = 15.sp, color = B2rInk, modifier = Modifier.weight(1f))
                                // Idade à direita, só se a data de nascimento estiver preenchida
                                idadeEm(
                                    aluno.dataNascimento,
                                    hoje.get(Calendar.DAY_OF_MONTH),
                                    hoje.get(Calendar.MONTH) + 1,
                                    hoje.get(Calendar.YEAR)
                                )?.let { idade ->
                                    Text(if (idade == 1) "1 ano" else "$idade anos", fontSize = 13.sp, color = B2rMuted)
                                }
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = B2rMuted)
                            }
                            if (i < alunos.lastIndex) HorizontalDivider(color = B2rDivider)
                        }
                    }
                }
            }
        }
        // Exportar a folha, preencher no Excel e importar: fica fixo em baixo, junto à barra de navegação
        Surface(color = Color.White, shadowElevation = 6.dp) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                ExcelAlunosBotoes(turma = turma, onImportar = onImportarAlunos, onAviso = onAviso)
            }
        }
    }

    if (showAddDialog) {
        val nextNumero = (alunos.maxOfOrNull { it.numero } ?: 0) + 1
        val defaultNewAluno = Aluno(numero = nextNumero, nome = "")
        EditAlunoDialog(
            aluno = defaultNewAluno,
            onDismiss = { showAddDialog = false },
            numerosOcupados = alunos.map { it.numero }.toSet(),
            onSave = { newAluno ->
                showAddDialog = false
                onAddAluno(newAluno)
            }
        )
    }
}

@Preview(showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun TurmaDetailPreview() {
    Boys2RentTheme {
        TurmaDetailScreen(Turma(id = "preview", ano = "7.º", letra = "A"), emptyList(), onBack = {}, onOpenAluno = {}, onAddAluno = {})
    }
}
