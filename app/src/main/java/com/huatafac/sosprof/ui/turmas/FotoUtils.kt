package com.huatafac.sosprof.ui.turmas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/*
 * Preparação das fotos dos alunos antes de serem guardadas no telemóvel:
 * reduz para no máximo FOTO_LADO_MAX px no lado maior e comprime em JPEG,
 * para ficarem com poucas dezenas de KB.
 */

private const val FOTO_LADO_MAX = 600
private const val FOTO_QUALIDADE = 80

/** Reduz e comprime a foto em JPEG. */
internal suspend fun fotoParaJpeg(bitmap: Bitmap): ByteArray = withContext(Dispatchers.Default) {
    val escala = minOf(1f, FOTO_LADO_MAX.toFloat() / maxOf(bitmap.width, bitmap.height))
    val reduzida = if (escala < 1f) {
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * escala).toInt().coerceAtLeast(1),
            (bitmap.height * escala).toInt().coerceAtLeast(1),
            true
        )
    } else bitmap
    ByteArrayOutputStream().use { out ->
        reduzida.compress(Bitmap.CompressFormat.JPEG, FOTO_QUALIDADE, out)
        out.toByteArray()
    }
}

/** Lê a imagem escolhida na galeria; devolve null se não for possível. */
internal suspend fun lerFoto(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // O ImageDecoder já aplica a rotação EXIF e reduz ao descodificar (poupa memória)
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val escala = minOf(1f, FOTO_LADO_MAX * 2f / maxOf(info.size.width, info.size.height))
                decoder.setTargetSize(
                    (info.size.width * escala).toInt().coerceAtLeast(1),
                    (info.size.height * escala).toInt().coerceAtLeast(1)
                )
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
    } catch (_: Exception) {
        null
    }
}
