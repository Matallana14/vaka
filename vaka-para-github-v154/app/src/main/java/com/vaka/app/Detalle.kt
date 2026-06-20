package com.vaka.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@Composable
fun DetailScreen(
    vaka: VakaItem,
    tabla: TablaTasas?,
    miUid: String?,
    miNombre: String,
    perfilesPorUid: Map<String, PerfilUsuario>,
    otrasVakas: List<VakaItem> = emptyList(),
    onBack: () -> Unit,
    onUpdate: (VakaItem) -> Unit,
    onCompartir: () -> Unit,      // convertir local en compartida
    onCompromiso: (Double) -> Unit,
    onAplicarEquitativo: (VakaItem, Double) -> Unit,
    onEliminar: () -> Unit,
    onMover: (origenVaka: VakaItem, destinoVaka: VakaItem, monto: Double, nota: String) -> Unit = { _, _, _, _ -> },
    puedeCompartir: Boolean,       // hay sesión iniciada
) {
    val clave = claveDe(vaka)
    var tab by remember(clave) { mutableStateOf(0) }
    val tabs = if (vaka.esCompartida)
        listOf("Movimientos", "Equipo", "Logros", "Ajustes")
    else
        listOf("Movimientos", "Logros", "Ajustes")

    // ===== Animación al cambiar el total (depósito/retiro) =====
    val totalActual = vaka.total
    var totalAnterior by remember(clave) { mutableStateOf(totalActual) }
    var chipDelta by remember { mutableStateOf<Double?>(null) }
    var chipMostrar by remember { mutableStateOf(false) }
    val totalAnimado by animateFloatAsState(
        targetValue = totalActual.toFloat(),
        animationSpec = tween(durationMillis = 700),
        label = "totalCount"
    )
    LaunchedEffect(totalActual) {
        if (totalActual != totalAnterior) {
            val delta = totalActual - totalAnterior
            if (kotlin.math.abs(delta) > 0.01) {
                chipDelta = delta
                chipMostrar = true
                delay(2200)
                chipMostrar = false
            }
            totalAnterior = totalActual
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            Text("← Inicio", fontWeight = FontWeight.ExtraBold)
        }

        // Anillo de progreso animado: se llena suavemente al entrar y al depositar
        val progAnim by animateFloatAsState(
            targetValue = vaka.progreso ?: 0f,
            animationSpec = tween(900),
            label = "progreso"
        )

        // Hero
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(Violeta, Color(0xFF8B46E0), Rosa)))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (vaka.progreso != null) {
                    AnilloOndaLiquida(
                        progreso = vaka.progreso,
                        tamano = 92.dp,
                        colorPrincipal = Color.White,
                        colorOnda = Dorado,
                        grosorAnillo = 6.dp,
                        mostrarPorcentaje = true
                    )
                } else {
                    // Sin meta: mostramos el emoji en un círculo
                    Box(
                        Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = .22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(vaka.emoji, fontSize = 38.sp)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        (if (vaka.esCompartida) "👥 " else "") + "${vaka.emoji} ${vaka.nombre}",
                        color = Color.White.copy(alpha = .9f),
                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold
                    )
                    Box {
                        Text(
                            formatea(totalAnimado.toDouble(), vaka.monedaCode, vaka.monedaSymbol),
                            color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold
                        )
                        // Chip flotante: +/- monto cuando hay movimiento nuevo
                        androidx.compose.animation.AnimatedVisibility(
                            visible = chipMostrar && chipDelta != null,
                            enter = androidx.compose.animation.slideInVertically(
                                animationSpec = tween(400)
                            ) { it } + androidx.compose.animation.fadeIn(tween(400)),
                            exit = androidx.compose.animation.slideOutVertically(
                                animationSpec = tween(500)
                            ) { -it } + androidx.compose.animation.fadeOut(tween(500)),
                            modifier = Modifier.align(Alignment.TopEnd)
                                .offset(y = (-28).dp, x = 8.dp)
                        ) {
                            val d = chipDelta ?: 0.0
                            val esPositivo = d > 0
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(
                                        if (esPositivo) Verde else Rojo
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    (if (esPositivo) "↓ +" else "↑ ") +
                                        formatea(d, vaka.monedaCode, vaka.monedaSymbol),
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    Text(
                        if (vaka.meta > 0)
                            "Meta: ${formatea(vaka.meta, vaka.monedaCode, vaka.monedaSymbol)}" +
                                (vaka.metaFecha?.let { " · ${fechaBonita(it)}" } ?: "")
                        else "Sin meta definida",
                        color = Color.White.copy(alpha = .85f), fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
            tabs.forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i },
                    text = { Text(t, fontSize = 13.sp) })
            }
        }

        Spacer(Modifier.height(14.dp))

        val seccion = tabs[tab]
        when (seccion) {
            "Movimientos" -> TabMovimientos(vaka, tabla, miUid, miNombre, onUpdate)
            "Equipo" -> TabEquipo(vaka, miUid, perfilesPorUid, onCompromiso, onAplicarEquitativo)
            "Logros" -> TabLogros(vaka)
            "Ajustes" -> TabAjustes(vaka, onUpdate, onCompartir, onEliminar, puedeCompartir,
                otrasVakas = otrasVakas, onMover = onMover)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ------------------------------------------------------------
// Movimientos
// ------------------------------------------------------------
@Composable
fun TabMovimientos(vaka: VakaItem, tabla: TablaTasas?, miUid: String?, miNombre: String, onUpdate: (VakaItem) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val clave = claveDe(vaka)
    var tipo by remember(clave) { mutableStateOf("deposito") }
    var monto by remember(clave) { mutableStateOf("") }  // crudo, ej. "50000.5"
    var nota by remember(clave) { mutableStateOf("") }
    var fecha by remember(clave) { mutableStateOf(hoy()) }
    var confirmarRetiro by remember(clave) { mutableStateOf<Mov?>(null) }
    var resaltado by remember(clave) { mutableStateOf<Long?>(null) }

    // Filtros del historial
    var busqueda by remember(clave) { mutableStateOf("") }
    var filtroTipo by remember(clave) { mutableStateOf("todos") } // todos | deposito | retiro

    fun guardar(m: Mov) {
        onUpdate(vaka.copy(movs = listOf(m) + vaka.movs))
        resaltado = m.id
        monto = ""; nota = ""; fecha = hoy()
        // Notificación con frase graciosa
        Notificaciones.notificarMovimientoPropio(
            ctx = ctx,
            nombreVaka = vaka.nombre,
            tipo = m.tipo,
            monto = m.monto,
            monedaCode = vaka.monedaCode,
            monedaSymbol = vaka.monedaSymbol,
            idMov = m.id
        )
    }
    LaunchedEffect(resaltado) {
        if (resaltado != null) {
            delay(2200)
            resaltado = null
        }
    }

    confirmarRetiro?.let { m ->
        ConfirmarRetiro(
            mov = m, vaka = vaka,
            onConfirmar = { guardar(m); confirmarRetiro = null },
            onCancelar = { confirmarRetiro = null }
        )
    }

    // ===== Tu parte del equipo (división equitativa) =====
    // Visible solo en Vakas compartidas con meta y más de 1 miembro.
    if (vaka.esCompartida && vaka.meta > 0 && vaka.miembros.size >= 2 && miUid != null) {
        val numMiembros = vaka.miembros.size
        val miMiembro = vaka.miembros.find { it.uid == miUid }
        val parteEquitativa = vaka.meta / numMiembros
        // Si ya pusiste tu compromiso, usa ese. Si no, usa la parte equitativa.
        val miParte = miMiembro?.compromiso?.takeIf { it > 0 } ?: parteEquitativa
        val miAporte = vaka.aportadoPor(miUid)
        val miAvance = (miAporte / miParte).toFloat().coerceIn(0f, 1f)
        val miFalta = (miParte - miAporte).coerceAtLeast(0.0)
        val esEquitativo = miMiembro?.compromiso?.let { it == 0.0 } ?: true

        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnilloOndaLiquida(
                    progreso = miAvance,
                    tamano = 72.dp,
                    colorPrincipal = MaterialTheme.colorScheme.primary,
                    colorOnda = Dorado,
                    grosorAnillo = 4.dp,
                    mostrarPorcentaje = true
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (esEquitativo) "TU PARTE EQUITATIVA ⚖️" else "TU COMPROMISO 🎯",
                        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        formatea(miParte, vaka.monedaCode, vaka.monedaSymbol),
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (miAporte >= miParte) {
                        Text(
                            "✅ ¡Ya cumpliste tu parte!",
                            fontSize = 12.sp, color = Verde, fontWeight = FontWeight.ExtraBold
                        )
                    } else {
                        Text(
                            "Te falta " + formatea(miFalta, vaka.monedaCode, vaka.monedaSymbol),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (esEquitativo) {
                        Text(
                            "Meta dividida entre $numMiembros (puedes definir tu compromiso en Equipo)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    // Plan de ahorro
    planDe(vaka)?.let { plan ->
        if (!plan.cumplida) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("TU PLAN PARA LLEGAR", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(Modifier.height(10.dp))
                    if (plan.vencida) {
                        Text("La fecha de tu meta ya pasó y faltan ${formatea(plan.restante, vaka.monedaCode, vaka.monedaSymbol)}. ¡No pasa nada! Ajusta la fecha en Ajustes y sigan adelante.",
                            fontSize = 14.sp)
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PlanBox("Faltan", formatea(plan.restante, vaka.monedaCode, vaka.monedaSymbol), Modifier.weight(1f))
                            PlanBox("Quedan", "${plan.dias} días", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        PlanBox("A ahorrar por semana", formatea(plan.porSemana, vaka.monedaCode, vaka.monedaSymbol), Modifier.fillMaxWidth())
                        plan.estado?.let { e ->
                            Spacer(Modifier.height(10.dp))
                            val (txt, col) = when (e) {
                                "adelantado" -> "🚀 Van adelantados, ¡qué nivel!" to Verde
                                "aldia" -> "👌 Van al día con el plan" to MaterialTheme.colorScheme.primary
                                else -> "⏰ Van un poco atrasados, ¡ánimo!" to Rojo
                            }
                            Text(txt, color = col, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }

    // Plan automático de meta (si hay meta y fecha)
    if (vaka.meta > 0 && !vaka.metaFecha.isNullOrBlank()) {
        PlanMetaCard(vaka)
        Spacer(Modifier.height(14.dp))
    }

    // Equivalencia en otra moneda
    if (tabla != null && vaka.total > 0) {
        var codObjetivo by remember(clave) {
            mutableStateOf(if (vaka.monedaCode.uppercase() == "USD") "COP" else "USD")
        }
        val equivale = Tasas.convertir(tabla, vaka.total, vaka.monedaCode, codObjetivo)
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("EQUIVALE HOY A", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = codObjetivo,
                        onValueChange = { codObjetivo = it.uppercase().filter { c -> c.isLetter() }.take(4) },
                        modifier = Modifier.width(104.dp),
                        shape = RoundedCornerShape(14.dp), singleLine = true
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        equivale?.let { "\u2248 " + formatea(it, codObjetivo, "") }
                            ?: "Sin tasa para $codObjetivo",
                        fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text("Tasa de referencia diaria · ${tabla.fecha}",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    // === Nuevo movimiento ===
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("NUEVO MOVIMIENTO", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { tipo = "deposito" }, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tipo == "deposito") Verde else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (tipo == "deposito") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { Text("＋ Depósito", fontWeight = FontWeight.ExtraBold) }
                Button(
                    onClick = { tipo = "retiro" }, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tipo == "retiro") Rojo else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (tipo == "retiro") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { Text("－ Retiro", fontWeight = FontWeight.ExtraBold) }
            }
            Spacer(Modifier.height(12.dp))
            // Monto con teclado tipo calculadora y formato en vivo
            CampoMonto(
                crudo = monto,
                onCrudoChange = { monto = it },
                monedaCode = vaka.monedaCode,
                monedaSymbol = vaka.monedaSymbol,
                placeholder = "Toca para escribir el monto"
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = nota, onValueChange = { nota = it },
                placeholder = { Text("Nota (opcional): ej. quincena, propina…") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            SelectorFecha(fechaIso = fecha, onSeleccionar = { fecha = it })
            Spacer(Modifier.height(12.dp))
            BotonPrincipal(
                if (tipo == "retiro") "Retirar" else "Agregar",
                Modifier.fillMaxWidth().height(50.dp),
                color = if (tipo == "retiro") Rojo else Violeta
            ) {
                val v = monto.replace(",", ".").toDoubleOrNull()
                if (v != null && v > 0 && fechaValida(fecha)) {
                    val nuevo = Mov(
                        id = System.currentTimeMillis(), tipo = tipo, monto = v,
                        nota = nota.trim(), fecha = fecha,
                        autorUid = miUid ?: "",
                        autorNombre = if (vaka.esCompartida) miNombre else ""
                    )
                    if (tipo == "retiro") confirmarRetiro = nuevo else guardar(nuevo)
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    // === Historial con filtros, búsqueda y agrupación ===
    var historialExpandido by remember(clave) { mutableStateOf(false) }
    var confirmarBorrarMov by remember { mutableStateOf<Mov?>(null) }
    val MOVS_PREVIEW = 5  // Cuántos movimientos mostrar en la vista colapsada

    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("HISTORIAL", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Text("${vaka.movs.size} en total", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Solo mostramos el buscador y filtros cuando se expandió el historial
            if (historialExpandido && vaka.movs.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = busqueda, onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar por nota…") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChipFiltro("Todos", filtroTipo == "todos", Modifier.weight(1f)) { filtroTipo = "todos" }
                    ChipFiltro("Depósitos", filtroTipo == "deposito", Modifier.weight(1f)) { filtroTipo = "deposito" }
                    ChipFiltro("Retiros", filtroTipo == "retiro", Modifier.weight(1f)) { filtroTipo = "retiro" }
                }
                Spacer(Modifier.height(10.dp))
            }

            // En modo colapsado: tomar solo los últimos MOVS_PREVIEW sin filtrar
            // En modo expandido: aplicar filtros y búsqueda
            val visibles = if (historialExpandido) {
                vaka.movs.filter { m ->
                    (filtroTipo == "todos" || m.tipo == filtroTipo) &&
                        (busqueda.isBlank() || m.nota.contains(busqueda, ignoreCase = true))
                }
            } else {
                vaka.movs.take(MOVS_PREVIEW)
            }

            if (vaka.movs.isEmpty()) {
                Text("Aún no hay movimientos en esta Vaka.\nAgrega tu primer depósito arriba ✨",
                    Modifier.fillMaxWidth().padding(vertical = 22.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (visibles.isEmpty()) {
                Text("Ningún movimiento coincide con tu búsqueda.",
                    Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                // Agrupamos por período relativo (Hoy / Esta semana / Octubre 2025…)
                val grupos = visibles.groupBy { grupoDeFecha(it.fecha) }
                grupos.forEach { (grupo, lista) ->
                    Spacer(Modifier.height(8.dp))
                    Text(grupo.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    lista.forEachIndexed { i, m ->
                        val resaltadoActual = resaltado == m.id
                        val pulso by animateFloatAsState(
                            if (resaltadoActual) 1f else 0f,
                            tween(durationMillis = 1400),
                            label = "pulso"
                        )
                        val escala by animateFloatAsState(
                            if (resaltadoActual) 1.02f else 1f,
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "escalaMov"
                        )
                        val colorMov = if (m.tipo == "deposito") Verde else Rojo
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = escala
                                    scaleY = escala
                                }
                                .clip(RoundedCornerShape(14.dp))
                                .background(colorMov.copy(alpha = pulso * .22f))
                                .border(
                                    width = (pulso * 2f).dp,
                                    color = colorMov.copy(alpha = pulso * .55f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(m.nota.ifBlank { if (m.tipo == "deposito") "Depósito" else "Retiro" },
                                    fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                                Text(
                                    fechaBonita(m.fecha) +
                                        if (m.autorNombre.isNotBlank()) " · por ${m.autorNombre}" else "",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                (if (m.tipo == "deposito") "+" else "−") +
                                    formatea(m.monto, vaka.monedaCode, vaka.monedaSymbol),
                                color = colorMov,
                                fontWeight = FontWeight.ExtraBold
                            )
                            // Botón ✕ solo en modo expandido (corrige errores de registro)
                            if (historialExpandido) {
                                TextButton(onClick = {
                                    confirmarBorrarMov = m
                                }) { Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                        if (i < lista.lastIndex) HorizontalDivider()
                    }
                }
            }

            // Botón para expandir/colapsar el historial
            if (vaka.movs.size > MOVS_PREVIEW || historialExpandido) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            historialExpandido = !historialExpandido
                            if (!historialExpandido) {
                                // Al colapsar limpiamos los filtros para que la siguiente
                                // vez que se expanda esté en estado limpio
                                busqueda = ""
                                filtroTipo = "todos"
                            }
                        }
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f))
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (historialExpandido) "Ver menos ▴" else "Ver todo el historial ▾",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // Diálogo de confirmación de borrado (NO accidental)
    confirmarBorrarMov?.let { m ->
        AlertDialog(
            onDismissRequest = { confirmarBorrarMov = null },
            title = { Text("¿Borrar este movimiento?") },
            text = {
                Text("Se restará del total de la Vaka. Esto solo debe hacerse si registraste mal el movimiento. No se puede deshacer.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdate(vaka.copy(movs = vaka.movs.filter { it.id != m.id }))
                    confirmarBorrarMov = null
                }) { Text("Borrar", color = Rojo, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrarMov = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ChipFiltro(
    etiqueta: String,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val fondo = if (seleccionado) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant
    val texto = if (seleccionado) Color.White else MaterialTheme.colorScheme.onSurface
    Box(
        modifier
            .clip(RoundedCornerShape(99.dp))
            .background(fondo)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(etiqueta, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = texto)
    }
}

@Composable
private fun ConfirmarRetiro(
    mov: Mov,
    vaka: VakaItem,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("¿Confirmar retiro?", fontWeight = FontWeight.ExtraBold) },
        text = {
            Text(
                "Vas a retirar " +
                    formatea(mov.monto, vaka.monedaCode, vaka.monedaSymbol) +
                    " de \"${vaka.nombre}\". " +
                    "Tu nuevo saldo será " +
                    formatea(vaka.total - mov.monto, vaka.monedaCode, vaka.monedaSymbol) + "."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text("Sí, retirar", color = Rojo, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

@Composable
fun PlanBox(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(titulo, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

// ------------------------------------------------------------
// Equipo: miembros, compromisos individuales y aportes reales
// ------------------------------------------------------------
@Composable
fun TabEquipo(
    vaka: VakaItem,
    miUid: String?,
    perfilesPorUid: Map<String, PerfilUsuario>,
    onCompromiso: (Double) -> Unit,
    onAplicarEquitativo: (VakaItem, Double) -> Unit,
) {
    var compromisoTxt by remember(claveDe(vaka), miUid) {
        mutableStateOf(
            vaka.miembros.find { it.uid == miUid }?.compromiso
                ?.takeIf { it > 0 }?.toString() ?: ""
        )
    }

    // === Invitar a alguien más (QR visible y prominente) ===
    var invitarExpandido by remember(claveDe(vaka)) { mutableStateOf(false) }
    if (!vaka.codigo.isNullOrBlank()) {
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { invitarExpandido = !invitarExpandido }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🤝", fontSize = 22.sp, modifier = Modifier.padding(end = 10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("INVITAR A ALGUIEN MÁS",
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                        Text(if (invitarExpandido) "Toca para esconder" else "Toca para ver el QR y el código",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(if (invitarExpandido) "▴" else "▾",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold)
                }
                AnimatedVisibility(invitarExpandido) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        QrVaka(vaka.codigo!!)
                        Spacer(Modifier.height(8.dp))
                        Text("La persona que escanee este QR se unirá automáticamente como miembro.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    // ===== 🏆 RANKING DE APORTADORES =====
    // Solo mostramos el ranking si hay al menos 2 miembros con aportes
    val aportesPorMiembro = remember(vaka) {
        vaka.miembros.map { miembro ->
            val aporte = vaka.movs
                .filter { it.autorUid == miembro.uid && it.tipo == "deposito" }
                .sumOf { it.monto }
            miembro to aporte
        }.sortedByDescending { it.second }
    }
    val totalAportado = remember(aportesPorMiembro) { aportesPorMiembro.sumOf { it.second } }
    val miembrosQueAportaron = aportesPorMiembro.count { it.second > 0 }

    if (miembrosQueAportaron >= 2) {
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏆", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("RANKING DEL EQUIPO",
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                        Text("Quién va liderando los aportes",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))

                aportesPorMiembro.forEachIndexed { idx, (miembro, aporte) ->
                    if (aporte <= 0) return@forEachIndexed
                    val medalla = when (idx) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> "  "
                    }
                    val esYo = miembro.uid == miUid
                    val porcentaje = if (totalAportado > 0) {
                        ((aporte / totalAportado) * 100).toInt()
                    } else 0
                    val perfil = perfilesPorUid[miembro.uid]
                    val nombreMostrar = if (esYo) "Tú" else miembro.nombre

                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(medalla, fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp))
                        AvatarPerfil(
                            fotoBase64 = perfil?.fotoBase64 ?: "",
                            nombre = miembro.nombre,
                            colorAvatar = perfil?.colorAvatar ?: "violeta",
                            tamano = 36.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(nombreMostrar,
                                fontSize = 14.sp,
                                fontWeight = if (esYo) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (esYo) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface)
                            Text("$porcentaje% del total aportado",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(formatea(aporte, vaka.monedaCode, vaka.monedaSymbol),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (idx == 0) Dorado
                            else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
    val numMiembros = vaka.miembros.size
    // Redondeamos al entero más cercano para evitar decimales feos.
    // El último miembro absorbe el residuo para que la suma cuadre con la meta.
    val parteEquitativa = if (vaka.meta > 0 && numMiembros > 0) {
        kotlin.math.round(vaka.meta / numMiembros)
    } else 0.0
    val totalComprometido = vaka.miembros.sumOf { it.compromiso }
    val sumaCoincideConMeta =
        vaka.meta > 0 && kotlin.math.abs(totalComprometido - vaka.meta) < numMiembros.toDouble()

    // Tarjeta de propuesta equitativa (solo si hay meta y más de 1 miembro)
    if (vaka.meta > 0 && numMiembros >= 2) {
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚖️ DIVISIÓN EQUITATIVA",
                        fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f))
                    if (sumaCoincideConMeta) {
                        Text("Ya aplicada ✓", fontSize = 11.sp,
                            color = Verde, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Si dividen la meta de ${formatea(vaka.meta, vaka.monedaCode, vaka.monedaSymbol)} entre $numMiembros, a cada uno le toca:",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = .12f))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        formatea(parteEquitativa, vaka.monedaCode, vaka.monedaSymbol),
                        fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "(${(100.0 / numMiembros).let { "%.1f".format(it) }}% por persona)",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!sumaCoincideConMeta) {
                    Spacer(Modifier.height(12.dp))
                    // Solo permitimos aplicar a TODOS si soy miembro
                    if (miUid != null) {
                        BotonPrincipal(
                            "Aplicar a todo el equipo",
                            Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            // Llamamos a onCompromiso para cada miembro (incluido yo)
                            // El callback solo cambia mi compromiso, así que para los demás
                            // necesitamos actualizar todos los compromisos como bloque.
                            // Lo hacemos vía un callback especial: enviamos un valor negativo
                            // codificado para distinguir... no, mejor agregamos un callback aparte.
                            onAplicarEquitativo(vaka, parteEquitativa)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Pondrá el compromiso de cada miembro en partes iguales.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("EL EQUIPO DE ESTA VAKA", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))

            vaka.miembros.forEach { mi ->
                val aportado = vaka.aportadoPor(mi.uid)
                val esYo = mi.uid == miUid
                // Si no tiene compromiso, usamos su parte equitativa como referencia
                val referencia = if (mi.compromiso > 0) mi.compromiso else parteEquitativa
                val tieneCompromiso = mi.compromiso > 0
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (esYo) Dorado.copy(alpha = .14f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val perfilMi = perfilesPorUid[mi.uid]
                    val fotoMi = perfilMi?.fotoBase64 ?: ""
                    val colorMi = perfilMi?.colorAvatar ?: "violeta"
                    // Anillo de onda líquida del miembro + foto/inicial dentro
                    if (referencia > 0) {
                        val progresoMiembro = (aportado / referencia).toFloat().coerceIn(0f, 1f)
                        Box(contentAlignment = Alignment.Center) {
                            AnilloOndaLiquida(
                                progreso = progresoMiembro,
                                tamano = 64.dp,
                                colorPrincipal = if (esYo) Dorado else MaterialTheme.colorScheme.primary,
                                colorOnda = if (esYo) Dorado else MaterialTheme.colorScheme.primary,
                                grosorAnillo = 4.dp,
                                mostrarPorcentaje = false  // ocultamos el % para que se vea la foto
                            )
                            // Foto del miembro como centro del anillo
                            AvatarPerfil(
                                fotoBase64 = fotoMi,
                                nombre = mi.nombre,
                                colorAvatar = colorMi,
                                tamano = 44.dp
                            )
                        }
                    } else {
                        AvatarPerfil(
                            fotoBase64 = fotoMi,
                            nombre = mi.nombre,
                            colorAvatar = colorMi,
                            tamano = 64.dp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            mi.nombre + if (esYo) " (tú)" else "",
                            fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
                        )
                        Text(
                            "Aportó ${formatea(aportado, vaka.monedaCode, vaka.monedaSymbol)}",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Verde
                        )
                        if (referencia > 0) {
                            val falta = (referencia - aportado).coerceAtLeast(0.0)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (tieneCompromiso)
                                    "Su parte: ${formatea(referencia, vaka.monedaCode, vaka.monedaSymbol)}"
                                else
                                    "Parte equitativa: ${formatea(referencia, vaka.monedaCode, vaka.monedaSymbol)}",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (aportado >= referencia) "✅ ¡Cumplió su parte!"
                                else "Le falta ${formatea(falta, vaka.monedaCode, vaka.monedaSymbol)}",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = if (aportado >= referencia) Verde
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Spacer(Modifier.height(2.dp))
                            Text("Aún sin meta definida",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    // Mi compromiso
    if (miUid != null) {
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("MI COMPROMISO PERSONAL", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (parteEquitativa > 0)
                        "¿Cuánto quieres aportar tú? (Por defecto sería ${formatea(parteEquitativa, vaka.monedaCode, vaka.monedaSymbol)} si dividen equitativo)"
                    else
                        "¿Cuánto quieres aportar tú a esta Vaka?",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                CampoMonto(
                    crudo = compromisoTxt,
                    onCrudoChange = { compromisoTxt = it },
                    monedaCode = vaka.monedaCode,
                    monedaSymbol = vaka.monedaSymbol,
                    placeholder = "Toca para escribir tu compromiso"
                )
                Spacer(Modifier.height(12.dp))
                BotonPrincipal("Guardar mi compromiso", Modifier.fillMaxWidth().height(48.dp)) {
                    val v = compromisoTxt.replace(",", ".").toDoubleOrNull() ?: 0.0
                    onCompromiso(if (v > 0) v else 0.0)
                }
                if (parteEquitativa > 0) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            compromisoTxt = parteEquitativa.toLong().toString()
                            onCompromiso(parteEquitativa)
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Usar mi parte equitativa (${formatea(parteEquitativa, vaka.monedaCode, vaka.monedaSymbol)})",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Tip: la suma de los compromisos del equipo idealmente debería alcanzar la meta de la Vaka 😉",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ------------------------------------------------------------
// Logros
// ------------------------------------------------------------
@Composable
fun TabLogros(vaka: VakaItem) {
    val todosLogros = remember(vaka) { logrosDe(vaka) }
    val desbloqueados = todosLogros.filter { it.ok }
    val bloqueados = todosLogros.filter { !it.ok }

    Column {
        // Encabezado de progreso
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(
                Modifier.fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Violeta, androidx.compose.ui.graphics.Color(0xFF8B46E0), Rosa)
                        )
                    )
                    .padding(20.dp)
            ) {
                Text("🏆 LOGROS",
                    color = Color.White.copy(alpha = .9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Text("${desbloqueados.size} de ${todosLogros.size}",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold)
                Text("desbloqueados",
                    color = Color.White.copy(alpha = .85f),
                    fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Desbloqueados primero
        if (desbloqueados.isNotEmpty()) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("DESBLOQUEADOS", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        color = Verde, letterSpacing = 1.sp)
                    Spacer(Modifier.height(10.dp))
                    desbloqueados.forEach { l -> FilaLogro(l) }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // Pendientes
        if (bloqueados.isNotEmpty()) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("POR DESBLOQUEAR", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(Modifier.height(10.dp))
                    bloqueados.forEach { l -> FilaLogro(l) }
                }
            }
        }
    }
}

@Composable
private fun FilaLogro(l: Logro) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (l.ok) Dorado.copy(alpha = .18f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(l.emoji, fontSize = 26.sp, modifier = Modifier.padding(end = 12.dp))
        Column(Modifier.weight(1f)) {
            Text(l.nombre,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = if (l.ok) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(l.descripcion,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(if (l.ok) "✓" else "🔒", fontSize = 14.sp,
            color = if (l.ok) Verde else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ------------------------------------------------------------
// Ajustes
// ------------------------------------------------------------
@Composable
fun TabAjustes(
    vaka: VakaItem,
    onUpdate: (VakaItem) -> Unit,
    onCompartir: () -> Unit,
    onEliminar: () -> Unit,
    puedeCompartir: Boolean,
    otrasVakas: List<VakaItem> = emptyList(),
    onMover: (origenVaka: VakaItem, destinoVaka: VakaItem, monto: Double, nota: String) -> Unit = { _, _, _, _ -> },
) {
    val clave = claveDe(vaka)
    var nombre by remember(clave) { mutableStateOf(vaka.nombre) }
    var metaTxt by remember(clave) { mutableStateOf(if (vaka.meta > 0) vaka.meta.toString() else "") }
    var metaFechaTxt by remember(clave) { mutableStateOf(vaka.metaFecha ?: "") }
    var codigoCustom by remember(clave) { mutableStateOf("") }
    var simboloCustom by remember(clave) { mutableStateOf("") }
    var expandida by remember { mutableStateOf(false) }
    // Estado: cuando el usuario elige una moneda nueva, abrimos un diálogo para
    // preguntar si quiere convertir los montos o solo cambiar el símbolo.
    var monedaPendiente by remember { mutableStateOf<Pair<String, String>?>(null) }
    var convirtiendo by remember { mutableStateOf(false) }
    var errorConversion by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf(false) }

    // Compartir / código del equipo (NO se muestra para la Vaka fija "Mi ahorro")
    if (!vaka.esFija) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(
                if (vaka.esCompartida) "CÓDIGO DE ESTA VAKA" else "AHORRAR EN EQUIPO",
                fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))
            if (vaka.esCompartida) {
                Text("Compártelo para que otros se unan al equipo. Pueden escanear el QR o usar el código de abajo:",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                QrVaka(vaka.codigo ?: "")
            } else {
                Text(
                    if (puedeCompartir)
                        "Convierte esta Vaka en compartida: obtendrás un código para que más personas se unan, definan su compromiso y aporten contigo."
                    else
                        "Para compartir Vakas necesitas iniciar sesión con tu cuenta.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                BotonPrincipal(
                    "👥 Compartir esta Vaka",
                    Modifier.fillMaxWidth().height(48.dp),
                    habilitado = puedeCompartir
                ) { onCompartir() }
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    }  // fin del if (!vaka.esFija) del bloque Compartir

    // Nombre (NO editable para la Vaka fija "Mi ahorro")
    if (!vaka.esFija) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("NOMBRE", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = nombre, onValueChange = { nombre = it },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { if (nombre.isNotBlank()) onUpdate(vaka.copy(nombre = nombre.trim())) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
            ) { Text("Cambiar nombre", fontWeight = FontWeight.Bold) }
        }
    }

    Spacer(Modifier.height(14.dp))
    }  // fin del if (!vaka.esFija) del bloque Nombre

    // Moneda
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("MONEDA DE ESTA VAKA", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            // Indicador claro de la moneda actual
            val monedaActual = remember(vaka.monedaCode) {
                MONEDAS.find { it.code == vaka.monedaCode }
            }
            Text(
                "Ahora: ${vaka.monedaSymbol} ${vaka.monedaCode}" +
                    (monedaActual?.let { " · ${it.nombre}" } ?: ""),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Box {
                OutlinedButton(onClick = { expandida = true },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("Tocar para cambiar de moneda")
                }
                DropdownMenu(expanded = expandida, onDismissRequest = { expandida = false }) {
                    MONEDAS.forEach { m ->
                        DropdownMenuItem(
                            text = { Text("${m.code} · ${m.nombre}") },
                            onClick = {
                                monedaPendiente = m.code to m.symbol
                                expandida = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("¿No está tu moneda? Escríbela tú mismo:",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = codigoCustom, onValueChange = { codigoCustom = it.uppercase() },
                    placeholder = { Text("Código (VES)") },
                    modifier = Modifier.weight(2f), shape = RoundedCornerShape(14.dp), singleLine = true)
                OutlinedTextField(value = simboloCustom, onValueChange = { simboloCustom = it },
                    placeholder = { Text("Símbolo") },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), singleLine = true)
            }
            Spacer(Modifier.height(10.dp))
            BotonPrincipal("Usar esta moneda", Modifier.fillMaxWidth().height(48.dp)) {
                val c = codigoCustom.trim().uppercase()
                if (c.isNotBlank()) {
                    monedaPendiente = c to simboloCustom.trim()
                    codigoCustom = ""; simboloCustom = ""
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Al cambiar la moneda podrás elegir si convertir los montos o solo cambiar el símbolo.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    // Diálogo de cambio de moneda: convertir o no convertir
    monedaPendiente?.let { (nuevoCode, nuevoSymbol) ->
        val ctx = LocalContext.current
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = {
                if (!convirtiendo) {
                    monedaPendiente = null
                    errorConversion = ""
                }
            },
            title = { Text("Cambiar a $nuevoCode") },
            text = {
                Column {
                    Text("Estás cambiando de ${vaka.monedaCode} a $nuevoCode. ¿Qué prefieres?")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• Convertir: los montos se recalculan usando la tasa de cambio del día. " +
                        "Ejemplo: $100.000 ${vaka.monedaCode} pasaría a su equivalente en $nuevoCode.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "• Solo cambiar símbolo: los montos se mantienen iguales, solo se muestran con la nueva moneda.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (errorConversion.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorConversion, color = Rojo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (convirtiendo) {
                        Spacer(Modifier.height(8.dp))
                        Text("Obteniendo tasa de cambio…", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !convirtiendo,
                    onClick = {
                        convirtiendo = true
                        errorConversion = ""
                        scope.launch {
                            val tabla = Tasas.obtener(ctx)
                            if (tabla == null) {
                                errorConversion = "No pudimos obtener la tasa de cambio. " +
                                    "Revisa tu conexión o usa 'Solo cambiar símbolo'."
                                convirtiendo = false
                                return@launch
                            }
                            val factor = Tasas.convertir(tabla, 1.0, vaka.monedaCode, nuevoCode)
                            if (factor == null) {
                                errorConversion = "No tenemos tasa para convertir " +
                                    "${vaka.monedaCode} → $nuevoCode."
                                convirtiendo = false
                                return@launch
                            }
                            // Convertir todos los movimientos + la meta
                            val nuevosMovs = vaka.movs.map { m -> m.copy(monto = m.monto * factor) }
                            val nuevaMeta = vaka.meta * factor
                            onUpdate(
                                vaka.copy(
                                    monedaCode = nuevoCode,
                                    monedaSymbol = nuevoSymbol,
                                    meta = nuevaMeta,
                                    movs = nuevosMovs
                                )
                            )
                            convirtiendo = false
                            monedaPendiente = null
                        }
                    }
                ) {
                    Text("Convertir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        enabled = !convirtiendo,
                        onClick = {
                            onUpdate(vaka.copy(monedaCode = nuevoCode, monedaSymbol = nuevoSymbol))
                            monedaPendiente = null
                            errorConversion = ""
                        }
                    ) { Text("Solo cambiar símbolo") }
                    TextButton(
                        enabled = !convirtiendo,
                        onClick = {
                            monedaPendiente = null
                            errorConversion = ""
                        }
                    ) { Text("Cancelar") }
                }
            }
        )
    }

    Spacer(Modifier.height(14.dp))

    // Meta
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("META DE AHORRO", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))
            CampoMonto(
                crudo = metaTxt,
                onCrudoChange = { metaTxt = it },
                monedaCode = vaka.monedaCode,
                monedaSymbol = vaka.monedaSymbol,
                placeholder = "Toca para escribir la meta"
            )
            Spacer(Modifier.height(10.dp))
            SelectorFecha(
                fechaIso = metaFechaTxt,
                onSeleccionar = { metaFechaTxt = it },
                etiqueta = "¿Para cuándo? (opcional)"
            )
            Spacer(Modifier.height(12.dp))
            BotonPrincipal("Guardar meta", Modifier.fillMaxWidth().height(48.dp)) {
                val m = metaTxt.replace(",", ".").toDoubleOrNull() ?: 0.0
                val f = metaFechaTxt.trim().ifBlank { null }
                val fOk = f != null && fechaValida(f) && m > 0
                onUpdate(vaka.copy(
                    meta = if (m > 0) m else 0.0,
                    metaFecha = if (fOk) f else null,
                    metaDesde = if (fOk) hoy() else null
                ))
            }
            Spacer(Modifier.height(8.dp))
            Text("Con fecha activas el plan de ahorro semanal en la pestaña Movimientos.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    Spacer(Modifier.height(14.dp))

    // === Favorita (solo en Vakas privadas, no fijas) ===
    if (!vaka.esCompartida && !vaka.esFija) {
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onUpdate(vaka.copy(favorita = !vaka.favorita)) }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (vaka.favorita) "⭐" else "☆",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        if (vaka.favorita) "VAKA FAVORITA"
                        else "MARCAR COMO FAVORITA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (vaka.favorita) Dorado
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        if (vaka.favorita) "Aparece arriba en tu lista de Vakas"
                        else "Las favoritas aparecen arriba en tu lista",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    // === Mover dinero a otra Vaka ===
    // Solo si la Vaka actual tiene dinero Y hay al menos otra Vaka en la
    // misma moneda donde mover.
    val candidatasParaMover = otrasVakas.filter {
        it.monedaCode == vaka.monedaCode && claveDe(it) != claveDe(vaka)
    }
    if (vaka.total > 0 && candidatasParaMover.isNotEmpty()) {
        MoverDineroCard(
            origen = vaka,
            candidatas = candidatasParaMover,
            onMover = { destino, monto, nota -> onMover(vaka, destino, monto, nota) }
        )
        Spacer(Modifier.height(14.dp))
    }

    // === Aporte recurrente (auto-aporte) ===
    // Solo en Vakas privadas (no compartidas) y no fijas.
    if (!vaka.esCompartida && !vaka.esFija) {
        AporteRecurrenteCard(
            vaka = vaka,
            onActualizar = { nuevoRecurrente ->
                onUpdate(vaka.copy(recurrente = nuevoRecurrente))
            }
        )
        Spacer(Modifier.height(14.dp))
    }

    // Zona de peligro
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("ZONA DE PELIGRO", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { confirmar = true },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Rojo)
            ) {
                Text(if (vaka.esCompartida) "🚪 Salir de esta Vaka" else "Eliminar esta Vaka",
                    fontWeight = FontWeight.Bold)
            }
        }
    }

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text(if (vaka.esCompartida) "¿Salir de \"${vaka.nombre}\"?" else "¿Eliminar \"${vaka.nombre}\"?") },
            text = {
                Text(
                    if (vaka.esCompartida)
                        "Los demás miembros seguirán teniendo acceso y podrás volver con el código."
                    else
                        "Se borrarán todos sus movimientos. Esto no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmar = false; onEliminar() }) {
                    Text(if (vaka.esCompartida) "Sí, salir" else "Sí, eliminar",
                        color = Rojo, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } }
        )
    }
}

/**
 * Tarjeta del "plan automático" que aparece en TabMovimientos cuando una Vaka
 * tiene meta + fecha límite. Calcula cuánto debe aportar el usuario por día,
 * semana y mes para alcanzar la meta a tiempo, considerando lo que ya lleva
 * ahorrado.
 */
@Composable
private fun PlanMetaCard(vaka: VakaItem) {
    val fechaFin = try {
        java.time.LocalDate.parse(vaka.metaFecha)
    } catch (e: Exception) { return }
    val hoy = java.time.LocalDate.now()
    val faltante = (vaka.meta - vaka.total).coerceAtLeast(0.0)

    // Si ya cumplió la meta, mostramos celebración corta
    if (faltante == 0.0) {
        ElevatedCard(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                containerColor = Verde.copy(alpha = 0.15f)
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("🏆 META CUMPLIDA",
                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = Verde, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Text("¡Felicitaciones! Llegaste a la meta. Sigue ahorrando si quieres superar.",
                    fontSize = 13.sp)
            }
        }
        return
    }

    val diasFaltan = java.time.temporal.ChronoUnit.DAYS.between(hoy, fechaFin).toInt()

    // Si ya pasó la fecha, mostrar alerta
    if (diasFaltan < 0) {
        ElevatedCard(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                containerColor = Rojo.copy(alpha = 0.10f)
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("⏰ FECHA VENCIDA",
                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = Rojo, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tu meta era para el ${fechaBonita(vaka.metaFecha ?: "")}, " +
                        "pero te faltan ${formatea(faltante, vaka.monedaCode, vaka.monedaSymbol)}.",
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(4.dp))
                Text("Ajusta la fecha en Ajustes para tener un nuevo plan.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    // Cálculos del plan
    val porDia = if (diasFaltan > 0) faltante / diasFaltan else faltante
    val porSemana = porDia * 7
    val porMes = porDia * 30

    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📊", fontSize = 18.sp, modifier = Modifier.padding(end = 6.dp))
                Text("PLAN PARA LOGRARLO",
                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(10.dp))

            // Contexto
            Text(
                "Te faltan ${formatea(faltante, vaka.monedaCode, vaka.monedaSymbol)} " +
                    "en $diasFaltan día${if (diasFaltan == 1) "" else "s"}.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))

            // Tres filas: día, semana, mes
            PlanFila("Por día", porDia, vaka.monedaCode, vaka.monedaSymbol)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            PlanFila("Por semana", porSemana, vaka.monedaCode, vaka.monedaSymbol)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            PlanFila("Por mes", porMes, vaka.monedaCode, vaka.monedaSymbol)

            Spacer(Modifier.height(8.dp))
            Text(
                "Estimado basado en aportes constantes hasta el ${fechaBonita(vaka.metaFecha ?: "")}.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlanFila(titulo: String, monto: Double, codigo: String, simbolo: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            titulo,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            formatea(monto, codigo, simbolo),
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Tarjeta para mover dinero de la Vaka actual a otra Vaka del usuario.
 * Útil cuando cambias de prioridades sin tener que borrar y recrear.
 *
 * Se muestra solo si:
 *  - La Vaka actual tiene saldo > 0
 *  - Existe al menos otra Vaka en la MISMA moneda (para no complicar con
 *    conversión que pueda generar números raros)
 */
@Composable
private fun MoverDineroCard(
    origen: VakaItem,
    candidatas: List<VakaItem>,
    onMover: (destinoVaka: VakaItem, monto: Double, nota: String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    var destinoSel by remember { mutableStateOf<VakaItem?>(null) }
    var montoTxt by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf(false) }

    val monto = montoTxt.replace(",", ".").toDoubleOrNull() ?: 0.0
    val montoValido = monto > 0 && monto <= origen.total
    val puedeMover = destinoSel != null && montoValido

    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expandido = !expandido },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔄", fontSize = 18.sp, modifier = Modifier.padding(end = 6.dp))
                Column(Modifier.weight(1f)) {
                    Text("MOVER DINERO A OTRA VAKA",
                        fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp)
                    if (!expandido) {
                        Text("Cambia de prioridades sin perder el dinero",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(if (expandido) "▲" else "▼",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold)
            }

            AnimatedVisibility(visible = expandido) {
                Column {
                    Spacer(Modifier.height(14.dp))

                    // Selector de Vaka destino
                    Text("Mover a:", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Column(Modifier.fillMaxWidth()) {
                        candidatas.forEach { v ->
                            val sel = destinoSel?.let { claveDe(it) == claveDe(v) } == true
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .clickable { destinoSel = v }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(v.emoji, fontSize = 18.sp,
                                    modifier = Modifier.padding(end = 8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(v.nombre,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sel) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface)
                                    Text(formatea(v.total, v.monedaCode, v.monedaSymbol),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (sel) Text("✓",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Monto
                    Text("Monto:", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = montoTxt,
                        onValueChange = { montoTxt = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        placeholder = { Text("Ej: 50000") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    if (montoTxt.isNotBlank() && !montoValido) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (monto > origen.total)
                                "No puedes mover más de ${formatea(origen.total, origen.monedaCode, origen.monedaSymbol)}"
                            else "Ingresa un monto mayor a 0",
                            color = Rojo,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Nota opcional
                    OutlinedTextField(
                        value = nota,
                        onValueChange = { nota = it.take(60) },
                        placeholder = { Text("Nota (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    BotonPrincipal(
                        "Mover ${if (puedeMover) formatea(monto, origen.monedaCode, origen.monedaSymbol) else "dinero"}",
                        Modifier.fillMaxWidth().height(50.dp),
                        habilitado = puedeMover
                    ) {
                        confirmar = true
                    }
                }
            }
        }
    }

    if (confirmar) {
        val dest = destinoSel ?: return
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("Confirmar movimiento") },
            text = {
                Column {
                    Text("Vas a mover:")
                    Spacer(Modifier.height(8.dp))
                    Text(formatea(monto, origen.monedaCode, origen.monedaSymbol),
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("De ${origen.emoji} ${origen.nombre}")
                    Text("A ${dest.emoji} ${dest.nombre}")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onMover(dest, monto, nota.ifBlank { "Movido a ${dest.nombre}" })
                    confirmar = false
                    expandido = false
                    montoTxt = ""
                    nota = ""
                    destinoSel = null
                }) {
                    Text("Confirmar", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = false }) { Text("Cancelar") }
            }
        )
    }
}

/**
 * Tarjeta para configurar aportes recurrentes automáticos a una Vaka.
 * El usuario elige: monto, frecuencia (diaria/semanal/mensual) y días.
 *
 * Cada vez que el usuario abre la app, Repo.aplicarAportesRecurrentesPendientes()
 * revisa qué aportes se deben haber aplicado desde la última vez y los crea
 * como movimientos automáticos con nota "Aporte automático".
 */
@Composable
private fun AporteRecurrenteCard(
    vaka: VakaItem,
    onActualizar: (AporteRecurrente?) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    val rActual = vaka.recurrente
    var activo by remember(claveDe(vaka)) { mutableStateOf(rActual != null) }
    var montoTxt by remember(claveDe(vaka)) {
        mutableStateOf(rActual?.monto?.let { if (it > 0) it.toString().trimEnd('0').trimEnd('.') else "" } ?: "")
    }
    var frecuencia by remember(claveDe(vaka)) {
        mutableStateOf(rActual?.frecuencia ?: "semanal")
    }
    var diasSel by remember(claveDe(vaka)) {
        mutableStateOf(rActual?.dias?.toSet() ?: setOf(1))  // por defecto lunes
    }

    val monto = montoTxt.replace(",", ".").toDoubleOrNull() ?: 0.0
    val puedeGuardar = activo && monto > 0 && diasSel.isNotEmpty()

    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expandido = !expandido },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚡", fontSize = 18.sp, modifier = Modifier.padding(end = 6.dp))
                Column(Modifier.weight(1f)) {
                    Text("APORTE AUTOMÁTICO",
                        fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp)
                    Text(
                        if (rActual != null && rActual.monto > 0) {
                            val frecTxt = when (rActual.frecuencia) {
                                "diaria" -> "cada día"
                                "semanal" -> "cada semana"
                                "mensual" -> "cada mes"
                                else -> ""
                            }
                            "Activo: ${formatea(rActual.monto, vaka.monedaCode, vaka.monedaSymbol)} $frecTxt"
                        } else "Aporta solo, sin pensarlo",
                        fontSize = 12.sp,
                        color = if (rActual != null) Verde else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (rActual != null) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Text(if (expandido) "▲" else "▼",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold)
            }

            AnimatedVisibility(visible = expandido) {
                Column {
                    Spacer(Modifier.height(14.dp))

                    // Switch activo/inactivo
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Activar auto-aporte",
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("La app aportará automáticamente en las fechas elegidas",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = activo, onCheckedChange = { activo = it })
                    }

                    if (activo) {
                        Spacer(Modifier.height(14.dp))

                        // Monto
                        Text("Monto a aportar:",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = montoTxt,
                            onValueChange = { montoTxt = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                            placeholder = { Text("Ej: 50000") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(Modifier.height(14.dp))

                        // Frecuencia
                        Text("Frecuencia:",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("diaria" to "Cada día", "semanal" to "Semanal", "mensual" to "Mensual").forEach { (key, txt) ->
                                val sel = frecuencia == key
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (sel) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                        .clickable {
                                            frecuencia = key
                                            diasSel = when (key) {
                                                "diaria" -> emptySet()
                                                "semanal" -> setOf(1)
                                                "mensual" -> setOf(1)
                                                else -> emptySet()
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(txt,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sel) Color.White
                                        else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Selección de días según frecuencia
                        when (frecuencia) {
                            "diaria" -> {
                                Text("Se aportará automáticamente todos los días.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            "semanal" -> {
                                Text("Días de la semana:",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                val nombresDias = listOf("L", "M", "X", "J", "V", "S", "D")
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    nombresDias.forEachIndexed { idx, n ->
                                        val dia = idx + 1
                                        val sel = dia in diasSel
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .clip(CircleShape)
                                                .background(
                                                    if (sel) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                )
                                                .clickable {
                                                    diasSel = if (sel) diasSel - dia else diasSel + dia
                                                }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(n,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (sel) Color.White
                                                else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                            "mensual" -> {
                                Text("Días del mes (elige uno o más):",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                // Grid de 1-31. Usamos Rows de 7
                                Column {
                                    for (semana in 0..4) {
                                        Row(Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            for (col in 1..7) {
                                                val dia = semana * 7 + col
                                                if (dia <= 31) {
                                                    val sel = dia in diasSel
                                                    Box(
                                                        Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(
                                                                if (sel) MaterialTheme.colorScheme.primary
                                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                            )
                                                            .clickable {
                                                                diasSel = if (sel) diasSel - dia else diasSel + dia
                                                            }
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("$dia",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (sel) Color.White
                                                            else MaterialTheme.colorScheme.onSurface)
                                                    }
                                                } else {
                                                    Box(Modifier.weight(1f))
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("Si el mes no tiene un día (ej. 31 en febrero), se omite ese mes.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    BotonPrincipal(
                        if (activo) "Guardar auto-aporte" else "Desactivar",
                        Modifier.fillMaxWidth().height(50.dp),
                        habilitado = !activo || puedeGuardar
                    ) {
                        if (activo) {
                            onActualizar(AporteRecurrente(
                                monto = monto,
                                frecuencia = frecuencia,
                                dias = diasSel.toList().sorted(),
                                ultimaFecha = rActual?.ultimaFecha ?: ""
                            ))
                        } else {
                            onActualizar(null)
                        }
                        expandido = false
                    }
                }
            }
        }
    }
}
