package com.vaka.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// (COLORES_AVATAR y colorAvatarPor ahora viven en Perfil.kt)

@Composable
fun CuentaScreen(
    correo: String,
    nombre: String,
    colorAvatar: String,
    fotoBase64: String,
    esCuentaGoogle: Boolean,                      // true si entró con "Continuar con Google"
    onCambiarColor: (String) -> Unit,
    onCambiarFoto: (String) -> Unit,              // String vacío = quitar foto
    onGuardarNombre: (String) -> Unit,
    onCambiarCorreo: (nuevoCorreo: String, clave: String, ok: () -> Unit, error: (String) -> Unit) -> Unit,
    onCambiarClave: (claveActual: String, nuevaClave: String, ok: () -> Unit, error: (String) -> Unit) -> Unit,
    onBorrarCuenta: (clave: String, ok: () -> Unit, error: (String) -> Unit) -> Unit,
    onBorrarCuentaGoogle: (ok: () -> Unit, error: (String) -> Unit) -> Unit,  // NUEVO
    onBack: () -> Unit,
) {
    var verCambioCorreo by remember { mutableStateOf(false) }
    var verCambioClave by remember { mutableStateOf(false) }
    var verBorrar by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    var mensajeColor by remember { mutableStateOf(Verde) }
    var nombreTxt by remember { mutableStateOf(nombre) }

    fun avisar(t: String, ok: Boolean = true) {
        mensaje = t
        mensajeColor = if (ok) Verde else Rojo
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            Text("← Mi cuenta", fontWeight = FontWeight.ExtraBold)
        }

        // Hero con avatar personalizado
        val colorActual by animateColorAsState(
            colorAvatarPor(colorAvatar), tween(400), label = "colorAvatar"
        )
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(colorActual, Color(0xFF8B46E0), Rosa)))
                .padding(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar: foto si la tiene, sino círculo con inicial
                Box(
                    Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = .25f))
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
                    Text("CUENTA Y SEGURIDAD", color = Color.White.copy(alpha = .85f),
                        fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    Text(nombre, color = Color.White,
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(correo, color = Color.White.copy(alpha = .85f), fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // === Foto de perfil ===
        FotoPerfilCard(
            fotoBase64 = fotoBase64,
            nombre = nombre,
            colorAvatar = colorAvatar,
            onCambiarFoto = onCambiarFoto
        )

        Spacer(Modifier.height(14.dp))

        // === Tu nombre ===
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("TU NOMBRE", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text("Es lo que ven tus amigos en Vakas compartidas y en sus aportes.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = nombreTxt, onValueChange = { nombreTxt = it },
                    label = { Text("Nombre visible") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                BotonPrincipal("Guardar nombre",
                    Modifier.fillMaxWidth().height(46.dp)) {
                    val t = nombreTxt.trim()
                    if (t.isNotBlank() && t != nombre) {
                        onGuardarNombre(t)
                        avisar("Nombre actualizado ✓")
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Personalización: color del avatar
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("PERSONALIZACIÓN", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Text("Color de tu avatar 🎨",
                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text("Decide cómo te verán tus amigos en Vakas compartidas",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    COLORES_AVATAR.take(4).forEach { (id, c) ->
                        ColorChip(id, c, colorAvatar == id, Modifier.weight(1f)) { onCambiarColor(id) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    COLORES_AVATAR.drop(4).forEach { (id, c) ->
                        ColorChip(id, c, colorAvatar == id, Modifier.weight(1f)) { onCambiarColor(id) }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (esCuentaGoogle) {
            // Para usuarios que entraron con Google, no aplican estos campos
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("CUENTA DE GOOGLE", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(correo, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Entraste a Vaka con tu cuenta de Google, así que tu correo y contraseña los administra Google directamente. Si quieres cambiarlos, hazlo desde myaccount.google.com.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Cambiar correo
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("CORREO ELECTRÓNICO", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(correo, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { verCambioCorreo = !verCambioCorreo; mensaje = "" },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (verCambioCorreo) "Cancelar" else "Cambiar correo",
                            fontWeight = FontWeight.Bold)
                    }
                    AnimatedVisibility(verCambioCorreo, enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()) {
                        CambioCorreoForm(
                            onConfirmar = { nuevo, clave ->
                                onCambiarCorreo(nuevo, clave,
                                    { avisar("Correo actualizado ✓"); verCambioCorreo = false },
                                    { e -> avisar(e, ok = false) })
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Cambiar contraseña
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("CONTRASEÑA", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Mantén tu cuenta segura con una contraseña fuerte.",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { verCambioClave = !verCambioClave; mensaje = "" },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (verCambioClave) "Cancelar" else "Cambiar contraseña",
                            fontWeight = FontWeight.Bold)
                    }
                    AnimatedVisibility(verCambioClave, enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()) {
                        CambioClaveForm(
                            onConfirmar = { actual, nueva ->
                                onCambiarClave(actual, nueva,
                                    { avisar("Contraseña actualizada ✓"); verCambioClave = false },
                                    { e -> avisar(e, ok = false) })
                            }
                        )
                    }
                }
            }
        }

        if (mensaje.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(mensaje, fontSize = 13.sp, color = mensajeColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
        }

        Spacer(Modifier.height(14.dp))

        // Zona de peligro: eliminar cuenta
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("ZONA DE PELIGRO", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = Rojo, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Text("Eliminar tu cuenta borra tu correo, contraseña, perfil público y te saca de todas las Vakas compartidas. Tus Vakas privadas guardadas en este teléfono no se pierden, pero tampoco podrás recuperarlas desde otro dispositivo.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { verBorrar = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Rojo)
                ) { Text("Eliminar mi cuenta", fontWeight = FontWeight.ExtraBold) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (verBorrar) {
        if (esCuentaGoogle) {
            BorrarCuentaGoogleDialog(
                onConfirmar = {
                    onBorrarCuentaGoogle({ verBorrar = false }) { e ->
                        avisar(e, ok = false)
                        verBorrar = false
                    }
                },
                onCancelar = { verBorrar = false }
            )
        } else {
            BorrarCuentaDialog(
                onConfirmar = { clave ->
                    onBorrarCuenta(clave, { verBorrar = false }) { e ->
                        avisar(e, ok = false)
                        verBorrar = false
                    }
                },
                onCancelar = { verBorrar = false }
            )
        }
    }
}

@Composable
private fun ColorChip(
    id: String, color: Color, sel: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
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
private fun CambioCorreoForm(onConfirmar: (String, String) -> Unit) {
    var nuevo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    Column(Modifier.padding(top = 12.dp)) {
        OutlinedTextField(
            value = nuevo, onValueChange = { nuevo = it },
            label = { Text("Nuevo correo") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp), singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = clave, onValueChange = { clave = it },
            label = { Text("Tu contraseña actual (para confirmar)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp), singleLine = true
        )
        Spacer(Modifier.height(10.dp))
        BotonPrincipal("Confirmar cambio", Modifier.fillMaxWidth().height(46.dp)) {
            if (nuevo.isNotBlank() && clave.isNotBlank()) onConfirmar(nuevo.trim(), clave)
        }
    }
}

@Composable
private fun CambioClaveForm(onConfirmar: (String, String) -> Unit) {
    var actual by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Column(Modifier.padding(top = 12.dp)) {
        OutlinedTextField(
            value = actual, onValueChange = { actual = it },
            label = { Text("Contraseña actual") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp), singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = nueva, onValueChange = { nueva = it },
            label = { Text("Nueva contraseña (mín. 6 caracteres)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp), singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = confirmar, onValueChange = { confirmar = it },
            label = { Text("Confirmar nueva contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp), singleLine = true
        )
        if (error.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(error, color = Rojo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        BotonPrincipal("Confirmar cambio", Modifier.fillMaxWidth().height(46.dp)) {
            error = when {
                nueva.length < 6 -> "La nueva contraseña debe tener al menos 6 caracteres."
                nueva != confirmar -> "Las contraseñas no coinciden."
                actual.isBlank() -> "Escribe tu contraseña actual."
                else -> { onConfirmar(actual, nueva); "" }
            }
        }
    }
}

@Composable
private fun BorrarCuentaDialog(
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var clave by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("⚠️ ¿Eliminar tu cuenta?", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                Text("Esta acción es PERMANENTE. Para confirmar:",
                    fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = clave, onValueChange = { clave = it },
                    label = { Text("Tu contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmacion, onValueChange = { confirmacion = it.uppercase() },
                    label = { Text("Escribe ELIMINAR para confirmar") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (clave.isNotBlank() && confirmacion == "ELIMINAR") onConfirmar(clave) },
                enabled = clave.isNotBlank() && confirmacion == "ELIMINAR"
            ) {
                Text("Sí, eliminar", color = Rojo, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

/**
 * Diálogo de eliminación para cuentas de Google: solo pide escribir ELIMINAR,
 * después la re-autenticación se hace abriendo el selector de cuentas de Google.
 */
@Composable
private fun BorrarCuentaGoogleDialog(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    var confirmacion by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("⚠️ ¿Eliminar tu cuenta?", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column {
                Text(
                    "Esta acción es PERMANENTE. Para confirmar te pediremos elegir tu cuenta de Google una última vez.",
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmacion, onValueChange = { confirmacion = it.uppercase() },
                    label = { Text("Escribe ELIMINAR para confirmar") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (confirmacion == "ELIMINAR") onConfirmar() },
                enabled = confirmacion == "ELIMINAR"
            ) {
                Text("Sí, eliminar", color = Rojo, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

/**
 * Tarjeta para subir, cambiar o quitar la foto de perfil.
 * Abre el selector nativo de imágenes (galería), procesa la imagen
 * (recorta cuadrada, comprime, codifica a Base64) y llama onCambiarFoto.
 */
@Composable
private fun FotoPerfilCard(
    fotoBase64: String,
    nombre: String,
    colorAvatar: String,
    onCambiarFoto: (String) -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var procesando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }

    // Selector nativo de imágenes (Photo Picker en Android 13+, GetContent antes)
    val seleccionador = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            procesando = true
            mensaje = null
            // Procesamiento puede ser pesado: ejecutamos en background
            Thread {
                val b64 = FotoPerfil.procesarUri(ctx, uri)
                procesando = false
                if (b64 != null) {
                    onCambiarFoto(b64)
                    mensaje = "Foto actualizada"
                } else {
                    mensaje = "No se pudo procesar la imagen"
                }
            }.start()
        }
    }

    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("FOTO DE PERFIL", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text("Tu foto aparece en tu perfil, en las Vakas compartidas y junto a tus aportes.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarPerfil(
                    fotoBase64 = fotoBase64,
                    nombre = nombre,
                    colorAvatar = colorAvatar,
                    tamano = 72.dp
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    BotonPrincipal(
                        texto = when {
                            procesando -> "Procesando…"
                            fotoBase64.isNotBlank() -> "Cambiar foto"
                            else -> "Elegir foto"
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        habilitado = !procesando,
                    ) {
                        seleccionador.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                    if (fotoBase64.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                onCambiarFoto("")
                                mensaje = "Foto quitada"
                            },
                            enabled = !procesando
                        ) {
                            Text("Quitar foto", color = Rojo, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            mensaje?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 12.sp, color = Verde, fontWeight = FontWeight.Bold)
            }
        }
    }
    Spacer(Modifier.height(14.dp))
}
