package com.huatafac.sosprof.ui.profile

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huatafac.sosprof.ui.components.InfoCard
import com.huatafac.sosprof.ui.components.InfoRow
import com.huatafac.sosprof.ui.components.SectionHeader
import com.huatafac.sosprof.ui.components.SosHeader
import com.huatafac.sosprof.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val generos = listOf("Masculino", "Feminino", "Outro", "Prefiro não dizer")
private val niveisEnsino = listOf("Pré-escolar", "1.º Ciclo", "2.º Ciclo", "3.º Ciclo", "Ensino Secundário", "Ensino Profissional", "Ensino Superior")
private val disciplinas = listOf(
    "Português", "Matemática", "Inglês", "Francês", "Espanhol", "Alemão", "História", "Geografia",
    "Ciências Naturais", "Físico-Química", "Física", "Química", "Biologia e Geologia", "Filosofia",
    "Educação Física", "Educação Visual", "Educação Musical", "TIC", "Economia", "Educação Especial"
)
private val situacoes = listOf("Quadro", "Quadro de Zona Pedagógica (QZP)", "Contratado", "Outra")
private val areasInteresse = listOf(
    "Tecnologia", "Avaliação", "Recursos didáticos", "Inclusão", "Gestão de sala de aula",
    "Formação contínua", "Projetos", "Saúde e bem-estar"
)

