package com.vaka.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

// Paleta Vaka
val Violeta = Color(0xFF5B3DF5)
val VioletaOscuro = Color(0xFF3D2E7C)
val Rosa = Color(0xFFD9569E)
val Dorado = Color(0xFFFFC53D)
val Verde = Color(0xFF19A974)
val Rojo = Color(0xFFD64545)

const val TEMA_SISTEMA = "sistema"
const val TEMA_CLARO = "claro"
const val TEMA_OSCURO = "oscuro"

/**
 * Estado global para comunicar a la UI a qué pestaña debe ir cuando la app
 * se abre/recibe un nuevo intent desde una notificación.
 * Se consume desde el composable Raiz vía LaunchedEffect.
 */
object NavegacionPendiente {
    var pestanaDestino: String? = null
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notificaciones.crearCanal(this)
        if (Repo.loadNotif(this)) Notificaciones.programar(this)
        // Si la app se abrió DESDE una notificación, guardamos la pestaña destino
        intent?.getStringExtra("pestana_destino")?.let {
            NavegacionPendiente.pestanaDestino = it
        }
        setContent { Raiz() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // La app ya estaba abierta y recibe un nuevo intent (otra notificación)
        intent.getStringExtra("pestana_destino")?.let {
            NavegacionPendiente.pestanaDestino = it
        }
        // Forzar que el composable Raiz re-evalúe el estado
        setIntent(intent)
    }
}

