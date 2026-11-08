package com.huatafac.sosprof.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huatafac.sosprof.model.Aniversario
import com.huatafac.sosprof.model.Aula
import com.huatafac.sosprof.model.DiaEspecial
import com.huatafac.sosprof.model.TipoDiaEspecial
import com.huatafac.sosprof.model.aniversariosEntre
import com.huatafac.sosprof.model.semanaDomingoASabado
import com.huatafac.sosprof.model.EntradaAgenda
import com.huatafac.sosprof.model.doDia
import com.huatafac.sosprof.model.Turma
import com.huatafac.sosprof.model.aulaAtual
import com.huatafac.sosprof.model.comoHora
import com.huatafac.sosprof.model.diasEspeciaisDoDia
import com.huatafac.sosprof.model.proximasAulas
import com.huatafac.sosprof.ui.agenda.DiasEspeciaisCard
import com.huatafac.sosprof.ui.agenda.EntradasAgendaCard
import com.huatafac.sosprof.ui.agenda.dataLegivel
import com.huatafac.sosprof.ui.agenda.dataDoCalendario
import com.huatafac.sosprof.ui.components.SectionHeader
import com.huatafac.sosprof.ui.components.SosHeader
import com.huatafac.sosprof.ui.components.SosLogo
import com.huatafac.sosprof.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val ptPT: Locale = Locale.forLanguageTag("pt-PT")

/** Dia útil (1 = segunda … 7 = domingo) e minutos desde a meia-noite. */
private data class Agora(val dia: Int, val minutos: Int, val dataTexto: String, val data: String)

private fun agora(): Agora {
    val c = Calendar.getInstance()
    val dia = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
    val texto = SimpleDateFormat("EEEE, d 'de' MMMM", ptPT).format(c.time)
        .replaceFirstChar { it.titlecase(ptPT) }
    return Agora(dia, c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE), texto, dataDoCalendario(c))
}

