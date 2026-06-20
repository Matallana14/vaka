package com.vaka.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/**
 * Procesamiento de imágenes de perfil:
 * - Reduce la imagen a 256×256 píxeles máximo
 * - Recorta cuadrada (center crop)
 * - Comprime como JPEG calidad 70
 * - Devuelve Base64 listo para guardar en Firestore
 *
 * Resultado típico: ~15-25 KB en Base64, MUY por debajo del límite de 1MB
 * de Firestore por documento.
 */
object FotoPerfil {

    private const val TAMANO_OBJETIVO = 256
    private const val CALIDAD_JPEG = 70

    /**
     * Caché en memoria de Bitmaps ya decodificados.
     * Cada foto (identificada por su Base64) se decodifica UNA sola vez por sesión.
     * Esto evita re-procesarla en cada recomposición de Compose (scroll en listas,
     * cambios de estado en la pantalla, etc.).
     *
     * Tamaño máximo del caché: 50 fotos (la app suele tener 5-15 perfiles activos).
     * Si se llena, se vacía completamente (estrategia simple FIFO global).
     */
    private val cacheBitmaps = LinkedHashMap<String, Bitmap>()
    private const val MAX_CACHE = 50

    /**
     * Procesa una imagen elegida por el usuario y la convierte a Base64
     * lista para guardar. Devuelve null si algo falla (imagen corrupta, etc).
     */
    fun procesarUri(ctx: Context, uri: Uri): String? {
        return try {
            // 1. Decodificar la imagen original (sample size para no llenar memoria)
            val opcionesInfo = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opcionesInfo)
            }
            val anchoOriginal = opcionesInfo.outWidth
            val altoOriginal = opcionesInfo.outHeight
            if (anchoOriginal <= 0 || altoOriginal <= 0) return null

            // Sample size: potencia de 2 más cercana para reducir antes de cargar
            var sample = 1
            while ((anchoOriginal / sample) > TAMANO_OBJETIVO * 2 &&
                   (altoOriginal / sample) > TAMANO_OBJETIVO * 2) {
                sample *= 2
            }

            // 2. Cargar con sample
            val opcionesLoad = BitmapFactory.Options().apply { inSampleSize = sample }
            val original: Bitmap = ctx.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opcionesLoad)
            } ?: return null

            // 3. Rotar según EXIF si es necesario (las fotos del celular suelen
            //    venir giradas)
            val rotada = rotarSegunExif(ctx, uri, original)

            // 4. Crop cuadrado al centro
            val lado = minOf(rotada.width, rotada.height)
            val xOffset = (rotada.width - lado) / 2
            val yOffset = (rotada.height - lado) / 2
            val cuadrada = Bitmap.createBitmap(rotada, xOffset, yOffset, lado, lado)

            // 5. Escalar a tamaño objetivo
            val escalada = Bitmap.createScaledBitmap(cuadrada, TAMANO_OBJETIVO, TAMANO_OBJETIVO, true)

            // 6. Comprimir a JPEG y codificar a Base64
            val baos = ByteArrayOutputStream()
            escalada.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, baos)
            val bytes = baos.toByteArray()

            // Limpieza de memoria
            if (cuadrada != rotada) rotada.recycle()
            if (escalada != cuadrada) cuadrada.recycle()
            if (rotada != original) original.recycle()
            escalada.recycle()

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decodifica un Base64 a Bitmap para mostrar.
     * Devuelve null si la cadena está vacía o es inválida.
     * Usa caché en memoria: la misma foto NO se decodifica dos veces.
     */
    @Synchronized
    fun decodificar(b64: String): Bitmap? {
        if (b64.isBlank()) return null
        // ¿Ya está cacheada?
        cacheBitmaps[b64]?.let { return it }
        // No → decodificar y cachear
        return try {
            val bytes = Base64.decode(b64, Base64.NO_WRAP)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            // Guardar en caché (si está lleno, vaciar todo)
            if (cacheBitmaps.size >= MAX_CACHE) {
                cacheBitmaps.clear()
            }
            cacheBitmaps[b64] = bmp
            bmp
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Limpia el caché de bitmaps (útil al cerrar sesión o si la memoria se llena).
     */
    @Synchronized
    fun limpiarCache() {
        cacheBitmaps.clear()
    }

    private fun rotarSegunExif(ctx: Context, uri: Uri, bmp: Bitmap): Bitmap {
        return try {
            ctx.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orient = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                val matriz = Matrix()
                when (orient) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matriz.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matriz.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matriz.postRotate(270f)
                    else -> return@use bmp
                }
                Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matriz, true)
            } ?: bmp
        } catch (e: Exception) { bmp }
    }
}

/**
 * Avatar reutilizable: muestra la foto de perfil si existe, o el círculo
 * con la inicial sobre el color de avatar si no hay foto.
 *
 * @param fotoBase64 cadena Base64 de la imagen (vacía si no hay)
 * @param nombre nombre del usuario (para la inicial de fallback)
 * @param colorAvatar identificador del color (violeta, rosa, etc.)
 * @param tamano diámetro del círculo
 * @param tamanoTexto tamaño de la fuente del fallback (opcional, se calcula automáticamente si no se especifica)
 */
@Composable
fun AvatarPerfil(
    fotoBase64: String,
    nombre: String,
    colorAvatar: String,
    tamano: Dp,
    tamanoTexto: androidx.compose.ui.unit.TextUnit? = null
) {
    val bmp = remember(fotoBase64) { FotoPerfil.decodificar(fotoBase64) }
    val color = colorAvatarPor(colorAvatar)
    val inicial = nombre.trim().take(1).uppercase().ifBlank { "?" }

    Box(
        Modifier
            .size(tamano)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Foto de perfil de $nombre",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(tamano).clip(CircleShape)
            )
        } else {
            val fontSize = tamanoTexto ?: (tamano.value * 0.42f).sp
            Text(
                inicial,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize
            )
        }
    }
}
