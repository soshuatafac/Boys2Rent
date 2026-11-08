package com.huatafac.boys2rent.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huatafac.boys2rent.model.DiaEspecial
import com.huatafac.boys2rent.model.TipoDiaEspecial
import com.huatafac.boys2rent.ui.theme.B2rDivider
import com.huatafac.boys2rent.ui.theme.B2rFeriado
import com.huatafac.boys2rent.ui.theme.B2rFestivo
import com.huatafac.boys2rent.ui.theme.B2rInk
import com.huatafac.boys2rent.ui.theme.B2rMuted

/** Cor de um feriado ou dia festivo. */
val TipoDiaEspecial.cor: Color
    get() = when (this) {
        TipoDiaEspecial.FERIADO -> B2rFeriado
        TipoDiaEspecial.FESTIVO -> B2rFestivo
    }

/**
 * Feriados e dias festivos num cartão branco, com a faixa da cor do tipo.
 * Com [mostrarData] cada linha diz também o dia (para a lista da semana).
 */
@Composable
fun DiasEspeciaisCard(dias: List<DiaEspecial>, mostrarData: Boolean, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = modifier.fillMaxWidth()) {
        Column {
            dias.forEachIndexed { i, dia ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(dia.tipo.cor))
                    Column(Modifier.weight(1f)) {
                        Text(dia.nome, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = B2rInk)
                        Text(
                            if (mostrarData) "${dia.tipo.rotulo} · ${dataLegivel(dia.data)}" else dia.tipo.rotulo,
                            fontSize = 13.sp,
                            color = if (dia.tipo == TipoDiaEspecial.FERIADO) B2rFeriado else B2rMuted
                        )
                    }
                }
                if (i < dias.lastIndex) HorizontalDivider(color = B2rDivider)
            }
        }
    }
}
