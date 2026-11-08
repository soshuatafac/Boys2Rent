package com.huatafac.sosprof.ui.turmas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Schedule
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
import com.huatafac.sosprof.model.Aula
import com.huatafac.sosprof.model.Turma
import com.huatafac.sosprof.model.comoHora
import com.huatafac.sosprof.model.daTurma
import com.huatafac.sosprof.model.diasSemana
import com.huatafac.sosprof.model.hm
import com.huatafac.sosprof.ui.components.SosFormCard
import com.huatafac.sosprof.ui.components.SosFormSheet
import com.huatafac.sosprof.ui.profile.ProfileClickField
import com.huatafac.sosprof.ui.profile.ProfileTextField
import com.huatafac.sosprof.ui.theme.*
import java.util.Calendar

/** Duração por defeito de uma aula nova (tempo letivo de 50 minutos). */
private const val DURACAO_AULA_MIN = 50

/** Dia do horário de hoje (1 = segunda … 5 = sexta); ao fim de semana, segunda. */
private fun diaDeHoje(): Int {
    val dia = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
    return if (dia in 1..5) dia else 1
}

/** true se hoje é dia útil (para destacar o dia na grelha). */
private fun hojeEDiaUtil(): Boolean = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) in Calendar.MONDAY..Calendar.FRIDAY

/**
 * Separador "Horário" da turma: as aulas de segunda a sexta, com hora e disciplina.
 * Abre na vista semanal (grelha, como a Agenda) e pode mudar-se para a vista diária.
 * As aulas são as mesmas do horário geral (users/{uid}/horario), por isso aparecem também na Agenda e em Hoje.
 */
@Composable
fun HorarioTurmaTab(
    turma: Turma,
    horario: List<Aula>,
    onGuardarAula: (Aula?, Aula, (String?) -> Unit) -> Unit,
    onApagarAula: (Aula) -> Unit
) {
    val aulas = remember(horario, turma.id) { horario.daTurma(turma.id) }
    // null = fechado; first = aula a editar (null = nova), second = valores iniciais do formulário
    var emEdicao by remember { mutableStateOf<Pair<Aula?, Aula>?>(null) }
    var vistaSemana by rememberSaveable { mutableStateOf(true) }
    var diaSelecionado by rememberSaveable { mutableIntStateOf(diaDeHoje()) }
    val cor = if (turma.corHex != 0L) Color(turma.corHex) else SosBlue
    val novaNoDia: (Int) -> Unit = { dia -> emEdicao = null to novaAula(turma, dia, aulas.filter { it.diaSemana == dia }) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (aulas.isEmpty()) "Ainda sem aulas."
                else {
                    val total = aulas.sumOf { it.fimMin - it.inicioMin }
                    val horas = if (total % 60 == 0) "${total / 60}h" else "${total / 60}h%02d".format(total % 60)
                    "${aulas.size} ${if (aulas.size == 1) "aula" else "aulas"} por semana · $horas"
                },
                fontSize = 13.sp,
                color = SosMuted,
                modifier = Modifier.weight(1f)
            )
            VistaToggle(vistaSemana = vistaSemana, onChange = { vistaSemana = it })
        }
        if (vistaSemana) {
            GrelhaHorario(
                aulas = aulas,
                cor = cor,
                hoje = if (hojeEDiaUtil()) diaDeHoje() else -1,
                onOpenAula = { aula -> emEdicao = aula to aula },
                onNovaAula = { dia, inicio ->
                    val base = novaAula(turma, dia, emptyList())
                    emEdicao = null to base.copy(
                        inicioMin = inicio,
                        fimMin = (inicio + DURACAO_AULA_MIN).coerceAtMost(hm(23, 59))
                    )
                }
            )
            FilledTonalButton(
                onClick = { novaNoDia(diaDeHoje()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Acrescentar aula", fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                diasSemana.forEachIndexed { i, nome ->
                    val selecionado = i + 1 == diaSelecionado
                    Text(
                        nome.take(3),
                        fontSize = 13.sp,
                        fontWeight = if (selecionado) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selecionado) Color.White else SosInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selecionado) SosBlue else Color.White)
                            .clickable { diaSelecionado = i + 1 }
                            .padding(vertical = 10.dp)
                    )
                }
            }
            DiaCard(
                nomeDia = diasSemana[diaSelecionado - 1],
                aulas = aulas.filter { it.diaSemana == diaSelecionado },
                onAdd = { novaNoDia(diaSelecionado) },
                onOpenAula = { aula -> emEdicao = aula to aula }
            )
        }
    }

    emEdicao?.let { (antiga, inicial) ->
        AulaDialog(
            inicial = inicial,
            editar = antiga != null,
            sugestoes = (listOf(turma.disciplina) + aulas.map { it.disciplina })
                .map { it.trim() }.filter { it.isNotBlank() }.distinct(),
            onDismiss = { emEdicao = null },
            onSave = { nova, terminou ->
                onGuardarAula(antiga, nova) { erro ->
                    terminou(erro)
                    if (erro == null) emEdicao = null
                }
            },
            onApagar = antiga?.let { aula ->
                {
                    emEdicao = null
                    onApagarAula(aula)
                }
            }
        )
    }
}

