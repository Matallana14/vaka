package com.vaka.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConversorScreen(
    tabla: TablaTasas?,
    monedaInicial: String,
    onBack: () -> Unit,
) {
    var monto by remember { mutableStateOf("") }
    var de by remember { mutableStateOf(monedaInicial.uppercase()) }
    var a by remember { mutableStateOf(if (monedaInicial.uppercase() == "USD") "COP" else "USD") }

    val valor = monto.replace(",", ".").toDoubleOrNull()
    val resultado = if (tabla != null && valor != null && valor > 0)
        Tasas.convertir(tabla, valor, de, a) else null
    val tasaUnitaria = if (tabla != null) Tasas.convertir(tabla, 1.0, de, a) else null

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            Text("← Inicio", fontWeight = FontWeight.ExtraBold)
        }

        // Hero con el resultado
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(Violeta, Color(0xFF8B46E0), Rosa)))
                .padding(22.dp)
        ) {
            Column {
                Text("💱 CONVERTIDOR DE MONEDAS", color = Color.White.copy(alpha = .85f),
                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Text(
                    when {
                        tabla == null -> "Cargando tasas…"
                        resultado != null -> formatea(resultado, a, "")
                        valor != null && valor > 0 -> "Sin tasa para $de o $a"
                        else -> "Escribe un monto 👇"
                    },
                    color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold
                )
                if (tasaUnitaria != null) {
                    Text("1 $de = ${formatea(tasaUnitaria, a, "")}",
                        color = Color.White.copy(alpha = .85f), fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("MONTO", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = monto, onValueChange = { monto = it },
                    placeholder = { Text("Cantidad a convertir") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), singleLine = true
                )

                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("De", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = de,
                            onValueChange = { de = it.uppercase().filter { c -> c.isLetter() }.take(4) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp), singleLine = true
                        )
                    }
                    OutlinedButton(
                        onClick = { val t = de; de = a; a = t },
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.padding(top = 18.dp)
                    ) { Text("⇄", fontSize = 18.sp) }
                    Column(Modifier.weight(1f)) {
                        Text("Para", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = a,
                            onValueChange = { a = it.uppercase().filter { c -> c.isLetter() }.take(4) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp), singleLine = true
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Rápidas:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    MONEDAS.forEach { m ->
                        Text(
                            m.code,
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (a == m.code) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(
                                    if (a == m.code) Violeta
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { a = m.code }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    if (tabla != null)
                        "Tasa de referencia diaria · actualizada: ${tabla.fecha}\nFuente: open.er-api.com (tasas de mercado, similares a las que ves en Google). Pueden diferir de las de bancos y casas de cambio. Si no hay internet, se usan las últimas tasas guardadas."
                    else
                        "No se pudieron cargar las tasas. Revisa tu conexión a internet e inténtalo de nuevo: la app guardará las tasas para que también funcionen sin conexión.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
