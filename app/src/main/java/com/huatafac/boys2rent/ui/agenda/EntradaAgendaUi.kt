package com.huatafac.boys2rent.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huatafac.boys2rent.model.EntradaAgenda
import com.huatafac.boys2rent.model.TipoAgenda
import com.huatafac.boys2rent.model.comoHora
import com.huatafac.boys2rent.model.dataAgenda
import com.huatafac.boys2rent.model.hm
import com.huatafac.boys2rent.ui.components.B2rFormCard
import com.huatafac.boys2rent.ui.components.B2rFormSheet
import com.huatafac.boys2rent.ui.profile.ProfileClickField
import com.huatafac.boys2rent.ui.profile.ProfileTextField
import com.huatafac.boys2rent.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val ptPT: Locale = Locale.forLanguageTag("pt-PT")

/** Data da agenda ("aaaa-MM-dd") de um dia do calendário local. */
fun dataDoCalendario(cal: Calendar): String =
    dataAgenda(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))

fun dataAgendaHoje(): String = dataDoCalendario(Calendar.getInstance())

/** "aaaa-MM-dd" em milissegundos UTC (o DatePicker do Material 3 trabalha em UTC). */
private fun dataParaUtcMillis(data: String): Long? = try {
    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(data)?.time
} catch (_: Exception) {
    null
}