@Composable
fun Raiz() {
    val ctx = LocalContext.current

    // ---- Sesión ----
    var usuario by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth -> usuario = auth.currentUser }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }
    var invitado by rememberSaveable { mutableStateOf(false) }
    // El onboarding se controla con un flag en memoria (`onboardingPendiente`)
    // que se inicializa desde SharedPreferences. Es `true` cuando:
    //  - Es la primera vez que se abre la app, sin sesión Firebase activa
    //  - Un usuario se acaba de registrar (email o Google con isNewUser)
    //
    // Si hay sesión Firebase activa al abrir la app (usuario existente), se
    // marca como visto automáticamente para no mostrárselo "tarde".
    var onboardingPendiente by remember {
        val haySesionFirebase = Firebase.auth.currentUser != null
        val yaSeVio = OnboardingPrefs.yaSeVio(ctx)
        if (haySesionFirebase && !yaSeVio) {
            OnboardingPrefs.marcarComoVisto(ctx)
            mutableStateOf(false)
        } else {
            mutableStateOf(!yaSeVio)
        }
    }
    var nombreEditado by remember { mutableStateOf<String?>(null) }

    // ---- Estado principal ----
    // Migración: si había ahorro fijo legacy con movimientos, lo convertimos
    // en una Vaka normal "Ahorros generales" la primera vez que abramos.
    var locales by remember {
        val migrada = Repo.migrarAhorroFijoSiHaceFalta(ctx, Repo.loadMonedaPrincipal(ctx))
        // Cargar Vakas aplicando aportes recurrentes pendientes
        val cargadas = Repo.aplicarAportesRecurrentesPendientes(ctx)
        if (migrada != null) {
            val mezcladas = cargadas + migrada
            Repo.save(ctx, mezcladas)
            mutableStateOf(mezcladas)
        } else {
            mutableStateOf(cargadas)
        }
    }
    var compartidas by remember { mutableStateOf<List<VakaItem>>(emptyList()) }
    var modoTema by remember { mutableStateOf(Repo.loadModoTema(ctx)) }
    var monedaPrincipal by remember { mutableStateOf(Repo.loadMonedaPrincipal(ctx)) }
    var colorAvatar by remember { mutableStateOf(Repo.loadColorAvatar(ctx)) }
    var pestana by remember { mutableStateOf(Pestana.INICIO) }

    // Si la app fue abierta desde una notificación con destino, ir allí
    LaunchedEffect(Unit) {
        val destino = NavegacionPendiente.pestanaDestino
        if (destino != null) {
            pestana = when (destino) {
                "amigos" -> Pestana.AMIGOS
                "vakas" -> Pestana.PRIVADAS
                "equipo" -> Pestana.COMPARTIDAS
                "perfil", "ajustes" -> Pestana.PERFIL
                else -> Pestana.INICIO
            }
            NavegacionPendiente.pestanaDestino = null  // limpiar para no repetir
        }
    }
    var activaClave by remember { mutableStateOf<String?>(null) }
    var verCuenta by remember { mutableStateOf(false) }
    var celebrar by remember { mutableStateOf(false) }
    var splash by remember { mutableStateOf(!SplashEstado.yaMostrado) }
    var notifOn by remember { mutableStateOf(Repo.loadNotif(ctx)) }
    var estadoGuardado by remember { mutableStateOf(EstadoGuardado.OCULTO) }

    // === Detección de logros nuevos ===
    // Guardamos los IDs de logros ya desbloqueados para detectar cuándo aparece uno nuevo.
    // Empieza vacío; en el primer LaunchedEffect lo poblamos sin disparar diálogo.
    var idsLogrosVistos by remember { mutableStateOf<Set<String>?>(null) }
    var logroNuevo by remember { mutableStateOf<Logro?>(null) }

    // === Avisos internos (banner animado) ===
    var avisoActual by remember { mutableStateOf<AvisoInterno?>(null) }
    fun mostrarAviso(aviso: AvisoInterno) { avisoActual = aviso }

    // Tiempo de arranque: usado para suprimir detecciones falsas durante los
    // primeros segundos mientras Firebase termina de cargar los datos iniciales.
    val tiempoArranque = remember { System.currentTimeMillis() }
    fun yaCargoTodo(): Boolean = System.currentTimeMillis() - tiempoArranque > 3000

    // === Celebración (diálogo a pantalla completa con confetti) ===
    // Tipos: top ganado, top perdido, reto cumplido, reto fallado.
    var celebracionPendiente by remember {
        mutableStateOf<Triple<TipoCelebracion, String, Pair<String, String>>?>(null)
    }

    // Detección del resultado del reto semanal:
    // Al abrir la app, si estamos en una semana nueva respecto a la última evaluada,
    // calculamos cómo le fue al usuario en la semana que acaba de cerrar y
    // mostramos la celebración apropiada.
    LaunchedEffect(locales) {
        val prefs = ctx.getSharedPreferences("vaka_prefs", android.content.Context.MODE_PRIVATE)
        val keyReto = "ultima_semana_reto_evaluada"
        val hoyD = java.time.LocalDate.now()
        val semanaActual = "${hoyD.year}-${hoyD.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())}"
        val ultimaEvaluada = prefs.getString(keyReto, "")
        if (ultimaEvaluada == semanaActual) return@LaunchedEffect
        // Es una nueva semana → evaluar la que acaba de cerrar
        // Solo si hay historial de movimientos (no spamear a usuarios nuevos)
        if (locales.flatMap { it.movs }.isEmpty()) {
            prefs.edit().putString(keyReto, semanaActual).apply()
            return@LaunchedEffect
        }

        // Calcular depósitos de la semana ya cerrada (= semana pasada respecto a hoy)
        // y de la semana anterior a esa (= hace 2 semanas)
        val inicioSemanaCerrada = hoyD.with(java.time.DayOfWeek.MONDAY).minusDays(7)
        val finSemanaCerrada = inicioSemanaCerrada.plusDays(6)
        val inicioSemanaAnterior = inicioSemanaCerrada.minusDays(7)
        val finSemanaAnterior = inicioSemanaCerrada.minusDays(1)

        var depCerrada = 0.0
        var depAnterior = 0.0
        for (v in locales) {
            for (m in v.movs) {
                if (m.tipo != "deposito") continue
                val fechaMov = try {
                    java.time.LocalDate.parse(m.fecha)
                } catch (e: Exception) { continue }
                when {
                    !fechaMov.isBefore(inicioSemanaCerrada) && !fechaMov.isAfter(finSemanaCerrada) ->
                        depCerrada += m.monto
                    !fechaMov.isBefore(inicioSemanaAnterior) && !fechaMov.isAfter(finSemanaAnterior) ->
                        depAnterior += m.monto
                }
            }
        }

        // Decidir el resultado:
        // - Si la semana anterior fue 0, no había nada que superar (no celebramos)
        // - Si la semana cerrada superó a la anterior → cumplido 🎉
        // - Si la semana cerrada no superó (y la anterior tenía algo) → fallado 💪
        if (depAnterior > 0) {
            val monedaSym = locales.firstOrNull()?.monedaSymbol ?: "$"
            val monedaCode = locales.firstOrNull()?.monedaCode ?: "COP"
            val cerradaFmt = formatea(depCerrada, monedaCode, monedaSym)
            val anteriorFmt = formatea(depAnterior, monedaCode, monedaSym)
            celebracionPendiente = if (depCerrada > depAnterior) {
                Triple(
                    TipoCelebracion.RetoCumplido,
                    "🏆",
                    "¡Cumpliste el reto!" to
                        "Ahorraste $cerradaFmt vs $anteriorFmt de la semana anterior. " +
                        "¿Lo repetimos esta semana? 💪"
                )
            } else {
                Triple(
                    TipoCelebracion.RetoFallado,
                    "💪",
                    "No cumpliste el reto" to
                        "Ahorraste $cerradaFmt vs $anteriorFmt de la anterior. " +
                        "Tranquilo, esta semana es una nueva oportunidad 🌱"
                )
            }
        }
        prefs.edit().putString(keyReto, semanaActual).apply()
    }

    LaunchedEffect(estadoGuardado) {
        if (estadoGuardado == EstadoGuardado.GUARDADO) {
            kotlinx.coroutines.delay(1800)
            estadoGuardado = EstadoGuardado.OCULTO
        }
    }

    // ---- Perfil en la nube: código único, amigos y solicitudes ----
    var miPerfil by remember { mutableStateOf<PerfilUsuario?>(null) }
    var amigosUids by remember { mutableStateOf<List<String>>(emptyList()) }
    var solicitudesUids by remember { mutableStateOf<List<String>>(emptyList()) }
    var amigosPerfiles by remember { mutableStateOf<List<PerfilUsuario>>(emptyList()) }
    var miembrosPerfiles by remember { mutableStateOf<List<PerfilUsuario>>(emptyList()) }
    var solicitudesPerfiles by remember { mutableStateOf<List<PerfilUsuario>>(emptyList()) }
    var companerosPerfiles by remember { mutableStateOf<List<PerfilUsuario>>(emptyList()) }

    // Vigila los logros: si aparece uno nuevo, dispara el diálogo de celebración.
    // El cálculo es por VAKA (no en agregado), así si depositas en "Viaje" y desbloqueas
    // un logro de esa Vaka, se celebra. Excluye la Vaka fija "Mis ahorros totales"
    // para no duplicar (porque sus logros se repiten con los de Inicio).
    // Estado en memoria de logros ya ganados (cargados desde SharedPreferences).
    // Una vez desbloqueado, un logro queda guardado aunque la Vaka que lo
    // originó sea borrada después.
    var logrosGanados by remember { mutableStateOf(Repo.loadLogrosGanados(ctx)) }

    LaunchedEffect(locales, compartidas) {
        val vakasParaLogros = locales + compartidas
        // Calculamos qué logros están activos AHORA en las Vakas actuales
        val activosAhora = vakasParaLogros.flatMap { v ->
            logrosDe(v).filter { it.ok }.map { it.id }
        }.toSet()

        val previos = idsLogrosVistos
        // Durante los primeros 3 segundos solo memorizamos sin celebrar
        if (!yaCargoTodo() || previos == null) {
            // Pero SÍ guardamos los nuevos logros para no perderlos
            val nuevos = activosAhora - logrosGanados
            if (nuevos.isNotEmpty()) {
                val mergeados = logrosGanados + nuevos
                logrosGanados = mergeados
                Repo.saveLogrosGanados(ctx, mergeados)
            }
            // El "Vistos" sigue siendo por Vaka (para detección)
            idsLogrosVistos = vakasParaLogros.flatMap { v ->
                logrosDe(v).filter { it.ok }.map { "${v.id}_${it.id}" }
            }.toSet()
            return@LaunchedEffect
        }

        val nuevosIds = vakasParaLogros.flatMap { v ->
            logrosDe(v).filter { it.ok }.map { "${v.id}_${it.id}" }
        }.toSet()

        if (nuevosIds.size > previos.size) {
            // Hay un logro nuevo: encuentra cuál
            val diff = nuevosIds - previos
            val claveNueva = diff.firstOrNull()
            if (claveNueva != null) {
                val partes = claveNueva.split("_", limit = 2)
                if (partes.size == 2) {
                    val idVaka = partes[0].toLongOrNull()
                    val idLogro = partes[1]
                    val vaka = vakasParaLogros.find { it.id == idVaka }
                    val logro = vaka?.let { logrosDe(it).find { l -> l.id == idLogro } }
                    if (logro != null && idLogro !in logrosGanados) {
                        logroNuevo = logro
                    }
                }
            }
        }
        idsLogrosVistos = nuevosIds

        // Persistir cualquier logro nuevo en SharedPreferences
        val logrosNuevos = activosAhora - logrosGanados
        if (logrosNuevos.isNotEmpty()) {
            val mergeados = logrosGanados + logrosNuevos
            logrosGanados = mergeados
            Repo.saveLogrosGanados(ctx, mergeados)
        }
    }

    // ---- Permiso de notificaciones (Android 13+) ----
    val permisoNotif = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            notifOn = true
            Repo.saveNotif(ctx, true)
            Notificaciones.programar(ctx)
        } else {
            notifOn = false
            Repo.saveNotif(ctx, false)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !Repo.permisoNotifSolicitado(ctx)) {
            Repo.marcarPermisoNotifSolicitado(ctx)
            val concedido = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!concedido) permisoNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ---- Tasas ----
    var tabla by remember { mutableStateOf<TablaTasas?>(null) }
    LaunchedEffect(Unit) { tabla = Tasas.obtener(ctx) }

    // ---- Suscripción a Vakas compartidas ----
    // ---- Suscripción a Vakas compartidas + detección de movimientos nuevos ----
    var compartidasAnteriores by remember { mutableStateOf<List<VakaItem>?>(null) }
    DisposableEffect(usuario?.uid) {
        val uid = usuario?.uid
        val reg = uid?.let {
            Nube.escuchar(uid) { lista ->
                // Detectar movimientos nuevos de compañeros (no míos)
                val anterior = compartidasAnteriores
                if (anterior != null && !splash) {
                    lista.forEach { vakaNueva ->
                        val vakaVieja = anterior.find { it.codigo == vakaNueva.codigo }
                        if (vakaVieja != null) {
                            // Movimientos que existen ahora pero no antes
                            val idsViejos = vakaVieja.movs.map { it.id }.toSet()
                            val nuevos = vakaNueva.movs.filter {
                                it.id !in idsViejos && it.autorUid != uid && it.autorUid.isNotBlank()
                            }
                            nuevos.forEach { m ->
                                Notificaciones.notificarMovimientoCompanero(
                                    ctx,
                                    nombreVaka = vakaNueva.nombre,
                                    autorNombre = m.autorNombre,
                                    tipo = m.tipo,
                                    monto = m.monto,
                                    monedaCode = vakaNueva.monedaCode,
                                    monedaSymbol = vakaNueva.monedaSymbol,
                                    idMov = m.id
                                )
                            }
                        }
                    }
                    // Comparación motivadora: si alguien te lleva ventaja
                    lista.forEach { v ->
                        if (v.meta > 0 && v.miembros.size >= 2 && v.codigo != null) {
                            val miParte = v.miembros.find { it.uid == uid }
                                ?.compromiso?.takeIf { it > 0 } ?: (v.meta / v.miembros.size)
                            val miAporte = v.aportadoPor(uid)
                            val miPct = (miAporte / miParte * 100).toInt().coerceAtLeast(0)
                            // Encontrar al miembro líder (sin ser yo)
                            val lider = v.miembros
                                .filter { it.uid != uid }
                                .maxByOrNull {
                                    val sp = it.compromiso.takeIf { c -> c > 0 } ?: (v.meta / v.miembros.size)
                                    v.aportadoPor(it.uid) / sp
                                }
                            if (lider != null) {
                                val sp = lider.compromiso.takeIf { c -> c > 0 } ?: (v.meta / v.miembros.size)
                                val liderPct = (v.aportadoPor(lider.uid) / sp * 100).toInt()
                                val diff = liderPct - miPct
                                // Solo si me lleva 20% o más de ventaja y no he completado mi parte
                                if (diff >= 20 && miPct < 100) {
                                    Notificaciones.notificarComparacionEquipo(
                                        ctx,
                                        nombreVaka = v.nombre,
                                        nombreLider = lider.nombre,
                                        diferenciaPct = diff,
                                        codigoVaka = v.codigo!!
                                    )
                                }
                            }
                        }
                    }

                    // Cambios en el TOP del ranking de aportes (por Vaka compartida)
                    lista.forEach { vakaNueva ->
                        val vakaVieja = anterior.find { it.codigo == vakaNueva.codigo }
                            ?: return@forEach
                        if (vakaNueva.miembros.size < 2) return@forEach

                        fun aportesDe(v: VakaItem) = v.miembros.map { mi ->
                            mi.uid to v.movs
                                .filter { it.autorUid == mi.uid && it.tipo == "deposito" }
                                .sumOf { it.monto }
                        }.sortedByDescending { it.second }

                        val rankingAntes = aportesDe(vakaVieja)
                        val rankingAhora = aportesDe(vakaNueva)
                        val miPosAntes = rankingAntes.indexOfFirst { it.first == uid }
                        val miPosAhora = rankingAhora.indexOfFirst { it.first == uid }

                        // Solo notificamos si participo y mi posición cambió,
                        // y si tengo al menos algún aporte (no notificar a fantasmas)
                        if (miPosAntes < 0 || miPosAhora < 0) return@forEach
                        if (miPosAntes == miPosAhora) return@forEach
                        val miAporteAhora = rankingAhora.firstOrNull { it.first == uid }
                            ?.second ?: 0.0
                        if (miAporteAhora <= 0) return@forEach
                        // El top también debe tener algún aporte real: si todos
                        // están en cero, no hay ranking que valga, son solo
                        // reordenamientos del array de miembros.
                        val topAporte = rankingAhora.firstOrNull()?.second ?: 0.0
                        if (topAporte <= 0) return@forEach
                        // Y por seguridad, requerimos cierta diferencia entre el #1 y el #2
                        // para no notificar oscilaciones por centavos
                        val segundoAporte = rankingAhora.getOrNull(1)?.second ?: 0.0
                        if (topAporte - segundoAporte < 1.0 && miPosAhora == 0) return@forEach

                        // ¡Pasé al primer lugar!
                        if (miPosAhora == 0 && miPosAntes > 0) {
                            Notificaciones.notificarTopVaka(
                                ctx,
                                nombreVaka = vakaNueva.nombre,
                                tipo = "subio_a_1",
                                otroNombre = "",
                                codigoVaka = vakaNueva.codigo ?: ""
                            )
                            celebracionPendiente = Triple(
                                TipoCelebracion.NuevoLider,
                                "👑",
                                "¡Eres el #1!" to "Lideras los aportes de \"${vakaNueva.nombre}\". ¡Sigue así, máquina! 🔥"
                            )
                        }
                        // Me destronaron del primer lugar
                        else if (miPosAntes == 0 && miPosAhora > 0) {
                            val nuevoLiderUid = rankingAhora.firstOrNull()?.first ?: ""
                            val nombreLider = vakaNueva.miembros
                                .find { it.uid == nuevoLiderUid }?.nombre ?: "alguien"
                            Notificaciones.notificarTopVaka(
                                ctx,
                                nombreVaka = vakaNueva.nombre,
                                tipo = "perdio_1",
                                otroNombre = nombreLider,
                                codigoVaka = vakaNueva.codigo ?: ""
                            )
                            celebracionPendiente = Triple(
                                TipoCelebracion.Destronado,
                                "🥈",
                                "$nombreLider te pasó" to "Quedaste segundo en \"${vakaNueva.nombre}\". ¿Te vas a quedar así? 😏"
                            )
                        }
                    }
                }
                compartidas = lista
                compartidasAnteriores = lista
                Repo.saveResumenCompartidas(ctx, lista)
            }
        }
        onDispose { reg?.remove() }
    }

    // ---- Suscripción al perfil (código único, amigos, solicitudes) ----
    DisposableEffect(usuario?.uid) {
        val reg = usuario?.uid?.let { uid ->
            Nube.escucharPerfil(uid) { perfil, amigos, solics ->
                miPerfil = perfil
                amigosUids = amigos
                solicitudesUids = solics
            }
        }
        onDispose { reg?.remove() }
    }

    // ---- Resolver amigos, solicitudes y compañeros a perfiles completos ----
    // Importante: NO eliminamos automáticamente "amigos fantasma" aquí.
    // El listener del perfil puede activarse antes de que cargarUsuarios responda,
    // y eso causaba que amistades recién aceptadas se autoeliminaran.
    // Si un amigo eliminó su cuenta, queda como UID huérfano sin perfil visible;
    // el usuario puede borrarlo manualmente con el botón 🗑.
    LaunchedEffect(amigosUids) {
        Nube.cargarUsuarios(amigosUids) { perfiles ->
            amigosPerfiles = perfiles
        }
    }

    // Detección de "alguien aceptó mi solicitud": vigilamos amigosPerfiles para
    // descubrir incorporaciones nuevas. Durante los primeros 3 segundos solo
    // memorizamos, no celebramos (evita falsos positivos al cargar datos).
    var amigosUidsVistos by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(amigosPerfiles) {
        val actualesUids = amigosPerfiles.map { it.uid }.toSet()
        val previos = amigosUidsVistos
        // Si aún estamos en la fase de carga inicial, solo memorizar
        if (!yaCargoTodo() || previos == null) {
            amigosUidsVistos = actualesUids
            return@LaunchedEffect
        }
        val nuevos = actualesUids - previos
        if (nuevos.isNotEmpty()) {
            val perfilNuevo = amigosPerfiles.firstOrNull { it.uid in nuevos }
            if (perfilNuevo != null) {
                avisoActual = AvisoInterno(
                    tipo = TipoAviso.ACEPTADO,
                    titulo = "¡${perfilNuevo.nombre} aceptó tu solicitud!",
                    texto = "Ya son amigos en Vaka 🎉"
                )
            }
        }
        amigosUidsVistos = actualesUids
    }

    // Detección de "alguien rechazó mi solicitud": vigilamos solicitudesEnviadas
    // del propio perfil. Mismo filtro de arranque.
    var enviadasVistas by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(miPerfil, amigosUids) {
        val perfil = miPerfil ?: return@LaunchedEffect
        val enviadas = perfil.solicitudesEnviadas.toSet()
        val previas = enviadasVistas
        if (!yaCargoTodo() || previas == null) {
            enviadasVistas = enviadas
            return@LaunchedEffect
        }
        val desaparecidas = previas - enviadas
        val rechazadas = desaparecidas - amigosUids.toSet()
        val primera = rechazadas.firstOrNull()
        if (primera != null) {
            val perfilRechazo = (amigosPerfiles + companerosPerfiles + miembrosPerfiles)
                .firstOrNull { it.uid == primera }
            avisoActual = AvisoInterno(
                tipo = TipoAviso.RECHAZADO,
                titulo = if (perfilRechazo != null)
                    "${perfilRechazo.nombre} no aceptó tu solicitud"
                else
                    "Una solicitud no fue aceptada",
                texto = "No te preocupes, hay muchas Vakas en el mar 🐄",
                duracionMs = 4000L
            )
        }
        enviadasVistas = enviadas
    }

    // Cargar perfiles de miembros de Vakas compartidas (para mostrar sus fotos)
    val uidsMiembros = remember(compartidas, usuario?.uid) {
        compartidas.flatMap { it.miembros.map { m -> m.uid } }
            .filter { it.isNotBlank() && it != usuario?.uid }
            .distinct()
    }
    LaunchedEffect(uidsMiembros) {
        Nube.cargarUsuarios(uidsMiembros) { perfiles ->
            miembrosPerfiles = perfiles
        }
    }
    LaunchedEffect(solicitudesUids) {
        Nube.cargarUsuarios(solicitudesUids) { perfiles ->
            solicitudesPerfiles = perfiles
        }
    }

    // Detección de "alguien me envió una solicitud de amistad nueva":
    // dispara notificación del sistema + banner interno si la app está abierta.
    var solicitudesUidsVistas by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(solicitudesPerfiles) {
        val actuales = solicitudesPerfiles.map { it.uid }.toSet()
        val previas = solicitudesUidsVistas
        // Durante los 3 primeros segundos solo memorizar, no spamear
        if (!yaCargoTodo() || previas == null) {
            solicitudesUidsVistas = actuales
            return@LaunchedEffect
        }
        val nuevas = actuales - previas
        // Para cada solicitud nueva, mostrar notificación del sistema
        nuevas.forEach { uidNuevo ->
            val perfil = solicitudesPerfiles.firstOrNull { it.uid == uidNuevo }
            if (perfil != null) {
                Notificaciones.notificarSolicitudAmistad(
                    ctx = ctx,
                    nombreEmisor = perfil.nombre,
                    uidEmisor = perfil.uid
                )
            }
        }
        // Si hay UNA nueva y la app está abierta, mostrar banner adicional
        if (nuevas.size == 1) {
            val perfil = solicitudesPerfiles.firstOrNull { it.uid == nuevas.first() }
            if (perfil != null) {
                avisoActual = AvisoInterno(
                    tipo = TipoAviso.INFO,
                    titulo = "🤝 ${perfil.nombre} quiere ser tu amigo",
                    texto = "Ve a la pestaña Amigos para responder",
                    duracionMs = 4000L
                )
            }
        } else if (nuevas.size > 1) {
            avisoActual = AvisoInterno(
                tipo = TipoAviso.INFO,
                titulo = "🤝 Tienes ${nuevas.size} solicitudes nuevas",
                texto = "Ve a la pestaña Amigos para responder",
                duracionMs = 4000L
            )
        }
        solicitudesUidsVistas = actuales
    }
    val miUid = usuario?.uid
    LaunchedEffect(compartidas, miUid) {
        if (miUid != null) {
            val uids = compartidas.flatMap { it.miembros }.map { it.uid }
                .filter { it != miUid }.distinct()
            Nube.cargarUsuarios(uids) { companerosPerfiles = it }
        } else {
            companerosPerfiles = emptyList()
        }
    }

    // ---- Mantener el perfil público (nombre, correo, color) sincronizado en Firestore ----
    LaunchedEffect(usuario?.uid, nombreEditado, colorAvatar) {
        val u = usuario
        if (u != null) {
            val nombre = nombreEditado
                ?: u.displayName?.takeIf { it.isNotBlank() }
                ?: u.email?.substringBefore("@") ?: "Yo"
            val correo = u.email ?: ""
            Nube.guardarPerfil(u.uid, nombre, correo, colorAvatar)
        }
    }

    fun persistirLocales(lista: List<VakaItem>) {
        locales = lista
        Repo.save(ctx, lista)
    }

    val miNombre = nombreEditado
        ?: usuario?.displayName?.takeIf { it.isNotBlank() }
        ?: usuario?.email?.substringBefore("@") ?: "Yo"

    // ---- Tema con transición animada ----
    val sistemaOscuro = androidx.compose.foundation.isSystemInDarkTheme()
    val dark = when (modoTema) {
        TEMA_CLARO -> false
        TEMA_OSCURO -> true
        else -> sistemaOscuro
    }
    val colorBase = colorAvatarPor(colorAvatar)
    // Para tema oscuro, suavizamos el color (más claro/desaturado)
    val colorOscuro = Color(
        red = (colorBase.red * 0.55f + 0.45f).coerceIn(0f, 1f),
        green = (colorBase.green * 0.55f + 0.45f).coerceIn(0f, 1f),
        blue = (colorBase.blue * 0.55f + 0.45f).coerceIn(0f, 1f)
    )
    val esquemaBase = if (dark) darkColorScheme(
        primary = colorOscuro,
        background = Color(0xFF15112A),
        surface = Color(0xFF221C3F),
        onBackground = Color(0xFFF1EEFA),
        onSurface = Color(0xFFF1EEFA),
        surfaceVariant = Color(0xFF322A58),
        onSurfaceVariant = Color(0xFFA89FC9),
    ) else lightColorScheme(
        primary = colorBase,
        background = Color(0xFFF7F5FB),
        surface = Color.White,
        onBackground = Color(0xFF2A2140),
        onSurface = Color(0xFF2A2140),
        surfaceVariant = Color(0xFFEFEBFB),
        onSurfaceVariant = Color(0xFF8A82A6),
    )
    val anim = tween<Color>(durationMillis = 450)
    val esquema = esquemaBase.copy(
        background = animateColorAsState(esquemaBase.background, anim, label = "bg").value,
        surface = animateColorAsState(esquemaBase.surface, anim, label = "sur").value,
        surfaceVariant = animateColorAsState(esquemaBase.surfaceVariant, anim, label = "sv").value,
        onBackground = animateColorAsState(esquemaBase.onBackground, anim, label = "ob").value,
        onSurface = animateColorAsState(esquemaBase.onSurface, anim, label = "os").value,
        onSurfaceVariant = animateColorAsState(esquemaBase.onSurfaceVariant, anim, label = "osv").value,
        primary = animateColorAsState(esquemaBase.primary, anim, label = "pr").value,
    )

    MaterialTheme(colorScheme = esquema) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {

                val todasParaSelect = locales + compartidas
                val activa = todasParaSelect.find { claveDe(it) == activaClave }
                var vakaRecuerdo by remember { mutableStateOf<VakaItem?>(null) }
                LaunchedEffect(activa) { if (activa != null) vakaRecuerdo = activa }

                fun actualizar(v: VakaItem) {
                    val antes = todasParaSelect.find { claveDe(it) == claveDe(v) }?.progreso ?: 0f
                    when {
                        v.esCompartida -> {
                            compartidas = compartidas.map { if (it.codigo == v.codigo) v else it }
                            estadoGuardado = EstadoGuardado.GUARDANDO
                            Nube.guardar(v) { ok ->
                                estadoGuardado = if (ok) EstadoGuardado.GUARDADO else EstadoGuardado.ERROR
                            }
                        }
                        else -> persistirLocales(locales.map { if (it.id == v.id) v else it })
                    }
                    if (antes < 1f && (v.progreso ?: 0f) >= 1f) celebrar = true
                }

                fun eliminar(v: VakaItem) {
                    when {
                        v.esFija -> { /* no se puede eliminar la Vaka fija */ }
                        v.esCompartida -> {
                            miUid?.let { Nube.salir(v.codigo!!, it) }
                            compartidas = compartidas.filter { it.codigo != v.codigo }
                        }
                        else -> persistirLocales(locales.filter { it.id != v.id })
                    }
                    if (activaClave == claveDe(v)) activaClave = null
                }

                // ---- BackHandler: navega hacia atrás en vez de salir ----
                BackHandler(
                    enabled = !splash && (
                        verCuenta || activaClave != null ||
                            pestana != Pestana.INICIO
                    )
                ) {
                    when {
                        verCuenta -> verCuenta = false
                        activaClave != null -> activaClave = null
                        pestana != Pestana.INICIO -> pestana = Pestana.INICIO
                    }
                }

                val destino = when {
                    splash -> "splash"
                    // Onboarding tiene PRIORIDAD: si está pendiente, se muestra
                    // antes que cualquier otra pantalla (excepto el splash).
                    onboardingPendiente -> "onboarding"
                    usuario == null && !invitado -> "auth"
                    verCuenta -> "cuenta"
                    activaClave != null -> "detalle"
                    else -> "tabs"
                }

                if (destino == "splash") {
                    SplashScreen(onFin = {
                        SplashEstado.yaMostrado = true
                        splash = false
                    })
                } else {
                    AnimatedContent(
                        targetState = destino,
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                        transitionSpec = {
                            (slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally(tween(240)) { -it / 4 } + fadeOut(tween(240)))
                        },
                        label = "pantallas"
                    ) { pantalla ->
                        when (pantalla) {
                            "onboarding" -> PantallaOnboarding(
                                onTerminado = {
                                    OnboardingPrefs.marcarComoVisto(ctx)
                                    onboardingPendiente = false
                                }
                            )

                            "auth" -> AuthScreen(
                                onInvitado = { invitado = true },
                                onRegistro = {
                                    // Solo mostramos onboarding al registrarse SI no lo
                                    // ha visto nunca (caso edge: usuario que entró como
                                    // invitado primero y después se registra).
                                    // Si ya lo vio (la mayoría de casos), no se vuelve
                                    // a mostrar — el onboarding es UNA vez por instalación.
                                    if (!OnboardingPrefs.yaSeVio(ctx)) {
                                        OnboardingPrefs.marcarComoNoVisto(ctx)
                                        onboardingPendiente = true
                                    }
                                }
                            )

                            "cuenta" -> {
                                // Detectar si el usuario entró con Google
                                val esGoogle = usuario?.providerData?.any {
                                    it.providerId == "google.com"
                                } ?: false

                                CuentaScreen(
                                correo = usuario?.email ?: "",
                                nombre = miNombre,
                                colorAvatar = colorAvatar,
                                fotoBase64 = miPerfil?.fotoBase64 ?: "",
                                esCuentaGoogle = esGoogle,
                                onCambiarColor = { id ->
                                    colorAvatar = id
                                    Repo.saveColorAvatar(ctx, id)
                                },
                                onCambiarFoto = { nuevaFoto ->
                                    usuario?.uid?.let { uid ->
                                        Nube.guardarFotoPerfil(uid, nuevaFoto)
                                    }
                                },
                                onGuardarNombre = { nuevo ->
                                    nombreEditado = nuevo
                                    usuario?.updateProfile(
                                        UserProfileChangeRequest.Builder()
                                            .setDisplayName(nuevo).build()
                                    )
                                },
                                onCambiarCorreo = { nuevoCorreo, clave, ok, error ->
                                    val u = usuario
                                    val correoActual = u?.email
                                    if (u == null || correoActual.isNullOrBlank()) {
                                        error("No hay sesión activa.")
                                    } else {
                                        val cred = EmailAuthProvider.getCredential(correoActual, clave)
                                        u.reauthenticate(cred)
                                            .addOnSuccessListener {
                                                u.verifyBeforeUpdateEmail(nuevoCorreo)
                                                    .addOnSuccessListener { ok() }
                                                    .addOnFailureListener { e ->
                                                        error(e.localizedMessage ?: "No se pudo cambiar el correo.")
                                                    }
                                            }
                                            .addOnFailureListener { error("Contraseña incorrecta.") }
                                    }
                                },
                                onCambiarClave = { actual, nueva, ok, error ->
                                    val u = usuario
                                    val correoActual = u?.email
                                    if (u == null || correoActual.isNullOrBlank()) {
                                        error("No hay sesión activa.")
                                    } else {
                                        val cred = EmailAuthProvider.getCredential(correoActual, actual)
                                        u.reauthenticate(cred)
                                            .addOnSuccessListener {
                                                u.updatePassword(nueva)
                                                    .addOnSuccessListener { ok() }
                                                    .addOnFailureListener { e ->
                                                        error(e.localizedMessage ?: "No se pudo cambiar la contraseña.")
                                                    }
                                            }
                                            .addOnFailureListener { error("Contraseña actual incorrecta.") }
                                    }
                                },
                                onBorrarCuenta = { clave, ok, error ->
                                    val u = usuario
                                    val correoActual = u?.email
                                    val uid = u?.uid
                                    if (u == null || correoActual.isNullOrBlank() || uid == null) {
                                        error("No hay sesión activa.")
                                    } else {
                                        val cred = EmailAuthProvider.getCredential(correoActual, clave)
                                        u.reauthenticate(cred)
                                            .addOnSuccessListener {
                                                Nube.borrarTodoDelUsuario(uid) {
                                                    u.delete()
                                                        .addOnSuccessListener {
                                                            Repo.borrarTodoLocal(ctx)
                                                            invitado = false
                                                            verCuenta = false
                                                            locales = emptyList()
                                                            compartidas = emptyList()
                                                            colorAvatar = "violeta"
                                                            monedaPrincipal = "COP"
                                                            activaClave = null
                                                            nombreEditado = null
                                                            logrosGanados = emptySet()
                                                            ok()
                                                        }
                                                        .addOnFailureListener { e ->
                                                            error(e.localizedMessage ?: "No se pudo eliminar la cuenta.")
                                                        }
                                                }
                                            }
                                            .addOnFailureListener { error("Contraseña incorrecta.") }
                                    }
                                },
                                onBorrarCuentaGoogle = { ok, error ->
                                    val u = usuario
                                    val uid = u?.uid
                                    val act = ctx as? android.app.Activity
                                    if (u == null || uid == null || act == null) {
                                        error("No hay sesión activa.")
                                    } else {
                                        // Re-autenticación con Google: abre el selector de cuentas
                                        val resId = ctx.resources.getIdentifier(
                                            "default_web_client_id", "string", ctx.packageName
                                        )
                                        if (resId == 0) {
                                            error("Google Sign-In no está configurado.")
                                        } else {
                                            val clientId = ctx.getString(resId)
                                            val opcion = com.google.android.libraries.identity.googleid
                                                .GetSignInWithGoogleOption.Builder(clientId).build()
                                            val solicitud = androidx.credentials.GetCredentialRequest.Builder()
                                                .addCredentialOption(opcion).build()
                                            val cm = androidx.credentials.CredentialManager.create(ctx)
                                            kotlinx.coroutines.GlobalScope.launch(
                                                kotlinx.coroutines.Dispatchers.Main
                                            ) {
                                                try {
                                                    val resp = cm.getCredential(act, solicitud)
                                                    val cr = resp.credential
                                                    if (cr is androidx.credentials.CustomCredential &&
                                                        cr.type == com.google.android.libraries.identity.googleid
                                                            .GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                                    ) {
                                                        val gCred = com.google.android.libraries.identity.googleid
                                                            .GoogleIdTokenCredential.createFrom(cr.data)
                                                        val fbCred = com.google.firebase.auth.GoogleAuthProvider
                                                            .getCredential(gCred.idToken, null)
                                                        u.reauthenticate(fbCred)
                                                            .addOnSuccessListener {
                                                                Nube.borrarTodoDelUsuario(uid) {
                                                                    u.delete()
                                                                        .addOnSuccessListener {
                                                                            Repo.borrarTodoLocal(ctx)
                                                                            invitado = false
                                                                            verCuenta = false
                                                                            locales = emptyList()
                                                                            compartidas = emptyList()
                                                                            colorAvatar = "violeta"
                                                                            monedaPrincipal = "COP"
                                                                            activaClave = null
                                                                            nombreEditado = null
                                                                            logrosGanados = emptySet()
                                                                            ok()
                                                                        }
                                                                        .addOnFailureListener { e ->
                                                                            error(e.localizedMessage
                                                                                ?: "No se pudo eliminar la cuenta.")
                                                                        }
                                                                }
                                                            }
                                                            .addOnFailureListener { e ->
                                                                error("Re-autenticación fallida: " +
                                                                    (e.localizedMessage ?: ""))
                                                            }
                                                    } else {
                                                        error("Credencial inesperada.")
                                                    }
                                                } catch (e: Exception) {
                                                    error("Cancelado o error: ${e.localizedMessage ?: ""}")
                                                }
                                            }
                                        }
                                    }
                                },
                                onBack = { verCuenta = false }
                            )
                            }

                            "detalle" -> {
                                val v = activa ?: vakaRecuerdo
                                if (v != null) {
                                    // Mapa con todos los perfiles disponibles (incluye el mío)
                                    val mapaPerfiles = remember(miPerfil, amigosPerfiles, miembrosPerfiles) {
                                        val m = mutableMapOf<String, PerfilUsuario>()
                                        miPerfil?.let { m[it.uid] = it }
                                        amigosPerfiles.forEach { m[it.uid] = it }
                                        miembrosPerfiles.forEach { m[it.uid] = it }
                                        m
                                    }
                                    DetailScreen(
                                        vaka = v, tabla = tabla,
                                        miUid = miUid, miNombre = miNombre,
                                        perfilesPorUid = mapaPerfiles,
                                        otrasVakas = (locales + compartidas).filter { claveDe(it) != claveDe(v) },
                                        onBack = { activaClave = null },
                                        onUpdate = { actualizar(it) },
                                        onCompartir = {
                                            val uid = miUid
                                            if (uid != null && !v.esCompartida && !v.esFija) {
                                                Nube.crear(
                                                    v, uid, miNombre,
                                                    onOk = { codigo ->
                                                        persistirLocales(locales.filter { it.id != v.id })
                                                        activaClave = "C:$codigo"
                                                    },
                                                    onError = { }
                                                )
                                            }
                                        },
                                        onCompromiso = { monto ->
                                            val uid = miUid
                                            val cod = v.codigo
                                            if (uid != null && cod != null) {
                                                compartidas = compartidas.map { vk ->
                                                    if (vk.codigo == cod) vk.copy(miembros = vk.miembros.map { m ->
                                                        if (m.uid == uid) m.copy(compromiso = monto) else m
                                                    }) else vk
                                                }
                                                Nube.setCompromiso(cod, uid, monto)
                                            }
                                        },
                                        onAplicarEquitativo = { vakaActual, parte ->
                                            val cod = vakaActual.codigo
                                            if (cod != null) {
                                                // Actualizar localmente el estado
                                                compartidas = compartidas.map { vk ->
                                                    if (vk.codigo == cod)
                                                        vk.copy(miembros = vk.miembros.map { it.copy(compromiso = parte) })
                                                    else vk
                                                }
                                                // Y en la nube, para cada miembro
                                                vakaActual.miembros.forEach { m ->
                                                    Nube.setCompromiso(cod, m.uid, parte)
                                                }
                                            }
                                        },
                                        onEliminar = { eliminar(v) },
                                        onMover = { origen, destino, monto, nota ->
                                            // Crea un retiro en la origen y un depósito en la destino,
                                            // ambos con la nota para que se puedan asociar.
                                            val ahora = hoy()
                                            val idBase = System.currentTimeMillis()
                                            val movRetiro = Mov(
                                                id = idBase,
                                                tipo = "retiro",
                                                monto = monto,
                                                nota = "→ Movido a ${destino.nombre}${if (nota.isNotBlank()) ": $nota" else ""}",
                                                fecha = ahora
                                            )
                                            val movDeposito = Mov(
                                                id = idBase + 1,
                                                tipo = "deposito",
                                                monto = monto,
                                                nota = "← Movido de ${origen.nombre}${if (nota.isNotBlank()) ": $nota" else ""}",
                                                fecha = ahora
                                            )
                                            actualizar(origen.copy(movs = listOf(movRetiro) + origen.movs))
                                            actualizar(destino.copy(movs = listOf(movDeposito) + destino.movs))
                                        },
                                        puedeCompartir = miUid != null && !v.esFija
                                    )
                                }
                            }

                            else -> Column(Modifier.fillMaxSize()) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .pointerInput(Unit) {
                                            // Detector de swipe horizontal para cambiar de pestaña.
                                            // Acumulamos el arrastre y solo cambiamos al final del
                                            // gesto si supera el umbral.
                                            var arrastreX = 0f
                                            detectHorizontalDragGestures(
                                                onDragStart = { arrastreX = 0f },
                                                onDragEnd = {
                                                    val umbral = 100f
                                                    if (kotlin.math.abs(arrastreX) > umbral) {
                                                        val pestanas = Pestana.values()
                                                        val idx = pestanas.indexOf(pestana)
                                                        if (arrastreX < 0 && idx < pestanas.lastIndex) {
                                                            pestana = pestanas[idx + 1]
                                                        } else if (arrastreX > 0 && idx > 0) {
                                                            pestana = pestanas[idx - 1]
                                                        }
                                                    }
                                                },
                                                onDragCancel = { arrastreX = 0f },
                                                onHorizontalDrag = { _, dx ->
                                                    arrastreX += dx
                                                }
                                            )
                                        }
                                ) {
                                    AnimatedContent(
                                        targetState = pestana,
                                        transitionSpec = {
                                            // Animación de slide horizontal cuando cambias por gesto o tap
                                            val anterior = initialState.ordinal
                                            val actual = targetState.ordinal
                                            if (actual > anterior) {
                                                (slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(220))) togetherWith
                                                    (slideOutHorizontally(tween(280)) { -it / 3 } + fadeOut(tween(180)))
                                            } else {
                                                (slideInHorizontally(tween(280)) { -it / 3 } + fadeIn(tween(220))) togetherWith
                                                    (slideOutHorizontally(tween(280)) { it / 3 } + fadeOut(tween(180)))
                                            }
                                        },
                                        label = "pestanas"
                                    ) { p ->
                                        when (p) {
                                            Pestana.INICIO -> TabInicio(
                                                nombreUsuario = if (usuario != null) miNombre else null,
                                                colorAvatar = colorAvatar,
                                                vakasPrivadas = locales,
                                                vakasCompartidas = compartidas,
                                                logrosGanados = logrosGanados,
                                                monedaPrincipal = monedaPrincipal,
                                                onAbrirVaka = { v -> activaClave = claveDe(v) },
                                                onIrAVakas = { pestana = Pestana.PRIVADAS }
                                            )
                                            Pestana.PRIVADAS -> TabPrivadas(
                                                privadas = locales,
                                                monedaPrincipal = monedaPrincipal,
                                                amigosDisponibles = amigosPerfiles,
                                                puedeCompartir = usuario != null,
                                                onAbrir = { activaClave = claveDe(it) },
                                                onCrear = { datos ->
                                                    if (datos.compartir && miUid != null) {
                                                        // Crear directamente como compartida en Firebase
                                                        val vakaTemporal = VakaItem(
                                                            id = System.currentTimeMillis(),
                                                            nombre = datos.nombre,
                                                            emoji = datos.emoji,
                                                            monedaCode = datos.moneda.code,
                                                            monedaSymbol = datos.moneda.symbol,
                                                            meta = datos.meta,
                                                            metaFecha = datos.fecha,
                                                            metaDesde = if (datos.meta > 0 && datos.fecha != null) hoy() else null,
                                                            movs = emptyList()
                                                        )
                                                        Nube.crear(
                                                            vakaTemporal, miUid!!, miNombre,
                                                            onOk = { codigo ->
                                                                // Invitar a los amigos seleccionados
                                                                datos.amigosInvitados.forEach { amigo ->
                                                                    Nube.invitarAVaka(
                                                                        codigo, amigo,
                                                                        onOk = { },
                                                                        onError = { }
                                                                    )
                                                                }
                                                                // Abrir la Vaka recién creada
                                                                activaClave = "C:$codigo"
                                                            },
                                                            onError = { }
                                                        )
                                                    } else {
                                                        // Crear como Vaka privada local (comportamiento original)
                                                        persistirLocales(
                                                            locales + VakaItem(
                                                                id = System.currentTimeMillis(),
                                                                nombre = datos.nombre,
                                                                emoji = datos.emoji,
                                                                monedaCode = datos.moneda.code,
                                                                monedaSymbol = datos.moneda.symbol,
                                                                meta = datos.meta,
                                                                metaFecha = datos.fecha,
                                                                metaDesde = if (datos.meta > 0 && datos.fecha != null) hoy() else null,
                                                                movs = emptyList()
                                                            )
                                                        )
                                                    }
                                                },
                                                onEliminar = { eliminar(it) }
                                            )
                                            Pestana.COMPARTIDAS -> TabCompartidas(
                                                compartidas = compartidas,
                                                nombreUsuario = if (usuario != null) miNombre else null,
                                                onAbrir = { activaClave = claveDe(it) },
                                                onUnirse = { codigo, onError ->
                                                    val uid = miUid
                                                    if (uid == null) onError("Inicia sesión para unirte.")
                                                    else Nube.unirse(
                                                        codigo, uid, miNombre,
                                                        onOk = { activaClave = "C:$codigo" },
                                                        onError = onError
                                                    )
                                                },
                                                onSalir = { eliminar(it) },
                                                onIrALogin = { invitado = false }
                                            )
                                            Pestana.AMIGOS -> TabAmigos(
                                                nombreUsuario = if (usuario != null) miNombre else null,
                                                miCodigoUsuario = miPerfil?.codigo ?: "",
                                                amigos = amigosPerfiles,
                                                solicitudes = solicitudesPerfiles,
                                                companeros = companerosPerfiles,
                                                misVakasPrivadas = locales,
                                                misVakasCompartidas = compartidas,
                                                onAgregarPorCodigo = { codigo, ok, error ->
                                                    val uid = miUid
                                                    if (uid == null) {
                                                        error("Inicia sesión primero.")
                                                    } else {
                                                        Nube.buscarPorCodigo(codigo) { p ->
                                                            when {
                                                                p == null -> error("No encontramos ese código en Vaka.")
                                                                p.uid == uid -> error("Ese eres tú 😄")
                                                                amigosPerfiles.any { it.uid == p.uid } ->
                                                                    error("${p.nombre} ya es tu amigo.")
                                                                else -> Nube.enviarSolicitud(uid, p.uid,
                                                                    onOk = {
                                                                        ok()
                                                                        avisoActual = AvisoInterno(
                                                                            tipo = TipoAviso.ENVIADO,
                                                                            titulo = "Solicitud enviada a ${p.nombre}",
                                                                            texto = "Te avisaremos cuando responda 🙌"
                                                                        )
                                                                    },
                                                                    onError = { error("No se pudo enviar.") })
                                                            }
                                                        }
                                                    }
                                                },
                                                onAceptar = { p ->
                                                    miUid?.let { uid ->
                                                        Nube.aceptarSolicitud(uid, p.uid) { err ->
                                                            android.util.Log.e("VakaNube", err)
                                                        }
                                                        // Aviso al usuario que aceptó (no es el remitente original)
                                                        avisoActual = AvisoInterno(
                                                            tipo = TipoAviso.ACEPTADO,
                                                            titulo = "¡Ahora son amigos!",
                                                            texto = "${p.nombre} ya puede unirse a tus Vakas compartidas"
                                                        )
                                                    }
                                                },
                                                onRechazar = { p ->
                                                    miUid?.let { Nube.rechazarSolicitud(it, p.uid) }
                                                },
                                                onEliminar = { p ->
                                                    miUid?.let { Nube.eliminarAmigo(it, p.uid) }
                                                },
                                                onInvitarAVaka = { amigo, v ->
                                                    v.codigo?.let { c ->
                                                        Nube.invitarAVaka(c, amigo, onOk = {}, onError = {})
                                                    }
                                                },
                                                onInvitarVariosACompartida = { lista, v ->
                                                    val c = v.codigo
                                                    if (c != null) {
                                                        lista.forEach { amigo ->
                                                            Nube.invitarAVaka(c, amigo, onOk = {}, onError = {})
                                                        }
                                                    }
                                                },
                                                onConvertirYInvitar = { vakaPrivada, lista ->
                                                    val uid = miUid ?: return@TabAmigos
                                                    // 1) Convertimos la Vaka privada en compartida (Nube.crear),
                                                    //    2) la borramos de las locales,
                                                    //    3) invitamos a cada compañero.
                                                    Nube.crear(
                                                        vakaPrivada, uid, miNombre,
                                                        onOk = { codigo ->
                                                            persistirLocales(
                                                                locales.filter { it.id != vakaPrivada.id }
                                                            )
                                                            lista.forEach { amigo ->
                                                                Nube.invitarAVaka(codigo, amigo,
                                                                    onOk = {}, onError = {})
                                                            }
                                                        },
                                                        onError = { }
                                                    )
                                                },
                                                onIrALogin = { invitado = false }
                                            )
                                            Pestana.PERFIL -> TabPerfil(
                                                esInvitado = usuario == null,
                                                nombre = miNombre,
                                                correo = usuario?.email ?: "",
                                                codigoUsuario = miPerfil?.codigo ?: "",
                                                colorAvatar = colorAvatar,
                                                fotoBase64 = miPerfil?.fotoBase64 ?: "",
                                                modoTema = modoTema,
                                                monedaPrincipal = monedaPrincipal,
                                                notifOn = notifOn,
                                                pausaNotifHasta = Repo.pausaNotifHasta(ctx),
                                                onPausarNotificaciones = { fecha ->
                                                    Repo.pausarNotificacionesHasta(ctx, fecha)
                                                    // Cancelar las que estaban programadas; volverán a programarse
                                                    // automáticamente cuando termine la pausa al abrir la app
                                                    if (fecha != null) Notificaciones.cancelar(ctx)
                                                    else Notificaciones.programar(ctx)
                                                },
                                                onCambiarColor = { id ->
                                                    colorAvatar = id
                                                    Repo.saveColorAvatar(ctx, id)
                                                },
                                                onCambiarTema = { nuevo ->
                                                    modoTema = nuevo
                                                    Repo.saveModoTema(ctx, nuevo)
                                                },
                                                onToggleNotif = {
                                                    if (notifOn) {
                                                        notifOn = false
                                                        Repo.saveNotif(ctx, false)
                                                        Notificaciones.cancelar(ctx)
                                                    } else {
                                                        if (Build.VERSION.SDK_INT >= 33 &&
                                                            ContextCompat.checkSelfPermission(
                                                                ctx, Manifest.permission.POST_NOTIFICATIONS
                                                            ) != PackageManager.PERMISSION_GRANTED
                                                        ) {
                                                            permisoNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                        } else {
                                                            notifOn = true
                                                            Repo.saveNotif(ctx, true)
                                                            Notificaciones.programar(ctx)
                                                        }
                                                    }
                                                },
                                                onMonedaPrincipal = {
                                                    monedaPrincipal = it
                                                    Repo.saveMonedaPrincipal(ctx, it)
                                                },
                                                onGuardarNombre = { nuevo ->
                                                    nombreEditado = nuevo
                                                    usuario?.updateProfile(
                                                        UserProfileChangeRequest.Builder()
                                                            .setDisplayName(nuevo).build()
                                                    )
                                                },
                                                onCerrarSesion = {
                                                    Firebase.auth.signOut()
                                                    FotoPerfil.limpiarCache()
                                                    invitado = false
                                                    compartidas = emptyList()
                                                    activaClave = null
                                                    nombreEditado = null
                                                    pestana = Pestana.INICIO
                                                },
                                                onIrALogin = { invitado = false; pestana = Pestana.INICIO },
                                                onIrACuenta = { verCuenta = true }
                                            )
                                        }
                                    }
                                }
                                BarraInferior(activa = pestana, onSeleccionar = { pestana = it })
                            }
                        }
                    }
                }

                if (celebrar) {
                    AlertDialog(
                        onDismissRequest = { celebrar = false },
                        confirmButton = {
                            TextButton(onClick = { celebrar = false }) { Text("¡A celebrar!") }
                        },
                        title = { Text("🎉 ¡Meta cumplida!",
                            fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) },
                        text = {
                            Text(
                                "🎊🥳🎊\n\nLo lograron. La constancia dio frutos: disfruten el logro (con moderación 😄).",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    )
                }

                // El ChipGuardado solo se muestra si NO hay un banner activo
                // para no superponer dos elementos en TopCenter.
                if (avisoActual == null) {
                    ChipGuardado(
                        estado = estadoGuardado,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 56.dp)
                    )
                }

                // Banner de avisos internos (solicitudes enviadas, amigos aceptados, etc.)
                AvisoBanner(
                    aviso = avisoActual,
                    onTerminado = { avisoActual = null },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 56.dp)
                )
            }

            // Diálogo de felicitación cuando se desbloquea un logro nuevo.
            // Tiene prioridad sobre las celebraciones de top/reto: si ambos están
            // activos, primero se muestra el logro; al cerrarlo aparece la otra.
            logroNuevo?.let { l ->
                DialogoLogroNuevo(logro = l, onCerrar = { logroNuevo = null })
            }

            // Diálogo de celebración general (top, reto semanal). Solo se muestra
            // si NO hay un logro pendiente, para no apilar dos diálogos a la vez.
            if (logroNuevo == null) {
                celebracionPendiente?.let { (tipo, emoji, datos) ->
                    DialogoCelebracion(
                        tipo = tipo,
                        emojiCentral = emoji,
                        titulo = datos.first,
                        descripcion = datos.second,
                        onCerrar = { celebracionPendiente = null }
                    )
                }
            }
        }
    }
}
