package com.vaka.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * Anillo de progreso con onda líquida animada en su interior.
 * La onda se mueve constantemente (efecto agua) y sube según el porcentaje.
 * Si no hay meta (progreso null) muestra un anillo decorativo sin llenar.
 */
@Composable
fun AnilloOndaLiquida(
    progreso: Float?,                       // 0f..1f o null
    tamano: Dp = 92.dp,
    colorPrincipal: Color = MaterialTheme.colorScheme.primary,
    colorOnda: Color = Dorado,
    grosorAnillo: Dp = 5.dp,
    mostrarPorcentaje: Boolean = true,
) {
    // Onda constante: una fase que avanza sin parar para crear el efecto agua
    val transicion = rememberInfiniteTransition(label = "onda")
    val faseOnda by transicion.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fase"
    )
    // Segunda onda con velocidad distinta para dar profundidad
    val faseOnda2 by transicion.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fase2"
    )

    // Nivel del agua animado suavemente al cambiar de progreso
    val nivelObjetivo = (progreso ?: 0f).coerceIn(0f, 1f)
    val nivel by animateFloatAsState(
        nivelObjetivo,
        animationSpec = tween(durationMillis = 900),
        label = "nivel"
    )

    Box(
        Modifier.size(tamano).clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(tamano)) {
            val w = size.width
            val h = size.height
            val grosor = grosorAnillo.toPx()
            val radio = (minOf(w, h) - grosor) / 2f
            val centro = Offset(w / 2f, h / 2f)

            // ---- 1) Fondo (anillo apagado) ----
            drawCircle(
                color = colorPrincipal.copy(alpha = 0.12f),
                radius = radio,
                center = centro
            )

            // ---- 2) Onda líquida dentro del círculo (clip al círculo interno) ----
            if (progreso != null && nivel > 0f) {
                val radioInterior = radio - grosor / 2f
                val amplitud = (4f + 3f * (1f - nivel)).coerceIn(2f, 8f) // onda más grande si está vacío
                val frecuencia = 2.0 * Math.PI / (w * 0.9f)
                val nivelY = centro.y + radioInterior - (2f * radioInterior * nivel)

                // Construimos el "path" del agua
                val path1 = Path().apply {
                    moveTo(centro.x - radioInterior, h)
                    val pasos = 30
                    for (i in 0..pasos) {
                        val x = centro.x - radioInterior + (2f * radioInterior * i / pasos)
                        val y = nivelY + amplitud * sin(frecuencia * x + faseOnda).toFloat()
                        lineTo(x, y)
                    }
                    lineTo(centro.x + radioInterior, h)
                    close()
                }
                val path2 = Path().apply {
                    moveTo(centro.x - radioInterior, h)
                    val pasos = 30
                    for (i in 0..pasos) {
                        val x = centro.x - radioInterior + (2f * radioInterior * i / pasos)
                        val y = nivelY + amplitud * 0.7f * sin(frecuencia * x * 1.4 + faseOnda2).toFloat()
                        lineTo(x, y)
                    }
                    lineTo(centro.x + radioInterior, h)
                    close()
                }

                // Recortamos las ondas dentro del círculo interior usando Path.op
                val circulo = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(
                        center = centro, radius = radioInterior
                    ))
                }
                val agua1 = Path().apply {
                    op(path1, circulo, androidx.compose.ui.graphics.PathOperation.Intersect)
                }
                val agua2 = Path().apply {
                    op(path2, circulo, androidx.compose.ui.graphics.PathOperation.Intersect)
                }
                // Onda de fondo (más tenue)
                drawPath(agua2, color = colorOnda.copy(alpha = 0.45f))
                // Onda principal
                drawPath(agua1, color = colorOnda.copy(alpha = 0.85f))
            }

            // ---- 3) Anillo exterior (el "borde" de progreso) ----
            if (progreso != null) {
                val barrido = 360f * nivel
                drawArc(
                    color = colorPrincipal,
                    startAngle = -90f,
                    sweepAngle = barrido,
                    useCenter = false,
                    topLeft = Offset(grosor / 2f, grosor / 2f),
                    size = androidx.compose.ui.geometry.Size(w - grosor, h - grosor),
                    style = Stroke(width = grosor)
                )
            } else {
                // Si no hay meta, dibujamos el anillo completo apagado
                drawCircle(
                    color = colorPrincipal.copy(alpha = 0.3f),
                    radius = radio,
                    center = centro,
                    style = Stroke(width = grosor)
                )
            }
        }

        if (mostrarPorcentaje && progreso != null) {
            Text(
                "${(nivel * 100).toInt()}%",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (tamano.value * 0.22f).sp
            )
        }
    }
}
