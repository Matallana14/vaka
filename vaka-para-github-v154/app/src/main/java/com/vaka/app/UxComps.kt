package com.vaka.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ============================================================
// Selector de fecha con calendario nativo
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorFecha(
    fechaIso: String,
    onSeleccionar: (String) -> Unit,
    etiqueta: String = "Fecha",
    modifier: Modifier = Modifier,
) {
    var abierto by remember { mutableStateOf(false) }

    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { abierto = true }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📅", fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                if (fechaIso.isBlank()) etiqueta else fechaBonita(fechaIso),
                color = if (fechaIso.isBlank())
                    MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }

    if (abierto) {
        val estadoInicial = if (fechaValida(fechaIso)) {
            LocalDate.parse(fechaIso).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        } else {
            Instant.now().toEpochMilli()
        }
        val estado = rememberDatePickerState(initialSelectedDateMillis = estadoInicial)
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { ms ->
                        val fecha = Instant.ofEpochMilli(ms)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                        onSeleccionar(fecha.toString())
                    }
                    abierto = false
                }) { Text("Aceptar", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { abierto = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estado)
        }
    }
}

// ============================================================
// Indicador de estado de guardado (sin conexión / guardando / guardado)
// ============================================================
enum class EstadoGuardado { OCULTO, GUARDANDO, GUARDADO, ERROR }

@Composable
fun ChipGuardado(estado: EstadoGuardado, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = estado != EstadoGuardado.OCULTO,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val (texto, color) = when (estado) {
            EstadoGuardado.GUARDANDO -> "Guardando…" to MaterialTheme.colorScheme.onSurfaceVariant
            EstadoGuardado.GUARDADO -> "Guardado ✓" to Verde
            EstadoGuardado.ERROR -> "Sin conexión · pendiente de sincronizar" to Rojo
            else -> "" to MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(
            texto,
            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color,
            modifier = modifier
                .clip(RoundedCornerShape(99.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ============================================================
// Agrupación de movimientos por períodos relativos
// ============================================================
fun grupoDeFecha(iso: String, hoyD: LocalDate = LocalDate.now()): String {
    val f = try { LocalDate.parse(iso) } catch (e: Exception) { return "Otros" }
    val dias = java.time.temporal.ChronoUnit.DAYS.between(f, hoyD)
    return when {
        dias == 0L -> "Hoy"
        dias == 1L -> "Ayer"
        dias in 2..7 -> "Esta semana"
        dias in 8..30 -> "Este mes"
        else -> {
            val meses = arrayOf(
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
            )
            "${meses[f.monthValue - 1]} ${f.year}"
        }
    }
}