private fun utcMillisParaData(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(millis)

/** "Sexta, 31 de outubro" para mostrar a data de uma entrada. */
fun dataLegivel(data: String): String {
    val millis = dataParaUtcMillis(data) ?: return data
    return SimpleDateFormat("EEEE, d 'de' MMMM", ptPT).apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(millis).replaceFirstChar { it.titlecase(ptPT) }
}

/** Linha de uma entrada da agenda (hora, faixa da cor do tipo, título e texto). */
@Composable
fun EntradaAgendaRow(entrada: EntradaAgenda, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tipo = entrada.tipoAgenda
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Editar", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            entrada.horaMin?.comoHora() ?: "Dia",
            color = B2rMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(48.dp)
        )
        Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(Color(tipo.corHex)))
        Column(Modifier.weight(1f)) {
            Text(entrada.titulo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = B2rInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(tipo.rotulo, entrada.texto).filter { it.isNotBlank() }.joinToString(" · "),
                fontSize = 13.sp,
                color = B2rMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Lista de entradas num cartão branco, com divisórias. */
@Composable
fun EntradasAgendaCard(entradas: List<EntradaAgenda>, onOpen: (EntradaAgenda) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
        Column {
            entradas.forEachIndexed { i, entrada ->
                EntradaAgendaRow(entrada, onClick = { onOpen(entrada) })
                if (i < entradas.lastIndex) HorizontalDivider(color = B2rDivider)
            }
        }
    }
}

/**
 * Criar ou editar uma entrada da agenda. [inicial] com id em branco = nova.
 * [onSave] recebe a entrada e uma função a chamar com o erro (ou null se gravou):
 * o diálogo fica aberto, com o erro à vista, até a gravação correr bem.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntradaAgendaDialog(
    inicial: EntradaAgenda,
    onDismiss: () -> Unit,
    onSave: (EntradaAgenda, (String?) -> Unit) -> Unit,
    onApagar: (() -> Unit)?
) {
    val editar = inicial.id.isNotBlank()
    var tipo by remember { mutableStateOf(inicial.tipoAgenda) }
    var titulo by remember { mutableStateOf(inicial.titulo) }
    var texto by remember { mutableStateOf(inicial.texto) }
    var data by remember { mutableStateOf(inicial.data) }
    var horaMin by remember { mutableStateOf(inicial.horaMin) }
    var erro by remember { mutableStateOf<String?>(null) }
    var aGravar by remember { mutableStateOf(false) }
    var escolherData by remember { mutableStateOf(false) }
    var escolherHora by remember { mutableStateOf(false) }
    var confirmarApagar by remember { mutableStateOf(false) }

    B2rFormSheet(
        titulo = if (editar) "Editar Entrada" else "Adicionar à Agenda",
        textoBotao = if (editar) "Guardar" else "Adicionar",
        aGravar = aGravar,
        erro = erro,
        onDismiss = onDismiss,
        onConfirmar = {
            aGravar = true
            val nova = inicial.copy(tipo = tipo.name, titulo = titulo, texto = texto, data = data, horaMin = horaMin)
            onSave(nova) { msgErro ->
                aGravar = false
                erro = msgErro
            }
        },
        acaoExtra = if (onApagar != null) {
            {
                TextButton(onClick = { confirmarApagar = true }, enabled = !aGravar) {
                    Text("Apagar entrada", color = B2rWarning, fontWeight = FontWeight.SemiBold)
                }
            }
        } else null
    ) {
        B2rFormCard {
            Text("Tipo", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = B2rInk)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TipoAgenda.entries.forEach { t ->
                    FilterChip(
                        selected = tipo == t,
                        onClick = { tipo = t },
                        label = { Text(t.rotulo, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(t.corHex).copy(alpha = 0.15f),
                            selectedLabelColor = Color(t.corHex)
                        )
                    )
                }
            }
            ProfileTextField(
                label = "Título *",
                value = titulo,
                onValueChange = { titulo = it; erro = null },
                icon = Icons.Rounded.Title,
                placeholder = "Ex.: Reunião de departamento"
            )
        }

        B2rFormCard {
            ProfileClickField(
                label = "Data *",
                value = dataLegivel(data),
                icon = Icons.Rounded.CalendarToday,
                onClick = { escolherData = true }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Com hora", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = B2rInk)
                    Text(if (horaMin == null) "Dia todo" else "A uma hora marcada", fontSize = 13.sp, color = B2rMuted)
                }
                Switch(
                    checked = horaMin != null,
                    onCheckedChange = { com -> horaMin = if (com) hm(9, 0) else null },
                    colors = SwitchDefaults.colors(checkedTrackColor = B2rBlue)
                )
            }
            horaMin?.let { minutos ->
                ProfileClickField(
                    label = "Hora",
                    value = minutos.comoHora(),
                    icon = Icons.Rounded.Schedule,
                    onClick = { escolherHora = true }
                )
            }
        }

        B2rFormCard {
            ProfileTextField(
                label = "Notas (opcional)",
                value = texto,
                onValueChange = { texto = it },
                icon = Icons.AutoMirrored.Rounded.Notes,
                singleLine = false
            )
        }
    }

    if (escolherData) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = dataParaUtcMillis(data))
        DatePickerDialog(
            onDismissRequest = { escolherData = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { data = utcMillisParaData(it); erro = null }
                    escolherData = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { escolherData = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = estado)
        }
    }

    if (escolherHora) {
        val atual = horaMin ?: hm(9, 0)
        val estado = rememberTimePickerState(initialHour = atual / 60, initialMinute = atual % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { escolherHora = false },
            title = { Text("Hora") },
            text = { TimePicker(state = estado) },
            confirmButton = {
                TextButton(onClick = { horaMin = hm(estado.hour, estado.minute); escolherHora = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { escolherHora = false }) { Text("Cancelar") } }
        )
    }

    if (confirmarApagar && onApagar != null) {
        AlertDialog(
            onDismissRequest = { confirmarApagar = false },
            title = { Text("Apagar entrada?") },
            text = { Text("\"${inicial.titulo}\" sai da agenda. Não dá para desfazer.") },
            confirmButton = {
                TextButton(onClick = { confirmarApagar = false; onApagar() }) { Text("Apagar", color = B2rWarning) }
            },
            dismissButton = { TextButton(onClick = { confirmarApagar = false }) { Text("Cancelar") } }
        )
    }
}
