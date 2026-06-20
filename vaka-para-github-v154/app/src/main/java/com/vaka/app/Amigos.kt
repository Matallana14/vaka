package com.vaka.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pestaña de Amigos:
 *  - Tu código único (VK-XXXXXX) para compartir
 *  - Lista de amigos
 *  - Solicitudes pendientes
 *  - "Compañeros de Vaka" sugeridos (gente con la que ya compartiste algo)
 */
@Composable
fun TabAmigos(
    nombreUsuario: String?,
    miCodigoUsuario: String,
    amigos: List<PerfilUsuario>,
    solicitudes: List<PerfilUsuario>,
    companeros: List<PerfilUsuario>,  // gente con la que ya he compartido alguna Vaka
    misVakasPrivadas: List<VakaItem>,
    misVakasCompartidas: List<VakaItem>,
    onAgregarPorCodigo: (codigo: String, ok: () -> Unit, error: (String) -> Unit) -> Unit,
    onAceptar: (PerfilUsuario) -> Unit,
    onRechazar: (PerfilUsuario) -> Unit,
    onEliminar: (PerfilUsuario) -> Unit,
    onInvitarAVaka: (amigo: PerfilUsuario, vaka: VakaItem) -> Unit,
    onInvitarVariosACompartida: (amigos: List<PerfilUsuario>, vaka: VakaItem) -> Unit,
    onConvertirYInvitar: (vakaPrivada: VakaItem, amigos: List<PerfilUsuario>) -> Unit,
    onIrALogin: () -> Unit,
) {
    var dialogoCodigo by remember { mutableStateOf(false) }
    var codigoTxt by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var msgColor by remember { mutableStateOf(Verde) }
    var invitando by remember { mutableStateOf<PerfilUsuario?>(null) }
    var eliminando by remember { mutableStateOf<PerfilUsuario?>(null) }
    // Para invitación múltiple desde Compañeros de Vaka
    var dialogoInvitarVarios by remember { mutableStateOf(false) }
    val clip = LocalClipboardManager.current

    // Launcher del scanner QR para agregar amigo por su QR
    val escanerAmigo = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = com.journeyapps.barcodescanner.ScanContract()
    ) { resultado ->
        val codigoEscaneado = codigoUsuarioDesdeQr(resultado.contents)
        if (codigoEscaneado != null) {
            onAgregarPorCodigo(
                codigoEscaneado,
                {
                    // El banner global se encarga de mostrar el aviso de envío
                    mensaje = ""
                },
                { err ->
                    mensaje = err
                    msgColor = Rojo
                }
            )
        } else if (resultado.contents != null) {
            mensaje = "Ese QR no parece ser de un usuario Vaka."
            msgColor = Rojo
        }
    }
    fun escanearQrAmigo() {
        val opciones = com.journeyapps.barcodescanner.ScanOptions()
            .setPrompt("Apunta la cámara al código QR del usuario")
            .setBeepEnabled(false)
            .setOrientationLocked(true)
            .setCaptureActivity(CaptureActivityPortrait::class.java)
        escanerAmigo.launch(opciones)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        TituloPestana("🤝 Amigos", "Invítalos a tus Vakas con un toque")
        Spacer(Modifier.height(14.dp))

        if (nombreUsuario == null) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Inicia sesión para usar amigos",
                        fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Las funciones de amigos requieren una cuenta para identificarte.",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    BotonPrincipal("Crear cuenta / Iniciar sesión",
                        Modifier.fillMaxWidth().height(48.dp)) { onIrALogin() }
                }
            }
            Spacer(Modifier.height(80.dp))
            return@Column
        }

        // === Mi código de usuario ===
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("COMPÁRTETE CON OTROS", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Text("Tu QR o tu código sirven para que te agreguen como amigo:",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))

                // Tarjeta del QR del usuario (ya viene con todo el styling)
                if (miCodigoUsuario.isNotBlank()) {
                    QrUsuario(miCodigoUsuario)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f))
                            .clickable {
                                clip.setText(AnnotatedString(miCodigoUsuario))
                                mensaje = "Código copiado al portapapeles 📋"; msgColor = Verde
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "📋  Tocar para copiar el código",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Box(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Generando…", fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(14.dp))
                // Botones apilados para que el texto se vea completo
                BotonPrincipal("📷  Escanear QR de un amigo",
                    Modifier.fillMaxWidth().height(48.dp)) { escanearQrAmigo() }
                Spacer(Modifier.height(8.dp))
                BotonPrincipal("✏️  Agregar por código",
                    Modifier.fillMaxWidth().height(48.dp),
                    color = Rosa) { dialogoCodigo = true }
            }
        }

        if (mensaje.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(mensaje, color = msgColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Spacer(Modifier.height(14.dp))

        // === Solicitudes pendientes ===
        AnimatedVisibility(solicitudes.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()) {
            Column {
                ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("SOLICITUDES PENDIENTES 📬",
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                        solicitudes.forEachIndexed { i, s ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarPequeno(s.nombre, s.colorAvatar, s.fotoBase64)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(s.nombre, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                    Text(s.codigo, fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = { onAceptar(s) }) {
                                    Text("✓ Aceptar", color = Verde, fontWeight = FontWeight.ExtraBold)
                                }
                                TextButton(onClick = { onRechazar(s) }) {
                                    Text("✕", color = Rojo, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            if (i < solicitudes.lastIndex) HorizontalDivider()
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }

        // === Compañeros de Vaka sugeridos ===
        val companerosNoAmigos = companeros.filter { c -> amigos.none { it.uid == c.uid } }
        AnimatedVisibility(companeros.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()) {
            Column {
                ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("COMPAÑEROS DE VAKA",
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Personas con las que ya has compartido una Vaka. Invítalas a más Vakas con un solo toque.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        BotonPrincipal(
                            "🎯 Invitar compañeros a una Vaka",
                            Modifier.fillMaxWidth().height(46.dp)
                        ) { dialogoInvitarVarios = true }

                        if (companerosNoAmigos.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            Text("O agrégalos como amigos:",
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            companerosNoAmigos.forEachIndexed { i, c ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarPequeno(c.nombre, c.colorAvatar, c.fotoBase64)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(c.nombre, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                        Text("Compartieron una Vaka", fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    TextButton(onClick = {
                                        onAgregarPorCodigo(c.codigo,
                                            { mensaje = "" },
                                            { e -> mensaje = e; msgColor = Rojo }
                                        )
                                    }) {
                                        Text("+ Agregar", color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                                if (i < companerosNoAmigos.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }

        // === Lista de amigos ===
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("MIS AMIGOS (${amigos.size})",
                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                if (amigos.isEmpty()) {
                    Text(
                        "Aún no tienes amigos en Vaka.\nAgrégalos por código o desde tus compañeros de Vaka",
                        Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    amigos.forEachIndexed { i, a ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarPequeno(a.nombre, a.colorAvatar, a.fotoBase64)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(a.nombre, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                Text(a.codigo, fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (misVakasCompartidas.isNotEmpty()) {
                                TextButton(onClick = { invitando = a }) {
                                    Text("Invitar", color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            TextButton(onClick = { eliminando = a }) {
                                Text("🗑", fontSize = 14.sp)
                            }
                        }
                        if (i < amigos.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }

    // Diálogo: agregar por código
    if (dialogoCodigo) {
        AlertDialog(
            onDismissRequest = { dialogoCodigo = false },
            title = { Text("Agregar amigo por código", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column {
                    Text("Pídele a tu amigo su código de usuario (lo encuentra arriba en esta pantalla):",
                        fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = codigoTxt,
                        onValueChange = { codigoTxt = it.uppercase() },
                        placeholder = { Text("VK-A3F5B2") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp), singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val c = codigoTxt.trim().uppercase()
                    if (c.isNotBlank()) {
                        onAgregarPorCodigo(c,
                            {
                                mensaje = ""
                                dialogoCodigo = false; codigoTxt = ""
                            },
                            { e -> mensaje = e; msgColor = Rojo }
                        )
                    }
                }) { Text("Enviar solicitud", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { dialogoCodigo = false; codigoTxt = "" }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo: invitar amigo a una Vaka compartida
    invitando?.let { a ->
        AlertDialog(
            onDismissRequest = { invitando = null },
            title = { Text("Invitar a ${a.nombre}", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column {
                    Text("Elige a cuál de tus Vakas compartidas quieres invitarlo:",
                        fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    misVakasCompartidas.forEach { v ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onInvitarAVaka(a, v)
                                    invitando = null
                                    mensaje = "${a.nombre} fue invitado a \"${v.nombre}\" ✓"
                                    msgColor = Verde
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(v.emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(v.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { invitando = null }) { Text("Cerrar") }
            }
        )
    }

    // Diálogo: eliminar amigo
    eliminando?.let { a ->
        AlertDialog(
            onDismissRequest = { eliminando = null },
            title = { Text("¿Eliminar a ${a.nombre}?") },
            text = { Text("Dejarán de ser amigos. Seguirá apareciendo en las Vakas que ya comparten.") },
            confirmButton = {
                TextButton(onClick = { onEliminar(a); eliminando = null }) {
                    Text("Sí, eliminar", color = Rojo, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { eliminando = null }) { Text("Cancelar") } }
        )
    }

    // ===== Diálogo: invitar varios compañeros a una Vaka =====
    if (dialogoInvitarVarios) {
        InvitarVariosDialog(
            companeros = companeros,
            privadas = misVakasPrivadas,
            compartidas = misVakasCompartidas,
            onCancelar = { dialogoInvitarVarios = false },
            onCompartida = { vaka, seleccionados ->
                onInvitarVariosACompartida(seleccionados, vaka)
                dialogoInvitarVarios = false
                mensaje = "${seleccionados.size} ${if (seleccionados.size == 1) "compañero invitado" else "compañeros invitados"} a \"${vaka.nombre}\" ✓"
                msgColor = Verde
            },
            onPrivadaAConvertir = { vaka, seleccionados ->
                onConvertirYInvitar(vaka, seleccionados)
                dialogoInvitarVarios = false
                mensaje = "\"${vaka.nombre}\" ahora es compartida con ${seleccionados.size} ${if (seleccionados.size == 1) "persona" else "personas"} ✓"
                msgColor = Verde
            }
        )
    }
}

@Composable
private fun InvitarVariosDialog(
    companeros: List<PerfilUsuario>,
    privadas: List<VakaItem>,
    compartidas: List<VakaItem>,
    onCancelar: () -> Unit,
    onCompartida: (VakaItem, List<PerfilUsuario>) -> Unit,
    onPrivadaAConvertir: (VakaItem, List<PerfilUsuario>) -> Unit,
) {
    // Paso 1: elegir compañeros (multi-select)
    // Paso 2: elegir a qué Vaka invitarlos (privada → se convierte, o compartida → se agregan)
    var paso by remember { mutableStateOf(1) }
    val seleccionados = remember { mutableStateListOf<PerfilUsuario>() }
    val privadasNoFijas = privadas.filter { !it.esFija }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Text(
                if (paso == 1) "Elige a quién invitar"
                else "¿A qué Vaka?",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (paso == 1) {
                    Text(
                        "Selecciona uno o varios compañeros (${seleccionados.size} seleccionado${if (seleccionados.size == 1) "" else "s"}):",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    if (companeros.isEmpty()) {
                        Text(
                            "Aún no tienes compañeros de Vaka.\nCrea una Vaka compartida con alguien primero.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            companeros.forEach { c ->
                                val sel = seleccionados.any { it.uid == c.uid }
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (sel) seleccionados.removeAll { it.uid == c.uid }
                                            else seleccionados.add(c)
                                        }
                                        .padding(vertical = 8.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (sel) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (sel) Text("✓", color = Color.White,
                                            fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    AvatarPequeno(c.nombre, c.colorAvatar, c.fotoBase64)
                                    Spacer(Modifier.width(10.dp))
                                    Text(c.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "Vas a invitar a ${seleccionados.size} ${if (seleccionados.size == 1) "persona" else "personas"}. Elige a qué Vaka:",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (compartidas.isNotEmpty()) {
                            Text("👥 VAKAS COMPARTIDAS",
                                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp)
                            Spacer(Modifier.height(4.dp))
                            compartidas.forEach { v ->
                                VakaSeleccionableRow(v, "Agregar miembros") {
                                    onCompartida(v, seleccionados.toList())
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                        if (privadasNoFijas.isNotEmpty()) {
                            Text("🐷 MIS VAKAS PRIVADAS",
                                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Si eliges una, se convertirá en compartida.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            privadasNoFijas.forEach { v ->
                                VakaSeleccionableRow(v, "Convertir y compartir") {
                                    onPrivadaAConvertir(v, seleccionados.toList())
                                }
                            }
                        }
                        if (compartidas.isEmpty() && privadasNoFijas.isEmpty()) {
                            Text(
                                "No tienes Vakas para invitarlos todavía. Crea una primero desde la pestaña 'Vakas'.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (paso == 1) {
                TextButton(
                    onClick = { paso = 2 },
                    enabled = seleccionados.isNotEmpty()
                ) {
                    Text("Siguiente →", fontWeight = FontWeight.ExtraBold)
                }
            } else {
                TextButton(onClick = { paso = 1 }) {
                    Text("← Atrás")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

@Composable
private fun VakaSeleccionableRow(v: VakaItem, accion: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(v.emoji, fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(v.nombre, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            Text(accion, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Text("→", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun AvatarPequeno(nombre: String, colorAvatar: String, fotoBase64: String = "") {
    AvatarPerfil(
        fotoBase64 = fotoBase64,
        nombre = nombre,
        colorAvatar = colorAvatar,
        tamano = 40.dp
    )
}