/** Aula nova no [dia]: começa onde acaba a última aula desse dia (ou às 08:30), com a disciplina e sala da turma. */
private fun novaAula(turma: Turma, dia: Int, doDia: List<Aula>): Aula {
    val inicio = doDia.maxOfOrNull { it.fimMin } ?: hm(8, 30)
    return Aula(
        diaSemana = dia,
        inicioMin = inicio,
        fimMin = (inicio + DURACAO_AULA_MIN).coerceAtMost(hm(23, 59)),
        titulo = turma.nome,
        local = turma.sala,
        turmaId = turma.id,
        disciplina = turma.disciplina
    )
}

/** Escolha entre a vista diária e a semanal (fundo claro). */
@Composable
internal fun VistaToggle(vistaSemana: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SosChipBg)
            .padding(3.dp)
    ) {
        listOf(false to "Dia", true to "Semana").forEach { (valor, texto) ->
            val selecionado = valor == vistaSemana
            Text(
                text = texto,
                color = if (selecionado) Color.White else SosBlueDark,
                fontWeight = if (selecionado) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selecionado) SosBlue else Color.Transparent)
                    .clickable(onClickLabel = "Vista $texto") { onChange(valor) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Grelha semanal da turma (segunda a sexta), como a da Agenda: uma linha por hora de início.
 * Tocar numa aula edita-a; tocar numa casa vazia cria uma aula nesse dia e a essa hora.
 */
@Composable
private fun GrelhaHorario(
    aulas: List<Aula>,
    cor: Color,
    hoje: Int,
    onOpenAula: (Aula) -> Unit,
    onNovaAula: (dia: Int, inicioMin: Int) -> Unit
) {
    val horas = aulas.map { it.inicioMin }.distinct().sorted()
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Spacer(Modifier.width(40.dp))
                diasSemana.forEachIndexed { i, nome ->
                    val ehHoje = i + 1 == hoje
                    Text(
                        nome.take(3),
                        fontSize = 12.sp,
                        fontWeight = if (ehHoje) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (ehHoje) Color.White else SosMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (ehHoje) SosBlue else Color.Transparent)
                            .padding(vertical = 6.dp)
                    )
                }
            }
            if (horas.isEmpty()) {
                Text(
                    "Sem aulas. Use o botão em baixo para acrescentar.",
                    fontSize = 13.sp,
                    color = SosMuted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                )
            }
            horas.forEach { inicio ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        inicio.comoHora(),
                        fontSize = 11.sp,
                        color = SosMuted,
                        modifier = Modifier.width(40.dp).padding(top = 4.dp)
                    )
                    (1..5).forEach { dia ->
                        val aula = aulas.firstOrNull { it.diaSemana == dia && it.inicioMin == inicio }
                        Box(
                            Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (aula == null) Modifier.clickable(onClickLabel = "Acrescentar aula") { onNovaAula(dia, inicio) }
                                    else Modifier
                                )
                        ) {
                            if (aula != null) {
                                Column(
                                    Modifier
                                        .fillMaxSize()
                                        .background(cor.copy(alpha = 0.15f))
                                        .clickable(onClickLabel = "Editar aula") { onOpenAula(aula) }
                                        .padding(horizontal = 5.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        aula.disciplina.ifBlank { aula.titulo },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SosInk,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        aula.nota ?: aula.local,
                                        fontSize = 11.sp,
                                        color = SosMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaCard(nomeDia: String, aulas: List<Aula>, onAdd: () -> Unit, onOpenAula: (Aula) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(nomeDia, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SosInk, modifier = Modifier.weight(1f))
                IconButton(onClick = onAdd) {
                    Icon(Icons.Rounded.Add, contentDescription = "Acrescentar aula à $nomeDia", tint = SosBlue)
                }
            }
            HorizontalDivider(color = SosDivider)
            if (aulas.isEmpty()) {
                Text("Sem aulas", fontSize = 13.sp, color = SosMuted, modifier = Modifier.padding(16.dp))
            }
            aulas.forEachIndexed { i, aula ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = "Editar aula") { onOpenAula(aula) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.width(56.dp)) {
                        Text(aula.inicioMin.comoHora(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SosInk)
                        Text(aula.fimMin.comoHora(), fontSize = 12.sp, color = SosMuted)
                    }
                    Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(SosBlue))
                    Column(Modifier.weight(1f)) {
                        Text(
                            aula.disciplina.ifBlank { aula.titulo },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SosInk
                        )
                        val detalhe = listOf(aula.local, aula.nota.orEmpty()).filter { it.isNotBlank() }.joinToString(" · ")
                        if (detalhe.isNotEmpty()) Text(detalhe, fontSize = 13.sp, color = SosMuted)
                    }
                }
                if (i < aulas.lastIndex) HorizontalDivider(color = SosDivider)
            }
        }
    }
}

