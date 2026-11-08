package com.huatafac.sosprof.ui.turmas

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huatafac.sosprof.data.Xlsx
import com.huatafac.sosprof.model.ColunaAluno
import com.huatafac.sosprof.model.ResultadoImportacao
import com.huatafac.sosprof.model.Turma
import com.huatafac.sosprof.model.aplicarFolhaAlunos
import com.huatafac.sosprof.model.folhaDeAlunos
import com.huatafac.sosprof.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Folha lida e o resultado previsto, à espera da confirmação do professor. */
private class Importacao(val linhas: List<List<String>>, val previsto: ResultadoImportacao)

/**
 * Botões "Exportar Excel" e "Importar Excel" da lista de alunos.
 * Exportar grava a folha da turma (com os alunos atuais, ou só o cabeçalho) onde o professor escolher.
 * Importar lê um .xlsx, mostra o que vai mudar e só grava depois de confirmado ([onImportar]).
 */
@Composable
fun ExcelAlunosBotoes(turma: Turma, onImportar: (List<List<String>>) -> Unit, onAviso: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var aImportar by remember { mutableStateOf<Importacao?>(null) }
    var ocupado by remember { mutableStateOf(false) }

    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(Xlsx.MIME)) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val linhas = folhaDeAlunos(turma.alunos)
        scope.launch {
            ocupado = true
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        Xlsx.escrever(out, "Alunos ${turma.nome}", linhas, ColunaAluno.entries.map { it.largura })
                    } ?: error("Não foi possível abrir o ficheiro")
                }.isSuccess
            }
            ocupado = false
            onAviso(
                if (ok) "Folha exportada com ${turma.alunos.size} alunos. Preenche-a e depois usa Importar."
                else "Não foi possível guardar a folha."
            )
        }
    }

    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            ocupado = true
            val linhas = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { Xlsx.ler(it) } }.getOrNull()
            }
            ocupado = false
            if (linhas == null) {
                onAviso("Não foi possível ler o ficheiro. Confirma que é uma folha Excel (.xlsx).")
            } else {
                aImportar = Importacao(linhas, aplicarFolhaAlunos(turma.alunos, linhas))
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { exportar.launch("Alunos ${turma.sigla.ifBlank { turma.nome }}.xlsx") },
            enabled = !ocupado,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Exportar Excel", fontSize = 14.sp)
        }
        OutlinedButton(
            onClick = {
                // Alguns gestores de ficheiros não identificam o tipo dos .xlsx: aceita-se também "binário"
                importar.launch(arrayOf(Xlsx.MIME, "application/octet-stream"))
            },
            enabled = !ocupado,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Importar Excel", fontSize = 14.sp)
        }
    }

    aImportar?.let { imp ->
        ConfirmarImportacaoDialog(
            turma = turma,
            resultado = imp.previsto,
            onDismiss = { aImportar = null },
            onConfirm = {
                aImportar = null
                onImportar(imp.linhas)
            }
        )
    }
}

@Composable
private fun ConfirmarImportacaoDialog(
    turma: Turma,
    resultado: ResultadoImportacao,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val temErros = resultado.erros.isNotEmpty()
    val nadaMuda = !temErros && resultado.novos == 0 && resultado.atualizados == 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (temErros) "A folha tem erros" else "Importar para ${turma.nome}?") },
        text = {
            Column(
                Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (temErros) {
                    Text("Nada foi gravado. Corrige estas linhas no Excel e importa outra vez:", color = SosInk)
                    resultado.erros.forEach { Text("• $it", color = SosWarningInk, fontSize = 14.sp) }
                } else {
                    Text("• ${contar(resultado.novos, "aluno novo", "alunos novos")}", color = SosInk)
                    Text("• ${contar(resultado.atualizados, "aluno atualizado", "alunos atualizados")}", color = SosInk)
                    if (resultado.semAlteracoes > 0) {
                        Text("• ${contar(resultado.semAlteracoes, "aluno sem alterações", "alunos sem alterações")}", color = SosMuted)
                    }
                    Text(
                        "Os alunos são identificados pelo N.º. As células vazias mantêm o valor atual; " +
                            "fotos, faltas e notas não mudam.",
                        color = SosMuted,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            if (!temErros && !nadaMuda) {
                TextButton(onClick = onConfirm) { Text("Importar", fontWeight = FontWeight.SemiBold) }
            } else {
                TextButton(onClick = onDismiss) { Text("Fechar") }
            }
        },
        dismissButton = {
            if (!temErros && !nadaMuda) TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun contar(n: Int, um: String, varios: String) = if (n == 1) "1 $um" else "$n $varios"
