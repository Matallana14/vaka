package com.vaka.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colores del avatar (los uso aquí y en Amigos)
val COLORES_AVATAR = listOf(
    "violeta" to Color(0xFF5B3DF5),
    "rosa" to Color(0xFFD9569E),
    "verde" to Color(0xFF19A974),
    "naranja" to Color(0xFFFF9F1C),
    "azul" to Color(0xFF3BA9E5),
    "rojo" to Color(0xFFE0556B),
    "morado" to Color(0xFF8B46E0),
    "dorado" to Color(0xFFE5A82E),
)
fun colorAvatarPor(nombre: String): Color =
    COLORES_AVATAR.find { it.first == nombre }?.second ?: Violeta

@Composable
fun TabPerfil(
    esInvitado: Boolean,
    nombre: String,
    correo: String,
    codigoUsuario: String,
    colorAvatar: String,
    fotoBase64: String,
    modoTema: String,
    monedaPrincipal: String,
    notifOn: Boolean,
    pausaNotifHasta: java.time.LocalDate? = null,
    onPausarNotificaciones: (String?) -> Unit = {},
    onCambiarColor: (String) -> Unit,
    onCambiarTema: (String) -> Unit,
    onToggleNotif: () -> Unit,
    onMonedaPrincipal: (String) -> Unit,
    onGuardarNombre: (String) -> Unit,
    onCerrarSesion: () -> Unit,
    onIrALogin: () -> Unit,
    onIrACuenta: () -> Unit,
) {
    val colorActual by animateColorAsState(
        colorAvatarPor(colorAvatar), tween(450), label = "colorAvatar"
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        // Hero: avatar grande con color personalizado
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(colorActual, Color(0xFF8B46E0), Rosa)))
                .padding(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(70.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = .22f))
                        .border(3.dp, Color.White.copy(alpha = .4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarPerfil(
                        fotoBase64 = fotoBase64,
                        nombre = nombre,
                        colorAvatar = colorAvatar,
                        tamano = 64.dp
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("PERFIL", color = Color.White.copy(alpha = .85f),
                        fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    Text(nombre, color = Color.White,
                        fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (esInvitado) "Modo invitado" else correo,
                        color = Color.White.copy(alpha = .85f), fontSize = 13.sp
                    )
                    if (!esInvitado && codigoUsuario.isNotBlank()) {
                        Text(codigoUsuario, color = Color.White.copy(alpha = .85f),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (esInvitado) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("CREA TU CUENTA", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Con una cuenta puedes compartir Vakas, tener amigos, y recuperar tus datos en otro teléfono.",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    BotonPrincipal("Crear cuenta / Iniciar sesión",
                        Modifier.fillMaxWidth().height(48.dp)) { onIrALogin() }
                }
            }
        } else {
            // Datos de la cuenta (resumen, sin formularios)
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("MI CUENTA", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(nombre, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text(correo, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onIrACuenta() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Editar nombre y seguridad 🔐", fontWeight = FontWeight.Bold) }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // === Personalización (color del avatar) ===
        SeccionAjustes(
            titulo = "PERSONALIZACIÓN 🎨",
            subtitulo = "Color de tu avatar"
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                COLORES_AVATAR.take(4).forEach { (id, c) ->
                    ColorChip(c, colorAvatar == id, Modifier.weight(1f)) { onCambiarColor(id) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                COLORES_AVATAR.drop(4).forEach { (id, c) ->
                    ColorChip(c, colorAvatar == id, Modifier.weight(1f)) { onCambiarColor(id) }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // === Apariencia (tema) ===
        SeccionAjustes(
            titulo = "APARIENCIA",
            subtitulo = "Tema visual de la app"
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TemaOpcion("📱", "Sistema", modoTema == TEMA_SISTEMA, Modifier.weight(1f)) {
                    onCambiarTema(TEMA_SISTEMA)
                }
                TemaOpcion("☀️", "Claro", modoTema == TEMA_CLARO, Modifier.weight(1f)) {
                    onCambiarTema(TEMA_CLARO)
                }
                TemaOpcion("🌙", "Oscuro", modoTema == TEMA_OSCURO, Modifier.weight(1f)) {
                    onCambiarTema(TEMA_OSCURO)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // === Preferencias ===
        SeccionAjustes(
            titulo = "PREFERENCIAS",
            subtitulo = "Recordatorios y moneda principal"
        ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Recordatorios 🔔",
                            fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text("Hitos, fechas límite y ánimos para no quedarte colgado",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = notifOn, onCheckedChange = { onToggleNotif() },
                        colors = SwitchDefaults.colors(checkedTrackColor = Violeta)
                    )
                }

                // Pausa temporal de notificaciones (solo si están activas globalmente)
                if (notifOn) {
                    Spacer(Modifier.height(8.dp))
                    var dialogoPausa by remember { mutableStateOf(false) }

                    if (pausaNotifHasta != null) {
                        // Notificaciones pausadas: mostrar info y botón para reactivar
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Dorado.copy(alpha = 0.18f))
                                .clickable { onPausarNotificaciones(null) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌙", fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("En pausa hasta el ${fechaBonita(pausaNotifHasta.toString())}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold)
                                    Text("Toca para reactivar ya",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        // Notificaciones activas: ofrecer pausar
                        TextButton(
                            onClick = { dialogoPausa = true },
                            modifier = Modifier.padding(start = 0.dp)
                        ) {
                            Text("🌙 Pausar por unos días",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (dialogoPausa) {
                        AlertDialog(
                            onDismissRequest = { dialogoPausa = false },
                            title = { Text("Pausar notificaciones") },
                            text = {
                                Column {
                                    Text("¿Por cuánto tiempo quieres pausar las notificaciones?")
                                    Spacer(Modifier.height(12.dp))
                                    val opciones = listOf(
                                        "3 días" to 3L,
                                        "1 semana" to 7L,
                                        "2 semanas" to 14L,
                                        "1 mes" to 30L
                                    )
                                    opciones.forEach { (txt, dias) ->
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    val fechaFin = java.time.LocalDate.now()
                                                        .plusDays(dias).toString()
                                                    onPausarNotificaciones(fechaFin)
                                                    dialogoPausa = false
                                                }
                                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                        ) {
                                            Text(txt, fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { dialogoPausa = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Moneda principal 💱",
                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text("Se usa por defecto al crear Vakas nuevas",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                // Indicador claro de la moneda actualmente seleccionada
                val monedaActual = remember(monedaPrincipal) {
                    MONEDAS.find { it.code == monedaPrincipal } ?: MONEDAS[0]
                }
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Violeta.copy(alpha = .12f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(monedaActual.symbol,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Violeta)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Estás ahorrando en:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${monedaActual.code} · ${monedaActual.nombre}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Cambiar a:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))

                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    MONEDAS.forEach { m ->
                        val sel = monedaPrincipal == m.code
                        val fondo by animateColorAsState(
                            if (sel) Violeta else MaterialTheme.colorScheme.surfaceVariant,
                            tween(250), label = "chip"
                        )
                        Text(
                            m.code,
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (sel) Color.White else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(fondo)
                                .clickable { onMonedaPrincipal(m.code) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
        }

        Spacer(Modifier.height(14.dp))

        if (!esInvitado) {
            SeccionAjustes(
                titulo = "SESIÓN",
                subtitulo = "Cerrar sesión en este dispositivo"
            ) {
                OutlinedButton(
                    onClick = onCerrarSesion,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Rojo)
                ) { Text("Cerrar sesión", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(14.dp))
        }

        AcercaDeVakaCard()
        Spacer(Modifier.height(80.dp))
    }
}

/**
 * Tarjeta "Acerca de Vaka" con versión instalada y botón para
 * buscar actualizaciones desde GitHub Releases.
 */
@Composable
private fun AcercaDeVakaCard() {
    val ctx = LocalContext.current
    var estado by remember { mutableStateOf<String?>(null) }
    var buscando by remember { mutableStateOf(false) }
    var progreso by remember { mutableStateOf<Int?>(null) }
    var resultado by remember { mutableStateOf<Actualizador.Resultado?>(null) }
    var dialogo by remember { mutableStateOf(false) }

    val versionLocal = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0"
        } catch (e: Exception) { "0" }
    }

    SeccionAjustes(
        titulo = "ACERCA DE VAKA",
        subtitulo = "Versión $versionLocal"
    ) {
        Text("Vaka 🐄",
            fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("Versión instalada: $versionLocal",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        BotonPrincipal(
            texto = when {
                progreso != null -> "Descargando… ${progreso}%"
                buscando -> "Buscando…"
                else -> "🔄 Buscar actualizaciones"
            },
            modifier = Modifier.fillMaxWidth().height(46.dp),
            habilitado = !buscando && progreso == null
        ) {
            buscando = true
            estado = null
            Actualizador.buscar(ctx) { r ->
                // El callback viene de un Thread, pero Compose state es seguro de mutar
                buscando = false
                resultado = r
                when {
                    r.error != null -> estado = r.error
                    r.hayActualizacion -> {
                        dialogo = true
                    }
                    else -> estado = "✓ Ya tienes la última versión (${r.versionLocal})"
                }
            }
        }
        estado?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp,
                color = if (it.startsWith("Error", ignoreCase = true)) Rojo else Verde,
                fontWeight = FontWeight.Bold)
        }
    }

    if (dialogo) {
        val r = resultado ?: return
        AlertDialog(
            onDismissRequest = { if (progreso == null) dialogo = false },
            title = { Text("🆕 Nueva versión ${r.versionRemota}", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column {
                    Text("Tienes la ${r.versionLocal} instalada.", fontSize = 13.sp)
                    if (r.notas.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Novedades:", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(r.notas, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                    progreso?.let { p ->
                        Spacer(Modifier.height(12.dp))
                        Text("Descargando: $p%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = progreso == null,
                    onClick = {
                        progreso = 0
                        estado = null
                        Actualizador.descargarEInstalar(
                            ctx, r.urlApk,
                            onProgreso = { p -> progreso = p },
                            onError = { msg ->
                                progreso = null
                                estado = "❌ $msg"
                                dialogo = false
                            }
                        )
                    }
                ) {
                    Text(if (progreso == null) "Descargar e instalar" else "Descargando…",
                        fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = progreso == null,
                    onClick = { dialogo = false }
                ) { Text("Después") }
            }
        )
    }
}

@Composable
private fun ColorChip(color: Color, sel: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val borde by animateColorAsState(
        if (sel) MaterialTheme.colorScheme.onSurface else Color.Transparent,
        tween(250), label = "borde"
    )
    Box(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .border(3.dp, borde, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (sel) Text("✓", color = Color.White,
            fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
    }
}

@Composable
private fun TemaOpcion(emoji: String, nombre: String, seleccionado: Boolean,
                      modifier: Modifier = Modifier, onClick: () -> Unit) {
    val fondo by animateColorAsState(
        if (seleccionado) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        tween(250), label = "fT"
    )
    val texto by animateColorAsState(
        if (seleccionado) Color.White else MaterialTheme.colorScheme.onSurface,
        tween(250), label = "tT"
    )
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(fondo)
            .clickable { onClick() }.padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(nombre, color = texto, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
    }
}

/**
 * Sección colapsable de Ajustes. Muestra un header con título + ícono + chevron,
 * y un contenido que se expande/colapsa con animación al tocar.
 *
 * Por defecto, las secciones nacen colapsadas para que la pantalla de Ajustes
 * se vea limpia. El usuario abre solo lo que necesita en el momento.
 */
@Composable
fun SeccionAjustes(
    titulo: String,
    subtitulo: String = "",
    iniciaExpandida: Boolean = false,
    contenido: @Composable () -> Unit
) {
    var expandida by remember { mutableStateOf(iniciaExpandida) }
    val rotacion by animateFloatAsState(
        if (expandida) 180f else 0f,
        tween(250), label = "rotChev"
    )

    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column {
            // Header: siempre visible, clickable
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expandida = !expandida }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        titulo,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )
                    if (subtitulo.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            subtitulo,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "▼",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer { rotationZ = rotacion }
                )
            }

            // Contenido colapsable con animación
            AnimatedVisibility(
                visible = expandida,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp)) {
                    contenido()
                }
            }
        }
    }
}
