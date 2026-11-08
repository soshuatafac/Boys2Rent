package com.huatafac.boys2rent.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huatafac.boys2rent.model.Aniversario
import com.huatafac.boys2rent.model.EntradaAgenda
import com.huatafac.boys2rent.model.TipoDiaEspecial
import com.huatafac.boys2rent.model.Turma
import com.huatafac.boys2rent.model.aniversariosEntre
import com.huatafac.boys2rent.model.diasEspeciaisEntre
import com.huatafac.boys2rent.model.doDia
import com.huatafac.boys2rent.ui.components.SectionHeader
import com.huatafac.boys2rent.ui.components.B2rHeader
import com.huatafac.boys2rent.ui.components.B2rLogo
import com.huatafac.boys2rent.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val ptPT: Locale = Locale.forLanguageTag("pt-PT")

/** Colunas do calendário: a semana começa ao domingo, como no resto da app. */
private val diasCurtos = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")

/** Primeiro dia do mês [offset] meses a partir do atual (0 = este mês). */
private fun primeiroDoMes(offset: Int): Calendar = Calendar.getInstance().apply {
    set(Calendar.DAY_OF_MONTH, 1)
    add(Calendar.MONTH, offset)
}

/**
 * Agenda pessoal num calendário mensal: reuniões, atividades e notas, feriados e dias festivos
 * e aniversários dos alunos. Tocar num dia mostra o que há nesse dia por baixo do calendário.
 * O horário das aulas está na página Turmas (separador Horário).
 */
