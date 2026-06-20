package com.vaka.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Tipos de celebración pre-configurados con su tema visual.
 * Cada uno define colores, etiqueta superior, emojis del confetti, etc.
 */
open class TipoCelebracion(
    val etiquetaSuperior: String,
    val colorEtiqueta: Color,
    val coloresCirculo: List<Color>,
    val emojisConfetti: List<String>,
    val mostrarRayos: Boolean = true
) {
    companion object {
        /** Subir al primer puesto en un ranking. */
        val NuevoLider = TipoCelebracion(
            etiquetaSuperior = "¡NUEVO LÍDER!",
            colorEtiqueta = Dorado,
            coloresCirculo = listOf(Dorado, Color(0xFFFFA94D)),
            emojisConfetti = listOf("👑", "🎉", "✨", "🏆", "🎊"),
            mostrarRayos = true
        )

        /** Te destronaron: tono con humor pero motivador. */
        val Destronado = TipoCelebracion(
            etiquetaSuperior = "TE DESTRONARON",
            colorEtiqueta = Rosa,
            coloresCirculo = listOf(Color(0xFF8B46E0), Rosa),
            emojisConfetti = listOf("😬", "💪", "🔥", "⚡", "👀"),
            mostrarRayos = false
        )

        /** Reto semanal cumplido: máxima celebración. */
        val RetoCumplido = TipoCelebracion(
            etiquetaSuperior = "¡RETO CUMPLIDO!",
            colorEtiqueta = Verde,
            coloresCirculo = listOf(Verde, Color(0xFF22BB87)),
            emojisConfetti = listOf("🔥", "💪", "🎉", "🚀", "⭐"),
            mostrarRayos = true
        )

        /** Reto semanal fallado: motivacional pero honesto. */
        val RetoFallado = TipoCelebracion(
            etiquetaSuperior = "RETO NO CUMPLIDO",
            colorEtiqueta = Color(0xFF6B6679),
            coloresCirculo = listOf(Color(0xFF6B6679), Color(0xFF514E62)),
            emojisConfetti = listOf("💪", "🌱", "🔄", "🐌", "🐢"),
            mostrarRayos = false
        )

        /** Logro desbloqueado. */
        val LogroDesbloqueado = TipoCelebracion(
            etiquetaSuperior = "¡LOGRO DESBLOQUEADO!",
            colorEtiqueta = Dorado,
            coloresCirculo = listOf(Dorado, Color(0xFFFFA94D)),
            emojisConfetti = listOf("🎉", "✨", "🎊", "⭐", "🎉"),
            mostrarRayos = true
        )
    }
}

/**
 * Diálogo animado de celebración, reutilizable para varios eventos.
 *
 * Estructura visual:
 *  - Etiqueta superior pequeña con color temático
 *  - Emoji gigante con efecto bounce, dentro de un círculo con gradiente
 *  - Rayos rotatorios detrás (opcional según tipo)
 *  - Fila de emojis de confetti que aparecen con fade staggered
 *  - Título grande y descripción
 *  - Botón "Entendido" para cerrar
 */
@Composable
fun DialogoCelebracion(
    tipo: TipoCelebracion,
    emojiCentral: String,
    titulo: String,
    descripcion: String,
    textoBoton: String = "¡Entendido!",
    onCerrar: () -> Unit
) {
    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var animar by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { animar = true }

        // Bounce del emoji central
        val escalaEmoji by animateFloatAsState(
            if (animar) 1f else 0f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "escalaEmoji"
        )

        // Rotación constante de los rayos (si aplican)
        val rotacionInfinita = rememberInfiniteTransition(label = "rayos")
        val rotacion by rotacionInfinita.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotacion"
        )
        val pulso by rotacionInfinita.animateFloat(
            initialValue = 0.85f, targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulso"
        )

        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(28.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        tipo.etiquetaSuperior,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tipo.colorEtiqueta,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))

                    // Emoji con rayos animados detrás (si aplica)
                    Box(contentAlignment = Alignment.Center) {
                        if (tipo.mostrarRayos) {
                            Box(
                                Modifier.size(160.dp).rotate(rotacion).scale(pulso),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    Modifier.fillMaxSize().clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    tipo.colorEtiqueta.copy(alpha = .35f),
                                                    tipo.colorEtiqueta.copy(alpha = .08f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                            }
                        }
                        Box(
                            Modifier.size(110.dp).scale(escalaEmoji)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(tipo.coloresCirculo)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emojiCentral, fontSize = 60.sp)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Confetti
                    Row {
                        tipo.emojisConfetti.forEachIndexed { i, e ->
                            val delay = i * 100
                            val opacidad by animateFloatAsState(
                                if (animar) 1f else 0f,
                                tween(durationMillis = 600, delayMillis = 400 + delay),
                                label = "confetti$i"
                            )
                            Text(
                                e,
                                fontSize = 22.sp,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .graphicsLayer(alpha = opacidad)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        titulo,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        descripcion,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(22.dp))

                    Button(
                        onClick = onCerrar,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Violeta)
                    ) {
                        Text(textoBoton,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color.White)
                    }
                }
            }
        }
    }
}
