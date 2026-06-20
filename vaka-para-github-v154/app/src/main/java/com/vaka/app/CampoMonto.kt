package com.vaka.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

/**
 * Da formato a un texto numérico para mostrarlo bonito mientras se escribe.
 * "50000" → "$ 50.000". Conserva los decimales y un punto/coma final.
 */
fun formatoMontoEnVivo(crudo: String, monedaCode: String, monedaSymbol: String): String {
    if (crudo.isBlank()) return ""
    val normalizado = crudo.replace(",", ".")
    val partes = normalizado.split(".", limit = 2)
    val entera = partes[0].filter { it.isDigit() }
    if (entera.isBlank() && partes.size < 2) return ""
    val numero = entera.toLongOrNull() ?: 0L
    val nf = NumberFormat.getIntegerInstance(Locale("es", "CO"))
    val texto = StringBuilder()
    texto.append("${monedaSymbol.ifBlank { monedaCode + " " }} ")
    texto.append(nf.format(numero))
    if (partes.size == 2) {
        texto.append(",")
        texto.append(partes[1].filter { it.isDigit() }.take(2))
    }
    return texto.toString()
}

/**
 * Campo de monto con formato en vivo + teclado tipo calculadora.
 * El usuario solo ve montos formateados; internamente trabajamos con el
 * string crudo (solo dígitos + punto opcional).
 */
@Composable
fun CampoMonto(
    crudo: String,
    onCrudoChange: (String) -> Unit,
    monedaCode: String,
    monedaSymbol: String,
    placeholder: String = "Monto",
    modifier: Modifier = Modifier,
) {
    var tecladoVisible by remember { mutableStateOf(false) }
    val formateado = formatoMontoEnVivo(crudo, monedaCode, monedaSymbol)

    Column(modifier) {
        // El "campo" mostrado al usuario (no es un OutlinedTextField real para
        // que el teclado del sistema no compita con nuestro teclado de calculadora)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { tecladoVisible = !tecladoVisible }
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            if (formateado.isBlank()) {
                Text(
                    placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
            } else {
                Text(
                    formateado,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        AnimatedVisibility(
            visible = tecladoVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Column(Modifier.padding(top = 10.dp)) {
                TecladoCalculadora(
                    onDigito = { d ->
                        // Solo permitimos un separador decimal
                        if (d == "." && crudo.contains(".")) return@TecladoCalculadora
                        // Si ya hay dos decimales, no agregamos más
                        if (crudo.contains(".") && crudo.substringAfter(".").length >= 2) {
                            if (d != ".") return@TecladoCalculadora
                        }
                        onCrudoChange(crudo + d)
                    },
                    onBorrar = {
                        if (crudo.isNotEmpty()) onCrudoChange(crudo.dropLast(1))
                    },
                    onListo = { tecladoVisible = false }
                )
            }
        }
    }
}

@Composable
private fun TecladoCalculadora(
    onDigito: (String) -> Unit,
    onBorrar: () -> Unit,
    onListo: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TeclaNum("1", Modifier.weight(1f)) { onDigito("1") }
            TeclaNum("2", Modifier.weight(1f)) { onDigito("2") }
            TeclaNum("3", Modifier.weight(1f)) { onDigito("3") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TeclaNum("4", Modifier.weight(1f)) { onDigito("4") }
            TeclaNum("5", Modifier.weight(1f)) { onDigito("5") }
            TeclaNum("6", Modifier.weight(1f)) { onDigito("6") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TeclaNum("7", Modifier.weight(1f)) { onDigito("7") }
            TeclaNum("8", Modifier.weight(1f)) { onDigito("8") }
            TeclaNum("9", Modifier.weight(1f)) { onDigito("9") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TeclaNum(",", Modifier.weight(1f)) { onDigito(".") }
            TeclaNum("0", Modifier.weight(1f)) { onDigito("0") }
            Tecla("⌫", Color(0xFFD64545), Color.White, Modifier.weight(1f)) { onBorrar() }
        }
        TextButton(
            onClick = onListo,
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            Text("Listo ✓", fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TeclaNum(texto: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Tecla(
        texto,
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.onSurface,
        modifier,
        onClick
    )
}

@Composable
private fun Tecla(
    texto: String,
    fondo: Color,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(fondo)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(texto, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}
