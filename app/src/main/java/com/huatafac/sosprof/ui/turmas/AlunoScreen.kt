package com.huatafac.sosprof.ui.turmas

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huatafac.sosprof.model.Aluno
import com.huatafac.sosprof.model.Familiar
import com.huatafac.sosprof.model.Morada
import com.huatafac.sosprof.model.NotaAluno
import com.huatafac.sosprof.model.Parentesco
import com.huatafac.sosprof.model.Turma
import com.huatafac.sosprof.model.idadeEm
import com.huatafac.sosprof.ui.components.InfoAction
import com.huatafac.sosprof.ui.components.InfoCard
import com.huatafac.sosprof.ui.components.InfoRow
import com.huatafac.sosprof.ui.components.SectionHeader
import com.huatafac.sosprof.ui.components.SosChip
import com.huatafac.sosprof.ui.components.SosFormCard
import com.huatafac.sosprof.ui.components.SosFormSheet
import com.huatafac.sosprof.ui.components.SosHeader
import com.huatafac.sosprof.ui.profile.FormRow
import com.huatafac.sosprof.ui.profile.ProfileDropdownField
import com.huatafac.sosprof.ui.profile.ProfileTextField
import com.huatafac.sosprof.ui.profile.formatarCodigoPostal
import com.huatafac.sosprof.ui.profile.formatarData
import com.huatafac.sosprof.ui.theme.*
import java.util.Calendar

/**
 * Ficha do aluno, com o mesmo formato do perfil do professor (campos dois a dois).
 * Em modo de leitura os campos não são editáveis; o lápis no topo passa para modo de edição,
 * onde as alterações ficam num rascunho até se carregar em Guardar.
 */