private val concelhos = listOf(
    "Lisboa", "Porto", "Sintra", "Vila Nova de Gaia", "Cascais", "Loures", "Braga", "Amadora", "Matosinhos",
    "Almada", "Oeiras", "Gondomar", "Seixal", "Guimarães", "Odivelas", "Coimbra", "Vila Franca de Xira",
    "Maia", "Santa Maria da Feira", "Leiria", "Setúbal", "Barcelos", "Viseu", "Funchal", "Valongo",
    "Viana do Castelo", "Paredes", "Vila do Conde", "Torres Vedras", "Aveiro", "Mafra", "Faro",
    "Évora", "Ponta Delgada", "Santarém", "Castelo Branco", "Bragança", "Beja", "Portalegre",
    "Guarda", "Vila Real", "Angra do Heroísmo"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    uid: String,
    userEmail: String,
    viewModel: UserProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val fullName by viewModel.fullName.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val quote by viewModel.quote.collectAsStateWithLifecycle()
    val profileImageUri by viewModel.profileImageUri.collectAsStateWithLifecycle()
    val birthDate by viewModel.birthDate.collectAsStateWithLifecycle()
    val nif by viewModel.nif.collectAsStateWithLifecycle()
    val citizenCard by viewModel.citizenCard.collectAsStateWithLifecycle()
    val gender by viewModel.gender.collectAsStateWithLifecycle()
    val address by viewModel.address.collectAsStateWithLifecycle()
    val postalCode by viewModel.postalCode.collectAsStateWithLifecycle()
    val locality by viewModel.locality.collectAsStateWithLifecycle()
    val phone by viewModel.phone.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val iban by viewModel.iban.collectAsStateWithLifecycle()
    val otherContacts by viewModel.otherContacts.collectAsStateWithLifecycle()
    val educationLevel by viewModel.educationLevel.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val schoolGroup by viewModel.schoolGroup.collectAsStateWithLifecycle()
    val county by viewModel.county.collectAsStateWithLifecycle()
    val serviceYears by viewModel.serviceYears.collectAsStateWithLifecycle()
    val professionalSituation by viewModel.professionalSituation.collectAsStateWithLifecycle()
    val interestAreas by viewModel.interestAreas.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val acceptedPrivacyTerms by viewModel.acceptedPrivacyTerms.collectAsStateWithLifecycle()

    LaunchedEffect(uid) {
        if (uid.isNotBlank()) {
            viewModel.loadProfile(uid, userEmail)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editingQuote by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    // Vista de leitura por defeito; o lápis no topo passa para o formulário de edição
    var editando by remember { mutableStateOf(false) }
    val cancelarEdicao = {
        // Volta a ler o perfil gravado para descartar as alterações por guardar
        viewModel.loadProfile(uid, userEmail)
        editando = false
    }
    BackHandler(enabled = editando) { cancelarEdicao() }

    val context = LocalContext.current
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val uriString = saveBitmapToCache(context, bitmap)
            viewModel.onProfileImageUriChanged(uriString)
            scope.launch { snackbarHostState.showSnackbar("Foto de perfil capturada com sucesso!") }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Permissão de câmara necessária para tirar foto.") }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is UserProfileState.Success) {
            val message = (uiState as UserProfileState.Success).successMessage
            if (message != null) {
                // Gravou com sucesso: volta à vista de leitura
                editando = false
                snackbarHostState.showSnackbar(message)
                viewModel.clearMessage()
            } else if ((uiState as UserProfileState.Success).profile.fullName.isBlank()) {
                // Perfil ainda por preencher: abre logo o formulário
                editando = true
            }
        }
    }

    val partesNome = fullName.split(" ").filter { it.isNotBlank() }
    val iniciais = listOfNotNull(partesNome.firstOrNull(), partesNome.drop(1).lastOrNull())
        .joinToString("") { it.first().uppercase() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SosBackground,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is UserProfileState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is UserProfileState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProfile(uid, userEmail) }) {
                            Text("Tentar novamente")
                        }
                    }
                }
                is UserProfileState.Success -> {
                    Column(
                        modifier = Modifier
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
                                    Text("A editar perfil", color = SosBlueOnDark, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.saveProfile(uid) }) {
                                        Icon(Icons.Rounded.Check, contentDescription = "Guardar perfil", tint = Color.White)
                                    }
                                } else {
                                    IconButton(onClick = onBack) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { editando = true }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Editar perfil", tint = Color.White)
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(
                                    Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(SosBlueLight)
                                        .border(3.dp, Color.White, CircleShape)
                                        .clickable(onClickLabel = "Tirar foto de perfil", onClick = {
                                            val permission = android.Manifest.permission.CAMERA
                                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                                cameraLauncher.launch(null)
                                            } else {
                                                permissionLauncher.launch(permission)
                                            }
                                        }),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profileImageUri.isNotBlank()) {
                                        coil.compose.AsyncImage(
                                            model = profileImageUri,
                                            contentDescription = "Foto de perfil",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else if (iniciais.isBlank()) {
                                        Icon(Icons.Rounded.Person, contentDescription = null, tint = SosBlueDark, modifier = Modifier.size(36.dp))
                                    } else {
                                        Text(iniciais, color = SosBlueDark, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        fullName.ifBlank { "O seu nome" },
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    // Por baixo do nome: o(s) nível(is) de ensino; sem nível escolhido, nada
                                    val niveis = splitList(educationLevel).joinToString(" · ")
                                    if (niveis.isNotBlank()) {
                                        Text(niveis, color = SosBlueOnDark, fontSize = 14.sp)
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
                                PerfilLeitura(
                                    quote = quote, birthDate = birthDate, nif = nif, citizenCard = citizenCard, gender = gender,
                                    address = address, postalCode = postalCode, locality = locality,
                                    phone = phone, email = email, iban = iban, otherContacts = otherContacts,
                                    educationLevel = educationLevel, subjects = subjects, schoolGroup = schoolGroup,
                                    county = county, serviceYears = serviceYears, professionalSituation = professionalSituation,
                                    interestAreas = interestAreas, notificationsEnabled = notificationsEnabled
                                )
                            } else {
                                SectionHeader("Apresentação")
                                Cartao {
                                    Column(
                                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { editingQuote = true }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null, tint = SosBlue, modifier = Modifier.size(20.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text("Frase do perfil", fontSize = 12.sp, color = SosMuted)
                                                Text(
                                                    text = if (quote.isBlank()) "+ Adicionar uma frase" else "“$quote”",
                                                    fontSize = 15.sp,
                                                    color = if (quote.isBlank()) SosBlue else SosInk,
                                                    fontWeight = if (quote.isBlank()) FontWeight.Medium else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }

                                // Dados Pessoais
                                SectionHeader("Dados Pessoais")
                                Cartao {
                                    Column(
                                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        FormRow(
                                            left = {
                                                ProfileTextField(
                                                    label = "Nome completo *",
                                                    value = fullName,
                                                    onValueChange = viewModel::onFullNameChanged,
                                                    icon = Icons.Rounded.PersonOutline
                                                )
                                            },
                                            right = {
                                                ProfileTextField(
                                                    label = "Data de nascimento *",
                                                    value = birthDate,
                                                    onValueChange = { viewModel.onBirthDateChanged(formatarData(it)) },
                                                    icon = Icons.Rounded.CalendarToday,
                                                    placeholder = "DD/MM/AAAA",
                                                    keyboardType = KeyboardType.Number,
                                                    trailing = {
                                                        IconButton(onClick = { showDatePicker = true }, modifier = Modifier.size(36.dp)) {
                                                            Icon(
                                                                Icons.Rounded.EditCalendar,
                                                                contentDescription = "Escolher data",
                                                                tint = SosMuted,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        )
                                        FormRow(
                                            left = {
                                                ProfileTextField(
                                                    label = "NIF *",
                                                    value = nif,
                                                    onValueChange = { viewModel.onNifChanged(it.filter(Char::isDigit).take(9)) },
                                                    icon = Icons.Rounded.CreditCard,
                                                    keyboardType = KeyboardType.Number
                                                )
                                            },
                                            right = {
                                                ProfileTextField(
                                                    label = "Cartão de Cidadão *",
                                                    value = citizenCard,
                                                    onValueChange = { viewModel.onCitizenCardChanged(it.take(20)) },
                                                    icon = Icons.Rounded.Badge,
                                                    keyboardType = KeyboardType.Text
                                                )
                                            }
                                        )
                                        FormRow(
                                            left = {
                                                ProfileDropdownField(
                                                    label = "Género *",
                                                    value = gender,
                                                    options = generos,
                                                    onValueChange = viewModel::onGenderChanged,
                                                    icon = Icons.Rounded.Wc
                                                )
                                            },
                                            right = {
                                                ProfileTextField(
                                                    label = "Morada *",
                                                    value = address,
                                                    onValueChange = viewModel::onAddressChanged,
                                                    icon = Icons.Rounded.Home
                                                )
                                            }
                                        )
                                        FormRow(
                                            left = {
                                                ProfileTextField(
                                                    label = "Código Postal *",
                                                    value = postalCode,
                                                    onValueChange = { viewModel.onPostalCodeChanged(formatarCodigoPostal(it)) },
                                                    icon = Icons.Rounded.LocationOn,
                                                    placeholder = "0000-000",
                                                    keyboardType = KeyboardType.Number
                                                )
                                            },
                                            right = {
                                                ProfileAutocompleteField(
                                                    label = "Localidade *",
                                                    value = locality,
                                                    suggestions = concelhos,
                                                    onValueChange = viewModel::onLocalityChanged,
                                                    icon = Icons.Rounded.LocationOn
                                                )
                                            }
                                        )
                                    }
                                }

                                // Contacto
                                SectionHeader("Contacto")
                                Cartao {
                                    Column(
                                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        FormRow(
                                            left = {
                                                ProfileTextField(
                                                    label = "Telemóvel *",
                                                    value = phone,
                                                    onValueChange = viewModel::onPhoneChanged,
                                                    icon = Icons.Rounded.Phone,
                                                    keyboardType = KeyboardType.Phone
                                                )
                                            },
                                            right = {
                                                ProfileTextField(
                                                    label = "E-mail *",
                                                    value = email,
                                                    onValueChange = viewModel::onEmailChanged,
                                                    icon = Icons.Rounded.MailOutline,
                                                    keyboardType = KeyboardType.Email
                                                )
                                            }
                                        )
                                        FormRow(
                                            left = {
                                                ProfileTextField(
                                                    label = "Outros contactos (opcional)",
                                                    value = otherContacts,
                                                    onValueChange = viewModel::onOtherContactsChanged,
                                                    icon = Icons.Rounded.Groups,
                                                    placeholder = "Telefone fixo, WhatsApp, etc."
                                                )
                                            },
                                            right = {
                                                ProfileTextField(
                                                    label = "IBAN *",
                                                    value = iban,
                                                    onValueChange = { viewModel.onIbanChanged(it.take(34)) },
                                                    icon = Icons.Rounded.AccountBalance,
                                                    keyboardType = KeyboardType.Text,
                                                    placeholder = "PT50..."
                                                )
                                            }
                                        )
                                    }
                                }

                                // Dados Profissionais
                                SectionHeader("Dados Profissionais")
                                Cartao {
                                    Column(
                                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        FormRow(
                                            left = {
                                                // Pode escolher-se mais do que um (ex.: 3.º ciclo e secundário)
                                                ProfileMultiSelectField(
                                                    label = "Nível(is) de ensino *",
                                                    value = educationLevel,
                                                    options = niveisEnsino,
                                                    onValueChange = viewModel::onEducationLevelChanged,
                                                    icon = Icons.Rounded.School
                                                )
                                            },
                                            right = {
                                                ProfileMultiSelectField(
                                                    label = "Disciplina(s) (opcional)",
                                                    value = subjects,
                                                    options = disciplinas,
                                                    onValueChange = viewModel::onSubjectsChanged,
                                                    icon = Icons.AutoMirrored.Rounded.MenuBook
                                                )
                                            }
                                        )
                                        FormRow(
                                            left = {
                                                ProfileTextField(
                                                    label = "Agrupamento / Escola *",
                                                    value = schoolGroup,
                                                    onValueChange = viewModel::onSchoolGroupChanged,
                                                    icon = Icons.Rounded.Apartment
                                                )
                                            },
                                            right = {
                                                ProfileAutocompleteField(
                                                    label = "Concelho *",
                                                    value = county,
                                                    suggestions = concelhos,
                                                    onValueChange = viewModel::onCountyChanged,
                                                    icon = Icons.Rounded.LocationOn
                                                )
                                            }
                                        )
                                        FormRow(
                                            left = {
                                                ProfileTextField(
                                                    label = "Tempo de serviço (anos) *",
                                                    value = serviceYears,
                                                    onValueChange = { viewModel.onServiceYearsChanged(it.filter(Char::isDigit).take(2)) },
                                                    icon = Icons.Rounded.CalendarMonth,
                                                    keyboardType = KeyboardType.Number
                                                )
                                            },
                                            right = {
                                                ProfileDropdownField(
                                                    label = "Situação profissional *",
                                                    value = professionalSituation,
                                                    options = situacoes,
                                                    onValueChange = viewModel::onProfessionalSituationChanged,
                                                    icon = Icons.Rounded.PersonOutline
                                                )
                                            }
                                        )
                                    }
                                }

                                // Preferências
                                SectionHeader("Preferências")
                                Cartao {
                                    Column(
                                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        FormRow(
                                            left = {
                                                ProfileMultiSelectField(
                                                    label = "Áreas de interesse",
                                                    value = interestAreas,
                                                    options = areasInteresse,
                                                    onValueChange = viewModel::onInterestAreasChanged,
                                                    icon = Icons.Rounded.LocalOffer,
                                                    asChips = true
                                                )
                                            },
                                            right = {
                                                NotificationsBox(
                                                    enabled = notificationsEnabled,
                                                    onChange = viewModel::onNotificationsEnabledChanged
                                                )
                                            }
                                        )
                                    }
                                }

                                // Privacidade
                                SectionHeader("Privacidade")
                                Cartao {
                                    Column(
                                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { viewModel.onAcceptedPrivacyTermsChanged(!acceptedPrivacyTerms) },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = acceptedPrivacyTerms,
                                                onCheckedChange = viewModel::onAcceptedPrivacyTermsChanged,
                                                colors = CheckboxDefaults.colors(checkedColor = SosBlue)
                                            )
                                            Text(
                                                text = buildAnnotatedString {
                                                    append("Li e aceito os ")
                                                    withStyle(SpanStyle(color = SosBlue, fontWeight = FontWeight.Medium)) { append("Termos e Condições") }
                                                    append(" e a ")
                                                    withStyle(SpanStyle(color = SosBlue, fontWeight = FontWeight.Medium)) { append("Política de Privacidade") }
                                                    append(".")
                                                },
                                                fontSize = 13.sp,
                                                color = SosInk
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { viewModel.saveProfile(uid) },
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
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingQuote) {
        QuoteDialog(
            initial = quote,
            onDismiss = { editingQuote = false },
            onSave = {
                viewModel.onQuoteChanged(it.trim())
                editingQuote = false
            }
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = parseData(birthDate))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.onBirthDateChanged(dataDeMillis(it)) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/**
 * Guarda a foto do perfil na pasta privada da app (filesDir), que, ao contrário da cache,
 * o Android não apaga sozinho. O nome leva a versão ("profile_{millis}.jpg"). As cópias antigas
 * só são apagadas ao gravar o perfil, para "Cancelar" a edição continuar a mostrar a foto anterior.
 */
private fun saveBitmapToCache(context: Context, bitmap: Bitmap): String {
    val pasta = File(context.filesDir, "foto_perfil").apply { mkdirs() }
    val file = File(pasta, "profile_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return Uri.fromFile(file).toString()
}

@Composable
private fun Cartao(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 6.dp), content = content)
    }
}

/** Vista de leitura do perfil, no mesmo formato da ficha do aluno. */
@Composable
private fun PerfilLeitura(
    quote: String,
    birthDate: String,
    nif: String,
    citizenCard: String,
    gender: String,
    address: String,
    postalCode: String,
    locality: String,
    phone: String,
    email: String,
    iban: String,
    otherContacts: String,
    educationLevel: String,
    subjects: String,
    schoolGroup: String,
    county: String,
    serviceYears: String,
    professionalSituation: String,
    interestAreas: String,
    notificationsEnabled: Boolean
) {
    val ou = { v: String -> v.ifBlank { "—" } }
    val morada = listOf(address, listOf(postalCode, locality).filter { it.isNotBlank() }.joinToString(" "))
        .filter { it.isNotBlank() }
        .joinToString(", ")

    if (quote.isNotBlank()) {
        SectionHeader("Apresentação")
        InfoCard {
            InfoRow(Icons.AutoMirrored.Rounded.Notes, "Frase do perfil", "“$quote”")
        }
    }

    SectionHeader("Dados Pessoais")
    InfoCard {
        InfoRow(Icons.Rounded.Cake, "Data de nascimento", ou(birthDate))
        InfoRow(Icons.Rounded.CreditCard, "NIF", ou(nif))
        InfoRow(Icons.Rounded.Badge, "Cartão de Cidadão", ou(citizenCard))
        InfoRow(Icons.Rounded.Wc, "Género", ou(gender))
        InfoRow(Icons.Rounded.Home, "Morada", ou(morada))
    }

    SectionHeader("Contacto")
    InfoCard {
        InfoRow(Icons.Rounded.Phone, "Telemóvel", ou(phone))
        InfoRow(Icons.Rounded.MailOutline, "E-mail", ou(email))
        InfoRow(Icons.Rounded.AccountBalance, "IBAN", ou(iban))
        if (otherContacts.isNotBlank()) {
            InfoRow(Icons.Rounded.ContactPhone, "Outros contactos", otherContacts)
        }
    }

    SectionHeader("Dados Profissionais")
    InfoCard {
        InfoRow(Icons.Rounded.School, "Nível de ensino", ou(educationLevel))
        InfoRow(Icons.AutoMirrored.Rounded.MenuBook, "Disciplina(s)", ou(subjects))
        InfoRow(Icons.Rounded.Apartment, "Agrupamento / Escola", ou(schoolGroup))
        InfoRow(Icons.Rounded.LocationOn, "Concelho", ou(county))
        InfoRow(Icons.Rounded.Schedule, "Tempo de serviço", if (serviceYears.isBlank()) "—" else "$serviceYears anos")
        InfoRow(Icons.Rounded.Work, "Situação profissional", ou(professionalSituation))
        if (interestAreas.isNotBlank()) {
            InfoRow(Icons.Rounded.LocalOffer, "Áreas de interesse", interestAreas)
        }
    }

    SectionHeader("Preferências")
    InfoCard {
        InfoRow(
            Icons.Rounded.Notifications,
            "Notificações",
            if (notificationsEnabled) "Ativas" else "Desativadas"
        )
    }
}

@Composable
private fun NotificationsBox(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Column {
        Spacer(Modifier.height(23.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFFD5DEEB), RoundedCornerShape(10.dp))
                .clickable { onChange(!enabled) }
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Notificações", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SosInk, modifier = Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = onChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = SosBlue),
                    modifier = Modifier.scale(0.8f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.NotificationsNone, contentDescription = null, tint = SosMuted, modifier = Modifier.size(20.dp))
                Text("Quero receber novidades e alertas", fontSize = 12.sp, color = SosMuted)
            }
        }
    }
}

@Composable
private fun QuoteDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Frase do perfil") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(120) },
                placeholder = { Text("Ex.: Educar é acreditar no futuro.") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun formatoData() = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply {
    timeZone = TimeZone.getTimeZone("UTC")
    isLenient = false
}

private fun dataDeMillis(millis: Long): String = formatoData().format(millis)

private fun parseData(texto: String): Long? =
    runCatching { formatoData().parse(texto)?.time }.getOrNull()

internal fun formatarData(input: String): String {
    val digitos = input.filter(Char::isDigit).take(8)
    return buildString {
        digitos.forEachIndexed { i, c ->
            if (i == 2 || i == 4) append('/')
            append(c)
        }
    }
}

internal fun formatarCodigoPostal(input: String): String {
    val digitos = input.filter(Char::isDigit).take(7)
    return if (digitos.length > 4) digitos.substring(0, 4) + "-" + digitos.substring(4) else digitos
}

@Preview(showBackground = true, heightDp = 1000, widthDp = 390)
@Composable
private fun UserProfileScreenPreview() {
    SOSProfTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Dados Pessoais")
            Cartao {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    ProfileTextField("Nome completo *", "", {}, Icons.Rounded.PersonOutline)
                }
            }
        }
    }
}
