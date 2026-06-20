package com.vaka.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Tipos de aviso interno con su tema visual asociado.
 */
enum class TipoAviso { ENVIADO, ACEPTADO, RECHAZADO, INFO, ERROR }

data class AvisoInterno(
    val id: Long = System.currentTimeMillis(),
    val tipo: TipoAviso,
    val titulo: String,
    val texto: String = "",
    val emoji: String? = null,
    val duracionMs: Long = 3500L
)

/**
 * Banner animado que se desliza desde arriba con un avance, se queda visible
 * durante `duracionMs` y luego se desliza hacia arriba con fade.
 * Tiene un ícono circular con bounce, título y texto.
 */
@Composable
fun AvisoBanner(
    aviso: AvisoInterno?,
    onTerminado: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-cerrar tras duracionMs
    LaunchedEffect(aviso?.id) {
        if (aviso != null) {
            delay(aviso.duracionMs)
            onTerminado()
        }
    }

    AnimatedVisibility(
        visible = aviso != null,
        enter = slideInVertically(
            initialOffsetY = { -it * 2 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it * 2 },
            animationSpec = tween(350, easing = FastOutLinearInEasing)
        ) + fadeOut(),
        modifier = modifier
    ) {
        aviso?.let { a ->
            val (colorFondo, colorIcono, emojiDefault) = when (a.tipo) {
                TipoAviso.ENVIADO -> Triple(
                    Brush.linearGradient(listOf(Violeta, Color(0xFF8B46E0))),
                    Color.White, "📤"
                )
                TipoAviso.ACEPTADO -> Triple(
                    Brush.linearGradient(listOf(Verde, Color(0xFF22BB87))),
                    Color.White, "🎉"
                )
                TipoAviso.RECHAZADO -> Triple(
                    Brush.linearGradient(listOf(Color(0xFF6B6679), Color(0xFF514E62))),
                    Color.White, "💔"
                )
                TipoAviso.INFO -> Triple(
                    Brush.linearGradient(listOf(Violeta, Rosa)),
                    Color.White, "ℹ️"
                )
                TipoAviso.ERROR -> Triple(
                    Brush.linearGradient(listOf(Rojo, Color(0xFFE07070))),
                    Color.White, "⚠️"
                )
            }

            // Bounce del ícono al entrar
            var bounceIniciado by remember(a.id) { mutableStateOf(false) }
            LaunchedEffect(a.id) { bounceIniciado = true }
            val escalaIcono by animateFloatAsState(
                if (bounceIniciado) 1f else 0f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "iconBounce"
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colorFondo)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .scale(escalaIcono)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = .22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(a.emoji ?: emojiDefault, fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.titulo,
                        color = colorIcono,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp)
                    if (a.texto.isNotBlank()) {
                        Text(a.texto,
                            color = colorIcono.copy(alpha = .9f),
                            fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