@Composable
fun AlunoScreen(
    aluno: Aluno,
    turma: Turma,
    onBack: () -> Unit,
    onSaveAluno: (Aluno) -> Unit,
    onDeleteAluno: () -> Unit,
    modifier: Modifier = Modifier,
    // Recebe o JPEG já reduzido; o segundo parâmetro é chamado quando o envio termina (com ou sem erro)
    onUploadFoto: (ByteArray, () -> Unit) -> Unit = { _, terminou -> terminou() }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editando by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFotoMenu by remember { mutableStateOf(false) }
    var aEnviarFoto by remember { mutableStateOf(false) }
    // Uma foto guardada só noutro dispositivo (file://) não abre aqui: mostra as iniciais
    var fotoFalhou by remember(aluno.fotoUrl) { mutableStateOf(false) }
    // Rascunho das alterações. Fora da edição acompanha o aluno (ex.: depois de guardar);
    // durante a edição só a foto é sincronizada, para não perder o que se está a escrever.
    var rascunho by remember { mutableStateOf(aluno) }
    LaunchedEffect(aluno) {
        rascunho = if (editando) rascunho.copy(fotoUrl = aluno.fotoUrl) else aluno
    }

    val enviarFoto: (Bitmap?) -> Unit = { bitmap ->
        if (bitmap != null) {
            aEnviarFoto = true
            scope.launch {
                val jpeg = fotoParaJpeg(bitmap)
                onUploadFoto(jpeg) { aEnviarFoto = false }
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { enviarFoto(it) }
    val permissaoCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        if (concedida) cameraLauncher.launch(null)
    }
    val galeriaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            aEnviarFoto = true
            scope.launch {
                val bitmap = lerFoto(context, uri)
                if (bitmap == null) {
                    aEnviarFoto = false
                } else {
                    enviarFoto(bitmap)
                }
            }
        }
    }

    val cancelarEdicao = {
        rascunho = aluno
        editando = false
    }
    val podeGuardar = rascunho.nome.isNotBlank()
    val guardar = {
        if (podeGuardar) {
            editando = false
            onSaveAluno(limparAluno(rascunho))
        }
    }

    // Em edição, o botão "voltar" do sistema cancela a edição em vez de sair da ficha
    BackHandler(enabled = editando) { cancelarEdicao() }

    val mostrado = if (editando) rascunho else aluno
    val idade = remember(mostrado.dataNascimento) {
        val hoje = Calendar.getInstance()
        idadeEm(
            mostrado.dataNascimento,
            hoje.get(Calendar.DAY_OF_MONTH),
            hoje.get(Calendar.MONTH) + 1,
            hoje.get(Calendar.YEAR)
        )
    }
    val partesNome = mostrado.nome.split(" ").filter { it.isNotBlank() }
    val iniciais = listOfNotNull(partesNome.firstOrNull(), partesNome.drop(1).lastOrNull())
        .joinToString("") { it.first().uppercase() }

    Column(
        modifier
            .fillMaxSize()
            .background(SosBackground)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        SosHeader(bottomPadding = 40.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editando) {
                    IconButton(onClick = cancelarEdicao) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancelar edição", tint = Color.White)
                    }
                    Text("A editar aluno", color = SosBlueOnDark, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = guardar, enabled = podeGuardar) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "Guardar alterações",
                            tint = if (podeGuardar) Color.White else SosBlueOnDark
                        )
                    }
                } else {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar à turma", tint = Color.White)
                    }
                    Text(
                        "${turma.nome} · N.º ${aluno.numero}",
                        color = SosBlueOnDark,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { rascunho = aluno; editando = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Editar aluno", tint = Color.White)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Eliminar aluno", tint = Color.White)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(SosBlueLight)
                            .border(3.dp, Color.White, CircleShape)
                            .clickable(enabled = !aEnviarFoto, onClickLabel = "Alterar foto do aluno") { showFotoMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            aluno.fotoUrl.isNotBlank() && !fotoFalhou -> AsyncImage(
                                model = aluno.fotoUrl,
                                contentDescription = "Foto de ${aluno.nome}",
                                contentScale = ContentScale.Crop,
                                onError = { fotoFalhou = true },
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                            iniciais.isBlank() -> Icon(Icons.Rounded.Person, contentDescription = null, tint = SosBlueDark, modifier = Modifier.size(36.dp))
                            else -> Text(iniciais, color = SosBlueDark, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }
                        if (aEnviarFoto) {
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                    // Pequeno distintivo de câmara para mostrar que a foto se pode mudar
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = SosBlueDark, modifier = Modifier.size(15.dp))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(mostrado.nome.ifBlank { "Nome do aluno" }, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    idade?.let {
                        Text(if (it == 1) "1 ano" else "$it anos", color = SosBlueOnDark, fontSize = 14.sp)
                    }
                }
            }
        }

        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!editando) {
                FichaLeitura(
                    aluno = aluno,
                    context = context,
                    onNovoTema = {
                        // Abre a edição já com um tema vazio no fim
                        rascunho = aluno.copy(notas = aluno.notas + NotaAluno())
                        editando = true
                    },
                    onNovoFamiliar = {
                        // Abre a edição já com um familiar vazio no fim (o primeiro fica encarregado)
                        rascunho = aluno.copy(familiares = aluno.familiares + novoFamiliar(primeiro = aluno.familiares.isEmpty()))
                        editando = true
                    }
                )
            } else {
                SectionHeader("Dados do aluno")
                Cartao {
                    FormRow(
                        left = {
                            Campo(
                                "Nome completo${if (editando) " *" else ""}", mostrado.nome, Icons.Rounded.PersonOutline, editando,
                                onChange = { rascunho = rascunho.copy(nome = it) }
                            )
                        },
                        right = {
                            Campo(
                                "Data de nascimento", mostrado.dataNascimento, Icons.Rounded.CalendarToday, editando,
                                placeholder = "DD/MM/AAAA",
                                keyboardType = KeyboardType.Number,
                                onChange = { rascunho = rascunho.copy(dataNascimento = formatarData(it)) }
                            )
                        }
                    )
                    FormRow(
                        left = {
                            Campo(
                                "NIF", mostrado.nif, Icons.Rounded.CreditCard, editando,
                                keyboardType = KeyboardType.Number,
                                onChange = { rascunho = rascunho.copy(nif = it.filter(Char::isDigit).take(9)) }
                            )
                        },
                        right = {
                            Campo(
                                "Cartão de Cidadão", mostrado.cartaoCidadao, Icons.Rounded.Badge, editando,
                                onChange = { rascunho = rascunho.copy(cartaoCidadao = it.take(20)) }
                            )
                        }
                    )
                    FormRow(
                        left = {
                            // O número identifica o aluno na turma (é a chave usada ao guardar), por isso não se edita aqui
                            Campo("Número", aluno.numero.toString(), Icons.Rounded.Tag, editando = false, onChange = {})
                        },
                        right = {
                            Campo(
                                "N.º de processo", mostrado.numeroProcesso, Icons.Rounded.Numbers, editando,
                                onChange = { rascunho = rascunho.copy(numeroProcesso = it.take(20)) }
                            )
                        }
                    )
                    Campo(
                        "Observações", mostrado.observacoes, Icons.AutoMirrored.Rounded.Notes, editando,
                        singleLine = false,
                        onChange = { rascunho = rascunho.copy(observacoes = it) }
                    )
                }

                SectionHeader("Saúde")
                Cartao {
                    Campo(
                        "Alergias e cuidados de saúde", mostrado.saude, Icons.Rounded.MedicalServices, editando,
                        placeholder = "Ex.: alergia a frutos secos; asma (bomba na mochila)",
                        singleLine = false,
                        onChange = { rascunho = rascunho.copy(saude = it) }
                    )
                }

                SectionHeader("Morada")
                val morada = mostrado.morada ?: Morada()
                val atualizarMorada: (Morada) -> Unit = { rascunho = rascunho.copy(morada = it) }
                Cartao {
                    Campo(
                        "Morada", morada.rua, Icons.Rounded.Home, editando,
                        onChange = { atualizarMorada(morada.copy(rua = it)) },
                        acao = if (!editando && temMorada(mostrado.morada)) {
                            { AcaoIcone(Icons.Rounded.Map, "Abrir morada no mapa") { abrirMapa(context, morada) } }
                        } else null
                    )
                    FormRow(
                        left = {
                            Campo(
                                "Código Postal", morada.codigoPostal, Icons.Rounded.LocationOn, editando,
                                placeholder = "0000-000",
                                keyboardType = KeyboardType.Number,
                                onChange = { atualizarMorada(morada.copy(codigoPostal = formatarCodigoPostal(it))) }
                            )
                        },
                        right = {
                            Campo(
                                "Localidade", morada.localidade, Icons.Rounded.LocationCity, editando,
                                onChange = { atualizarMorada(morada.copy(localidade = it)) }
                            )
                        }
                    )
                }

                SectionHeader("Encarregado de educação e familiares")
                if (mostrado.familiares.isEmpty() && !editando) {
                    Text("Sem familiares registados.", color = SosMuted, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }
                mostrado.familiares.forEachIndexed { i, familiar ->
                    FamiliarCard(
                        familiar = familiar,
                        moradaAluno = mostrado.morada,
                        editando = editando,
                        context = context,
                        onChange = { novo ->
                            rascunho = rascunho.copy(
                                familiares = rascunho.familiares.mapIndexed { j, f ->
                                    when {
                                        j == i -> novo
                                        // Só pode haver um encarregado de educação
                                        novo.encarregadoEducacao && !familiar.encarregadoEducacao -> f.copy(encarregadoEducacao = false)
                                        else -> f
                                    }
                                }
                            )
                        },
                        onRemove = {
                            rascunho = rascunho.copy(familiares = rascunho.familiares.filterIndexed { j, _ -> j != i })
                        }
                    )
                }
                if (editando) {
                    OutlinedButton(
                        onClick = {
                            rascunho = rascunho.copy(
                                familiares = rascunho.familiares + novoFamiliar(primeiro = rascunho.familiares.isEmpty())
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Adicionar familiar")
                    }

                    SectionHeader("Outros temas")
                    rascunho.notas.forEachIndexed { i, nota ->
                        val atualizarNota: (NotaAluno) -> Unit = { nova ->
                            rascunho = rascunho.copy(notas = rascunho.notas.mapIndexed { j, n -> if (j == i) nova else n })
                        }
                        Cartao {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    nota.titulo.ifBlank { "Novo tema" },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SosInk,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { rascunho = rascunho.copy(notas = rascunho.notas.filterIndexed { j, _ -> j != i }) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remover tema", tint = SosMuted)
                                }
                            }
                            Campo(
                                "Título", nota.titulo, Icons.Rounded.Title, editando = true,
                                placeholder = "Ex.: Apoio educativo",
                                onChange = { atualizarNota(nota.copy(titulo = it.take(60))) }
                            )
                            Campo(
                                "Texto", nota.texto, Icons.AutoMirrored.Rounded.Notes, editando = true,
                                singleLine = false,
                                onChange = { atualizarNota(nota.copy(texto = it)) }
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { rascunho = rascunho.copy(notas = rascunho.notas + NotaAluno()) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Novo tema")
                    }

                    Button(
                        onClick = guardar,
                        enabled = podeGuardar,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SosBlue, contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Guardar", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    if (!podeGuardar) {
                        Text("O nome do aluno é obrigatório.", color = SosWarningInk, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }

            Text(
                "Dados pessoais de menores: usa-os só para fins escolares e não os partilhes fora da escola.",
                color = SosMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }
    }

    if (showFotoMenu) {
        AlertDialog(
            onDismissRequest = { showFotoMenu = false },
            title = { Text("Foto do aluno") },
            text = {
                Column {
                    TextButton(onClick = {
                        showFotoMenu = false
                        val permissao = Manifest.permission.CAMERA
                        if (ContextCompat.checkSelfPermission(context, permissao) == PackageManager.PERMISSION_GRANTED) {
                            cameraLauncher.launch(null)
                        } else {
                            permissaoCamera.launch(permissao)
                        }
                    }) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("Tirar foto")
                    }
                    TextButton(onClick = {
                        showFotoMenu = false
                        galeriaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("Escolher da galeria")
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showFotoMenu = false }) { Text("Cancelar") } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar aluno") },
            text = { Text("Tem a certeza que deseja eliminar ${aluno.nome} da turma?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAluno()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SosWarning)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/** Familiar novo e vazio: o primeiro do aluno é a mãe e fica encarregado de educação; os seguintes, o pai. */
private fun novoFamiliar(primeiro: Boolean) = Familiar(
    parentesco = if (primeiro) Parentesco.MAE else Parentesco.PAI,
    encarregadoEducacao = primeiro
)

private fun temMorada(morada: Morada?): Boolean =
    morada != null && (morada.rua.isNotBlank() || morada.codigoPostal.isNotBlank() || morada.localidade.isNotBlank())

/** Antes de gravar: tira espaços a mais, apaga a morada vazia e os familiares sem dados. */
private fun limparAluno(aluno: Aluno): Aluno {
    val morada = aluno.morada?.let { Morada(it.rua.trim(), it.codigoPostal.trim(), it.localidade.trim()) }
    val familiares = aluno.familiares
        .map { it.copy(nome = it.nome.trim(), telemovel = it.telemovel.trim(), email = it.email.trim(), profissao = it.profissao.trim()) }
        .filter { it.nome.isNotBlank() || it.telemovel.isNotBlank() || it.email.isNotBlank() }
    return aluno.copy(
        nome = aluno.nome.trim(),
        observacoes = aluno.observacoes.trim(),
        morada = if (temMorada(morada)) morada else null,
        familiares = familiares,
        numeroProcesso = aluno.numeroProcesso.trim(),
        saude = aluno.saude.trim(),
        notas = aluno.notas
            .map { NotaAluno(it.titulo.trim(), it.texto.trim()) }
            .filter { it.titulo.isNotBlank() || it.texto.isNotBlank() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlunoDialog(
    aluno: Aluno?,
    onDismiss: () -> Unit,
    onSave: (Aluno) -> Unit,
    // Números já usados por outros alunos da turma (gravar por cima apagaria esse aluno)
    numerosOcupados: Set<Int> = emptySet()
) {
    var numeroText by remember { mutableStateOf(aluno?.numero?.toString() ?: "") }
    var nomeText by remember { mutableStateOf(aluno?.nome ?: "") }
    var cartaoCidadaoText by remember { mutableStateOf(aluno?.cartaoCidadao ?: "") }
    var nifText by remember { mutableStateOf(aluno?.nif ?: "") }
    var dataNascText by remember { mutableStateOf(aluno?.dataNascimento ?: "") }
    var obsText by remember { mutableStateOf(aluno?.observacoes ?: "") }

    var ruaText by remember { mutableStateOf(aluno?.morada?.rua ?: "") }
    var cpText by remember { mutableStateOf(aluno?.morada?.codigoPostal ?: "") }
    var localText by remember { mutableStateOf(aluno?.morada?.localidade ?: "") }

    val encarregado = aluno?.encarregado ?: aluno?.familiares?.firstOrNull()
    val outroFamiliar = aluno?.outrosFamiliares?.firstOrNull() ?: aluno?.familiares?.drop(1)?.firstOrNull()

    var encNomeText by remember { mutableStateOf(encarregado?.nome ?: "") }
    var encParentesco by remember { mutableStateOf(encarregado?.parentesco ?: Parentesco.MAE) }
    var encTelText by remember { mutableStateOf(encarregado?.telemovel ?: "") }
    var encEmailText by remember { mutableStateOf(encarregado?.email ?: "") }
    var encProfissaoText by remember { mutableStateOf(encarregado?.profissao ?: "") }

    var outroNomeText by remember { mutableStateOf(outroFamiliar?.nome ?: "") }
    var outroParentesco by remember { mutableStateOf(outroFamiliar?.parentesco ?: Parentesco.PAI) }
    var outroTelText by remember { mutableStateOf(outroFamiliar?.telemovel ?: "") }
    var outroEmailText by remember { mutableStateOf(outroFamiliar?.email ?: "") }
    var outroProfissaoText by remember { mutableStateOf(outroFamiliar?.profissao ?: "") }

    var erro by remember { mutableStateOf<String?>(null) }
    val parentescos = Parentesco.entries.map { it.descricao }
    val parentescoDe: (String) -> Parentesco = { d -> Parentesco.entries.firstOrNull { it.descricao == d } ?: Parentesco.OUTRO }

    SosFormSheet(
        titulo = if (aluno?.nome.isNullOrBlank()) "Adicionar Aluno" else "Editar Aluno",
        textoBotao = if (aluno?.nome.isNullOrBlank()) "Adicionar aluno" else "Guardar",
        erro = erro,
        onDismiss = onDismiss,
        onConfirmar = {
            val num = numeroText.toIntOrNull()
            when {
                num == null || num <= 0 -> erro = "Indica o número do aluno."
                num != aluno?.numero && num in numerosOcupados -> erro = "Já existe um aluno com o n.º $num nesta turma."
                nomeText.isBlank() -> erro = "Indica o nome do aluno."
                else -> {
                    val morada = if (ruaText.isNotBlank() || cpText.isNotBlank() || localText.isNotBlank()) {
                        Morada(ruaText.trim(), cpText.trim(), localText.trim())
                    } else aluno?.morada

                    val familiares = mutableListOf<Familiar>()
                    if (encNomeText.isNotBlank() || encTelText.isNotBlank() || encEmailText.isNotBlank()) {
                        familiares.add(
                            Familiar(
                                nome = encNomeText.trim(),
                                parentesco = encParentesco,
                                telemovel = encTelText.trim(),
                                email = encEmailText.trim(),
                                profissao = encProfissaoText.trim(),
                                encarregadoEducacao = true
                            )
                        )
                    }
                    if (outroNomeText.isNotBlank() || outroTelText.isNotBlank() || outroEmailText.isNotBlank()) {
                        familiares.add(
                            Familiar(
                                nome = outroNomeText.trim(),
                                parentesco = outroParentesco,
                                telemovel = outroTelText.trim(),
                                email = outroEmailText.trim(),
                                profissao = outroProfissaoText.trim(),
                                encarregadoEducacao = false
                            )
                        )
                    }

                    // copy() mantém os campos que o formulário não mostra (n.º de processo, saúde, notas)
                    val updatedAluno = (aluno ?: Aluno()).copy(
                        numero = num,
                        nome = nomeText.trim(),
                        cartaoCidadao = cartaoCidadaoText.trim(),
                        nif = nifText,
                        faltasPeriodo = aluno?.faltasPeriodo ?: 0,
                        dataNascimento = dataNascText,
                        morada = morada,
                        observacoes = obsText.trim(),
                        familiares = if (familiares.isNotEmpty()) familiares else (aluno?.familiares ?: emptyList())
                    )
                    onSave(updatedAluno)
                }
            }
        }
    ) {
        SosFormCard("Dados do aluno") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileTextField(
                    label = "N.º *",
                    value = numeroText,
                    onValueChange = { numeroText = it.filter(Char::isDigit).take(4); erro = null },
                    icon = Icons.Rounded.Tag,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.width(110.dp)
                )
                ProfileTextField(
                    label = "Data de nascimento",
                    value = dataNascText,
                    onValueChange = { dataNascText = formatarData(it) },
                    icon = Icons.Rounded.CalendarToday,
                    placeholder = "DD/MM/AAAA",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
            ProfileTextField(
                label = "Nome completo *",
                value = nomeText,
                onValueChange = { nomeText = it; erro = null },
                icon = Icons.Rounded.PersonOutline
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileTextField(
                    label = "Cartão de Cidadão",
                    value = cartaoCidadaoText,
                    onValueChange = { cartaoCidadaoText = it.take(20) },
                    icon = Icons.Rounded.Badge,
                    modifier = Modifier.weight(1f)
                )
                ProfileTextField(
                    label = "NIF",
                    value = nifText,
                    onValueChange = { nifText = it.filter(Char::isDigit).take(9) },
                    icon = Icons.Rounded.CreditCard,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
            ProfileTextField(
                label = "Observações",
                value = obsText,
                onValueChange = { obsText = it },
                icon = Icons.AutoMirrored.Rounded.Notes,
                singleLine = false
            )
        }

        SosFormCard("Morada") {
            ProfileTextField(
                label = "Rua",
                value = ruaText,
                onValueChange = { ruaText = it },
                icon = Icons.Rounded.Home
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileTextField(
                    label = "Código postal",
                    value = cpText,
                    onValueChange = { cpText = formatarCodigoPostal(it) },
                    icon = Icons.Rounded.LocationOn,
                    placeholder = "0000-000",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                ProfileTextField(
                    label = "Localidade",
                    value = localText,
                    onValueChange = { localText = it },
                    icon = Icons.Rounded.LocationCity,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SosFormCard("Encarregado de educação") {
            ProfileTextField(
                label = "Nome",
                value = encNomeText,
                onValueChange = { encNomeText = it },
                icon = Icons.Rounded.PersonOutline
            )
            ProfileDropdownField(
                label = "Parentesco",
                value = encParentesco.descricao,
                options = parentescos,
                onValueChange = { encParentesco = parentescoDe(it) },
                icon = Icons.Rounded.FamilyRestroom
            )
            ProfileTextField(
                label = "Telemóvel",
                value = encTelText,
                onValueChange = { encTelText = it },
                icon = Icons.Rounded.Phone,
                keyboardType = KeyboardType.Phone
            )
            ProfileTextField(
                label = "E-mail",
                value = encEmailText,
                onValueChange = { encEmailText = it },
                icon = Icons.Rounded.MailOutline,
                keyboardType = KeyboardType.Email
            )
            ProfileTextField(
                label = "Profissão",
                value = encProfissaoText,
                onValueChange = { encProfissaoText = it },
                icon = Icons.Rounded.WorkOutline
            )
        }

        SosFormCard("Outro familiar (opcional)") {
            ProfileTextField(
                label = "Nome",
                value = outroNomeText,
                onValueChange = { outroNomeText = it },
                icon = Icons.Rounded.PersonOutline
            )
            ProfileDropdownField(
                label = "Parentesco",
                value = outroParentesco.descricao,
                options = parentescos,
                onValueChange = { outroParentesco = parentescoDe(it) },
                icon = Icons.Rounded.FamilyRestroom
            )
            ProfileTextField(
                label = "Telemóvel",
                value = outroTelText,
                onValueChange = { outroTelText = it },
                icon = Icons.Rounded.Phone,
                keyboardType = KeyboardType.Phone
            )
            ProfileTextField(
                label = "E-mail",
                value = outroEmailText,
                onValueChange = { outroEmailText = it },
                icon = Icons.Rounded.MailOutline,
                keyboardType = KeyboardType.Email
            )
            ProfileTextField(
                label = "Profissão",
                value = outroProfissaoText,
                onValueChange = { outroProfissaoText = it },
                icon = Icons.Rounded.WorkOutline
            )
        }
    }
}

/** Cartão branco com os campos empilhados, como no perfil do professor. */
@Composable
private fun Cartao(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/** Campo no formato do perfil; em leitura não é editável e mostra "—" quando está vazio. */
@Composable
private fun Campo(
    rotulo: String,
    valor: String,
    icon: ImageVector,
    editando: Boolean,
    onChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    acao: (@Composable () -> Unit)? = null
) {
    ProfileTextField(
        label = rotulo,
        value = valor,
        onValueChange = onChange,
        icon = icon,
        placeholder = if (editando) placeholder else "—",
        keyboardType = keyboardType,
        readOnly = !editando,
        singleLine = singleLine,
        trailing = acao
    )
}

@Composable
private fun FamiliarCard(
    familiar: Familiar,
    moradaAluno: Morada?,
    editando: Boolean,
    context: Context,
    onChange: (Familiar) -> Unit,
    onRemove: () -> Unit
) {
    Cartao {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                familiar.nome.ifBlank { "Novo familiar" },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = SosInk,
                modifier = Modifier.weight(1f)
            )
            if (familiar.encarregadoEducacao) {
                SosChip("Encarregado", background = SosBlueLight, color = SosBlueDark)
            }
            if (editando) {
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remover familiar", tint = SosMuted)
                }
            }
        }

        FormRow(
            left = {
                Campo(
                    "Nome", familiar.nome, Icons.Rounded.PersonOutline, editando,
                    onChange = { onChange(familiar.copy(nome = it)) }
                )
            },
            right = {
                if (editando) {
                    ProfileDropdownField(
                        label = "Parentesco",
                        value = familiar.parentesco.descricao,
                        options = Parentesco.entries.map { it.descricao },
                        onValueChange = { escolhido ->
                            Parentesco.entries.firstOrNull { it.descricao == escolhido }?.let { onChange(familiar.copy(parentesco = it)) }
                        },
                        icon = Icons.Rounded.FamilyRestroom
                    )
                } else {
                    Campo("Parentesco", familiar.parentesco.descricao, Icons.Rounded.FamilyRestroom, editando = false, onChange = {})
                }
            }
        )
        FormRow(
            left = {
                Campo(
                    "Telemóvel", familiar.telemovel, Icons.Rounded.Phone, editando,
                    keyboardType = KeyboardType.Phone,
                    onChange = { onChange(familiar.copy(telemovel = it)) },
                    acao = if (!editando && familiar.telemovel.isNotBlank()) {
                        {
                            AcaoIcone(Icons.Rounded.Call, "Ligar a ${familiar.nome}") {
                                abrir(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${familiar.telemovel.filter { it.isDigit() || it == '+' }}")))
                            }
                        }
                    } else null
                )
            },
            right = {
                Campo(
                    "E-mail", familiar.email, Icons.Rounded.MailOutline, editando,
                    keyboardType = KeyboardType.Email,
                    onChange = { onChange(familiar.copy(email = it)) },
                    acao = if (!editando && familiar.email.isNotBlank()) {
                        {
                            AcaoIcone(Icons.Rounded.Email, "Enviar e-mail a ${familiar.nome}") {
                                abrir(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${familiar.email}")))
                            }
                        }
                    } else null
                )
            }
        )
        FormRow(
            left = {
                Campo(
                    "Profissão", familiar.profissao, Icons.Rounded.WorkOutline, editando,
                    onChange = { onChange(familiar.copy(profissao = it)) }
                )
            },
            right = {
                // A morada própria do familiar não se edita aqui; mantém-se a que já existe
                val morada = familiar.morada ?: moradaAluno
                Campo(
                    if (familiar.morada == null) "Morada (a do aluno)" else "Morada",
                    if (temMorada(morada)) morada!!.linhaCompleta else "",
                    Icons.Rounded.Home,
                    editando = false,
                    onChange = {},
                    acao = if (!editando && temMorada(morada)) {
                        { AcaoIcone(Icons.Rounded.Map, "Abrir morada no mapa") { abrirMapa(context, morada!!) } }
                    } else null
                )
            }
        )
        if (editando) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Encarregado de educação", fontSize = 14.sp, color = SosInk, modifier = Modifier.weight(1f))
                Switch(
                    checked = familiar.encarregadoEducacao,
                    onCheckedChange = { onChange(familiar.copy(encarregadoEducacao = it)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = SosBlue)
                )
            }
        }
    }
}

/** Botão pequeno dentro do campo (ligar, e-mail, mapa). */
@Composable
private fun AcaoIcone(icon: ImageVector, descricao: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(icon, contentDescription = descricao, tint = SosBlue, modifier = Modifier.size(20.dp))
    }
}

/** Vista de leitura da ficha: linhas com ícone, rótulo e valor, e atalhos para ligar, e-mail e mapa. */
@Composable
private fun FichaLeitura(aluno: Aluno, context: Context, onNovoTema: () -> Unit, onNovoFamiliar: () -> Unit) {
    SectionHeader("Dados do aluno")
    InfoCard {
        InfoRow(Icons.Rounded.Numbers, "N.º de processo", aluno.numeroProcesso.ifBlank { "—" })
        InfoRow(Icons.Rounded.Cake, "Data de nascimento", aluno.dataNascimento.ifBlank { "—" })
        if (aluno.cartaoCidadao.isNotBlank()) {
            InfoRow(Icons.Rounded.Badge, "Cartão de Cidadão", aluno.cartaoCidadao)
        }
        if (aluno.nif.isNotBlank()) {
            InfoRow(Icons.Rounded.AccountBalance, "NIF", aluno.nif)
        }
        aluno.morada?.takeIf { temMorada(it) }?.let { morada ->
            InfoRow(Icons.Rounded.Home, "Morada", morada.linhaCompleta) {
                InfoAction(Icons.Rounded.Map, "Abrir morada no mapa") { abrirMapa(context, morada) }
            }
        }
        if (aluno.observacoes.isNotBlank()) {
            InfoRow(Icons.AutoMirrored.Rounded.Notes, "Observações", aluno.observacoes)
        }
    }

    SectionHeader("Saúde")
    InfoCard {
        InfoRow(
            Icons.Rounded.MedicalServices,
            "Alergias e cuidados de saúde",
            aluno.saude.ifBlank { "Sem informação registada" },
            valorCor = if (aluno.saude.isBlank()) SosMuted else SosInk
        )
    }

    SectionHeader("Encarregado de educação")
    val encarregado = aluno.encarregado
    if (encarregado == null) {
        Text("Sem encarregado de educação registado.", color = SosMuted, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp))
    } else {
        FamiliarInfoCard(encarregado, moradaAluno = aluno.morada, context = context)
    }

    if (aluno.outrosFamiliares.isNotEmpty()) {
        SectionHeader("Pais e familiares")
        aluno.outrosFamiliares.forEach { familiar ->
            FamiliarInfoCard(familiar, moradaAluno = aluno.morada, context = context)
        }
    }
    OutlinedButton(
        onClick = onNovoFamiliar,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Familiar")
    }

    SectionHeader("Outros temas")
    val temas = aluno.notas.filter { it.titulo.isNotBlank() || it.texto.isNotBlank() }
    if (temas.isNotEmpty()) {
        InfoCard {
            temas.forEach { nota ->
                InfoRow(Icons.AutoMirrored.Rounded.Notes, nota.titulo.ifBlank { "Sem título" }, nota.texto.ifBlank { "—" })
            }
        }
    }
    OutlinedButton(
        onClick = onNovoTema,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Novo tema")
    }
}

@Composable
private fun FamiliarInfoCard(familiar: Familiar, moradaAluno: Morada?, context: Context) {
    InfoCard {
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(familiar.nome, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SosInk)
                Text(
                    listOf(familiar.parentesco.descricao, familiar.profissao).filter { it.isNotBlank() }.joinToString(" · "),
                    fontSize = 13.sp,
                    color = SosMuted
                )
            }
            if (familiar.encarregadoEducacao) {
                SosChip("Encarregado", background = SosBlueLight, color = SosBlueDark)
            }
        }
        if (familiar.telemovel.isNotBlank()) {
            InfoRow(Icons.Rounded.Phone, "Telemóvel", familiar.telemovel) {
                InfoAction(Icons.Rounded.Phone, "Ligar a ${familiar.nome}") {
                    abrir(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${familiar.telemovel.filter { it.isDigit() || it == '+' }}")))
                }
            }
        }
        if (familiar.email.isNotBlank()) {
            InfoRow(Icons.Rounded.Email, "E-mail", familiar.email) {
                InfoAction(Icons.Rounded.Email, "Enviar e-mail a ${familiar.nome}") {
                    abrir(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${familiar.email}")))
                }
            }
        }
        val morada = (familiar.morada ?: moradaAluno)?.takeIf { temMorada(it) }
        if (morada != null) {
            InfoRow(
                Icons.Rounded.Home,
                if (familiar.morada == null) "Morada (a mesma do aluno)" else "Morada",
                morada.linhaCompleta
            ) {
                InfoAction(Icons.Rounded.Map, "Abrir morada no mapa") { abrirMapa(context, morada) }
            }
        }
    }
}

private fun abrirMapa(context: Context, morada: Morada) {
    abrir(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(morada.linhaCompleta))))
}

private fun abrir(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Sem app para tratar o pedido (ex.: emulador sem mapas); não há nada a fazer
    }
}

@Preview(showBackground = true, heightDp = 1400, widthDp = 390)
@Composable
private fun AlunoScreenPreview() {
    SOSProfTheme {
        val aluno = Aluno(numero = 1, nome = "Aluno")
        val turma = Turma(id = "preview", ano = "7.º", letra = "A", alunos = listOf(aluno))
        AlunoScreen(aluno = aluno, turma = turma, onBack = {}, onSaveAluno = {}, onDeleteAluno = {})
    }
}