/**
 * Formulário de uma aula do horário: dia (segunda a sexta), hora de início e de fim, disciplina e sala.
 * [onSave] recebe a aula e uma função a chamar com a mensagem de erro (ou null se gravou):
 * o painel fica aberto, com o erro à vista, até a gravação correr bem.
 */
@Composable
private fun AulaDialog(
    inicial: Aula,
    editar: Boolean,
    sugestoes: List<String>,
    onDismiss: () -> Unit,
    onSave: (Aula, (String?) -> Unit) -> Unit,
    onApagar: (() -> Unit)?
) {
    var dia by remember { mutableIntStateOf(inicial.diaSemana) }
    var inicio by remember { mutableIntStateOf(inicial.inicioMin) }
    var fim by remember { mutableIntStateOf(inicial.fimMin) }
    var disciplina by remember { mutableStateOf(inicial.disciplina) }
    var local by remember { mutableStateOf(inicial.local) }
    var erro by remember { mutableStateOf<String?>(null) }
    var aGravar by remember { mutableStateOf(false) }
    // null = fechado; true = a escolher a hora de início; false = a hora de fim
    var escolherInicio by remember { mutableStateOf<Boolean?>(null) }

    SosFormSheet(
        titulo = if (editar) "Editar Aula" else "Adicionar Nova Aula",
        textoBotao = if (editar) "Guardar" else "Adicionar aula",
        aGravar = aGravar,
        erro = erro,
        onDismiss = onDismiss,
        onConfirmar = {
            aGravar = true
            val nova = inicial.copy(
                diaSemana = dia, inicioMin = inicio, fimMin = fim,
                disciplina = disciplina.trim(), local = local.trim()
            )
            onSave(nova) { msgErro ->
                aGravar = false
                erro = msgErro
            }
        },
        acaoExtra = if (onApagar != null) {
            {
                TextButton(onClick = onApagar, enabled = !aGravar) {
                    Text("Remover do horário", color = SosWarning, fontWeight = FontWeight.SemiBold)
                }
            }
        } else null
    ) {
        SosFormCard {
            Text("Dia *", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SosInk)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                diasSemana.forEachIndexed { i, nome ->
                    val selecionado = dia == i + 1
                    Text(
                        nome.take(3),
                        fontSize = 13.sp,
                        fontWeight = if (selecionado) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selecionado) Color.White else SosInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selecionado) SosBlue else SosChipBg)
                            .clickable { dia = i + 1; erro = null }
                            .padding(vertical = 10.dp)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileClickField(
                    label = "Início *",
                    value = inicio.comoHora(),
                    icon = Icons.Rounded.Schedule,
                    onClick = { escolherInicio = true },
                    modifier = Modifier.weight(1f)
                )
                ProfileClickField(
                    label = "Fim *",
                    value = fim.comoHora(),
                    icon = Icons.Rounded.Schedule,
                    onClick = { escolherInicio = false },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SosFormCard {
            ProfileTextField(
                label = "Disciplina (opcional)",
                value = disciplina,
                onValueChange = { disciplina = it; erro = null },
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                placeholder = "Ex.: Matemática"
            )
            if (sugestoes.isNotEmpty()) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sugestoes.forEach { s ->
                        AssistChip(onClick = { disciplina = s; erro = null }, label = { Text(s, fontSize = 13.sp) })
                    }
                }
            }
            ProfileTextField(
                label = "Sala (opcional)",
                value = local,
                onValueChange = { local = it },
                icon = Icons.Rounded.MeetingRoom,
                placeholder = "Ex.: Sala 14"
            )
        }
    }

    escolherInicio?.let { paraInicio ->
        HoraDialog(
            titulo = if (paraInicio) "Hora de início" else "Hora de fim",
            minutos = if (paraInicio) inicio else fim,
            onDismiss = { escolherInicio = null },
            onConfirm = { escolhida ->
                if (paraInicio) {
                    // Mantém a duração da aula ao mudar a hora de início
                    val duracao = (fim - inicio).takeIf { it > 0 } ?: DURACAO_AULA_MIN
                    inicio = escolhida
                    fim = (escolhida + duracao).coerceAtMost(hm(23, 59))
                } else {
                    fim = escolhida
                }
                erro = null
                escolherInicio = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HoraDialog(titulo: String, minutos: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val estado = rememberTimePickerState(initialHour = minutos / 60, initialMinute = minutos % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = { TimePicker(state = estado) },
        confirmButton = {
            TextButton(onClick = { onConfirm(hm(estado.hour, estado.minute)) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
