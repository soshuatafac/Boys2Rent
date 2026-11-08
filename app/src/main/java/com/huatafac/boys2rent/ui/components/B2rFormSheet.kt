package com.huatafac.boys2rent.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huatafac.boys2rent.ui.theme.*

/**
 * Painel de formulário que sobe de baixo, igual em toda a app (turma, agenda, aluno):
 * título, cartões brancos com os campos ([content]), aviso de erro, botão largo e "Cancelar".
 * Enquanto [aGravar], o painel não fecha e o botão mostra o indicador de progresso.
 * [acaoExtra] aparece por baixo de "Cancelar" (ex.: "Apagar entrada").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun B2rFormSheet(
    titulo: String,
    textoBotao: String,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit,
    aGravar: Boolean = false,
    erro: String? = null,
    acaoExtra: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val gravando by rememberUpdatedState(aGravar)
    val estado = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !gravando }
    )
    ModalBottomSheet(
        onDismissRequest = { if (!gravando) onDismiss() },
        sheetState = estado,
        containerColor = B2rBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(titulo, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = B2rInk)
            content()
            erro?.let {
                Surface(shape = RoundedCornerShape(12.dp), color = B2rWarningBg) {
                    Text(it, color = B2rWarningInk, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(12.dp))
                }
            }
            Button(
                enabled = !aGravar,
                colors = ButtonDefaults.buttonColors(containerColor = B2rBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                onClick = onConfirmar
            ) {
                if (aGravar) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(textoBotao, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
            TextButton(onClick = onDismiss, enabled = !aGravar, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar", color = B2rMuted)
            }
            acaoExtra?.let { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { it() } }
        }
    }
}

/** Cartão branco de um formulário, com título opcional e os campos empilhados. */
@Composable
fun B2rFormCard(titulo: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            titulo?.let { Text(it, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = B2rBlueDark) }
            content()
        }
    }
}
