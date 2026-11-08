package com.huatafac.sosprof.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huatafac.sosprof.model.Turma
import com.huatafac.sosprof.ui.theme.SosBlue
import com.huatafac.sosprof.ui.theme.SosBlueDark
import com.huatafac.sosprof.ui.theme.SosBlueLight
import com.huatafac.sosprof.ui.theme.SosYellow

/** Fundo do cabeçalho com a base em onda, como no formulário de perfil. */
private class WaveShape(private val waveHeightPx: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val bottom = size.height
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, bottom - waveHeightPx)
            cubicTo(
                size.width * 0.70f, bottom + waveHeightPx * 0.4f,
                size.width * 0.35f, bottom - waveHeightPx * 1.6f,
                0f, bottom - waveHeightPx * 0.2f
            )
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Cabeçalho azul em onda. O conteúdo fica por cima, com espaço para a onda no fundo.
 * Desenha também a área da barra de estado (edge-to-edge).
 */
@Composable
fun SosHeader(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 36.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val waveHeight = 22.dp
    val density = LocalDensity.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(WaveShape(with(density) { waveHeight.toPx() }))
            .background(SosBlue)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

/** Logótipo em texto. TODO: trocar pelo logótipo real (drawable) quando existir. */
@Composable
fun SosLogo(modifier: Modifier = Modifier, fontSize: TextUnit = 28.sp) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = "SOS",
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Prof",
            color = Color.White,
            fontSize = fontSize * 0.93f,
            fontFamily = FontFamily.Cursive,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.drawBehind {
                val stroke = 3.dp.toPx()
                drawLine(
                    color = SosYellow,
                    start = Offset(0f, size.height - stroke),
                    end = Offset(size.width, size.height - stroke * 2),
                    strokeWidth = stroke
                )
            }
        )
    }
}

/** Título de secção numa faixa azul-claro. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SosBlueLight)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = SosBlueDark,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

/** Quadrado com a sigla da turma (ex.: "8B") na cor dela. */
@Composable
fun TurmaBadge(turma: Turma, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val cor = Color(turma.corHex)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(cor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = turma.sigla, color = cor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

/** Etiqueta pequena e arredondada. */
@Composable
fun SosChip(text: String, background: Color, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
