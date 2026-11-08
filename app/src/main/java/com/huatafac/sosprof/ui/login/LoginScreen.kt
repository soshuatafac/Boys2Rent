package com.huatafac.sosprof.ui.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huatafac.sosprof.auth.AuthState
import com.huatafac.sosprof.ui.components.SosFormCard
import com.huatafac.sosprof.ui.components.SosFormSheet
import com.huatafac.sosprof.ui.components.SosHeader
import com.huatafac.sosprof.ui.components.SosLogo
import com.huatafac.sosprof.ui.profile.ProfileTextField
import com.huatafac.sosprof.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authState: AuthState,
    email: String,
    password: String,
    isSignUpMode: Boolean,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onToggleAuthMode: () -> Unit,
    onSignInWithEmail: () -> Unit,
    onSignUpWithEmail: () -> Unit,
    onResendVerificationEmail: () -> Unit,
    onSignInWithGoogle: (context: android.content.Context) -> Unit,
    onErrorDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    // E-mail e função a chamar com o erro (null = e-mail de recuperação enviado)
    onRecuperarPassword: (String, (String?) -> Unit) -> Unit = { _, cb -> cb("Indisponível") }
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var recuperarAberto by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isLoading = authState is AuthState.Loading
    val canSubmit = email.isNotBlank() && password.isNotEmpty() && !isLoading
    val submit = {
        focusManager.clearFocus()
        if (isSignUpMode) onSignUpWithEmail() else onSignInWithEmail()
    }

    if (authState is AuthState.Error) {
        LaunchedEffect(authState.message) {
            snackbarHostState.showSnackbar(authState.message)
            onErrorDismiss()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        containerColor = SosBackground,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Cabeçalho azul em onda com o logótipo
            SosHeader(bottomPadding = 72.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SosLogo(fontSize = 44.sp)
                    Text(
                        text = "O teu aliado no dia a dia do ensino!",
                        color = Color.White,
                        fontFamily = FontFamily.Cursive,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.rotate(-3f)
                    )
                }
            }

            // 2. Cartão do formulário, sobreposto à onda
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-44).dp)
                    .padding(horizontal = 20.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = SosBlueDark, spotColor = SosBlueDark),
                shape = RoundedCornerShape(24.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ModoToggle(isSignUpMode = isSignUpMode, enabled = !isLoading, onToggle = onToggleAuthMode)

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isSignUpMode) "Criar nova conta" else "Bem-vindo de volta!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = SosInk
                        )
                        Text(
                            text = if (isSignUpMode) {
                                "Vais receber um e-mail para confirmar a conta."
                            } else {
                                "Entra para veres a tua agenda e as tuas turmas."
                            },
                            fontSize = 14.sp,
                            color = SosMuted
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChanged,
                        label = { Text("E-mail") },
                        placeholder = { Text("exemplo@exemplo.pt") },
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = SosBlue) },
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChanged,
                        label = { Text("Palavra-passe") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = SosBlue) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Esconder palavra-passe" else "Mostrar palavra-passe",
                                    tint = SosMuted
                                )
                            }
                        },
                        supportingText = if (isSignUpMode) {
                            { Text("Pelo menos 6 caracteres.") }
                        } else null,
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (canSubmit) submit() }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (!isSignUpMode) {
                        Text(
                            "Esqueceste-te da palavra-passe?",
                            color = SosBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isLoading, onClickLabel = "Recuperar palavra-passe") { recuperarAberto = true }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Button(
                        onClick = submit,
                        enabled = canSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SosBlue, contentColor = Color.White)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUpMode) "Criar conta" else "Entrar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!isSignUpMode) {
                        TextButton(
                            onClick = onResendVerificationEmail,
                            enabled = !isLoading,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Não recebeste o e-mail de confirmação? Reenviar", fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = SosDivider)
                        Text("ou", modifier = Modifier.padding(horizontal = 16.dp), fontSize = 14.sp, color = SosMuted)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = SosDivider)
                    }

                    OutlinedButton(
                        onClick = { onSignInWithGoogle(context) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SosDivider),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SosInk)
                    ) {
                        GoogleIcon()
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Continuar com Google", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Text(
                text = "Juntos tornamos a educação mais fácil!",
                color = SosMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-28).dp)
                    .navigationBarsPadding()
            )
        }
    }

    if (recuperarAberto) {
        RecuperarPasswordSheet(
            emailInicial = email,
            onDismiss = { recuperarAberto = false },
            onEnviar = { endereco, terminou ->
                onRecuperarPassword(endereco) { erro ->
                    terminou(erro)
                    if (erro == null) {
                        recuperarAberto = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Enviámos um e-mail para $endereco com o link para criares uma nova palavra-passe. " +
                                    "Se não o vires, procura no spam."
                            )
                        }
                    }
                }
            }
        )
    }
}

