package com.huatafac.sosprof.ui.turmas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huatafac.sosprof.model.Aula
import com.huatafac.sosprof.model.Turma
import com.huatafac.sosprof.model.comoHora
import com.huatafac.sosprof.ui.components.SosChip
import com.huatafac.sosprof.ui.components.SosHeader
import com.huatafac.sosprof.ui.components.SosLogo
import com.huatafac.sosprof.ui.components.TurmaBadge
import com.huatafac.sosprof.ui.theme.*
import java.util.Calendar

private val diasLongos = listOf("seg", "ter", "qua", "qui", "sex")

@Composable
fun TurmasScreen(
    turmas: List<Turma>,
    horario: List<Aula>,
    onOpenTurma: (String) -> Unit,
    onNovaTurma: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pesquisa by rememberSaveable { mutableStateOf("") }
    // false = lista das turmas; true = horário semanal de todas as turmas
    var verHorario by rememberSaveable { mutableStateOf(false) }
    val termo = pesquisa.trim().lowercase()
    val filtradas = if (termo.isEmpty()) turmas else turmas.filter { t ->
        t.nome.lowercase().contains(termo) ||
            t.disciplina.lowercase().contains(termo) ||
            t.alunos.any { it.nome.lowercase().contains(termo) }
    }
    val anoLetivo = remember {
        val c = Calendar.getInstance()
        val inicio = if (c.get(Calendar.MONTH) >= Calendar.SEPTEMBER) c.get(Calendar.YEAR) else c.get(Calendar.YEAR) - 1
        "Ano letivo $inicio/${(inicio + 1) % 100}"
    }

    Box(modifier.fillMaxSize().background(SosBackground)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SosHeader(bottomPadding = 40.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SosLogo(Modifier.weight(1f))
                    Text(anoLetivo, color = SosBlueOnDark, fontSize = 13.sp)
                }
                Text("Turmas", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                SeparadoresTurmas(verHorario = verHorario, onChange = { verHorario = it })
                if (!verHorario) Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = SosMuted, modifier = Modifier.size(20.dp))
                    Box(Modifier.weight(1f)) {
                        if (pesquisa.isEmpty()) Text("Procurar turma ou aluno", color = SosMuted, fontSize = 15.sp)
                        BasicTextField(
                            value = pesquisa,
                            onValueChange = { pesquisa = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 15.sp, color = SosInk),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (verHorario) {
                HorarioGeral(
                    horario = horario,
                    turmas = turmas,
                    onOpenTurma = onOpenTurma,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                )
                Spacer(Modifier.height(16.dp))
            } else Column(
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filtradas.isEmpty()) {
                    Text(
                        if (turmas.isEmpty()) "Ainda não tem turmas. Toque em \"Nova turma\" para criar a primeira."
                        else "Nenhuma turma encontrada.",
                        color = SosMuted,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                filtradas.forEach { turma ->
                    TurmaCard(turma, horario.filter { it.turmaId == turma.id }) { onOpenTurma(turma.id) }
                }
                Spacer(Modifier.height(80.dp))
            }
        }

        if (!verHorario) ExtendedFloatingActionButton(
            onClick = onNovaTurma,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Nova turma", fontWeight = FontWeight.SemiBold) },
            containerColor = SosBlue,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        )
    }
}

/** Separadores "Turmas" e "Horário" no cabeçalho azul. */
@Composable
private fun SeparadoresTurmas(verHorario: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(3.dp)
    ) {
        listOf(false to "Turmas", true to "Horário").forEach { (valor, texto) ->
            val selecionado = valor == verHorario
            Text(
                text = texto,
                color = if (selecionado) SosBlueDark else Color.White,
                fontWeight = if (selecionado) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selecionado) Color.White else Color.Transparent)
                    .clickable(onClickLabel = "Ver $texto") { onChange(valor) }
                    .padding(vertical = 10.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TurmaCard(turma: Turma, aulas: List<Aula>, onClick: () -> Unit) {
    val emRisco = turma.alunos.count { it.faltasPeriodo >= 3 }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth().clickable(onClickLabel = "Abrir ${turma.nome}", onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TurmaBadge(turma)
                Column(Modifier.weight(1f)) {
                    Text(listOf(turma.nome, turma.disciplina).filter { it.isNotBlank() }.joinToString(" · "), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SosInk)
                    val detalhe = buildList {
                        add("${turma.alunos.size} alunos")
                        add(if (turma.diretorTurma) "Diretor de turma" else "${aulas.size} aulas/semana")
                    }.joinToString(" · ")
                    Text(detalhe, fontSize = 13.sp, color = SosMuted)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = SosMuted)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (aulas.isNotEmpty()) {
                    SosChip(
                        "${turma.sala} · ${aulas.map { it.diaSemana }.distinct().sorted().joinToString(", ") { diasLongos[it - 1] }}",
                        background = SosChipBg, color = SosInk
                    )
                }
                if (emRisco > 0) {
                    SosChip(
                        if (emRisco == 1) "1 aluno com faltas" else "$emRisco alunos com faltas",
                        background = SosWarningBg, color = SosWarningInk
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun TurmasScreenPreview() {
    SOSProfTheme {
        TurmasScreen(emptyList(), emptyList(), onOpenTurma = {}, onNovaTurma = {})
    }
}
