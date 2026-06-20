package com.vaka.app

import androidx.compose.runtime.Composable

/**
 * Diálogo animado de felicitación cuando el usuario desbloquea un logro.
 * Wrapper alrededor de DialogoCelebracion usando el tipo LogroDesbloqueado.
 */
@Composable
fun DialogoLogroNuevo(logro: Logro, onCerrar: () -> Unit) {
    DialogoCelebracion(
        tipo = TipoCelebracion.LogroDesbloqueado,
        emojiCentral = logro.emoji,
        titulo = logro.nombre,
        descripcion = logro.descripcion,
        textoBoton = "¡Genial! 🎉",
        onCerrar = onCerrar
    )
}