/** Separador "Entrar | Criar conta". */
@Composable
private fun ModoToggle(isSignUpMode: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SosChipBg)
            .padding(4.dp)
    ) {
        listOf(false to "Entrar", true to "Criar conta").forEach { (modo, texto) ->
            val selecionado = modo == isSignUpMode
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selecionado) Color.White else Color.Transparent)
                    .clickable(enabled = enabled && !selecionado, role = Role.Tab, onClick = onToggle)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    texto,
                    fontSize = 14.sp,
                    fontWeight = if (selecionado) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selecionado) SosBlueDark else SosMuted
                )
            }
        }
    }
}

@Composable
fun GoogleIcon() {
    Canvas(modifier = Modifier.size(20.dp)) {
        val width = size.width
        val height = size.height
        val blue = Color(0xFF4285F4)

        drawCircle(color = blue, radius = width / 2f, center = center)
        drawCircle(color = Color.White, radius = width / 2.6f, center = center)
        drawRect(
            color = blue,
            topLeft = androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.35f),
            size = androidx.compose.ui.geometry.Size(width * 0.5f, height * 0.3f)
        )
        drawRect(
            color = blue,
            topLeft = androidx.compose.ui.geometry.Offset(width * 0.65f, height * 0.5f),
            size = androidx.compose.ui.geometry.Size(width * 0.35f, height * 0.2f)
        )
    }
}

/** Painel para pedir o e-mail de recuperação da palavra-passe. */
@Composable
private fun RecuperarPasswordSheet(
    emailInicial: String,
    onDismiss: () -> Unit,
    onEnviar: (String, (String?) -> Unit) -> Unit
) {
    var endereco by remember { mutableStateOf(emailInicial.trim()) }
    var erro by remember { mutableStateOf<String?>(null) }
    var aEnviar by remember { mutableStateOf(false) }
    SosFormSheet(
        titulo = "Recuperar Palavra-passe",
        textoBotao = "Enviar e-mail",
        aGravar = aEnviar,
        erro = erro,
        onDismiss = onDismiss,
        onConfirmar = {
            aEnviar = true
            onEnviar(endereco.trim()) { e ->
                aEnviar = false
                erro = e
            }
        }
    ) {
        Text(
            "Indica o e-mail da tua conta. Vais receber um e-mail com um link para criares uma nova palavra-passe.",
            fontSize = 14.sp,
            color = SosMuted
        )
        SosFormCard {
            ProfileTextField(
                label = "E-mail *",
                value = endereco,
                onValueChange = { endereco = it; erro = null },
                icon = Icons.Rounded.Email,
                placeholder = "exemplo@exemplo.pt",
                keyboardType = KeyboardType.Email
            )
        }
        Text(
            "Se entras com a conta Google, não precisas de palavra-passe: usa \"Continuar com Google\".",
            fontSize = 13.sp,
            color = SosMuted
        )
    }
}

@Preview(name = "Login Screen Preview", showBackground = true)
@Composable
fun LoginScreenPreview() {
    SOSProfTheme {
        LoginScreen(
            authState = AuthState.Unauthenticated,
            email = "",
            password = "",
            isSignUpMode = false,
            onEmailChanged = {},
            onPasswordChanged = {},
            onToggleAuthMode = {},
            onSignInWithEmail = {},
            onSignUpWithEmail = {},
            onResendVerificationEmail = {},
            onSignInWithGoogle = {},
            onErrorDismiss = {}
        )
    }
}
