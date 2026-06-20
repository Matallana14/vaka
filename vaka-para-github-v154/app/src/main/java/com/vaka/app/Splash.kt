package com.vaka.app

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Bandera que sobrevive a rotaciones/recreaciones de Activity,
 *  pero se reinicia cuando el proceso muere (cuando cierras la app). */
object SplashEstado {
    var yaMostrado: Boolean = false
}

/**
 * Pantalla de bienvenida (~1.8 segundos):
 * la moneda entra rebotando con un giro y el nombre
 * aparece deslizándose hacia arriba con un fundido.
 */
@Composable
fun SplashScreen(onFin: () -> Unit) {
    var arranca by remember { mutableStateOf(false) }
    var saliendo by remember { mutableStateOf(false) }

    // Moneda: crece desde cero con resorte y medio giro
    val escala by animateFloatAsState(
        targetValue = if (arranca) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "escalaMoneda"
    )
    val giro by animateFloatAsState(
        targetValue = if (arranca) 0f else -180f,
        animationSpec = tween(700),
        label = "giroMoneda"
    )

    // Texto: aparece después, subiendo con fundido
    val alfaTexto by animateFloatAsState(
        targetValue = if (arranca) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 450),
        label = "alfaTexto"
    )
    val subida by animateDpAsState(
        targetValue = if (arranca) 0.dp else 18.dp,
        animationSpec = tween(durationMillis = 500, delayMillis = 450),
        label = "subidaTexto"
    )

    // Fundido global de salida: toda la pantalla se desvanece suavemente al final
    val alfaPantalla by animateFloatAsState(
        targetValue = if (saliendo) 0f else 1f,
        animationSpec = tween(durationMillis = 450),
        label = "fundidoSalida",
        finishedListener = { if (saliendo) onFin() }
    )

    LaunchedEffect(Unit) {
        arranca = true
        delay(1800)
        saliendo = true   // dispara el fundido y al terminar llama onFin
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = alfaPantalla }
            .background(Brush.linearGradient(listOf(Violeta, Color(0xFF8B46E0), Rosa))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // La moneda dorada de Vaka
            Box(
                Modifier
                    .size(112.dp)
                    .graphicsLayer {
                        scaleX = escala
                        scaleY = escala
                        rotationZ = giro
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFFE08A), Dorado, Color(0xFFE59E13))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(80.dp)
                        .border(4.dp, Color(0xFFD88F0A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "V",
                        color = VioletaOscuro,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Vaka",
                color = Color.White,
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .offset(y = subida)
                    .graphicsLayer { alpha = alfaTexto }
            )
        }
    }
}
