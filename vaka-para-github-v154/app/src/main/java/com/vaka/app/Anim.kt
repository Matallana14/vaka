package com.vaka.app

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Botón principal de Vaka con animación de "rebote" al presionar:
 * se encoge suavemente mientras está presionado y vuelve con un
 * resorte al soltarlo.
 */
@Composable
fun BotonPrincipal(
    texto: String,
    modifier: Modifier = Modifier,
    color: Color = Violeta,
    habilitado: Boolean = true,
    onClick: () -> Unit,
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "escalaBoton"
    )

    Button(
        onClick = onClick,
        enabled = habilitado,
        interactionSource = interaccion,
        modifier = modifier.graphicsLayer {
            scaleX = escala
            scaleY = escala
        },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(texto, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}
