package com.huatafac.boys2rent.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huatafac.boys2rent.ui.theme.*

/*
 * Campos do formulário de perfil, no formato da imagem de referência:
 * rótulo por cima, caixa com o ícone numa célula à esquerda e o valor à direita.
 */

private val FieldShape = RoundedCornerShape(10.dp)
private val FieldBorder = Color(0xFFD5DEEB)
private val FieldHeight = 48.dp
private val FieldText = TextStyle(fontSize = 15.sp, color = B2rInk)

/** Duas colunas lado a lado; num ecrã muito estreito ficam uma por baixo da outra. */
@Composable
fun FormRow(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 300.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                left()
                right()
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { left() }
                Box(Modifier.weight(1f)) { right() }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = B2rInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
    )
}

/** A caixa do campo: célula do ícone, separador e conteúdo. */
@Composable
private fun FieldBox(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    minHeight: Dp = FieldHeight,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(FieldShape)
            .background(Color.White)
            .border(1.dp, FieldBorder, FieldShape)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(40.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = B2rMuted, modifier = Modifier.size(20.dp))
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(FieldBorder))
        Box(
            Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart,
            content = content
        )
        if (trailing != null) {
            Box(Modifier.padding(end = 6.dp)) { trailing() }
        }
    }
}

@Composable
fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(modifier) {
        FieldLabel(label)
        FieldBox(icon = icon, trailing = trailing) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                readOnly = readOnly,
                singleLine = singleLine,
                textStyle = FieldText,
                cursorBrush = SolidColor(B2rBlue),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, style = FieldText.copy(color = B2rMuted), maxLines = 1)
                    }
                    inner()
                }
            )
        }
    }
}

/** Escolha de uma opção de uma lista fixa. */
@Composable
fun ProfileDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    placeholder: String = "Selecionar"
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        FieldLabel(label)
        Box {
            FieldBox(
                icon = icon,
                modifier = Modifier.clickable(onClickLabel = label) { expanded = true },
                trailing = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = B2rMuted) }
            ) {
                Text(
                    text = value.ifBlank { placeholder },
                    style = if (value.isBlank()) FieldText.copy(color = B2rMuted) else FieldText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/** Campo que abre outra escolha ao tocar (ex.: data ou hora num seletor), com o aspeto dos restantes. */
@Composable
fun ProfileClickField(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Escolher",
    trailing: (@Composable () -> Unit)? = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = B2rMuted) }
) {
    Column(modifier) {
        FieldLabel(label)
        FieldBox(
            icon = icon,
            modifier = Modifier.clickable(onClickLabel = label, onClick = onClick),
            trailing = trailing
        ) {
            Text(
                text = value.ifBlank { placeholder },
                style = if (value.isBlank()) FieldText.copy(color = B2rMuted) else FieldText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Campo de texto livre com sugestões (ex.: localidade, concelho).
 * Aceita valores que não estejam na lista.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAutocompleteField(
    label: String,
    value: String,
    suggestions: List<String>,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    var expanded by remember { mutableStateOf(false) }
    val filtradas = remember(value, suggestions) {
        val termo = value.trim()
        if (termo.isEmpty()) suggestions
        else suggestions.filter { it.contains(termo, ignoreCase = true) && !it.equals(termo, ignoreCase = true) }
    }
    Column(modifier) {
        FieldLabel(label)
        ExposedDropdownMenuBox(expanded = expanded && filtradas.isNotEmpty(), onExpandedChange = { expanded = it }) {
            FieldBox(
                icon = icon,
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                trailing = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = B2rMuted) }
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = {
                        onValueChange(it)
                        expanded = true
                    },
                    singleLine = true,
                    textStyle = FieldText,
                    cursorBrush = SolidColor(B2rBlue),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, style = FieldText.copy(color = B2rMuted), maxLines = 1)
                        }
                        inner()
                    }
                )
            }
            ExposedDropdownMenu(
                expanded = expanded && filtradas.isNotEmpty(),
                onDismissRequest = { expanded = false }
            ) {
                filtradas.take(8).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/** Escolha de várias opções; o valor é guardado como texto separado por vírgulas. */
@Composable
fun ProfileMultiSelectField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    asChips: Boolean = false,
    placeholder: String = "Selecionar"
) {
    var expanded by remember { mutableStateOf(false) }
    val selecionados = splitList(value)
    val alternar: (String) -> Unit = { opcao ->
        val nova = if (opcao in selecionados) selecionados - opcao else selecionados + opcao
        onValueChange(nova.joinToString(", "))
    }
    Column(modifier) {
        FieldLabel(label)
        Box {
            FieldBox(
                icon = icon,
                modifier = Modifier.clickable(onClickLabel = label) { expanded = true },
                trailing = { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = B2rMuted) }
            ) {
                when {
                    selecionados.isEmpty() -> Text(placeholder, style = FieldText.copy(color = B2rMuted), maxLines = 1)
                    asChips -> ChipsRow(selecionados, onRemove = alternar)
                    else -> Text(
                        selecionados.joinToString(", "),
                        style = FieldText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                // Mostra também valores antigos que já não estejam na lista
                (options + selecionados.filterNot { it in options }).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        leadingIcon = { Checkbox(checked = option in selecionados, onCheckedChange = null) },
                        onClick = { alternar(option) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsRow(itens: List<String>, onRemove: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        itens.forEach { item ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(B2rBlueLight)
                    .padding(start = 10.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item, fontSize = 12.sp, color = B2rBlueDark)
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Remover $item",
                    tint = B2rBlueDark,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(18.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { onRemove(item) }
                        .padding(2.dp)
                )
            }
        }
    }
}

fun splitList(value: String): List<String> =
    value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()

/** Título da secção: faixa azul-claro com ícone. */
@Composable
fun ProfileSectionTitle(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(B2rBlueLight)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = B2rBlueDark, modifier = Modifier.size(22.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = B2rBlueDark)
    }
}
