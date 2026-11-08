package com.huatafac.sosprof.ui.turmas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huatafac.sosprof.model.Turma
import com.huatafac.sosprof.model.coresTurma
import com.huatafac.sosprof.ui.components.SosFormCard
import com.huatafac.sosprof.ui.components.SosFormSheet
import com.huatafac.sosprof.ui.profile.ProfileTextField
import com.huatafac.sosprof.ui.theme.*

/**
 * Formulário para criar ([turma] = null) ou editar uma turma, num painel que sobe de baixo,
 * com os mesmos campos do resto da app (perfil).
 * [onSave] recebe a turma e uma função a chamar no fim com a mensagem de erro (ou null se gravou):
 * o painel fica aberto, com o erro à vista, até a gravação correr bem.
 */
@Composable
fun TurmaFormDialog(
    turma: Turma?,
    onDismiss: () -> Unit,
    onSave: (Turma, (String?) -> Unit) -> Unit
) {
    val editar = turma != null
    var ano by remember { mutableStateOf(turma?.ano ?: "") }
    var letra by remember { mutableStateOf(turma?.letra ?: "") }
    var disciplina by remember { mutableStateOf(turma?.disciplina ?: "") }
    var sala by remember { mutableStateOf(turma?.sala ?: "") }
    var cor by remember { mutableStateOf(turma?.corHex?.takeIf { it != 0L } ?: coresTurma.first()) }
    var diretorTurma by remember { mutableStateOf(turma?.diretorTurma ?: false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var aGravar by remember { mutableStateOf(false) }
    SosFormSheet(
        titulo = if (editar) "Editar Turma" else "Adicionar Nova Turma",
        textoBotao = if (editar) "Guardar" else "Adicionar turma",
        aGravar = aGravar,
        erro = erro,
        onDismiss = onDismiss,
        onConfirmar = {
            aGravar = true
            val dados = (turma ?: Turma()).copy(
                ano = ano, letra = letra, disciplina = disciplina, sala = sala,
                corHex = cor, diretorTurma = diretorTurma
            )
            onSave(dados) { msgErro ->
                aGravar = false
                erro = msgErro
            }
        }
    ) {
        SosFormCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileTextField(
                    label = "Ano *",
                    value = ano,
                    onValueChange = { ano = it; erro = null },
                    icon = Icons.Rounded.School,
                    placeholder = "8.º",
                    modifier = Modifier.weight(1f)
                )
                ProfileTextField(
                    label = "Letra *",
                    value = letra,
                    onValueChange = { letra = it.take(3); erro = null },
                    icon = Icons.Rounded.Tag,
                    placeholder = "B",
                    modifier = Modifier.weight(1f)
                )
            }
            ProfileTextField(
                label = "Disciplina (opcional)",
                value = disciplina,
                onValueChange = { disciplina = it; erro = null },
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                placeholder = "Ex.: Matemática"
            )
            ProfileTextField(
                label = "Sala (opcional)",
                value = sala,
                onValueChange = { sala = it },
                icon = Icons.Rounded.MeetingRoom,
                placeholder = "Ex.: Sala 14"
            )
        }

        SosFormCard {
            Text("Cor da turma", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SosInk)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                coresTurma.forEach { c ->
                    val selecionada = c == cor
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .then(if (selecionada) Modifier.border(3.dp, SosBlueLight, CircleShape) else Modifier)
                            .clickable { cor = c }
                            .semantics {
                                contentDescription = "Cor da turma"
                                selected = selecionada
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selecionada) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
            HorizontalDivider(color = SosDivider)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Diretor de turma", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SosInk)
                    Text("Sou diretor(a) desta turma", fontSize = 13.sp, color = SosMuted)
                }
                Switch(
                    checked = diretorTurma,
                    onCheckedChange = { diretorTurma = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = SosBlue)
                )
            }
        }
    }
}

/**
 * Confirmação para apagar uma turma. Como apaga dados de alunos sem volta atrás,
 * o professor tem de escrever a sigla da turma (ex.: "8B") para o botão ficar ativo.
 */
@Composable
fun ApagarTurmaDialog(
    turma: Turma,
    aulasNoHorario: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var confirmacao by remember { mutableStateOf("") }
    val confirmado = confirmacao.trim().equals(turma.sigla, ignoreCase = true)
    val fotos = turma.alunos.count { it.fotoUrl.isNotBlank() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apagar ${turma.nome}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Isto apaga para sempre, sem possibilidade de recuperar:", color = SosInk)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("• ${turma.alunos.size} alunos, com as fichas e as faltas", color = SosInk)
                    if (fotos > 0) Text("• $fotos fotos de alunos", color = SosInk)
                    Text("• $aulasNoHorario aulas desta turma no horário", color = SosInk)
                }
                Text(
                    "Para confirmar, escreva ${turma.sigla}:",
                    fontWeight = FontWeight.SemiBold,
                    color = SosInk
                )
                OutlinedTextField(
                    value = confirmacao,
                    onValueChange = { confirmacao = it },
                    singleLine = true,
                    placeholder = { Text(turma.sigla) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmado,
                colors = ButtonDefaults.buttonColors(containerColor = SosWarning)
            ) { Text("Apagar turma") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
