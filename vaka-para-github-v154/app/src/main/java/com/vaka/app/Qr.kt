package com.vaka.app

import android.graphics.Bitmap
import android.graphics.Color as ColorAndroid
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Prefijo que identifica los QR de Vaka. */
const val QR_PREFIJO = "VAKA:"

/** Prefijo que identifica los QR de código de usuario para amistad. */
const val QR_PREFIJO_AMIGO = "VAKA_USER:"

/**
 * Genera el bitmap de un código QR.
 *
 * @param colorOscuro color de los pixeles "encendidos" (default: negro)
 * @param colorClaro color del fondo (default: blanco)
 * @param nivelCorreccion nivel de corrección de errores. H permite hasta 30% de oclusión
 *                       (necesario si vamos a poner un logo encima)
 */
fun generarQr(
    texto: String,
    tam: Int = 600,
    colorOscuro: Int = ColorAndroid.BLACK,
    colorClaro: Int = ColorAndroid.WHITE,
    nivelCorreccion: ErrorCorrectionLevel = ErrorCorrectionLevel.H
): Bitmap {
    val matriz = QRCodeWriter().encode(
        texto, BarcodeFormat.QR_CODE, tam, tam,
        mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to nivelCorreccion
        )
    )
    val bmp = Bitmap.createBitmap(tam, tam, Bitmap.Config.ARGB_8888)
    for (x in 0 until tam) {
        for (y in 0 until tam) {
            bmp.setPixel(x, y, if (matriz[x, y]) colorOscuro else colorClaro)
        }
    }
    return bmp
}

/**
 * Tarjeta decorada para mostrar un QR de Vaka con el estilo de la app.
 * Incluye: marco con gradiente violeta-rosa, esquinas redondeadas, emoji central
 * sobre el QR, y etiqueta con el código alfanumérico abajo.
 */
@Composable
private fun TarjetaQr(
    contenido: String,
    codigoLegible: String,
    titulo: String,
    subtitulo: String,
    emojiCentral: String,
    modifier: Modifier = Modifier
) {
    // El color "oscuro" del QR usa el violeta de Vaka en vez de negro puro.
    // Sigue siendo lo suficientemente contrastado con el blanco para escanearse.
    val violetaQr = ColorAndroid.parseColor("#3D2E7C")
    val bmp = remember(contenido) {
        generarQr(
            contenido,
            colorOscuro = violetaQr,
            colorClaro = ColorAndroid.WHITE
        )
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF5B3DF5),  // Violeta
                        Color(0xFF8B46E0),
                        Color(0xFFD9569E)   // Rosa
                    )
                )
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            titulo,
            color = Color.White.copy(alpha = .85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
        if (subtitulo.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                subtitulo,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(16.dp))

        // El QR sobre un fondo blanco con bordes redondeados, y el emoji
        // centrado encima sobre un círculo violeta (parece un logo).
        Box(
            Modifier
                .shadow(8.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Código QR de $codigoLegible",
                modifier = Modifier.size(200.dp)
            )
            // Emoji central como "logo" del QR — los QR con nivel H aguantan esto
            Box(
                Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3D2E7C)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emojiCentral, fontSize = 22.sp)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Etiqueta con el código legible
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = .22f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                codigoLegible,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
        }
    }
}

/** Muestra el QR de una Vaka compartida con el estilo de la app. */
@Composable
fun QrVaka(codigo: String, modifier: Modifier = Modifier) {
    TarjetaQr(
        contenido = QR_PREFIJO + codigo,
        codigoLegible = codigo,
        titulo = "CÓDIGO DE LA VAKA",
        subtitulo = "Escanéame para unirte",
        emojiCentral = "🐷",
        modifier = modifier
    )
}

/** Muestra el QR del código de usuario con el estilo de la app. */
@Composable
fun QrUsuario(codigoUsuario: String, modifier: Modifier = Modifier) {
    TarjetaQr(
        contenido = QR_PREFIJO_AMIGO + codigoUsuario,
        codigoLegible = codigoUsuario,
        titulo = "MI CÓDIGO DE USUARIO",
        subtitulo = "Escanéame y nos hacemos amigos",
        emojiCentral = "🤝",
        modifier = modifier
    )
}

/** Extrae el código de Vaka del contenido de un QR escaneado, o null si no es válido. */
fun codigoDesdeQr(contenido: String?): String? {
    if (contenido == null) return null
    val limpio = contenido.trim()
    val codigo = if (limpio.uppercase().startsWith(QR_PREFIJO)) {
        limpio.substring(QR_PREFIJO.length)
    } else {
        limpio
    }.trim().uppercase()
    return if (codigo.length in 4..8 && codigo.all { it.isLetterOrDigit() }) codigo else null
}

/**
 * Extrae el código de usuario (VK-XXXXXX) del contenido de un QR escaneado.
 * Acepta el formato con prefijo o el código pelado.
 */
fun codigoUsuarioDesdeQr(contenido: String?): String? {
    if (contenido == null) return null
    val limpio = contenido.trim()
    val codigo = if (limpio.uppercase().startsWith(QR_PREFIJO_AMIGO)) {
        limpio.substring(QR_PREFIJO_AMIGO.length)
    } else {
        limpio
    }.trim().uppercase()
    return if (codigo.startsWith("VK-") && codigo.length == 9) codigo else null
}
