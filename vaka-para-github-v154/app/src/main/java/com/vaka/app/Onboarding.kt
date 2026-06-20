package com.vaka.app

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Gestión de estado del onboarding: se muestra una sola vez para usuarios nuevos.
 * Usuarios con cuenta existente (los que vienen del login) ya conocen la app
 * y no necesitan verlo.
 */
object OnboardingPrefs {
    private const val PREFS = "vaka_onboarding"
    private const val KEY_VISTO = "onboarding_visto"

    fun yaSeVio(ctx: Context): Boolean {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_VISTO, false)
    }

    fun marcarComoVisto(ctx: Context) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_VISTO, true).apply()
    }

    /**
     * Resetea el flag para que el onboarding vuelva a mostrarse. Lo usamos
     * cuando un usuario hace registro o crea una cuenta nueva con Google,
     * ya que esos casos sí ameritan mostrar el onboarding aunque ya se haya
     * visto antes en este dispositivo.
     */
    fun marcarComoNoVisto(ctx: Context) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_VISTO, false).apply()
    }
}

/**
 * Datos de una pantalla del onboarding: emoji grande, título, descripción y
 * gradiente de fondo (cada pantalla tiene su propio color).
 */
private data class PaginaOnboarding(
    val emoji: String,
    val titulo: String,
    val descripcion: String,
    val colores: List<Color>
)

private val paginas = listOf(
    PaginaOnboarding(
        emoji = "🐄",
        titulo = "Bienvenido a Vaka",
        descripcion = "Una app para ahorrar con tu gente. Crea metas y aporten juntos a su ritmo.",
        colores = listOf(Violeta, Color(0xFF8B46E0))
    ),
    PaginaOnboarding(
        emoji = "👥",
        titulo = "Crea Vakas con tu gente",
        descripcion = "Un viaje, un regalo, el arriendo. Invita a tus amigos por código o QR y vean en tiempo real cuánto va aportando cada uno.",
        colores = listOf(Color(0xFF8B46E0), Rosa)
    ),
    PaginaOnboarding(
        emoji = "🏆",
        titulo = "Hazlo divertido",
        descripcion = "Desbloquea logros, completa retos semanales y compite sanamente con tu equipo por el primer puesto del ranking.",
        colores = listOf(Rosa, Dorado)
    ),
    PaginaOnboarding(
        emoji = "🚀",
        titulo = "¿Listo para empezar?",
        descripcion = "Puedes usar Vaka sin crear cuenta para ahorros personales, o crear una cuenta para Vakas compartidas con amigos.",
        colores = listOf(Dorado, Verde)
    )
)

/**
 * Pantalla de onboarding con 4 páginas. Soporta:
 * - Swipe horizontal entre páginas
 * - Botones "Saltar" (esquina superior) y "Continuar →" / "Empezar"
 * - Indicadores de página (dots) en la parte inferior
 * - Animación de transición entre páginas
 *
 * Al terminar (o saltar) llama a onTerminado(), que guarda el flag y muestra
 * la pantalla normal de la app.
 */
