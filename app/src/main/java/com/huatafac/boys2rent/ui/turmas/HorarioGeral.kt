package com.huatafac.boys2rent.ui.turmas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huatafac.boys2rent.model.Aula
import com.huatafac.boys2rent.model.Turma
import com.huatafac.boys2rent.model.comoHora
import com.huatafac.boys2rent.model.diasEspeciaisDoDia
import com.huatafac.boys2rent.model.eFeriado
import com.huatafac.boys2rent.ui.agenda.DiasEspeciaisCard
import com.huatafac.boys2rent.ui.agenda.dataDoCalendario
import com.huatafac.boys2rent.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val ptPT: Locale = Locale.forLanguageTag("pt-PT")
private val diasCurtos = listOf("Seg", "Ter", "Qua", "Qui", "Sex")

/**
 * Segunda a sexta da semana [offset] semanas a partir da atual (0 = esta semana).
 * A semana começa ao domingo, como na Agenda: ao domingo já se mostra a semana que começa nesse dia.
 */
private fun diasUteis(offset: Int): List<Calendar> {
    val domingo = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, Calendar.SUNDAY - get(Calendar.DAY_OF_WEEK))
        add(Calendar.DAY_OF_MONTH, 7 * offset)
    }
    return (1..5).map { i -> (domingo.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) } }
}

/** Dia do horário de hoje (1 = segunda … 5 = sexta), ou -1 ao fim de semana. */
private fun diaUtilDeHoje(): Int {
    val dia = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
    return if (dia in 1..5) dia else -1
}

/**
 * Horário semanal de todas as turmas (separador "Horário" da página Turmas).
 * Vista semanal (grelha de segunda a sexta) ou diária, com setas para mudar de semana;
 * os feriados ficam a vermelho. Tocar numa aula abre a turma.
 */