@Composable
fun HomeScreen(
    userName: String,
    horario: List<Aula>,
    turmas: List<Turma>,
    onOpenProfile: () -> Unit,
    onSignOut: () -> Unit,
    onOpenAgenda: () -> Unit,
    onOpenTurma: (String) -> Unit,
    modifier: Modifier = Modifier,
    agenda: List<EntradaAgenda> = emptyList(),
    // Data ("aaaa-MM-dd") da nova entrada
    onAddEntrada: (String) -> Unit = {},
    onOpenEntrada: (EntradaAgenda) -> Unit = {},
    // URI da foto do perfil ("" = sem foto: mostra as iniciais)
    fotoPerfil: String = "",
    // turmaId e n.º do aluno: abre a ficha do aluno (aniversários)
    onOpenAluno: (String, Int) -> Unit = { turmaId, _ -> onOpenTurma(turmaId) }
) {
    // Atualiza a hora a cada 30 s para o destaque "a decorrer" mudar sozinho
    val now by produceState(initialValue = agora()) {
        while (true) {
            delay(30_000)
            value = agora()
        }
    }
    val atual = horario.aulaAtual(now.dia, now.minutos)
    val seguintes = horario.proximasAulas(now.dia, now.minutos)
    val iniciais = userName.split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "P" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SosBackground)
            .verticalScroll(rememberScrollState())
    ) {
        SosHeader(bottomPadding = 44.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SosLogo(modifier = Modifier.weight(1f))
                IconButton(onClick = onSignOut) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                        contentDescription = "Terminar sessão",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SosBlueLight)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable(onClickLabel = "Abrir perfil", onClick = onOpenProfile),
                    contentAlignment = Alignment.Center
                ) {
                    // Se a foto não abrir (ex.: ficheiro apagado), ficam as iniciais
                    var fotoFalhou by remember(fotoPerfil) { mutableStateOf(false) }
                    if (fotoPerfil.isNotBlank() && !fotoFalhou) {
                        AsyncImage(
                            model = fotoPerfil,
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            onError = { fotoFalhou = true },
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(iniciais, color = SosBlueDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(now.dataTexto, color = SosBlueOnDark, fontSize = 13.sp)
                    Text(
                        "Olá, ${userName.substringBefore(" ")}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Juntos tornamos\na educação mais fácil!",
                    color = Color.White,
                    fontFamily = FontFamily.Cursive,
                    fontSize = 16.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.rotate(-5f)
                )
            }
        }

        Column(
            modifier = Modifier
                .offset(y = (-20).dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val especiaisHoje = remember(now.data) { diasEspeciaisDoDia(now.data) }
            // Aniversários dos alunos nesta semana (domingo a sábado)
            val aniversarios = remember(now.data, turmas) {
                val (ano, mes, dia) = now.data.split("-").map { it.toInt() }
                val (domingo, sabado) = semanaDomingoASabado(ano, mes, dia)
                aniversariosEntre(turmas, domingo, sabado)
            }
            val semMaisAulas = atual == null && seguintes.isEmpty()
            // Com aulas, o feriado ou dia festivo aparece num cartão por cima; sem aulas, vai dentro do destaque
            if (!semMaisAulas && especiaisHoje.isNotEmpty()) DiasEspeciaisCard(especiaisHoje, mostrarData = false)

            DestaqueCard(
                atual = atual,
                proxima = seguintes.firstOrNull(),
                agoraMin = now.minutos,
                onOpenTurma = onOpenTurma,
                onOpenAluno = onOpenAluno,
                especiaisHoje = especiaisHoje,
                aniversarios = aniversarios,
                hoje = now.data
            )

            SectionHeader("Resto do dia") {
                TextButton(onClick = onOpenAgenda, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Ver agenda", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
            val restantes = if (atual == null) seguintes.drop(1) else seguintes
            if (restantes.isEmpty()) {
                Text(
                    "Não tens mais nada marcado para hoje.",
                    color = SosMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            } else {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                    Column {
                        restantes.forEachIndexed { i, aula ->
                            AulaRow(aula, turmas, onClick = aula.turmaId?.let { id -> { onOpenTurma(id) } })
                            if (i < restantes.lastIndex) HorizontalDivider(color = SosDivider)
                        }
                    }
                }
            }

            // Agenda pessoal (reuniões, atividades, notas) do dia, por baixo do horário
            SectionHeader("Agenda de hoje") {
                TextButton(onClick = { onAddEntrada(now.data) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Adicionar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
            val agendaHoje = agenda.doDia(now.data)
            if (agendaHoje.isEmpty()) {
                Text(
                    "Sem reuniões, atividades ou notas para hoje.",
                    color = SosMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            } else {
                EntradasAgendaCard(agendaHoje, onOpenEntrada)
            }
        }
    }
}

@Composable
private fun DestaqueCard(
    atual: Aula?,
    proxima: Aula?,
    agoraMin: Int,
    onOpenTurma: (String) -> Unit,
    onOpenAluno: (String, Int) -> Unit = { turmaId, _ -> onOpenTurma(turmaId) },
    especiaisHoje: List<DiaEspecial> = emptyList(),
    aniversarios: List<Aniversario> = emptyList(),
    hoje: String = ""
) {
    val aula = atual ?: proxima
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SosBlueDark,
        contentColor = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = SosBlueDark, spotColor = SosBlueDark)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (aula == null) {
                Text("Sem mais aulas hoje", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Aproveita para preparar as próximas aulas.", fontSize = 14.sp, color = SosBlueOnDark)
                if (especiaisHoje.isNotEmpty()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
                    especiaisHoje.forEach { DiaEspecialLinha(it) }
                }
                if (aniversarios.isNotEmpty()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
                    Text(
                        "ANIVERSÁRIOS ESTA SEMANA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = SosBlueOnDark
                    )
                    aniversarios.forEach { AniversarioLinha(it, ehHoje = it.data == hoje, onClick = { onOpenAluno(it.turmaId, it.numero) }) }
                }
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val etiqueta = if (atual != null) "A decorrer · faltam ${atual.fimMin - agoraMin} min" else "Próxima aula"
                Text(
                    etiqueta.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                Spacer(Modifier.weight(1f))
                Text("${aula.inicioMin.comoHora()} – ${aula.fimMin.comoHora()}", fontSize = 13.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(aula.titulo, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(aula.detalhe, fontSize = 14.sp, color = SosBlueOnDark)
            }
            aula.turmaId?.let { turmaId ->
                Button(
                    onClick = { onOpenTurma(turmaId) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = SosBlueDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) { Text("Abrir turma", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
            }
        }
    }
}

/** Feriado ou dia festivo de hoje, dentro do cartão azul. */
@Composable
private fun DiaEspecialLinha(dia: DiaEspecial) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Rounded.Celebration, contentDescription = null, tint = SosYellow, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(dia.nome, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (dia.tipo == TipoDiaEspecial.FERIADO) "Hoje é feriado nacional" else "Hoje é dia festivo",
                fontSize = 13.sp,
                color = SosBlueOnDark
            )
        }
    }
}

/** Aniversário de um aluno nesta semana, dentro do cartão azul; tocar abre a ficha do aluno. */
@Composable
private fun AniversarioLinha(aniversario: Aniversario, ehHoje: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = "Abrir ficha do aluno", onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.Cake, contentDescription = null, tint = SosYellow, modifier = Modifier.size(22.dp))
        // Nome, turma e idade numa só linha; o dia fica à direita
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(aniversario.nome) }
                withStyle(SpanStyle(color = SosBlueOnDark)) {
                    append(" · ${aniversario.turma} · faz ${aniversario.idade} anos")
                }
            },
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            if (ehHoje) "Hoje" else dataLegivel(aniversario.data).substringBefore(" de "),
            fontSize = 13.sp,
            fontWeight = if (ehHoje) FontWeight.Bold else FontWeight.Medium,
            color = if (ehHoje) SosYellow else Color.White
        )
    }
}

@Composable
private fun AulaRow(aula: Aula, turmas: List<Turma>, onClick: (() -> Unit)?) {
    val cor = turmas.firstOrNull { it.id == aula.turmaId }?.let { Color(it.corHex) } ?: SosMuted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(aula.inicioMin.comoHora(), color = SosMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(48.dp))
        Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(cor))
        Column(Modifier.weight(1f)) {
            Text(aula.titulo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SosInk)
            Text(aula.detalhe, fontSize = 13.sp, color = SosMuted)
        }
    }
}

@Preview(showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun HomeScreenPreview() {
    SOSProfTheme {
        HomeScreen(
            userName = "Professor",
            horario = emptyList(),
            turmas = emptyList(),
            onOpenProfile = {}, onSignOut = {}, onOpenAgenda = {}, onOpenTurma = {}
        )
    }
}