@Composable
fun PantallaOnboarding(onTerminado: () -> Unit) {
    var paginaActual by remember { mutableStateOf(0) }
    val pagina = paginas[paginaActual]

    AnimatedContent(
        targetState = paginaActual,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInHorizontally(tween(350)) { it } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(350)) { -it } + fadeOut(tween(220)))
            } else {
                (slideInHorizontally(tween(350)) { -it } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(350)) { it } + fadeOut(tween(220)))
            }
        },
        label = "onboarding"
    ) { idx ->
        val p = paginas[idx]
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(p.colores))
                .pointerInput(idx) {
                    // Swipe horizontal entre páginas
                    var arrastreX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { arrastreX = 0f },
                        onDragEnd = {
                            val umbral = 100f
                            if (arrastreX < -umbral && idx < paginas.lastIndex) {
                                paginaActual = idx + 1
                            } else if (arrastreX > umbral && idx > 0) {
                                paginaActual = idx - 1
                            }
                        },
                        onDragCancel = { arrastreX = 0f },
                        onHorizontalDrag = { _, dx -> arrastreX += dx }
                    )
                }
        ) {
            // Botón Saltar (esquina superior derecha)
            if (idx < paginas.lastIndex) {
                TextButton(
                    onClick = onTerminado,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 36.dp, end = 16.dp)
                ) {
                    Text("Saltar",
                        color = Color.White.copy(alpha = .9f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp)
                }
            }

            // Animaciones de entrada para los elementos de la página.
            // Cada vez que cambia la página, se re-disparan desde 0.
            var entrar by remember(idx) { mutableStateOf(false) }
            LaunchedEffect(idx) { entrar = true }

            // Escala del emoji: aparece con bounce desde 0
            val escalaEmoji by animateFloatAsState(
                if (entrar) 1f else 0f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "escalaEmoji"
            )
            // Subida sutil del emoji (de 20dp abajo a 0)
            val offsetEmoji by animateDpAsState(
                if (entrar) 0.dp else 20.dp,
                tween(600, easing = FastOutSlowInEasing),
                label = "offsetEmoji"
            )
            // Pulso infinito del círculo (efecto "respiración")
            val pulsoInfinito = rememberInfiniteTransition(label = "pulso")
            val escalaPulso by pulsoInfinito.animateFloat(
                initialValue = 1f,
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        2500,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "escalaPulso"
            )
            // Título: fade + slide-up con delay
            val opacidadTitulo by animateFloatAsState(
                if (entrar) 1f else 0f,
                tween(400, delayMillis = 200),
                label = "opacidadTitulo"
            )
            val offsetTitulo by animateDpAsState(
                if (entrar) 0.dp else 12.dp,
                tween(400, delayMillis = 200, easing = FastOutSlowInEasing),
                label = "offsetTitulo"
            )
            // Descripción: fade + slide-up con delay mayor
            val opacidadDesc by animateFloatAsState(
                if (entrar) 1f else 0f,
                tween(400, delayMillis = 350),
                label = "opacidadDesc"
            )
            val offsetDesc by animateDpAsState(
                if (entrar) 0.dp else 12.dp,
                tween(400, delayMillis = 350, easing = FastOutSlowInEasing),
                label = "offsetDesc"
            )

            // Contenido central
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Círculo grande con el emoji (bounce de entrada + pulso continuo).
                // Calculamos la escala combinada en una variable explícitamente Float
                // para evitar ambigüedad con BigDecimal/BigInteger.
                val escalaTotal: Float = escalaEmoji * escalaPulso
                Box(
                    Modifier
                        .size(180.dp)
                        .offset(y = offsetEmoji)
                        .scale(escalaTotal)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = .18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(p.emoji, fontSize = 88.sp)
                }

                Spacer(Modifier.height(40.dp))

                Text(
                    p.titulo,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .offset(y = offsetTitulo)
                        .alpha(opacidadTitulo)
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    p.descripcion,
                    color = Color.White.copy(alpha = .9f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .offset(y = offsetDesc)
                        .alpha(opacidadDesc)
                )
            }

            // Sección inferior: dots + botón
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicadores de página con ancho animado
                Row {
                    paginas.indices.forEach { i ->
                        val activa = i == idx
                        val ancho by animateDpAsState(
                            if (activa) 26.dp else 8.dp,
                            tween(
                                300, easing = FastOutSlowInEasing
                            ),
                            label = "dotAncho$i"
                        )
                        val colorDot by animateColorAsState(
                            if (activa) Color.White else Color.White.copy(alpha = .35f),
                            tween(300),
                            label = "dotColor$i"
                        )
                        Box(
                            Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = ancho, height = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colorDot)
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Botón principal
                val esUltima = idx == paginas.lastIndex
                Button(
                    onClick = {
                        if (esUltima) onTerminado()
                        else paginaActual = idx + 1
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Violeta
                    )
                ) {
                    Text(
                        if (esUltima) "Empezar 🚀" else "Continuar →",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
