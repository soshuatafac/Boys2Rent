package com.huatafac.sosprof.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huatafac.sosprof.ui.theme.*

/*
 * Componentes da vista de leitura das fichas (aluno e perfil do professor):
 * cartão branco com linhas de ícone, rótulo e valor, e um botão de ação opcional à direita.
 */

@Composable
fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 6.dp), content = content)
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    rotulo: String,
    valor: String,
    valorCor: Color = SosInk,
    acao: @Composable () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = SosBlue, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(rotulo, fontSize = 12.sp, color = SosMuted)
            Text(valor, fontSize = 15.sp, color = valorCor)
        }
        acao()
    }
}

@Composable
fun InfoAction(icon: ImageVector, descricao: String, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = SosBlueLight, contentColor = SosBlueDark)
    ) {
        Icon(icon, contentDescription = descricao, modifier = Modifier.size(20.dp))
    }
}