@Composable
fun HorarioGeral(horario: List<Aula>, turmas: List<Turma>, onOpenTurma: (String) -> Unit, modifier: Modifier = Modifier) {
    var semana by rememberSaveable { mutableIntStateOf(0) }
    var vistaSemana by rememberSaveable { mutableStateOf(true) }
    var diaSelecionado by rememberSaveable { mutableIntStateOf(diaUtilDeHoje().takeIf { it > 0 } ?: 1) }
    val dias = remember(semana) { diasUteis(semana) }
    val intervalo = remember(dias) {
        val d = SimpleDateFormat("d", ptPT)
        val m = SimpleDateFormat("d 'de' MMMM", ptPT)
        "${d.format(dias.first().time)} – ${m.format(dias.last().time)}"
    }
    val hoje = if (semana == 0) diaUtilDeHoje() else -1
    val corDe: (Aula) -> Color = { aula ->
        turmas.firstOrNull { it.id == aula.turmaId }?.let { Color(it.corHex) } ?: B2rMuted
    }
    val ehFeriado: (Calendar) -> Boolean = { cal -> eFeriado(dataDoCalendario(cal)) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { semana-- }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Semana anterior", tint = B2rBlueDark)
            }
            Text(
                intervalo,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = B2rInk,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clickable(onClickLabel = "Voltar a esta semana") { semana = 0 }
            )
            IconButton(onClick = { semana++ }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Semana seguinte", tint = B2rBlueDark)
            }
            VistaToggle(vistaSemana = vistaSemana, onChange = { vistaSemana = it })
        }

        if (vistaSemana) {
            Grelha(horario, dias, hoje, corDe, onOpenTurma, ehFeriado)
            Legenda(turmas)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                dias.forEachIndexed { i, cal ->
                    val selecionado = i + 1 == diaSelecionado
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selecionado) B2rBlue else Color.White)
                            .clickable { diaSelecionado = i + 1 }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(diasCurtos[i], fontSize = 12.sp, color = if (selecionado) Color.White else B2rMuted)
                        Text(
                            cal.get(Calendar.DAY_OF_MONTH).toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                selecionado -> Color.White
                                ehFeriado(cal) -> B2rFeriado
                                else -> B2rInk
                            }
                        )
                    }
                }
            }
            val especiais = diasEspeciaisDoDia(dataDoCalendario(dias[diaSelecionado - 1]))
            if (especiais.isNotEmpty()) DiasEspeciaisCard(especiais, mostrarData = false)
            val doDia = horario.filter { it.diaSemana == diaSelecionado }.sortedBy { it.inicioMin }
            if (doDia.isEmpty()) {
                Text("Sem aulas neste dia.", color = B2rMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
            }
            doDia.forEach { aula ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (aula.turmaId != null) Modifier.clickable { onOpenTurma(aula.turmaId) } else Modifier)
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.width(52.dp)) {
                            Text(aula.inicioMin.comoHora(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = B2rInk)
                            Text(aula.fimMin.comoHora(), fontSize = 12.sp, color = B2rMuted)
                        }
                        Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(corDe(aula)))
                        Column(Modifier.weight(1f)) {
                            Text(aula.titulo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = B2rInk)
                            Text(aula.detalhe, fontSize = 13.sp, color = B2rMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Grelha(
    horario: List<Aula>,
    dias: List<Calendar>,
    hoje: Int,
    corDe: (Aula) -> Color,
    onOpenTurma: (String) -> Unit,
    ehFeriado: (Calendar) -> Boolean
) {
    val horas = horario.map { it.inicioMin }.distinct().sorted()
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Spacer(Modifier.width(40.dp))
                dias.forEachIndexed { i, cal ->
                    val ehHoje = i + 1 == hoje
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (ehHoje) B2rBlue else Color.Transparent)
                            .padding(vertical = 2.dp)
                    ) {
                        Text(diasCurtos[i], fontSize = 12.sp, color = if (ehHoje) Color.White else B2rMuted)
                        Text(
                            cal.get(Calendar.DAY_OF_MONTH).toString(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                ehHoje -> Color.White
                                ehFeriado(cal) -> B2rFeriado
                                else -> B2rInk
                            }
                        )
                    }
                }
            }
            if (horas.isEmpty()) {
                Text(
                    "Ainda sem aulas no horário. As aulas acrescentam-se no separador Horário de cada turma.",
                    fontSize = 13.sp,
                    color = B2rMuted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                )
            }
            horas.forEach { inicio ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        inicio.comoHora(),
                        fontSize = 11.sp,
                        color = B2rMuted,
                        modifier = Modifier.width(40.dp).padding(top = 4.dp)
                    )
                    (1..5).forEach { dia ->
                        val aula = horario.firstOrNull { it.diaSemana == dia && it.inicioMin == inicio }
                        Box(Modifier.weight(1f).height(42.dp)) {
                            if (aula != null) BlocoAula(aula, corDe(aula), onOpenTurma)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlocoAula(aula: Aula, cor: Color, onOpenTurma: (String) -> Unit) {
    val destaque = aula.nota != null
    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(cor.copy(alpha = 0.15f))
            .then(if (destaque) Modifier.border(2.dp, cor, RoundedCornerShape(8.dp)) else Modifier)
            .then(if (aula.turmaId != null) Modifier.clickable { onOpenTurma(aula.turmaId) } else Modifier)
            .padding(horizontal = 5.dp, vertical = 3.dp)
    ) {
        Text(aula.titulo, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = B2rInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(aula.nota ?: aula.disciplina.ifBlank { aula.local }, fontSize = 11.sp, color = B2rMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legenda(turmas: List<Turma>) {
    FlowRow(
        Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        (turmas.map { it.nome to Color(it.corHex) } + ("Outros" to B2rMuted)).forEach { (nome, cor) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(cor))
                Text(nome, fontSize = 12.sp, color = B2rInk)
            }
        }
    }
}