@Composable
fun AgendaScreen(
    turmas: List<Turma>,
    modifier: Modifier = Modifier,
    agenda: List<EntradaAgenda> = emptyList(),
    // Data ("aaaa-MM-dd") sugerida para a nova entrada
    onAddEntrada: (String) -> Unit = {},
    onOpenEntrada: (EntradaAgenda) -> Unit = {},
    onOpenTurma: (String) -> Unit = {},
    // turmaId e n.º do aluno: abre a ficha do aluno (aniversários)
    onOpenAluno: (String, Int) -> Unit = { turmaId, _ -> onOpenTurma(turmaId) }
) {
    val hoje = dataAgendaHoje()
    var mes by rememberSaveable { mutableIntStateOf(0) }
    var dataSelecionada by rememberSaveable { mutableStateOf(hoje) }
    val primeiro = remember(mes) { primeiroDoMes(mes) }
    val diasNoMes = primeiro.getActualMaximum(Calendar.DAY_OF_MONTH)
    val datas = remember(mes) {
        (0 until diasNoMes).map { i -> dataDoCalendario((primeiro.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }) }
    }
    val titulo = remember(mes) {
        SimpleDateFormat("LLLL 'de' yyyy", ptPT).format(primeiro.time).replaceFirstChar { it.titlecase(ptPT) }
    }
    val especiais = remember(datas) { diasEspeciaisEntre(datas.first(), datas.last()) }
    val aniversarios = remember(datas, turmas) { aniversariosEntre(turmas, datas.first(), datas.last()) }
    val comEntradas = remember(agenda) { agenda.map { it.data }.toSet() }

    val mudarMes: (Int) -> Unit = { novo ->
        mes = novo
        // Este mês: volta a hoje; outro mês: o dia 1
        dataSelecionada = if (novo == 0) hoje else dataDoCalendario(primeiroDoMes(novo))
    }

    Box(modifier = modifier.fillMaxSize().background(B2rBackground)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            B2rHeader {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    B2rLogo(Modifier.weight(1f))
                    HeaderIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Mês anterior") { mudarMes(mes - 1) }
                    Spacer(Modifier.width(8.dp))
                    HeaderIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Mês seguinte") { mudarMes(mes + 1) }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("Agenda", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(titulo, color = B2rBlueOnDark, fontSize = 13.sp)
                    }
                    if (mes != 0 || dataSelecionada != hoje) {
                        TextButton(onClick = { mudarMes(0) }) {
                            Text("Hoje", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Column(Modifier.padding(horizontal = 12.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CalendarioMes(
                    primeiro = primeiro,
                    datas = datas,
                    hoje = hoje,
                    selecionada = dataSelecionada,
                    onSelecionar = { dataSelecionada = it },
                    feriados = especiais.filter { it.tipo == TipoDiaEspecial.FERIADO }.map { it.data }.toSet(),
                    festivos = especiais.filter { it.tipo == TipoDiaEspecial.FESTIVO }.map { it.data }.toSet(),
                    comEntradas = comEntradas,
                    comAniversarios = aniversarios.map { it.data }.toSet()
                )
                Legenda()

                // O que há no dia escolhido
                Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(if (dataSelecionada == hoje) "Hoje · ${dataLegivel(dataSelecionada)}" else dataLegivel(dataSelecionada)) {
                        TextButton(onClick = { onAddEntrada(dataSelecionada) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("Adicionar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                    val especiaisDia = especiais.filter { it.data == dataSelecionada }
                    if (especiaisDia.isNotEmpty()) DiasEspeciaisCard(especiaisDia, mostrarData = false)
                    val aniversariosDia = aniversarios.filter { it.data == dataSelecionada }
                    if (aniversariosDia.isNotEmpty()) AniversariosCard(aniversariosDia, onOpenAluno)
                    val entradas = agenda.doDia(dataSelecionada)
                    if (entradas.isNotEmpty()) {
                        EntradasAgendaCard(entradas, onOpenEntrada)
                    } else {
                        Text(
                            "Sem reuniões, atividades ou notas neste dia.",
                            color = B2rMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(76.dp)) // espaço para o botão flutuante
            }
        }

        ExtendedFloatingActionButton(
            onClick = { onAddEntrada(dataSelecionada) },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Agenda", fontWeight = FontWeight.SemiBold) },
            containerColor = B2rBlue,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        )
    }
}

@Composable
private fun HeaderIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f))
    ) {
        Icon(icon, contentDescription = label, tint = Color.White)
    }
}

/**
 * Grelha do mês (domingo a sábado). Hoje tem contorno azul, o dia escolhido fundo azul e os feriados
 * o número a vermelho. Pontos por baixo: amarelo = agenda, roxo = dia festivo, verde = aniversário.
 */
@Composable
private fun CalendarioMes(
    primeiro: Calendar,
    datas: List<String>,
    hoje: String,
    selecionada: String,
    onSelecionar: (String) -> Unit,
    feriados: Set<String>,
    festivos: Set<String>,
    comEntradas: Set<String>,
    comAniversarios: Set<String>
) {
    // Dias em branco antes do dia 1 (0 se o mês começa ao domingo)
    val vazios = primeiro.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val casas: List<String?> = List(vazios) { null } + datas
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row {
                diasCurtos.forEachIndexed { i, nome ->
                    Text(
                        nome,
                        fontSize = 12.sp,
                        color = if (i == 0) B2rFeriado.copy(alpha = 0.8f) else B2rMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                    )
                }
            }
            casas.chunked(7).forEach { semana ->
                Row {
                    (0 until 7).forEach { i ->
                        val data = semana.getOrNull(i)
                        Box(Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.Center) {
                            if (data != null) {
                                DiaCasa(
                                    dia = data.takeLast(2).toInt(),
                                    ehHoje = data == hoje,
                                    selecionado = data == selecionada,
                                    feriado = data in feriados,
                                    pontos = listOfNotNull(
                                        B2rYellow.takeIf { data in comEntradas },
                                        B2rFestivo.takeIf { data in festivos },
                                        B2rPresent.takeIf { data in comAniversarios }
                                    ),
                                    onClick = { onSelecionar(data) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaCasa(dia: Int, ehHoje: Boolean, selecionado: Boolean, feriado: Boolean, pontos: List<Color>, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selecionado) B2rBlue else Color.Transparent)
            .then(if (ehHoje && !selecionado) Modifier.border(1.5.dp, B2rBlue, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(top = 6.dp)
    ) {
        Text(
            dia.toString(),
            fontSize = 15.sp,
            fontWeight = if (ehHoje || selecionado || feriado) FontWeight.Bold else FontWeight.Medium,
            color = when {
                selecionado -> Color.White
                feriado -> B2rFeriado
                ehHoje -> B2rBlue
                else -> B2rInk
            }
        )
        Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            pontos.forEach { cor ->
                Box(Modifier.size(5.dp).clip(CircleShape).background(if (selecionado) Color.White else cor))
            }
        }
    }
}

@Composable
private fun Legenda() {
    Row(
        Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(B2rFeriado to "Feriado", B2rYellow to "Agenda", B2rFestivo to "Festivo", B2rPresent to "Aniversário").forEach { (cor, nome) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(cor))
                Text(nome, fontSize = 12.sp, color = B2rMuted)
            }
        }
    }
}

/** Aniversários dos alunos no dia escolhido; tocar abre a ficha do aluno. */
@Composable
private fun AniversariosCard(aniversarios: List<Aniversario>, onOpenAluno: (String, Int) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column {
            aniversarios.forEachIndexed { i, a ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = "Abrir ficha do aluno") { onOpenAluno(a.turmaId, a.numero) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Cake, contentDescription = null, tint = B2rPresent, modifier = Modifier.size(22.dp))
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = B2rInk)) { append(a.nome) }
                            withStyle(SpanStyle(color = B2rMuted)) { append(" · ${a.turma} · faz ${a.idade} anos") }
                        },
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (i < aniversarios.lastIndex) HorizontalDivider(color = B2rDivider)
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun AgendaScreenPreview() {
    Boys2RentTheme {
        AgendaScreen(turmas = emptyList())
    }
}
