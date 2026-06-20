package com.vaka.app

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onInvitado: () -> Unit, onRegistro: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var modo by remember { mutableStateOf("login") }
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var dialogoRecuperar by remember { mutableStateOf(false) }

    // ---- Inicio de sesión con Google usando Credential Manager ----
    fun iniciarConGoogle() {
        error = ""
        val act = context as? Activity
        if (act == null) {
            error = "No se pudo iniciar Google Sign-In."
            return
        }
        val resId = context.resources.getIdentifier(
            "default_web_client_id", "string", context.packageName
        )
        if (resId == 0) {
            error = "Google Sign-In no está configurado todavía. Habilítalo en Firebase Authentication y reemplaza tu google-services.json."
            return
        }
        val clientId = context.getString(resId)
        cargando = true
        scope.launch {
            val cm = CredentialManager.create(context)

            // GetSignInWithGoogleOption SIEMPRE abre el selector de cuentas,
            // sin importar si es la primera vez o no. Es la opción correcta
            // para un botón "Continuar con Google" universal.
            val opcion = GetSignInWithGoogleOption.Builder(clientId).build()
            val solicitud = GetCredentialRequest.Builder()
                .addCredentialOption(opcion)
                .build()

            try {
                val resp = cm.getCredential(act, solicitud)

                val cred = resp.credential
                android.util.Log.d("VakaAuth", "Tipo credencial: ${cred::class.java.simpleName}")
                if (cred is CustomCredential &&
                    cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val gCred = GoogleIdTokenCredential.createFrom(cred.data)
                    val idToken = gCred.idToken
                    android.util.Log.d("VakaAuth", "idToken length: ${idToken.length}, displayName: ${gCred.displayName}")
                    if (idToken.isBlank()) {
                        cargando = false
                        error = "Google devolvió un token vacío. Verifica el SHA-1 en Firebase."
                        return@launch
                    }
                    val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
                    Firebase.auth.signInWithCredential(firebaseCred)
                        .addOnSuccessListener { authResult ->
                            authResult.user?.let { u ->
                                if (u.displayName.isNullOrBlank() && !gCred.displayName.isNullOrBlank()) {
                                    u.updateProfile(
                                        UserProfileChangeRequest.Builder()
                                            .setDisplayName(gCred.displayName).build()
                                    )
                                }
                            }
                            cargando = false
                            // Detección robusta de "usuario nuevo": Firebase a veces
                            // marca isNewUser correctamente y otras veces no, por eso
                            // comprobamos también si la creación y último login son
                            // virtualmente iguales (margen de 5 segundos = es la primera vez).
                            val esNuevoPorFirebase = authResult.additionalUserInfo?.isNewUser == true
                            val metadata = authResult.user?.metadata
                            val creacion = metadata?.creationTimestamp ?: 0L
                            val ultimoLogin = metadata?.lastSignInTimestamp ?: 0L
                            val esNuevoPorTimestamp = (ultimoLogin - creacion) < 5000L
                            if (esNuevoPorFirebase || esNuevoPorTimestamp) {
                                onRegistro()
                            }
                        }
                        .addOnFailureListener { e ->
                            cargando = false
                            android.util.Log.e("VakaAuth", "Firebase signIn failed", e)
                            error = "Firebase rechazó el token: ${e.localizedMessage ?: e.javaClass.simpleName}"
                        }
                } else {
                    cargando = false
                    error = "Credencial inesperada (${cred::class.java.simpleName})."
                }
            } catch (e: GetCredentialCancellationException) {
                cargando = false
                error = ""
            } catch (e: NoCredentialException) {
                cargando = false
                android.util.Log.e("VakaAuth", "NoCredentialException", e)
                error = "No hay credenciales disponibles. Asegúrate de tener al menos una cuenta de Google en este teléfono (Ajustes → Cuentas → Añadir cuenta de Google)."
            } catch (e: GetCredentialException) {
                cargando = false
                android.util.Log.e("VakaAuth", "GetCredentialException type=${e.type}", e)
                error = "No se completó (${e.javaClass.simpleName}): ${e.localizedMessage ?: e.type}"
            } catch (e: Exception) {
                cargando = false
                android.util.Log.e("VakaAuth", "Exception", e)
                error = "Error inesperado: ${e.javaClass.simpleName} — ${e.localizedMessage ?: ""}"
            }
        }
    }

    fun enviar() {
        error = ""
        val e = email.trim()
        if (e.isBlank()) {
            error = "Escribe tu correo."
            return
        }
        // En login, basta con que la contraseña no esté vacía
        if (modo == "login" && clave.isBlank()) {
            error = "Escribe tu contraseña."
            return
        }
        // En registro aplicamos reglas estrictas
        if (modo == "registro") {
            if (nombre.isBlank()) {
                error = "Escribe tu nombre: así te verán en las Vakas compartidas."
                return
            }
            val problemaClave = validarFortalezaClave(clave)
            if (problemaClave != null) {
                error = problemaClave
                return
            }
        }
        cargando = true
        if (modo == "registro") {
            Firebase.auth.createUserWithEmailAndPassword(e, clave)
                .addOnSuccessListener { res ->
                    val u = res.user
                    if (u != null && nombre.isNotBlank()) {
                        // Actualizar displayName y SOLO entonces marcar el registro,
                        // así el onboarding aparece cuando todo está listo.
                        u.updateProfile(
                            UserProfileChangeRequest.Builder()
                                .setDisplayName(nombre.trim()).build()
                        ).addOnCompleteListener {
                            cargando = false
                            onRegistro()
                        }
                    } else {
                        cargando = false
                        onRegistro()
                    }
                }
                .addOnFailureListener {
                    cargando = false
                    error = "No se pudo crear la cuenta. ¿Quizá el correo ya está registrado?"
                }
        } else {
            Firebase.auth.signInWithEmailAndPassword(e, clave)
                .addOnFailureListener {
                    cargando = false
                    error = "Correo o contraseña incorrectos."
                }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(
                    Brush.radialGradient(listOf(Color(0xFFFFE08A), Dorado, Color(0xFFE59E13)))
                )
            )
            Spacer(Modifier.width(8.dp))
            Text("Vaka", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = VioletaOscuro)
        }
        Spacer(Modifier.height(26.dp))

        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    if (modo == "login") "INICIAR SESIÓN" else "CREAR CUENTA",
                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp
                )
                Spacer(Modifier.height(12.dp))

                if (modo == "registro") {
                    OutlinedTextField(
                        value = nombre, onValueChange = { nombre = it },
                        placeholder = { Text("Tu nombre (visible en Vakas compartidas)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp), singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    placeholder = { Text("Correo electrónico") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = clave, onValueChange = { clave = it },
                    placeholder = {
                        Text(
                            if (modo == "registro")
                                "Contraseña (8+ caracteres, 1 mayúscula, 1 especial)"
                            else "Contraseña"
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), singleLine = true
                )

                // Indicador visual de fortaleza (solo en modo registro y cuando hay texto)
                if (modo == "registro" && clave.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    val (progreso, etiqueta, colorFortaleza) = calcularFortalezaClave(clave)
                    val animado by animateFloatAsState(
                        targetValue = progreso,
                        animationSpec = tween(durationMillis = 400),
                        label = "fortaleza"
                    )
                    Column {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animado)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(colorFortaleza)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Fortaleza: $etiqueta",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorFortaleza
                        )
                    }
                }

                if (error.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(error, color = Rojo, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(14.dp))
                BotonPrincipal(
                    if (cargando) "Un momento…"
                    else if (modo == "login") "Entrar" else "Crear mi cuenta",
                    Modifier.fillMaxWidth().height(50.dp),
                    habilitado = !cargando
                ) { enviar() }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { iniciarConGoogle() },
                    enabled = !cargando,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Continuar con Google", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))
                // Botón "¿Olvidaste tu contraseña?" solo visible en modo login
                if (modo == "login") {
                    TextButton(
                        onClick = { dialogoRecuperar = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("¿Olvidaste tu contraseña?",
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp)
                    }
                }
                TextButton(
                    onClick = { modo = if (modo == "login") "registro" else "login"; error = "" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (modo == "login") "¿No tienes cuenta? Crea una aquí"
                        else "¿Ya tienes cuenta? Inicia sesión",
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onInvitado) {
            Text("Continuar sin cuenta (solo Vakas privadas)",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(20.dp))
    }

    if (dialogoRecuperar) {
        DialogoRecuperarClave(
            correoInicial = email,
            onCerrar = { dialogoRecuperar = false }
        )
    }
}

/**
 * Diálogo de recuperación de contraseña.
 * Solo aplica a cuentas creadas con email/contraseña (las cuentas Google
 * recuperan su acceso desde Google directamente, no desde Vaka).
 */
@Composable
private fun DialogoRecuperarClave(
    correoInicial: String,
    onCerrar: () -> Unit
) {
    var correo by remember { mutableStateOf(correoInicial) }
    var enviando by remember { mutableStateOf(false) }
    var enviado by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Text(
                if (enviado) "✓ Correo enviado"
                else "Recuperar contraseña"
            )
        },
        text = {
            if (enviado) {
                Column {
                    Text("Te enviamos un correo a $correo con un enlace para crear una nueva contraseña.")
                    Spacer(Modifier.height(8.dp))
                    Text("Revisa también tu carpeta de spam por si acaso.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Importante: si te registraste con Google, no recibirás el correo. " +
                        "En ese caso recupera tu acceso desde tu cuenta de Google.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column {
                    Text("Escribe el correo de tu cuenta y te enviaremos un enlace para crear una nueva contraseña.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it.trim() },
                        placeholder = { Text("tu@correo.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        enabled = !enviando
                    )
                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = Rojo, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Solo funciona con cuentas creadas con correo y contraseña. " +
                        "Las cuentas de Google se recuperan desde Google.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (enviado) {
                TextButton(onClick = onCerrar) {
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(
                    onClick = {
                        val c = correo.trim()
                        if (c.isBlank() || !c.contains("@")) {
                            error = "Escribe un correo válido."
                            return@TextButton
                        }
                        enviando = true
                        error = ""
                        Firebase.auth.sendPasswordResetEmail(c)
                            .addOnSuccessListener {
                                enviando = false
                                enviado = true
                            }
                            .addOnFailureListener { e ->
                                enviando = false
                                error = when {
                                    e.message?.contains("no user", ignoreCase = true) == true ->
                                        "No hay ninguna cuenta con ese correo."
                                    e.message?.contains("badly formatted", ignoreCase = true) == true ->
                                        "Ese correo no parece tener un formato válido."
                                    else ->
                                        "No se pudo enviar el correo. Revisa tu conexión."
                                }
                            }
                    },
                    enabled = !enviando
                ) {
                    Text(if (enviando) "Enviando…" else "Enviar correo",
                        fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = if (!enviado) {
            { TextButton(onClick = onCerrar) { Text("Cancelar") } }
        } else null
    )
}

/**
 * Valida que una contraseña cumpla con los requisitos mínimos para registro.
 * Devuelve null si está OK, o un mensaje de error si falla.
 *
 * Reglas (estándar de seguridad moderno sin ser excesivo):
 *  - Mínimo 8 caracteres
 *  - Al menos una letra mayúscula
 *  - Al menos un carácter especial (no letra ni dígito)
 */
fun validarFortalezaClave(clave: String): String? {
    if (clave.length < 8) {
        return "Tu contraseña debe tener al menos 8 caracteres."
    }
    if (!clave.any { it.isUpperCase() }) {
        return "Tu contraseña debe incluir al menos una letra mayúscula."
    }
    if (!clave.any { !it.isLetterOrDigit() }) {
        return "Tu contraseña debe incluir al menos un carácter especial (ej. ! @ # $ % & *)."
    }
    return null
}

/**
 * Calcula la fortaleza visual de una contraseña, devolviendo un Triple:
 *  - Float 0..1 (porcentaje de barra)
 *  - String etiqueta ("Débil", "Media", "Fuerte", "Excelente")
 *  - Color para la barra
 */
fun calcularFortalezaClave(clave: String): Triple<Float, String, androidx.compose.ui.graphics.Color> {
    if (clave.isEmpty()) {
        return Triple(0f, "", androidx.compose.ui.graphics.Color.Gray)
    }
    var puntos = 0
    if (clave.length >= 8) puntos++
    if (clave.length >= 12) puntos++
    if (clave.any { it.isUpperCase() }) puntos++
    if (clave.any { it.isLowerCase() }) puntos++
    if (clave.any { it.isDigit() }) puntos++
    if (clave.any { !it.isLetterOrDigit() }) puntos++

    return when (puntos) {
        0, 1, 2 -> Triple(0.33f, "Débil", Rojo)
        3, 4 -> Triple(0.66f, "Media", Dorado)
        5 -> Triple(0.85f, "Fuerte", Verde)
        else -> Triple(1f, "Excelente", Verde)
    }
}
