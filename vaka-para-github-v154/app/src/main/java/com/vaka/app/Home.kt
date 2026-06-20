package com.vaka.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PeopleAlt
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

// ============================================================
// Pestañas
// ============================================================
enum class Pestana(val titulo: String, val icono: androidx.compose.ui.graphics.vector.ImageVector) {
    INICIO("Inicio", Icons.Rounded.Home),
    PRIVADAS("Vakas", Icons.Rounded.Savings),
    COMPARTIDAS("Equipo", Icons.Rounded.Groups),
    AMIGOS("Amigos", Icons.Rounded.PeopleAlt),
    PERFIL("Ajustes", Icons.Rounded.Settings),
}

@Composable
fun BarraInferior(activa: Pestana, onSeleccionar: (Pestana) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Pestana.entries.forEach { p ->
            val sel = activa == p
            val color by animateColorAsState(
                if (sel) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                tween(250), label = "tab"
            )
            val escala by animateFloatAsState(
                if (sel) 1.08f else 1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "esc"
            )
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSeleccionar(p) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = p.icono,
                    contentDescription = p.titulo,
                    tint = color,
                    modifier = Modifier
                        .size((26 * escala).dp)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    p.titulo,
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.Medium
                )
            }
        }
    }
}

// ============================================================
// Inicio (Mi ahorro fijo destacado + atajos)
// ============================================================
@Composable
fun TabInicio(
    nombreUsuario: String?,
    colorAvatar: String,
    vakasPrivadas: List<VakaItem>,
    vakasCompartidas: List<VakaItem>,
    logrosGanados: Set<String> = emptySet(),
    monedaPrincipal: String = "COP",
    onAbrirVaka: (VakaItem) -> Unit,
    onIrAVakas: () -> Unit = {},
) {
    val colorPrincipal = colorAvatarPor(colorAvatar)
    val totalGeneral = vakasPrivadas.size + vakasCompartidas.size

    // Símbolo de la moneda principal para mostrar en patrimonio convertido.
    // Tomamos el de la primera Vaka que use esa moneda, o "$" si nadie.
    val simboloMonedaPrincipal = remember(vakasPrivadas, vakasCompartidas, monedaPrincipal) {
        (vakasPrivadas + vakasCompartidas)
            .firstOrNull { it.monedaCode == monedaPrincipal }?.monedaSymbol ?: "$"
    }

    // Cálculos útiles para los widgets — todos memoizados con remember para que
    // SOLO se recalculen cuando cambian las Vakas reales, no en cada recomposición.
    val todasLasVakas = remember(vakasPrivadas, vakasCompartidas) {
        vakasPrivadas + vakasCompartidas
    }
    // Suma "ingenua" sin conversión (la usamos como fallback si no hay tasas)
    val totalAhorradoGlobal = remember(todasLasVakas) {
        todasLasVakas.sumOf { it.total }
    }
    // Detectar si hay múltiples monedas en juego. Si todas las Vakas usan la
    // misma moneda que la principal, no necesitamos convertir nada.
    val monedasUnicas = remember(todasLasVakas) {
        todasLasVakas.map { it.monedaCode }.toSet()
    }
    val hayMultiplesMonedas = remember(monedasUnicas, monedaPrincipal) {
        monedasUnicas.size > 1 || !monedasUnicas.contains(monedaPrincipal)
    }
    // Estados para conversión de monedas
    val ctxLocal = androidx.compose.ui.platform.LocalContext.current
    var tablaTasas by remember { mutableStateOf<TablaTasas?>(null) }
    var cargandoTasas by remember { mutableStateOf(false) }
    var errorTasas by remember { mutableStateOf(false) }

    // Cargar tasas SOLO si hay múltiples monedas (no gastar batería sin necesidad)
    LaunchedEffect(hayMultiplesMonedas) {
        if (hayMultiplesMonedas && tablaTasas == null) {
            cargandoTasas = true
            val t = Tasas.obtener(ctxLocal)
            tablaTasas = t
            errorTasas = (t == null)
            cargandoTasas = false
        }
    }

    // Calcular patrimonio convertido a la moneda principal.
    // Si no hay tablas (error de red), cae al método simple.
    val patrimonioConvertido = remember(todasLasVakas, tablaTasas, monedaPrincipal) {
        val tabla = tablaTasas
        if (tabla == null) {
            // Sin tasas: solo sumamos las Vakas que YA están en la moneda principal
            todasLasVakas.filter { it.monedaCode == monedaPrincipal }.sumOf { it.total }
        } else {
            todasLasVakas.sumOf { v ->
                if (v.monedaCode == monedaPrincipal) v.total
                else Tasas.convertir(tabla, v.total, v.monedaCode, monedaPrincipal) ?: 0.0
            }
        }
    }

    // Por cada Vaka, calcular su equivalente en la moneda principal (para el desglose)
    val desgloseConvertido = remember(todasLasVakas, tablaTasas, monedaPrincipal) {
        todasLasVakas.filter { it.total > 0 }.map { v ->
            val convertido = if (v.monedaCode == monedaPrincipal) v.total
            else tablaTasas?.let { Tasas.convertir(it, v.total, v.monedaCode, monedaPrincipal) } ?: 0.0
            Triple(v, v.total, convertido)
        }
    }

    var desgloseAbierto by remember { mutableStateOf(false) }
    // Top 3 Vakas con meta más cerca de cumplir (excluye ahorro fijo)
    val vakasConMeta = remember(vakasPrivadas, vakasCompartidas) {
        (vakasPrivadas + vakasCompartidas)
            .filter { it.meta > 0 }
            .sortedByDescending {
                (it.total / it.meta).coerceAtMost(1.0)
            }
            .take(3)
    }
    // Racha: días consecutivos con al menos un movimiento de depósito
    val racha = remember(todasLasVakas) { calcularRacha(todasLasVakas) }
    // Reto semanal: superar lo depositado la semana pasada
    val retoSemanal = remember(todasLasVakas, tablaTasas, monedaPrincipal) {
        calcularRetoSemanal(todasLasVakas, monedaPrincipal, tablaTasas)
    }
    // Resumen del mes pasado (solo se muestra en los primeros 7 días del mes nuevo)
    val resumenMes = remember(todasLasVakas, tablaTasas, monedaPrincipal) {
        calcularResumenMensual(todasLasVakas, monedaPrincipal, tablaTasas)
    }
    // Logros AGREGADOS de todas tus Vakas. Construimos una Vaka virtual con
    // todos los movimientos para que los logros midan tu progreso global, no
    // solo el ahorro fijo.
    // Vaka virtual que agrega todos los movimientos de TODAS las Vakas reales.
    // La usamos para calcular logros sobre todo tu historial, no solo el de
    // una Vaka en particular.
    // Vaka virtual que agrega todos los movimientos de TODAS las Vakas reales.
    // La usamos para calcular logros sobre todo tu historial, no solo el de
    // una Vaka en particular. Los montos se convierten a la moneda principal
    // para que los logros (1 millón, aporte ballena, etc.) midan tu progreso
    // GLOBAL real, no la suma cruda de distintas monedas.
    val vakaAgregada = remember(todasLasVakas, monedaPrincipal, simboloMonedaPrincipal, tablaTasas) {
        val movsConvertidos = todasLasVakas.flatMap { v ->
            v.movs.map { m ->
                val montoConvertido = if (v.monedaCode == monedaPrincipal) {
                    m.monto
                } else if (tablaTasas != null) {
                    Tasas.convertir(tablaTasas!!, m.monto, v.monedaCode, monedaPrincipal) ?: m.monto
                } else {
                    // Sin tasas: respetamos el monto crudo pero será impreciso.
                    // En este caso preferimos no contar el movimiento para no
                    // engañar al usuario.
                    if (v.monedaCode == monedaPrincipal) m.monto else 0.0
                }
                m.copy(monto = montoConvertido)
            }
        }
        VakaItem(
            id = -2L,
            nombre = "agregada",
            emoji = "💎",
            monedaCode = monedaPrincipal,
            monedaSymbol = simboloMonedaPrincipal,
            meta = todasLasVakas.maxOfOrNull { it.meta } ?: 0.0,
            metaFecha = null,
            metaDesde = null,
            movs = movsConvertidos
        )
    }
    val logrosCalculados = remember(vakaAgregada) { logrosDe(vakaAgregada) }
    // Combinamos los logros actualmente activos con los que ya estaban ganados
    // (persistidos en SharedPreferences). Así, aunque borres una Vaka, los
    // logros que ganaste por ella siguen marcados como desbloqueados.
    val logros = remember(logrosCalculados, logrosGanados) {
        logrosCalculados.map { l ->
            if (l.ok || l.id in logrosGanados) l.copy(ok = true) else l
        }
    }
    val logrosDesbloqueados = remember(logros) { logros.count { it.ok } }
    var dialogoLogros by remember { mutableStateOf(false) }

    // Detectar si es un usuario "primera vez". Ahora la lógica es más estricta:
    // ningún movimiento en NINGUNA Vaka, sin Vakas privadas/compartidas, Y sin
    // logros previamente desbloqueados (para no confundir a usuarios que borraron
    // sus Vakas pero sí tienen historia con la app).
    val esPrimeraVez = remember(todasLasVakas, logrosGanados) {
        todasLasVakas.all { it.movs.isEmpty() } &&
            vakasPrivadas.isEmpty() &&
            vakasCompartidas.isEmpty() &&
            logrosGanados.isEmpty()
    }

    if (esPrimeraVez) {
        HeroBienvenida(
            nombreUsuario = nombreUsuario,
            onCrearVaka = onIrAVakas
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Saludo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(24.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFFFFE08A), Dorado, Color(0xFFE59E13))))
            )
            Spacer(Modifier.width(8.dp))
            Text("Vaka", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary)
        }
        Text(
            if (nombreUsuario != null) "Hola, $nombreUsuario 👋" else "Modo invitado",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))

        // === 📊 Total ahorrado global ===
        if (totalGeneral > 0 || totalAhorradoGlobal > 0) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💰", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
                        Text("TOTAL AHORRADO", fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(8.dp))

                    // Si hay múltiples monedas Y ya tenemos las tasas, mostramos el
                    // total convertido con un "≈" para indicar que es aproximado.
                    if (hayMultiplesMonedas && tablaTasas != null) {
                        Text(
                            "≈ ${formatea(patrimonioConvertido, monedaPrincipal, simboloMonedaPrincipal)}",
                            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                            color = colorPrincipal
                        )
                        Text("Convertido a tu moneda principal con tasas del día",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (hayMultiplesMonedas && cargandoTasas) {
                        Text(
                            "Calculando…",
                            fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                            color = colorPrincipal
                        )
                        Text("Obteniendo las tasas de cambio del día",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (hayMultiplesMonedas && errorTasas) {
                        // Sin internet: solo mostramos lo que está en la moneda principal
                        Text(
                            formatea(patrimonioConvertido, monedaPrincipal, simboloMonedaPrincipal),
                            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                            color = colorPrincipal
                        )
                        Text("Sin conexión: solo se suman Vakas en $monedaPrincipal",
                            fontSize = 12.sp, color = Rojo, fontWeight = FontWeight.Bold)
                    } else {
                        // Caso normal: todas las Vakas están en la misma moneda
                        Text(
                            formatea(totalAhorradoGlobal, monedaPrincipal, simboloMonedaPrincipal),
                            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                            color = colorPrincipal
                        )
                        Text("Sumando todas tus Vakas y tu ahorro general",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Botón "Ver desglose" solo si hay múltiples monedas y vale la pena
                    if (hayMultiplesMonedas && tablaTasas != null) {
                        Spacer(Modifier.height(10.dp))
                        TextButton(
                            onClick = { desgloseAbierto = !desgloseAbierto },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
                        ) {
                            Text(
                                if (desgloseAbierto) "▲ Ocultar desglose"
                                else "▼ Ver desglose por moneda",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(
                            visible = desgloseAbierto,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                desgloseConvertido.forEach { (v, original, convertido) ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(v.emoji, fontSize = 18.sp,
                                            modifier = Modifier.padding(end = 8.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(v.nombre, fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold)
                                            Text(
                                                if (v.monedaCode == monedaPrincipal)
                                                    formatea(original, v.monedaCode, v.monedaSymbol)
                                                else
                                                    "${formatea(original, v.monedaCode, v.monedaSymbol)}  ≈  " +
                                                        formatea(convertido, monedaPrincipal, simboloMonedaPrincipal),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "📊 Tasas obtenidas hoy. Pueden variar.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (totalGeneral > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "$totalGeneral ${if (totalGeneral == 1) "Vaka activa" else "Vakas activas"} · " +
                                "${vakasCompartidas.size} compartida${if (vakasCompartidas.size == 1) "" else "s"}",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // === 📈 Mini-gráfica de 7 días ===
        val datosSemana = remember(todasLasVakas, tablaTasas, monedaPrincipal) {
            calcularSemana(todasLasVakas, monedaPrincipal, tablaTasas)
        }
        val tieneMovimientosRecientes = datosSemana.any { it.deposito > 0 || it.retiro > 0 }
        if (tieneMovimientosRecientes) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📈", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
                        Text("ÚLTIMOS 7 DÍAS", fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    GraficaSemana(datos = datosSemana, colorPrincipal = colorPrincipal,
                        moneda = monedaPrincipal, simbolo = simboloMonedaPrincipal)
                    val totalSemana = datosSemana.sumOf { it.deposito - it.retiro }
                    if (totalSemana != 0.0) {
                        Spacer(Modifier.height(10.dp))
                        val esPositivo = totalSemana > 0
                        Text(
                            (if (esPositivo) "↑ Ahorraste " else "↓ Sacaste ") +
                                formatea(kotlin.math.abs(totalSemana), monedaPrincipal, simboloMonedaPrincipal) +
                                " esta semana",
                            fontSize = 13.sp,
                            color = if (esPositivo) Verde else Rojo,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // === 📜 Historial unificado: últimos movimientos de TODAS las Vakas ===
        val ultimosMovs = remember(todasLasVakas) {
            todasLasVakas.flatMap { v -> v.movs.map { m -> v to m } }
                .sortedByDescending { it.second.fecha + it.second.id.toString() }
                .take(8)
        }
        if (ultimosMovs.isNotEmpty()) {
            HistorialUnificadoCard(ultimosMovs, onAbrirVaka)
            Spacer(Modifier.height(14.dp))
        }

        // === 📅 Resumen del mes pasado (solo primeros 7 días del mes nuevo) ===
        if (resumenMes.mostrar) {
            ResumenMensualCard(resumenMes, monedaPrincipal, simboloMonedaPrincipal)
            Spacer(Modifier.height(14.dp))
        }

        // === 🔥 Reto semanal ===
        if (todasLasVakas.any { v -> v.movs.any { it.tipo == "deposito" } }) {
            RetoSemanalCard(
                reto = retoSemanal,
                monedaCode = monedaPrincipal,
                monedaSymbol = simboloMonedaPrincipal,
                hayConversion = hayMultiplesMonedas && tablaTasas != null
            )
            Spacer(Modifier.height(14.dp))
        }

        // === 🎯 Top Vakas con meta cerca de cumplir ===
        if (vakasConMeta.isNotEmpty()) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎯", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
                        Text("MÁS CERCA DE CUMPLIR", fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    vakasConMeta.forEachIndexed { idx, v ->
                        val avance = (v.total / v.meta).coerceIn(0.0, 1.0)
                        val pct = (avance * 100).toInt()
                        val falta = (v.meta - v.total).coerceAtLeast(0.0)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onAbrirVaka(v) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnilloOndaLiquida(
                                progreso = avance.toFloat(),
                                tamano = 48.dp,
                                colorPrincipal = colorPrincipal,
                                colorOnda = Dorado,
                                grosorAnillo = 3.dp,
                                mostrarPorcentaje = false
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${v.emoji} ${v.nombre}",
                                    fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                                    maxLines = 1)
                                Text(
                                    if (avance >= 1.0) "✓ ¡Meta cumplida!"
                                    else "Faltan " + formatea(falta, v.monedaCode, v.monedaSymbol),
                                    fontSize = 12.sp,
                                    color = if (avance >= 1.0) Verde
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("$pct%", fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp, color = colorPrincipal)
                        }
                        if (idx < vakasConMeta.lastIndex) HorizontalDivider()
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // === 🔥 Racha + 🏆 Logros (lado a lado) ===
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Racha
            ElevatedCard(Modifier.weight(1f), shape = RoundedCornerShape(20.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔥", fontSize = 30.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("$racha", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (racha > 0) Dorado else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (racha == 0) "Sin racha"
                        else if (racha == 1) "día seguido"
                        else "días seguidos",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Logros desbloqueados — clickeable para abrir el diálogo
            ElevatedCard(
                Modifier.weight(1f).clickable { dialogoLogros = true },
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏆", fontSize = 30.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("$logrosDesbloqueados/${logros.size}",
                        fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (logrosDesbloqueados > 0) Dorado else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("logros",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    Text("Toca para ver",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    // Diálogo con todos los logros y su progreso
    if (dialogoLogros) {
        DialogoTodosLosLogros(
            logros = logros,
            vakaAgregada = vakaAgregada,
            onCerrar = { dialogoLogros = false }
        )
    }
}

/**
 * Datos de un día para la gráfica semanal.
 */
private data class DiaDato(val etiqueta: String, val deposito: Double, val retiro: Double)

/**
 * Calcula los depósitos y retiros totales de cada uno de los últimos 7 días,
 * sumando los movimientos de todas las Vakas dadas.
 */
private fun calcularSemana(
    vakas: List<VakaItem>,
    monedaPrincipal: String = "COP",
    tablaTasas: TablaTasas? = null
): List<DiaDato> {
    val hoy = java.time.LocalDate.now()
    val etiquetas = listOf("L", "M", "X", "J", "V", "S", "D")
    return (6 downTo 0).map { atrás ->
        val dia = hoy.minusDays(atrás.toLong())
        val diaStr = dia.toString()
        var dep = 0.0
        var ret = 0.0
        vakas.forEach { v ->
            v.movs.filter { it.fecha == diaStr }.forEach { m ->
                // Convertir el monto a la moneda principal antes de sumar
                val montoConv = if (v.monedaCode == monedaPrincipal) {
                    m.monto
                } else if (tablaTasas != null) {
                    Tasas.convertir(tablaTasas, m.monto, v.monedaCode, monedaPrincipal) ?: 0.0
                } else {
                    0.0  // sin tasas, ignoramos para no mostrar valores falsos
                }
                if (m.tipo == "deposito") dep += montoConv else ret += montoConv
            }
        }
        val idx = (dia.dayOfWeek.value - 1).coerceIn(0, 6)  // lunes = 0
        DiaDato(etiqueta = etiquetas[idx], deposito = dep, retiro = ret)
    }
}

/**
 * Mini-gráfica de barras de 7 días.
 * Cada barra muestra el depósito (verde) sobre la línea cero y el retiro (rojo) debajo.
 */
@Composable
private fun GraficaSemana(
    datos: List<DiaDato>,
    colorPrincipal: Color,
    moneda: String,
    simbolo: String,
) {
    val maxMonto = (datos.maxOf { maxOf(it.deposito, it.retiro) }).coerceAtLeast(1.0)
    Row(
        Modifier.fillMaxWidth().height(110.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        datos.forEach { d ->
            val esHoy = datos.last() == d
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                val alturaDep = ((d.deposito / maxMonto) * 70).toFloat().coerceAtLeast(if (d.deposito > 0) 3f else 0f)
                val alturaRet = ((d.retiro / maxMonto) * 70).toFloat().coerceAtLeast(if (d.retiro > 0) 3f else 0f)
                // Barra retiro (encima)
                if (d.retiro > 0) {
                    Box(
                        Modifier
                            .width(18.dp).height(alturaRet.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(Rojo.copy(alpha = .8f))
                    )
                }
                // Barra depósito
                if (d.deposito > 0) {
                    Box(
                        Modifier
                            .width(18.dp).height(alturaDep.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(Verde)
                    )
                } else if (d.retiro == 0.0) {
                    // Marcador para días sin movimiento
                    Box(
                        Modifier
                            .width(18.dp).height(3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(d.etiqueta, fontSize = 11.sp,
                    fontWeight = if (esHoy) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (esHoy) colorPrincipal else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Calcula la racha actual: días consecutivos (terminando hoy o ayer)
 * con al menos un depósito en cualquiera de las Vakas.
 */
private fun calcularRacha(vakas: List<VakaItem>): Int {
    val fechasConDeposito = vakas
        .flatMap { it.movs }
        .filter { it.tipo == "deposito" }
        .map { it.fecha }
        .toSet()
    if (fechasConDeposito.isEmpty()) return 0
    val hoy = java.time.LocalDate.now()
    // Permitimos que la racha empiece hoy o ayer
    var dia = if (hoy.toString() in fechasConDeposito) hoy else hoy.minusDays(1)
    if (dia.toString() !in fechasConDeposito) return 0
    var racha = 0
    while (dia.toString() in fechasConDeposito) {
        racha++
        dia = dia.minusDays(1)
    }
    return racha
}

/**
 * Resultado del reto semanal: compara cuánto depositaste esta semana vs la pasada.
 */
/**
 * Tipos de reto semanal. Cada semana rota a uno diferente según el número
 * de semana del año, para mantenerlo interesante.
 *  - SUPERAR: ahorrar más que la semana pasada (clásico)
 *  - APORTAR_DIARIO: hacer al menos un aporte cada día
 *  - META_FIJA: ahorrar al menos X (porcentaje sobre el patrimonio actual)
 *  - VARIOS_APORTES: hacer N aportes en la semana
 */
enum class TipoReto {
    SUPERAR, APORTAR_DIARIO, META_FIJA, VARIOS_APORTES
}

data class RetoSemanal(
    val tipo: TipoReto,
    val depositadoSemanaActual: Double,
    val depositadoSemanaPasada: Double,
    val metaSugerida: Double,
    val faltante: Double,
    val cumplido: Boolean,
    val esPrimeraVez: Boolean,
    // Campos específicos según tipo
    val diasConAporte: Int = 0,          // para APORTAR_DIARIO
    val diasFaltantes: Int = 0,           // días que quedan en la semana
    val cantidadAportes: Int = 0,         // para VARIOS_APORTES
    val aportesObjetivo: Int = 0          // para VARIOS_APORTES
)

private fun calcularRetoSemanal(
    vakas: List<VakaItem>,
    monedaPrincipal: String = "COP",
    tablaTasas: TablaTasas? = null
): RetoSemanal {
    val hoy = java.time.LocalDate.now()
    val inicioSemanaActual = hoy.with(java.time.DayOfWeek.MONDAY)
    val finSemanaActual = inicioSemanaActual.plusDays(6)  // Domingo
    val finSemanaPasada = inicioSemanaActual.minusDays(1)
    val inicioSemanaPasada = finSemanaPasada.with(java.time.DayOfWeek.MONDAY)

    var actual = 0.0
    var pasada = 0.0
    val fechasConAporteEstaSemana = mutableSetOf<java.time.LocalDate>()
    var cantidadAportes = 0
    for (v in vakas) {
        for (m in v.movs) {
            if (m.tipo != "deposito") continue
            val fecha = try {
                java.time.LocalDate.parse(m.fecha)
            } catch (e: Exception) { continue }
            // Convertir el monto a la moneda principal usando la tabla de tasas.
            // Si la moneda ya es la principal, no se convierte. Si no hay tablas
            // disponibles (sin internet) y la moneda es distinta, ignoramos el
            // movimiento para no sumar manzanas con peras.
            val montoConvertido = if (v.monedaCode == monedaPrincipal) {
                m.monto
            } else if (tablaTasas != null) {
                Tasas.convertir(tablaTasas, m.monto, v.monedaCode, monedaPrincipal) ?: 0.0
            } else {
                0.0  // fallback: ignora si no podemos convertir
            }
            when {
                !fecha.isBefore(inicioSemanaActual) && !fecha.isAfter(hoy) -> {
                    actual += montoConvertido
                    fechasConAporteEstaSemana.add(fecha)
                    cantidadAportes++
                }
                !fecha.isBefore(inicioSemanaPasada) && !fecha.isAfter(finSemanaPasada) -> pasada += montoConvertido
            }
        }
    }

    // Días restantes hasta el domingo (inclusive)
    val diasFaltantes = java.time.temporal.ChronoUnit.DAYS.between(hoy, finSemanaActual).toInt() + 1

    // Rotación de retos por número de semana del año
    val numeroSemana = hoy.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
    val esPrimeraVez = pasada == 0.0 && actual == 0.0
    val tipoBase = TipoReto.values()[numeroSemana % TipoReto.values().size]
    // Si no hay datos pasada, usamos VARIOS_APORTES como reto inicial
    val tipo = if (esPrimeraVez) TipoReto.VARIOS_APORTES else tipoBase

    return when (tipo) {
        TipoReto.SUPERAR -> {
            val meta = pasada + 1.0
            RetoSemanal(
                tipo = tipo,
                depositadoSemanaActual = actual,
                depositadoSemanaPasada = pasada,
                metaSugerida = meta,
                faltante = (meta - actual).coerceAtLeast(0.0),
                cumplido = pasada > 0 && actual > pasada,
                esPrimeraVez = esPrimeraVez,
                diasFaltantes = diasFaltantes
            )
        }
        TipoReto.APORTAR_DIARIO -> {
            // Días transcurridos esta semana (lunes a hoy)
            val diasObjetivo = java.time.temporal.ChronoUnit.DAYS.between(inicioSemanaActual, hoy).toInt() + 1
            RetoSemanal(
                tipo = tipo,
                depositadoSemanaActual = actual,
                depositadoSemanaPasada = pasada,
                metaSugerida = 0.0,
                faltante = 0.0,
                cumplido = fechasConAporteEstaSemana.size >= 7,  // 7 días con aportes
                esPrimeraVez = esPrimeraVez,
                diasConAporte = fechasConAporteEstaSemana.size,
                diasFaltantes = diasFaltantes,
                aportesObjetivo = diasObjetivo
            )
        }
        TipoReto.META_FIJA -> {
            // Meta = 2% del patrimonio total (convertido a moneda principal),
            // o lo de la semana pasada si fue mayor, con un mínimo razonable.
            val totalGlobal = vakas.sumOf { v ->
                if (v.monedaCode == monedaPrincipal) v.total
                else tablaTasas?.let { Tasas.convertir(it, v.total, v.monedaCode, monedaPrincipal) } ?: 0.0
            }
            val metaFija = maxOf(pasada, totalGlobal * 0.02).coerceAtLeast(10000.0)
            RetoSemanal(
                tipo = tipo,
                depositadoSemanaActual = actual,
                depositadoSemanaPasada = pasada,
                metaSugerida = metaFija,
                faltante = (metaFija - actual).coerceAtLeast(0.0),
                cumplido = actual >= metaFija,
                esPrimeraVez = esPrimeraVez,
                diasFaltantes = diasFaltantes
            )
        }
        TipoReto.VARIOS_APORTES -> {
            val objetivo = 3  // 3 aportes esta semana
            RetoSemanal(
                tipo = tipo,
                depositadoSemanaActual = actual,
                depositadoSemanaPasada = pasada,
                metaSugerida = 0.0,
                faltante = 0.0,
                cumplido = cantidadAportes >= objetivo,
                esPrimeraVez = esPrimeraVez,
                diasFaltantes = diasFaltantes,
                cantidadAportes = cantidadAportes,
                aportesObjetivo = objetivo
            )
        }
    }
}

/**
 * Resumen del mes pasado para mostrar en los primeros días del mes nuevo.
 * Si estamos pasado el día 7 del mes, ya no tiene mucho sentido mostrarlo.
 */
data class ResumenMensual(
    val nombreMes: String,           // ej. "enero"
    val depositadoEnMes: Double,
    val retiradoEnMes: Double,
    val cantidadDepositos: Int,
    val esElMejorMes: Boolean,       // ¿es el mes con más depósitos del año?
    val mostrar: Boolean             // false si ya pasaron muchos días del mes nuevo
)

private fun calcularResumenMensual(
    vakas: List<VakaItem>,
    monedaPrincipal: String = "COP",
    tablaTasas: TablaTasas? = null
): ResumenMensual {
    val hoy = java.time.LocalDate.now()
    // Solo mostramos el resumen en los primeros 7 días del mes
    val mostrar = hoy.dayOfMonth <= 7
    val mesPasado = hoy.minusMonths(1)
    val anoMesPasado = mesPasado.year
    val mesPasadoNum = mesPasado.monthValue

    var deposit = 0.0
    var retiro = 0.0
    var nDeps = 0
    // Para "esElMejorMes" guardamos cuánto se depositó cada mes del año
    val depPorMes = mutableMapOf<Int, Double>()

    for (v in vakas) {
        for (m in v.movs) {
            val fecha = try {
                java.time.LocalDate.parse(m.fecha)
            } catch (e: Exception) { continue }
            // Convertir monto a moneda principal
            val montoConv = if (v.monedaCode == monedaPrincipal) {
                m.monto
            } else if (tablaTasas != null) {
                Tasas.convertir(tablaTasas, m.monto, v.monedaCode, monedaPrincipal) ?: 0.0
            } else {
                0.0
            }
            if (fecha.year == anoMesPasado && fecha.monthValue == mesPasadoNum) {
                if (m.tipo == "deposito") {
                    deposit += montoConv
                    nDeps++
                } else {
                    retiro += montoConv
                }
            }
            if (fecha.year == anoMesPasado && m.tipo == "deposito") {
                depPorMes[fecha.monthValue] = (depPorMes[fecha.monthValue] ?: 0.0) + montoConv
            }
        }
    }
    val mejorMes = depPorMes.maxByOrNull { it.value }?.key
    val nombresMeses = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
    return ResumenMensual(
        nombreMes = nombresMeses[mesPasadoNum - 1],
        depositadoEnMes = deposit,
        retiradoEnMes = retiro,
        cantidadDepositos = nDeps,
        esElMejorMes = mejorMes == mesPasadoNum,
        mostrar = mostrar && deposit > 0
    )
}

// ============================================================
// Tab: Vakas privadas
// ============================================================
@Composable
fun TabPrivadas(
    privadas: List<VakaItem>,
    monedaPrincipal: String,
    amigosDisponibles: List<PerfilUsuario> = emptyList(),
    puedeCompartir: Boolean = false,
    onAbrir: (VakaItem) -> Unit,
    onCrear: (DatosNuevaVaka) -> Unit,
    onEliminar: (VakaItem) -> Unit,
) {
    var creando by remember { mutableStateOf(false) }
    var borrando by remember { mutableStateOf<VakaItem?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        TituloPestana("🐷 Mis Vakas", "Solo tú las ves y editas")
        Spacer(Modifier.height(14.dp))

        if (privadas.isEmpty() && !creando) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Text(
                    "Aún no tienes Vakas privadas.\nCrea una para empezar a ahorrar ✨",
                    Modifier.fillMaxWidth().padding(26.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        // Favoritas primero, luego el resto en su orden original
        val privadasOrdenadas = remember(privadas) {
            privadas.sortedByDescending { it.favorita }
        }
        privadasOrdenadas.forEach { v ->
            VakaTarjeta(v, onAbrir = onAbrir, onBorrar = { borrando = it })
        }

        AnimatedVisibility(creando, enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()) {
            CrearVakaCard(
                monedaInicial = monedaPrincipal,
                amigosDisponibles = amigosDisponibles,
                puedeCompartir = puedeCompartir,
                nombresExistentes = privadas.map { it.nombre },
                onCrear = { datos -> onCrear(datos); creando = false },
                onCancelar = { creando = false }
            )
        }

        if (!creando) {
            BotonPrincipal("＋ Nueva Vaka", Modifier.fillMaxWidth().height(52.dp)) { creando = true }
        }
        Spacer(Modifier.height(80.dp))
    }

    borrando?.let { v ->
        val tieneDinero = v.total > 0
        var textoConfirmacion by remember(v) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { borrando = null; textoConfirmacion = "" },
            title = {
                Text(
                    if (tieneDinero) "⚠️ Eliminar \"${v.nombre}\""
                    else "¿Eliminar \"${v.nombre}\"?"
                )
            },
            text = {
                Column {
                    if (tieneDinero) {
                        Text(
                            "Esta Vaka tiene ${formatea(v.total, v.monedaCode, v.monedaSymbol)} acumulados.",
                            fontWeight = FontWeight.Bold,
                            color = Rojo
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Se borrarán los ${v.movs.size} movimientos. " +
                                "Esto NO afecta tu dinero real, solo el registro. " +
                                "Esta acción no se puede deshacer."
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Para confirmar, escribe ELIMINAR:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = textoConfirmacion,
                            onValueChange = { textoConfirmacion = it },
                            placeholder = { Text("ELIMINAR") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Se borrarán todos sus movimientos. Esto no se puede deshacer.")
                    }
                }
            },
            confirmButton = {
                val puedeBorrar = !tieneDinero || textoConfirmacion.trim().uppercase() == "ELIMINAR"
                TextButton(
                    onClick = {
                        onEliminar(v)
                        borrando = null
                        textoConfirmacion = ""
                    },
                    enabled = puedeBorrar
                ) {
                    Text(
                        "Sí, eliminar",
                        color = if (puedeBorrar) Rojo else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { borrando = null; textoConfirmacion = "" }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ============================================================
// Tab: Vakas compartidas
// ============================================================
@Composable
fun TabCompartidas(
    compartidas: List<VakaItem>,
    nombreUsuario: String?,
    onAbrir: (VakaItem) -> Unit,
    onUnirse: (String, (String) -> Unit) -> Unit,
    onSalir: (VakaItem) -> Unit,
    onIrALogin: () -> Unit,
) {
    var uniendo by remember { mutableStateOf(false) }
    var codigo by remember { mutableStateOf("") }
    var errorUnion by remember { mutableStateOf("") }
    var saliendo by remember { mutableStateOf<VakaItem?>(null) }

    val escaner = rememberLauncherForActivityResult(ScanContract()) { resultado ->
        val cod = codigoDesdeQr(resultado.contents)
        if (resultado.contents != null) {
            if (cod != null) onUnirse(cod) { e -> errorUnion = e; uniendo = true }
            else { errorUnion = "Ese QR no parece ser de una Vaka."; uniendo = true }
        }
    }
    fun escanearQr() {
        val opciones = ScanOptions()
            .setPrompt("Apunta la cámara al código QR de la Vaka")
            .setBeepEnabled(false)
            .setOrientationLocked(true)
            .setCaptureActivity(CaptureActivityPortrait::class.java)
        escaner.launch(opciones)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        TituloPestana("👥 Vakas compartidas", "Ahorras en equipo con amigos")
        Spacer(Modifier.height(14.dp))

        if (nombreUsuario == null) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Inicia sesión para Vakas compartidas",
                        fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Las Vakas compartidas se sincronizan en la nube. Necesitas una cuenta para crearlas o unirte a una.",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    BotonPrincipal("Crear cuenta / Iniciar sesión",
                        Modifier.fillMaxWidth().height(48.dp)) { onIrALogin() }
                }
            }
            Spacer(Modifier.height(80.dp))
            return@Column
        }

        if (compartidas.isEmpty() && !uniendo) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Text(
                    "Aún no tienes Vakas compartidas.\nÚnete a una con QR o crea una desde tus Vakas privadas ✨",
                    Modifier.fillMaxWidth().padding(26.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        compartidas.forEach { v ->
            VakaTarjeta(v, onAbrir = onAbrir, onBorrar = { saliendo = it })
        }

        AnimatedVisibility(uniendo, enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("UNIRSE A UNA VAKA", fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(Modifier.height(10.dp))
                    BotonPrincipal("📷 Escanear código QR",
                        Modifier.fillMaxWidth().height(48.dp)) { escanearQr() }
                    Spacer(Modifier.height(10.dp))
                    Text("O escribe el código:",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = codigo,
                        onValueChange = { codigo = it.uppercase() },
                        placeholder = { Text("Código: ej. K7M3PX") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp), singleLine = true
                    )
                    if (errorUnion.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorUnion, color = Rojo, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    BotonPrincipal("Unirme", Modifier.fillMaxWidth().height(48.dp)) {
                        errorUnion = ""
                        if (codigo.trim().length < 4) errorUnion = "Escribe el código completo."
                        else onUnirse(codigo.trim()) { e -> errorUnion = e }
                    }
                    TextButton(onClick = { uniendo = false; errorUnion = "" },
                        modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (!uniendo) {
            BotonPrincipal("📷 Unirme escaneando un QR",
                Modifier.fillMaxWidth().height(50.dp)) { escanearQr() }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { uniendo = true }, modifier = Modifier.fillMaxWidth()) {
                Text("o escribe el código manualmente",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(80.dp))
    }

    saliendo?.let { v ->
        AlertDialog(
            onDismissRequest = { saliendo = null },
            title = { Text("¿Salir de \"${v.nombre}\"?") },
            text = { Text("Los demás miembros seguirán teniendo acceso y podrás volver con el código.") },
            confirmButton = {
                TextButton(onClick = { onSalir(v); saliendo = null }) {
                    Text("Sí, salir", color = Rojo, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { saliendo = null }) { Text("Cancelar") } }
        )
    }
}

// ============================================================
// Componentes compartidos
// ============================================================
@Composable
fun TituloPestana(titulo: String, sub: String) {
    Column {
        Text(titulo, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground)
        Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun VakaTarjeta(v: VakaItem, onAbrir: (VakaItem) -> Unit, onBorrar: (VakaItem) -> Unit) {
    ElevatedCard(
        Modifier.fillMaxWidth().clickable { onAbrir(v) }.padding(bottom = 10.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Mini anillo de onda líquida (o emoji si no hay meta)
            if (v.progreso != null) {
                Box(contentAlignment = Alignment.Center) {
                    AnilloOndaLiquida(
                        progreso = v.progreso,
                        tamano = 54.dp,
                        colorPrincipal = MaterialTheme.colorScheme.primary,
                        colorOnda = Dorado,
                        grosorAnillo = 3.dp,
                        mostrarPorcentaje = false
                    )
                    Text(v.emoji, fontSize = 22.sp)
                }
            } else {
                Box(
                    Modifier.size(54.dp).clip(RoundedCornerShape(17.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) { Text(v.emoji, fontSize = 26.sp) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (v.favorita) {
                        Text("⭐", fontSize = 14.sp,
                            modifier = Modifier.padding(end = 4.dp))
                    }
                    Text(v.nombre, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
                }
                Text(
                    formatea(v.total, v.monedaCode, v.monedaSymbol),
                    fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (v.esCompartida && v.miembros.isNotEmpty()) {
                    Text(
                        v.miembros.joinToString(", ") { it.nombre },
                        fontSize = 11.sp, maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                v.progreso?.let { p ->
                    val pAnim by animateFloatAsState(p, tween(800), label = "pct")
                    Spacer(Modifier.height(3.dp))
                    Text("${(pAnim * 100).toInt()}% de tu meta",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    v.monedaCode, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                )
                TextButton(onClick = { onBorrar(v) }) {
                    Text(if (v.esCompartida) "🚪" else "🗑", fontSize = 15.sp)
                }
            }
        }
    }
}

/**
 * Datos extendidos de creación de Vaka.
 * Si compartir=true, se crea como Vaka compartida en Firebase y se invitan
 * automáticamente a los amigos seleccionados.
 */
data class DatosNuevaVaka(
    val nombre: String,
    val emoji: String,
    val moneda: MonedaSugerida,
    val meta: Double,
    val fecha: String?,
    val compartir: Boolean,
    val amigosInvitados: List<PerfilUsuario>,
)

@Composable
fun CrearVakaCard(
    monedaInicial: String = "COP",
    amigosDisponibles: List<PerfilUsuario> = emptyList(),
    puedeCompartir: Boolean = false,
    nombresExistentes: List<String> = emptyList(),
    onCrear: (DatosNuevaVaka) -> Unit,
    onCancelar: () -> Unit,
) {
    var nombre by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🐷") }
    var moneda by remember {
        mutableStateOf(MONEDAS.find { it.code == monedaInicial } ?: MONEDAS[0])
    }
    var metaTxt by remember { mutableStateOf("") }
    var fechaTxt by remember { mutableStateOf("") }
    var expandida by remember { mutableStateOf(false) }
    var compartir by remember { mutableStateOf(false) }
    var seleccionadosUids by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Detección de nombre duplicado (case-insensitive, ignorando espacios extras).
    // Se calcula en cada cambio del campo nombre.
    val nombreDuplicado = remember(nombre, nombresExistentes) {
        val limpio = nombre.trim().lowercase()
        limpio.isNotBlank() && nombresExistentes.any { it.trim().lowercase() == limpio }
    }

    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("NUEVA VAKA", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))

            // Plantillas rápidas (solo se muestran si aún no han escrito nombre)
            if (nombre.isBlank()) {
                Text("O empieza con una plantilla:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PLANTILLAS_VAKA.forEach { p ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    nombre = p.nombre
                                    emoji = p.emoji
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.emoji, fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 4.dp))
                                Text(p.nombre, fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = nombre, onValueChange = { nombre = it },
                placeholder = { Text("Nombre: ej. Viaje a Cartagena") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            Text("Ícono:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                EMOJIS.forEach { e ->
                    val sel = emoji == e
                    val fondo by animateColorAsState(
                        if (sel) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        tween(200), label = "fe"
                    )
                    Box(
                        Modifier.padding(end = 6.dp, top = 6.dp).size(44.dp)
                            .clip(RoundedCornerShape(14.dp)).background(fondo)
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center
                    ) { Text(e, fontSize = 22.sp) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Box {
                OutlinedButton(
                    onClick = { expandida = true },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                ) { Text("${moneda.code} · ${moneda.nombre}") }
                DropdownMenu(expanded = expandida, onDismissRequest = { expandida = false }) {
                    MONEDAS.forEach { m ->
                        DropdownMenuItem(
                            text = { Text("${m.code} · ${m.nombre}") },
                            onClick = { moneda = m; expandida = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Meta de ahorro (opcional)", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            Text("Define cuánto quieres ahorrar para activar tu progreso",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            CampoMonto(
                crudo = metaTxt, onCrudoChange = { metaTxt = it },
                monedaCode = moneda.code, monedaSymbol = moneda.symbol,
                placeholder = "Toca para escribir la meta"
            )
            Spacer(Modifier.height(8.dp))
            SelectorFecha(
                fechaIso = fechaTxt, onSeleccionar = { fechaTxt = it },
                etiqueta = "¿Para cuándo?"
            )

            // ===== SECCIÓN COMPARTIR =====
            if (puedeCompartir) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("¿Compartir esta Vaka? 👥",
                            fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Text(
                            if (compartir)
                                "Otras personas podrán aportar y verán los movimientos"
                            else
                                "Activa para ahorrar en equipo con amigos",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = compartir,
                        onCheckedChange = {
                            compartir = it
                            if (!it) seleccionadosUids = emptySet()
                        }
                    )
                }

                // Sub-sección: seleccionar amigos a invitar
                AnimatedVisibility(visible = compartir) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        if (amigosDisponibles.isEmpty()) {
                            // Sin amigos: solo se mostrará el QR/código después
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📱", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
                                Column {
                                    Text("Aún no tienes amigos en Vaka",
                                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Al crear la Vaka recibirás un código QR para compartir.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            Text("INVITAR AMIGOS (opcional)",
                                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp)
                            Spacer(Modifier.height(8.dp))
                            amigosDisponibles.forEach { amigo ->
                                val sel = amigo.uid in seleccionadosUids
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            seleccionadosUids = if (sel)
                                                seleccionadosUids - amigo.uid
                                            else
                                                seleccionadosUids + amigo.uid
                                        }
                                        .background(
                                            if (sel) MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                                            else Color.Transparent
                                        )
                                        .padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarPerfil(
                                        fotoBase64 = amigo.fotoBase64,
                                        nombre = amigo.nombre,
                                        colorAvatar = amigo.colorAvatar,
                                        tamano = 36.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(amigo.nombre, fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    if (sel) {
                                        Text("✓", color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (seleccionadosUids.isEmpty())
                                    "No selecciones nada si prefieres invitarlos luego con código/QR"
                                else
                                    "${seleccionadosUids.size} ${if (seleccionadosUids.size == 1) "amigo seleccionado" else "amigos seleccionados"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Aviso de nombre duplicado, sobre el botón
            if (nombreDuplicado) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ya tienes una Vaka con ese nombre. Elige otro.",
                    color = Rojo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(14.dp))
            BotonPrincipal(
                if (compartir) "Crear y compartir" else "Crear Vaka",
                Modifier.fillMaxWidth().height(50.dp),
                habilitado = !nombreDuplicado
            ) {
                if (nombre.isNotBlank() && !nombreDuplicado) {
                    val meta = metaTxt.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val fecha = fechaTxt.trim().ifBlank { null }?.takeIf { fechaValida(it) }
                    val invitados = amigosDisponibles.filter { it.uid in seleccionadosUids }
                    onCrear(DatosNuevaVaka(
                        nombre = nombre.trim(),
                        emoji = emoji,
                        moneda = moneda,
                        meta = if (meta > 0) meta else 0.0,
                        fecha = fecha,
                        compartir = compartir && puedeCompartir,
                        amigosInvitados = invitados
                    ))
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
        }
    }
}

// ============================================================
// Widgets de Inicio: Resumen mensual y Reto semanal
// ============================================================

/**
 * Tarjeta de resumen del mes pasado.
 * Aparece solo en los primeros 7 días del mes nuevo y si hubo movimientos.
 * Refuerzo positivo: muestra cuánto se ahorró y celebra si fue el mejor mes del año.
 */
@Composable
private fun ResumenMensualCard(
    resumen: ResumenMensual,
    monedaCode: String,
    monedaSymbol: String
) {
    val depositadoFmt = formatea(resumen.depositadoEnMes, monedaCode, monedaSymbol)
    val esMejor = resumen.esElMejorMes

    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        if (esMejor) listOf(Dorado, Color(0xFFFFA94D), Rosa)
                        else listOf(Violeta, Color(0xFF8B46E0), Rosa)
                    )
                )
                .padding(18.dp)
        ) {
            Text(
                if (esMejor) "🏆 ¡TU MEJOR MES DEL AÑO!"
                else "📅 RESUMEN DE ${resumen.nombreMes.uppercase()}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(6.dp))
            Text("Ahorraste",
                color = Color.White.copy(alpha = .85f),
                fontSize = 13.sp)
            Text(depositadoFmt,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "${resumen.cantidadDepositos} ${if (resumen.cantidadDepositos == 1) "depósito" else "depósitos"} en ${resumen.nombreMes}." +
                    if (esMejor) " ¡Récord del año! 🎉"
                    else " ¡Sigue así! 💪",
                color = Color.White.copy(alpha = .9f),
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Tarjeta del reto semanal. Cambia su aspecto y mensaje según:
 *  - El tipo de reto activo de la semana
 *  - Si está cumplido, en progreso, o ya falló
 *  - Cuántos días faltan hasta el domingo (entra en modo "alerta" cuando queda 1 día)
 */
@Composable
private fun RetoSemanalCard(
    reto: RetoSemanal,
    monedaCode: String,
    monedaSymbol: String,
    hayConversion: Boolean = false
) {
    // Cuando hay múltiples monedas, prefijamos los montos con "≈" para
    // indicar que son aproximaciones convertidas a la moneda principal.
    val pref = if (hayConversion) "≈ " else ""
    val depActualFmt = pref + formatea(reto.depositadoSemanaActual, monedaCode, monedaSymbol)
    val depPasadaFmt = pref + formatea(reto.depositadoSemanaPasada, monedaCode, monedaSymbol)
    val faltanteFmt = pref + formatea(reto.faltante, monedaCode, monedaSymbol)
    val metaFmt = pref + formatea(reto.metaSugerida, monedaCode, monedaSymbol)

    // Modo "alerta": cuando queda 1 solo día y el reto NO está cumplido todavía
    val esAlerta = reto.diasFaltantes <= 1 && !reto.cumplido && !reto.esPrimeraVez

    // Gradiente del fondo dependiendo del estado
    val colores = when {
        reto.cumplido -> listOf(Verde, Color(0xFF22BB87))
        esAlerta -> listOf(Rojo, Color(0xFFE05050))
        reto.esPrimeraVez -> listOf(Violeta, Rosa)
        else -> listOf(Color(0xFFE07700), Dorado)
    }

    // Pulso del borde si está en alerta (efecto "atención!")
    val pulsoValor: Float? = if (esAlerta) {
        val transition = rememberInfiniteTransition(label = "pulsoReto")
        val anim by transition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(700),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alphaPulso"
        )
        anim
    } else null

    // Texto del header dependiendo del estado
    val tituloHeader = when {
        reto.cumplido -> "🎉 ¡RETO CUMPLIDO!"
        esAlerta -> "🚨 ¡ÚLTIMO DÍA DEL RETO!"
        reto.esPrimeraVez -> "🚀 PRIMERA SEMANA"
        else -> when (reto.tipo) {
            TipoReto.SUPERAR -> "🔥 RETO DE LA SEMANA"
            TipoReto.APORTAR_DIARIO -> "📅 RETO DIARIO"
            TipoReto.META_FIJA -> "🎯 RETO DE META"
            TipoReto.VARIOS_APORTES -> "💪 RETO DE CONSTANCIA"
        }
    }

    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(colores))
                .androidx_alpha_si_alerta(pulsoValor)
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tituloHeader,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f)
                )
                // Chip con días restantes (solo si reto activo y no cumplido)
                if (!reto.esPrimeraVez && !reto.cumplido) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (reto.diasFaltantes) {
                                0 -> "Cierra hoy"
                                1 -> "1 día"
                                else -> "${reto.diasFaltantes} días"
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            when {
                reto.cumplido -> when (reto.tipo) {
                    TipoReto.SUPERAR -> {
                        Text(
                            "Esta semana llevas $depActualFmt — ¡superaste los $depPasadaFmt de la pasada!",
                            color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Sigue así y la próxima semana repites 💪",
                            color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
                    }
                    TipoReto.APORTAR_DIARIO -> {
                        Text(
                            "¡Ahorraste todos los días! 7/7 ✅",
                            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Constancia perfecta. Eres una máquina del ahorro 🤖",
                            color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
                    }
                    TipoReto.META_FIJA -> {
                        Text(
                            "Lograste tu meta de $metaFmt esta semana 🎯",
                            color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Aportaste $depActualFmt en total. ¡Excelente!",
                            color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
                    }
                    TipoReto.VARIOS_APORTES -> {
                        Text(
                            "${reto.cantidadAportes} aportes esta semana 🎉",
                            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Superaste el objetivo de ${reto.aportesObjetivo} aportes. ¡Bien hecho!",
                            color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
                    }
                }

                reto.esPrimeraVez -> {
                    Text(
                        "¡Bienvenido a tu primera semana ahorrando!",
                        color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Intenta hacer ${reto.aportesObjetivo} aportes esta semana para empezar con el pie derecho 🚀",
                        color = Color.White.copy(alpha = .9f), fontSize = 12.sp
                    )
                }

                else -> when (reto.tipo) {
                    TipoReto.SUPERAR -> {
                        Text(
                            if (esAlerta) "¡Te faltan $faltanteFmt y se acaba el tiempo!"
                            else "Te faltan $faltanteFmt",
                            color = Color.White,
                            fontSize = if (esAlerta) 18.sp else 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "para superar los $depPasadaFmt de la semana pasada.",
                            color = Color.White.copy(alpha = .92f), fontSize = 13.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Vas $depActualFmt esta semana 🔥",
                            color = Color.White.copy(alpha = .85f), fontSize = 12.sp
                        )
                    }
                    TipoReto.APORTAR_DIARIO -> {
                        val emoji = if (esAlerta) "🚨" else "📆"
                        Text(
                            "$emoji ${reto.diasConAporte}/${reto.aportesObjetivo} días con aporte",
                            color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (esAlerta) "¡Hoy es tu última oportunidad para mantener la racha!"
                            else "Haz al menos un aporte HOY para mantener la racha.",
                            color = Color.White.copy(alpha = .9f), fontSize = 12.sp
                        )
                    }
                    TipoReto.META_FIJA -> {
                        Text(
                            if (esAlerta) "¡Te faltan $faltanteFmt para cerrar la semana!"
                            else "Te faltan $faltanteFmt",
                            color = Color.White,
                            fontSize = if (esAlerta) 18.sp else 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "para alcanzar tu meta semanal de $metaFmt 🎯",
                            color = Color.White.copy(alpha = .92f), fontSize = 13.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Vas $depActualFmt esta semana",
                            color = Color.White.copy(alpha = .85f), fontSize = 12.sp
                        )
                    }
                    TipoReto.VARIOS_APORTES -> {
                        val faltanAportes = reto.aportesObjetivo - reto.cantidadAportes
                        Text(
                            if (esAlerta) "¡Te falta $faltanAportes aporte${if (faltanAportes==1) "" else "s"}, ya es hoy o nunca!"
                            else "${reto.cantidadAportes}/${reto.aportesObjetivo} aportes esta semana",
                            color = Color.White,
                            fontSize = if (esAlerta) 16.sp else 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (esAlerta) "Haz un aporte ya mismo para no perder el reto."
                            else "Haz $faltanAportes ${if (faltanAportes==1) "aporte más" else "aportes más"} antes del domingo.",
                            color = Color.White.copy(alpha = .9f), fontSize = 12.sp
                        )
                    }
                }
            }

            // Aviso pequeño si los montos están convertidos
            if (hayConversion) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "📊 Montos convertidos a $monedaCode con tasas del día",
                    color = Color.White.copy(alpha = .7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Helper que aplica .alpha solo si el valor es no-nulo (modo alerta con pulso)
private fun Modifier.androidx_alpha_si_alerta(valor: Float?): Modifier =
    if (valor != null) this.alpha(valor) else this

// ============================================================
// Diálogo de todos los logros (accesible desde Inicio)
// ============================================================

/**
 * Calcula el progreso 0..1 de un logro. Devuelve null si el logro no es de tipo "cuantificable".
 * Se basa en el id estable del logro definido en Datos.kt.
 */
private fun progresoLogro(logro: Logro, vaka: VakaItem): Float? {
    if (logro.ok) return 1f
    val deps = vaka.movs.filter { it.tipo == "deposito" }
    val cantidadDeps = deps.size
    val totalDep = deps.sumOf { it.monto }
    val depMaximo = deps.maxOfOrNull { it.monto } ?: 0.0
    val wf = java.time.temporal.WeekFields.ISO
    val semanas = deps.mapNotNull {
        try {
            val d = java.time.LocalDate.parse(it.fecha)
            "${d.year}-${d.get(wf.weekOfWeekBasedYear())}"
        } catch (e: Exception) { null }
    }.toSet().size
    val p = if (vaka.meta > 0) vaka.total / vaka.meta else null

    return when (logro.id) {
        "primer_dep" -> if (cantidadDeps > 0) 1f else 0f
        "cinco_deps" -> (cantidadDeps / 5f).coerceIn(0f, 1f)
        "diez_deps" -> (cantidadDeps / 10f).coerceIn(0f, 1f)
        "veinticinco_deps" -> (cantidadDeps / 25f).coerceIn(0f, 1f)
        "cincuenta_deps" -> (cantidadDeps / 50f).coerceIn(0f, 1f)
        "constancia_3" -> (semanas / 3f).coerceIn(0f, 1f)
        "constancia_8" -> (semanas / 8f).coerceIn(0f, 1f)
        "constancia_16" -> (semanas / 16f).coerceIn(0f, 1f)
        "mitad" -> p?.let { (it / 0.5).toFloat().coerceIn(0f, 1f) } ?: 0f
        "setenta_cinco" -> p?.let { (it / 0.75).toFloat().coerceIn(0f, 1f) } ?: 0f
        "meta" -> p?.toFloat()?.coerceIn(0f, 1f) ?: 0f
        "superameta" -> p?.let { (it / 1.2).toFloat().coerceIn(0f, 1f) } ?: 0f
        "primer_millon" -> (totalDep / 1_000_000.0).toFloat().coerceIn(0f, 1f)
        "gran_aporte" -> (depMaximo / 500_000.0).toFloat().coerceIn(0f, 1f)
        else -> null
    }
}

/** Texto que indica el avance hacia el logro (ej. "12 / 25 depósitos"). */
private fun textoProgresoLogro(logro: Logro, vaka: VakaItem): String? {
    if (logro.ok) return null
    val deps = vaka.movs.filter { it.tipo == "deposito" }
    val cantidadDeps = deps.size
    val totalDep = deps.sumOf { it.monto }
    val depMaximo = deps.maxOfOrNull { it.monto } ?: 0.0
    val wf = java.time.temporal.WeekFields.ISO
    val semanas = deps.mapNotNull {
        try {
            val d = java.time.LocalDate.parse(it.fecha)
            "${d.year}-${d.get(wf.weekOfWeekBasedYear())}"
        } catch (e: Exception) { null }
    }.toSet().size
    val p = if (vaka.meta > 0) vaka.total / vaka.meta else null

    return when (logro.id) {
        "primer_dep" -> "Haz tu primer depósito"
        "cinco_deps" -> "$cantidadDeps / 5 depósitos"
        "diez_deps" -> "$cantidadDeps / 10 depósitos"
        "veinticinco_deps" -> "$cantidadDeps / 25 depósitos"
        "cincuenta_deps" -> "$cantidadDeps / 50 depósitos"
        "constancia_3" -> "$semanas / 3 semanas con aportes"
        "constancia_8" -> "$semanas / 8 semanas con aportes"
        "constancia_16" -> "$semanas / 16 semanas con aportes"
        "mitad" -> if (p != null) "${(p * 100).toInt()}% de la meta (necesitas 50%)" else "Define una meta primero"
        "setenta_cinco" -> if (p != null) "${(p * 100).toInt()}% de la meta (necesitas 75%)" else "Define una meta primero"
        "meta" -> if (p != null) "${(p * 100).toInt()}% de la meta" else "Define una meta primero"
        "superameta" -> if (p != null) "${(p * 100).toInt()}% de la meta (necesitas 120%)" else "Define una meta primero"
        "primer_millon" -> {
            val pct = (totalDep / 1_000_000.0 * 100).toInt()
            "${formatea(totalDep, vaka.monedaCode, vaka.monedaSymbol)} / ${formatea(1_000_000.0, vaka.monedaCode, vaka.monedaSymbol)} ($pct%)"
        }
        "gran_aporte" -> {
            val pct = (depMaximo / 500_000.0 * 100).toInt()
            "Tu mayor depósito: ${formatea(depMaximo, vaka.monedaCode, vaka.monedaSymbol)} ($pct% de 500k)"
        }
        else -> null
    }
}

@Composable
private fun DialogoTodosLosLogros(
    logros: List<Logro>,
    vakaAgregada: VakaItem,
    onCerrar: () -> Unit
) {
    val desbloqueados = logros.filter { it.ok }
    val bloqueados = logros.filter { !it.ok }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onCerrar,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxSize()) {
                    // === Header ===
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(Violeta, Color(0xFF8B46E0), Rosa)))
                            .padding(horizontal = 22.dp, vertical = 18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("🏆 TUS LOGROS",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("${desbloqueados.size} de ${logros.size} desbloqueados",
                                    color = Color.White.copy(alpha = .9f),
                                    fontSize = 13.sp)
                            }
                            IconButton(onClick = onCerrar) {
                                Text("✕", color = Color.White, fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        // Barra de progreso global, debajo del texto "X de Y desbloqueados"
                        BarraProgresoLimpia(
                            progreso = desbloqueados.size.toFloat() / logros.size.coerceAtLeast(1),
                            colorRelleno = Dorado,
                            colorFondo = Color.White.copy(alpha = .25f),
                            altura = 8.dp
                        )
                    }

                    // === Lista de logros ===
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp)
                    ) {
                        if (desbloqueados.isNotEmpty()) {
                            Text("DESBLOQUEADOS",
                                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                color = Verde, letterSpacing = 1.sp)
                            Spacer(Modifier.height(8.dp))
                            desbloqueados.forEach { l ->
                                FilaLogroConProgreso(l, vakaAgregada)
                                Spacer(Modifier.height(8.dp))
                            }
                            Spacer(Modifier.height(14.dp))
                        }

                        if (bloqueados.isNotEmpty()) {
                            Text("POR DESBLOQUEAR",
                                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp)
                            Spacer(Modifier.height(8.dp))
                            bloqueados.forEach { l ->
                                FilaLogroConProgreso(l, vakaAgregada)
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaLogroConProgreso(logro: Logro, vakaAgregada: VakaItem) {
    val progreso = remember(logro, vakaAgregada) { progresoLogro(logro, vakaAgregada) }
    val textoProg = remember(logro, vakaAgregada) { textoProgresoLogro(logro, vakaAgregada) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (logro.ok) Dorado.copy(alpha = .15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(logro.emoji, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(logro.nombre,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = if (logro.ok) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(if (logro.ok) "✓" else "🔒",
                    fontSize = 14.sp,
                    color = if (logro.ok) Verde else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(3.dp))
            Text(logro.descripcion,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Barra de progreso para logros bloqueados
            if (!logro.ok && progreso != null) {
                Spacer(Modifier.height(8.dp))
                BarraProgresoLimpia(
                    progreso = progreso,
                    colorRelleno = Violeta,
                    colorFondo = MaterialTheme.colorScheme.surfaceVariant,
                    altura = 6.dp
                )
                if (textoProg != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(textoProg,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Barra de progreso custom con look limpio (sin la "stop indicator" raya
 * del extremo derecho que añade Material 3 a LinearProgressIndicator).
 * Incluye animación suave del relleno cuando el progreso cambia.
 */
@Composable
private fun BarraProgresoLimpia(
    progreso: Float,
    colorRelleno: Color,
    colorFondo: Color,
    altura: androidx.compose.ui.unit.Dp = 6.dp
) {
    val animado by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progreso.coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 800,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "barraAnim"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(altura)
            .clip(RoundedCornerShape(altura / 2))
            .background(colorFondo)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animado)
                .clip(RoundedCornerShape(altura / 2))
                .background(colorRelleno)
        )
    }
}

/**
 * Hero de bienvenida que aparece en la pestaña Inicio cuando el usuario
 * no tiene ninguna Vaka ni movimientos. Lo guía a su primer paso lógico:
 * crear una Vaka. NO es un onboarding (eso es otro componente); es un
 * placeholder amigable para la pantalla vacía.
 */
@Composable
private fun HeroBienvenida(nombreUsuario: String?, onCrearVaka: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Círculo grande con vaca
        Box(
            Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Violeta, Rosa))),
            contentAlignment = Alignment.Center
        ) {
            Text("🐄", fontSize = 80.sp)
        }

        Spacer(Modifier.height(28.dp))

        Text(
            if (nombreUsuario != null) "¡Hola, $nombreUsuario!"
            else "¡Bienvenido a Vaka!",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(10.dp))

        Text(
            "Crea tu primera Vaka para empezar a ahorrar.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        // Botón gigante de acción primaria
        BotonPrincipal(
            "＋ Crear mi primera Vaka",
            Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            onCrearVaka()
        }
    }
}


/**
 * Plantillas predefinidas para crear Vakas más rápido.
 * Al tocar una, se autocompletan el nombre y el emoji; el usuario solo
 * tiene que completar la meta y la fecha si quiere.
 */
data class PlantillaVaka(val nombre: String, val emoji: String)

val PLANTILLAS_VAKA = listOf(
    PlantillaVaka("Viaje", "✈️"),
    PlantillaVaka("Regalo", "🎁"),
    PlantillaVaka("Emergencia", "🚨"),
    PlantillaVaka("Casa", "🏠"),
    PlantillaVaka("Carro", "🚗"),
    PlantillaVaka("Tecnología", "💻"),
    PlantillaVaka("Boda", "💍"),
    PlantillaVaka("Estudios", "📚")
)

/**
 * Tarjeta de "actividad reciente" que muestra los últimos movimientos de
 * TODAS las Vakas en orden cronológico, con un toque visual para ver
 * rápido qué es depósito (verde) y qué es retiro (rojo). Cada fila lleva
 * a la Vaka correspondiente al tocarla.
 *
 * Es colapsable: por defecto muestra 5, se puede expandir para ver hasta 8.
 */
@Composable
private fun HistorialUnificadoCard(
    movs: List<Pair<VakaItem, Mov>>,
    onAbrirVaka: (VakaItem) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    val mostrar = if (expandido) movs else movs.take(5)

    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📜", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
                Text("ACTIVIDAD RECIENTE", fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(12.dp))

            mostrar.forEachIndexed { idx, (vaka, m) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onAbrirVaka(vaka) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(vaka.emoji, fontSize = 18.sp,
                        modifier = Modifier.padding(end = 10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            vaka.nombre,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            "${fechaCorta(m.fecha)} · ${if (m.nota.isNotBlank()) m.nota.take(28) else if (m.tipo == "deposito") "Depósito" else "Retiro"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    Text(
                        (if (m.tipo == "deposito") "+" else "−") +
                            formatea(m.monto, vaka.monedaCode, vaka.monedaSymbol),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (m.tipo == "deposito") Verde else Rojo
                    )
                }
                if (idx < mostrar.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }

            if (movs.size > 5) {
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = { expandido = !expandido },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (expandido) "▲ Mostrar menos"
                        else "▼ Mostrar ${movs.size - 5} más",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Versión corta de fecha para el historial unificado: "Hoy", "Ayer",
 * "Lun 9 jun", etc., para que los movimientos se identifiquen rápido.
 */
private fun fechaCorta(iso: String): String {
    return try {
        val d = java.time.LocalDate.parse(iso)
        val hoy = java.time.LocalDate.now()
        val dias = java.time.temporal.ChronoUnit.DAYS.between(d, hoy)
        when (dias) {
            0L -> "Hoy"
            1L -> "Ayer"
            in 2..6 -> {
                val nombresDias = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
                nombresDias[d.dayOfWeek.value - 1]
            }
            else -> {
                val nombresMeses = listOf("ene", "feb", "mar", "abr", "may", "jun",
                    "jul", "ago", "sep", "oct", "nov", "dic")
                "${d.dayOfMonth} ${nombresMeses[d.monthValue - 1]}"
            }
        }
    } catch (e: Exception) { iso }
}
