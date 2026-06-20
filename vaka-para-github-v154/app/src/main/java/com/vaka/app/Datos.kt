package com.vaka.app

import android.content.Context
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Currency
import java.util.Locale

// ============================================================
// Modelos
// ============================================================
data class Mov(
    val id: Long,
    val tipo: String,
    val monto: Double,
    val nota: String,
    val fecha: String,
    val autorUid: String = "",
    val autorNombre: String = ""
)

data class Miembro(
    val uid: String,
    val nombre: String,
    val compromiso: Double
)

data class VakaItem(
    val id: Long,
    val nombre: String,
    val emoji: String,
    val monedaCode: String,
    val monedaSymbol: String,
    val meta: Double,
    val metaFecha: String?,
    val metaDesde: String?,
    val movs: List<Mov>,
    val codigo: String? = null,
    val miembros: List<Miembro> = emptyList(),
    // Aporte recurrente automático (null = sin recurrencia)
    val recurrente: AporteRecurrente? = null,
    val favorita: Boolean = false
) {
    val esCompartida: Boolean get() = codigo != null
    val esFija: Boolean get() = id == -1L
    val total: Double get() = movs.sumOf { if (it.tipo == "deposito") it.monto else -it.monto }
    val progreso: Float? get() = if (meta > 0) (total / meta).toFloat().coerceIn(0f, 1f) else null
    fun aportadoPor(uid: String): Double =
        movs.filter { it.tipo == "deposito" && it.autorUid == uid }.sumOf { it.monto }
}

/**
 * Configuración de aporte recurrente automático para una Vaka.
 *
 * @property monto Cantidad a aportar cada vez
 * @property frecuencia "diaria" | "semanal" | "mensual"
 * @property dias Lista de días según frecuencia:
 *   - diaria: lista vacía (se aporta cada día)
 *   - semanal: 1..7 (1=lunes, 7=domingo)
 *   - mensual: 1..31 (días del mes; si el mes no tiene ese día, no se aporta)
 * @property ultimaFecha Fecha del último aporte automático aplicado (yyyy-MM-dd)
 */
data class AporteRecurrente(
    val monto: Double,
    val frecuencia: String,
    val dias: List<Int>,
    val ultimaFecha: String = ""
)

fun claveDe(v: VakaItem): String = when {
    v.esFija -> "F"
    v.codigo != null -> "C:${v.codigo}"
    else -> "L:${v.id}"
}

data class PerfilUsuario(
    val uid: String,
    val nombre: String,
    val correo: String,
    val codigo: String = "",
    val colorAvatar: String = "violeta",
    val fotoBase64: String = "",  // Imagen comprimida en Base64 (vacío = sin foto)
    val solicitudesEnviadas: List<String> = emptyList()  // UIDs a los que mandé solicitud
)

fun generarCodigoUsuario(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return "VK-" + (1..6).map { chars.random() }.joinToString("")
}

data class ResumenVaka(
    val clave: String, val nombre: String, val meta: Double, val metaFecha: String?,
    val total: Double, val ultima: String, val monedaCode: String, val monedaSymbol: String,
)

data class MonedaSugerida(val code: String, val symbol: String, val nombre: String)

val MONEDAS = listOf(
    MonedaSugerida("COP", "$", "Peso colombiano"),
    MonedaSugerida("USD", "$", "Dólar estadounidense"),
    MonedaSugerida("EUR", "€", "Euro"),
    MonedaSugerida("MXN", "$", "Peso mexicano"),
    MonedaSugerida("ARS", "$", "Peso argentino"),
    MonedaSugerida("PEN", "S/", "Sol peruano"),
    MonedaSugerida("CLP", "$", "Peso chileno"),
    MonedaSugerida("BRL", "R$", "Real brasileño"),
    MonedaSugerida("GBP", "£", "Libra esterlina"),
    MonedaSugerida("JPY", "¥", "Yen japonés"),
)

val EMOJIS = listOf("🐷", "✈️", "🏠", "🚗", "🎓", "💍", "🎁", "🛟", "💻", "🌴")

// ============================================================
// Utilidades
// ============================================================
fun hoy(): String = LocalDate.now().toString()

/**
 * Formatea un monto con su símbolo y el código de moneda al final.
 * Ej: "$25.000 COP" o "US$1,500 USD".
 */
fun formatea(monto: Double, code: String, symbol: String): String {
    val numFmt = try {
        val nf = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        nf.currency = Currency.getInstance(code)
        nf.maximumFractionDigits = 2
        nf.format(monto)
    } catch (e: Exception) {
        val nf = NumberFormat.getNumberInstance(Locale("es", "CO"))
        "${symbol}${nf.format(monto)}"
    }
    // Aseguramos que SIEMPRE aparezca el código al final
    return if (numFmt.endsWith(code)) numFmt else "$numFmt $code"
}

/**
 * Versión sin el código de moneda al final, para contextos donde el código
 * ya está mostrado en otra parte (ej. dentro de un selector).
 */
fun formateaSinCodigo(monto: Double, code: String, symbol: String): String {
    return try {
        val nf = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        nf.currency = Currency.getInstance(code)
        nf.maximumFractionDigits = 2
        nf.format(monto)
    } catch (e: Exception) {
        val nf = NumberFormat.getNumberInstance(Locale("es", "CO"))
        "${symbol}${nf.format(monto)}"
    }
}

fun fechaBonita(iso: String): String = try {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "CO")))
} catch (e: Exception) { iso }

fun fechaValida(iso: String): Boolean = try { LocalDate.parse(iso); true } catch (e: Exception) { false }

// ============================================================
// Plan y logros
// ============================================================
data class Plan(
    val cumplida: Boolean = false, val vencida: Boolean = false,
    val restante: Double = 0.0, val dias: Long = 0,
    val porSemana: Double = 0.0, val estado: String? = null
)

fun planDe(v: VakaItem): Plan? {
    if (v.meta <= 0 || v.metaFecha == null || !fechaValida(v.metaFecha)) return null
    val t = v.total
    val restante = v.meta - t
    if (restante <= 0) return Plan(cumplida = true)
    val ahora = LocalDate.now()
    val fin = LocalDate.parse(v.metaFecha)
    val dias = ChronoUnit.DAYS.between(ahora, fin)
    if (dias <= 0) return Plan(vencida = true, restante = restante)
    val semanas = maxOf(1L, (dias + 6) / 7)
    val porSemana = restante / semanas
    var estado: String? = null
    val desde = v.metaDesde
    if (desde != null && fechaValida(desde)) {
        val ini = LocalDate.parse(desde)
        val totalDias = ChronoUnit.DAYS.between(ini, fin).toDouble()
        val trans = ChronoUnit.DAYS.between(ini, ahora).toDouble()
        if (totalDias > 2 && trans > 0) {
            val esperado = v.meta * minOf(1.0, trans / totalDias)
            estado = when {
                t >= esperado * 1.02 -> "adelantado"
                t >= esperado * 0.92 -> "aldia"
                else -> "atrasado"
            }
        }
    }
    return Plan(restante = restante, dias = dias, porSemana = porSemana, estado = estado)
}

data class Logro(
    val id: String,           // ID estable para detectar cuándo se desbloquea uno nuevo
    val emoji: String,
    val nombre: String,
    val descripcion: String,
    val ok: Boolean
)

fun logrosDe(v: VakaItem): List<Logro> {
    val deps = v.movs.filter { it.tipo == "deposito" }
    val wf = WeekFields.ISO
    val semanas = deps.mapNotNull {
        try {
            val d = LocalDate.parse(it.fecha)
            "${d.year}-${d.get(wf.weekOfWeekBasedYear())}"
        } catch (e: Exception) { null }
    }.toSet().size
    val p = if (v.meta > 0) v.total / v.meta else null
    val totalDep = deps.sumOf { it.monto }
    val depMaximo = deps.maxOfOrNull { it.monto } ?: 0.0
    val cantidadDeps = deps.size

    return listOf(
        Logro("primer_dep", "🥇", "Primer depósito",
            "Acabas de empezar tu camino del ahorro", deps.isNotEmpty()),
        Logro("diez_deps", "🐣", "Diez aportes",
            "Has hecho 10 depósitos en total", cantidadDeps >= 10),
        Logro("cincuenta_deps", "🦅", "Cincuenta aportes",
            "Has hecho 50 depósitos. ¡Constancia pura!", cantidadDeps >= 50),
        Logro("constancia_1mes", "🔥", "Un mes ahorrando",
            "Ahorraste durante 4 semanas distintas", semanas >= 4),
        Logro("constancia_3meses", "💎", "Tres meses ahorrando",
            "Ahorraste durante 12 semanas distintas", semanas >= 12),
        Logro("mitad", "💪", "Mitad del camino",
            "Llegaste al 50% de una meta", p != null && p >= 0.5),
        Logro("meta", "🏆", "Meta cumplida",
            "Alcanzaste el 100% de tu meta", p != null && p >= 1.0),
        Logro("primer_millon", "💸", "Primer millón",
            "Acumulaste 1.000.000 en depósitos", totalDep >= 1_000_000.0),
        Logro("gran_aporte", "🐳", "Aporte ballena",
            "Hiciste un depósito de más de 500.000", depMaximo >= 500_000.0),
    )
}

// ============================================================
// Persistencia local
// ============================================================
object Repo {
    private const val PREFS = "vaka_prefs"
    private const val KEY = "vakas"
    private const val KEY_DARK = "dark"
    private const val KEY_TEMA = "modo_tema"
    private const val KEY_PERMISO_NOTIF = "permiso_notif_solicitado"
    private const val KEY_MONEDA = "moneda_principal"
    private const val KEY_COLOR_AVATAR = "color_avatar"
    private const val KEY_AHORRO_FIJO = "ahorro_fijo"
    private const val KEY_NOTIF = "notif_on"
    private const val KEY_RESUMEN = "resumen_compartidas"
    private const val KEY_LOGROS_GANADOS = "logros_ganados"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Carga los IDs de logros que el usuario ha desbloqueado ALGUNA VEZ.
     * Una vez desbloqueado, un logro permanece para siempre aunque la Vaka
     * que lo originó sea borrada.
     */
    fun loadLogrosGanados(ctx: Context): Set<String> {
        val raw = prefs(ctx).getString(KEY_LOGROS_GANADOS, "") ?: ""
        return if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    /** Guarda el conjunto actual de logros ganados (sobrescribe). */
    fun saveLogrosGanados(ctx: Context, logros: Set<String>) {
        prefs(ctx).edit().putString(KEY_LOGROS_GANADOS, logros.joinToString(",")).apply()
    }

    fun loadDark(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_DARK, false)
    fun saveDark(ctx: Context, d: Boolean) { prefs(ctx).edit().putBoolean(KEY_DARK, d).apply() }

    fun loadModoTema(ctx: Context): String = prefs(ctx).getString(KEY_TEMA, TEMA_SISTEMA) ?: TEMA_SISTEMA
    fun saveModoTema(ctx: Context, modo: String) { prefs(ctx).edit().putString(KEY_TEMA, modo).apply() }

    fun permisoNotifSolicitado(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_PERMISO_NOTIF, false)
    fun marcarPermisoNotifSolicitado(ctx: Context) {
        prefs(ctx).edit().putBoolean(KEY_PERMISO_NOTIF, true).apply()
    }

    fun loadMonedaPrincipal(ctx: Context): String = prefs(ctx).getString(KEY_MONEDA, "COP") ?: "COP"
    fun saveMonedaPrincipal(ctx: Context, code: String) {
        prefs(ctx).edit().putString(KEY_MONEDA, code).apply()
    }

    fun loadColorAvatar(ctx: Context): String = prefs(ctx).getString(KEY_COLOR_AVATAR, "violeta") ?: "violeta"
    fun saveColorAvatar(ctx: Context, color: String) {
        prefs(ctx).edit().putString(KEY_COLOR_AVATAR, color).apply()
    }

    fun loadNotif(ctx: Context): Boolean {
        // Si las notif están desactivadas globalmente, no se muestran
        if (!prefs(ctx).getBoolean(KEY_NOTIF, true)) return false
        // Si están en pausa temporal y la pausa NO ha terminado, no se muestran
        val pausaHasta = prefs(ctx).getString("notif_pausa_hasta", "") ?: ""
        if (pausaHasta.isNotBlank()) {
            try {
                val fin = java.time.LocalDate.parse(pausaHasta)
                if (!java.time.LocalDate.now().isAfter(fin)) return false
            } catch (e: Exception) { /* fecha inválida: ignorar */ }
        }
        return true
    }
    fun saveNotif(ctx: Context, activadas: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_NOTIF, activadas).apply()
    }

    /**
     * Pausar notificaciones hasta una fecha específica (inclusive).
     * Pasar null o cadena vacía cancela la pausa.
     */
    fun pausarNotificacionesHasta(ctx: Context, fechaIso: String?) {
        prefs(ctx).edit().putString("notif_pausa_hasta", fechaIso ?: "").apply()
    }

    /** Devuelve la fecha de fin de pausa o null si no hay pausa activa. */
    fun pausaNotifHasta(ctx: Context): java.time.LocalDate? {
        val s = prefs(ctx).getString("notif_pausa_hasta", "") ?: ""
        if (s.isBlank()) return null
        return try {
            val fin = java.time.LocalDate.parse(s)
            if (java.time.LocalDate.now().isAfter(fin)) null else fin
        } catch (e: Exception) { null }
    }

    /** Borra TODOS los datos locales (uso al eliminar cuenta). */
    fun borrarTodoLocal(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }

    /** Vaka fija de "Mi ahorro": solo guarda movimientos, sin meta. */
    /**
     * Migración (única vez): convierte el ahorro fijo legacy en una Vaka
     * normal "Ahorros generales" si tenía movimientos. Se ejecuta una sola
     * vez por instalación gracias al flag KEY_AHORRO_FIJO_MIGRADO.
     *
     * Devuelve la Vaka migrada (si la hubo) para que el caller la añada a
     * sus listas. Devuelve null si no había nada que migrar.
     */
    fun migrarAhorroFijoSiHaceFalta(ctx: Context, monedaPrincipal: String = "COP"): VakaItem? {
        val p = prefs(ctx)
        if (p.getBoolean("ahorro_fijo_migrado", false)) return null

        val s = p.getString(KEY_AHORRO_FIJO, null) ?: run {
            // Marcar como migrado aunque no hubiera nada que migrar
            p.edit().putBoolean("ahorro_fijo_migrado", true).apply()
            return null
        }

        try {
            val o = JSONObject(s)
            val ma = o.optJSONArray("movs") ?: JSONArray()
            if (ma.length() == 0) {
                // Sin movimientos: solo borramos el ahorro fijo viejo
                p.edit()
                    .remove(KEY_AHORRO_FIJO)
                    .putBoolean("ahorro_fijo_migrado", true)
                    .apply()
                return null
            }

            // Construir una Vaka regular con los movimientos del ahorro fijo
            val vakaMigrada = VakaItem(
                id = System.currentTimeMillis(),
                nombre = "Ahorros generales",
                emoji = "💰",
                monedaCode = o.optString("monedaCode", monedaPrincipal),
                monedaSymbol = o.optString("monedaSymbol", "$"),
                meta = 0.0,
                metaFecha = null,
                metaDesde = null,
                movs = (0 until ma.length()).map { i ->
                    val m = ma.getJSONObject(i)
                    Mov(
                        id = m.getLong("id"),
                        tipo = m.optString("tipo", "deposito"),
                        monto = m.optDouble("monto", 0.0),
                        nota = m.optString("nota", ""),
                        fecha = m.optString("fecha", hoy()),
                    )
                }
            )

            // Limpiar el ahorro fijo viejo y marcar como migrado
            p.edit()
                .remove(KEY_AHORRO_FIJO)
                .putBoolean("ahorro_fijo_migrado", true)
                .apply()

            return vakaMigrada
        } catch (e: Exception) {
            // Si algo falla, marcamos como migrado para no reintentar
            p.edit().putBoolean("ahorro_fijo_migrado", true).apply()
            return null
        }
    }

    fun loadAhorroFijo(ctx: Context, monedaPrincipal: String = "COP"): VakaItem {
        val s = prefs(ctx).getString(KEY_AHORRO_FIJO, null)
        if (s != null) {
            try {
                val o = JSONObject(s)
                val ma = o.optJSONArray("movs") ?: JSONArray()
                return VakaItem(
                    id = -1L, nombre = "Mis ahorros totales", emoji = "💰",
                    monedaCode = o.optString("monedaCode", monedaPrincipal),
                    monedaSymbol = o.optString("monedaSymbol", "$"),
                    meta = 0.0, metaFecha = null, metaDesde = null,
                    movs = (0 until ma.length()).map { i ->
                        val m = ma.getJSONObject(i)
                        Mov(
                            id = m.getLong("id"),
                            tipo = m.optString("tipo", "deposito"),
                            monto = m.optDouble("monto", 0.0),
                            nota = m.optString("nota", ""),
                            fecha = m.optString("fecha", hoy()),
                        )
                    }
                )
            } catch (e: Exception) { /* recreamos */ }
        }
        return VakaItem(
            id = -1L, nombre = "Mis ahorros totales", emoji = "💰",
            monedaCode = monedaPrincipal, monedaSymbol = "$",
            meta = 0.0, metaFecha = null, metaDesde = null, movs = emptyList()
        )
    }

    fun saveAhorroFijo(ctx: Context, v: VakaItem) {
        val o = JSONObject()
        o.put("monedaCode", v.monedaCode)
        o.put("monedaSymbol", v.monedaSymbol)
        val ma = JSONArray()
        v.movs.forEach { m ->
            ma.put(JSONObject().apply {
                put("id", m.id); put("tipo", m.tipo); put("monto", m.monto)
                put("nota", m.nota); put("fecha", m.fecha)
            })
        }
        o.put("movs", ma)
        prefs(ctx).edit().putString(KEY_AHORRO_FIJO, o.toString()).apply()
    }

    fun saveResumenCompartidas(ctx: Context, vakas: List<VakaItem>) {
        val arr = JSONArray()
        vakas.forEach { v ->
            arr.put(JSONObject().apply {
                put("clave", claveDe(v)); put("nombre", v.nombre)
                put("meta", v.meta); put("metaFecha", v.metaFecha ?: JSONObject.NULL)
                put("total", v.total); put("ultima", v.movs.maxOfOrNull { it.fecha } ?: "")
                put("monedaCode", v.monedaCode); put("monedaSymbol", v.monedaSymbol)
            })
        }
        prefs(ctx).edit().putString(KEY_RESUMEN, arr.toString()).apply()
    }

    fun loadResumenCompartidas(ctx: Context): List<ResumenVaka> {
        val s = prefs(ctx).getString(KEY_RESUMEN, null) ?: return emptyList()
        return try {
            val arr = JSONArray(s)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ResumenVaka(
                    clave = o.getString("clave"), nombre = o.getString("nombre"),
                    meta = o.optDouble("meta", 0.0),
                    metaFecha = if (o.isNull("metaFecha")) null else o.optString("metaFecha"),
                    total = o.optDouble("total", 0.0),
                    ultima = o.optString("ultima", ""),
                    monedaCode = o.optString("monedaCode", "COP"),
                    monedaSymbol = o.optString("monedaSymbol", "$")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun load(ctx: Context): List<VakaItem> {
        val s = prefs(ctx).getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(s)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ma = o.optJSONArray("movs") ?: JSONArray()
                VakaItem(
                    id = o.getLong("id"), nombre = o.getString("nombre"),
                    emoji = o.optString("emoji", "🐷"),
                    monedaCode = o.optString("monedaCode", "COP"),
                    monedaSymbol = o.optString("monedaSymbol", "$"),
                    meta = o.optDouble("meta", 0.0),
                    metaFecha = if (o.isNull("metaFecha")) null else o.optString("metaFecha"),
                    metaDesde = if (o.isNull("metaDesde")) null else o.optString("metaDesde"),
                    movs = (0 until ma.length()).map { j ->
                        val m = ma.getJSONObject(j)
                        Mov(
                            id = m.getLong("id"),
                            tipo = m.optString("tipo", "deposito"),
                            monto = m.optDouble("monto", 0.0),
                            nota = m.optString("nota", ""),
                            fecha = m.optString("fecha", hoy()),
                            autorUid = m.optString("autorUid", ""),
                            autorNombre = m.optString("autorNombre", "")
                        )
                    },
                    recurrente = if (o.has("recurrente") && !o.isNull("recurrente")) {
                        val r = o.getJSONObject("recurrente")
                        val diasArr = r.optJSONArray("dias") ?: JSONArray()
                        AporteRecurrente(
                            monto = r.optDouble("monto", 0.0),
                            frecuencia = r.optString("frecuencia", "semanal"),
                            dias = (0 until diasArr.length()).map { diasArr.getInt(it) },
                            ultimaFecha = r.optString("ultimaFecha", "")
                        )
                    } else null,
                    favorita = o.optBoolean("favorita", false)
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    /**
     * Revisa todas las Vakas locales con aporte recurrente y aplica los
     * aportes pendientes desde la última vez que se ejecutó.
     *
     * Esto se ejecuta cada vez que el usuario abre la app (idempotente):
     * si hoy ya se aplicó, no aplica de nuevo.
     *
     * Devuelve la lista actualizada de Vakas (con los aportes aplicados).
     */
    fun aplicarAportesRecurrentesPendientes(ctx: Context): List<VakaItem> {
        val vakas = load(ctx)
        val hoyD = java.time.LocalDate.now()
        var cambiosHechos = false

        val actualizadas = vakas.map { v ->
            val r = v.recurrente ?: return@map v

            // Determinar cuántos aportes aplicar desde ultimaFecha hasta hoy
            val desde = if (r.ultimaFecha.isBlank()) {
                hoyD  // primera vez: empieza desde hoy
            } else {
                try {
                    java.time.LocalDate.parse(r.ultimaFecha).plusDays(1)
                } catch (e: Exception) { hoyD }
            }

            val nuevosMovs = mutableListOf<Mov>()
            var fecha = desde
            while (!fecha.isAfter(hoyD)) {
                val debeAportar = when (r.frecuencia) {
                    "diaria" -> true
                    "semanal" -> {
                        // dias: 1..7 (1=lunes)
                        r.dias.contains(fecha.dayOfWeek.value)
                    }
                    "mensual" -> {
                        // dias: 1..31; si el mes no tiene ese día, no aporta
                        r.dias.contains(fecha.dayOfMonth)
                    }
                    else -> false
                }
                if (debeAportar) {
                    nuevosMovs.add(Mov(
                        id = System.currentTimeMillis() + fecha.toEpochDay(),
                        tipo = "deposito",
                        monto = r.monto,
                        nota = "Aporte automático",
                        fecha = fecha.toString()
                    ))
                }
                fecha = fecha.plusDays(1)
            }

            if (nuevosMovs.isEmpty() && r.ultimaFecha == hoyD.toString()) {
                v  // sin cambios
            } else {
                cambiosHechos = true
                v.copy(
                    movs = nuevosMovs.reversed() + v.movs,  // los más recientes primero
                    recurrente = r.copy(ultimaFecha = hoyD.toString())
                )
            }
        }

        if (cambiosHechos) save(ctx, actualizadas)
        return actualizadas
    }

    fun save(ctx: Context, vakas: List<VakaItem>) {
        val arr = JSONArray()
        vakas.filter { !it.esCompartida && !it.esFija }.forEach { v ->
            val o = JSONObject()
            o.put("id", v.id); o.put("nombre", v.nombre); o.put("emoji", v.emoji)
            o.put("monedaCode", v.monedaCode); o.put("monedaSymbol", v.monedaSymbol)
            o.put("meta", v.meta)
            o.put("metaFecha", v.metaFecha ?: JSONObject.NULL)
            o.put("metaDesde", v.metaDesde ?: JSONObject.NULL)
            val ma = JSONArray()
            v.movs.forEach { m ->
                ma.put(JSONObject().apply {
                    put("id", m.id); put("tipo", m.tipo); put("monto", m.monto)
                    put("nota", m.nota); put("fecha", m.fecha)
                    put("autorUid", m.autorUid); put("autorNombre", m.autorNombre)
                })
            }
            o.put("movs", ma)
            v.recurrente?.let { r ->
                val rObj = JSONObject()
                rObj.put("monto", r.monto)
                rObj.put("frecuencia", r.frecuencia)
                val diasArr = JSONArray()
                r.dias.forEach { diasArr.put(it) }
                rObj.put("dias", diasArr)
                rObj.put("ultimaFecha", r.ultimaFecha)
                o.put("recurrente", rObj)
            }
            if (v.favorita) o.put("favorita", true)
            arr.put(o)
        }
        prefs(ctx).edit().putString(KEY, arr.toString()).apply()
    }
}

// ============================================================
// Nube (Firestore)
// ============================================================
object Nube {
    private val db get() = Firebase.firestore
    private const val COL = "vakas"
    private const val COL_USUARIOS = "usuarios"

    fun genCodigo(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun vakaAMapa(v: VakaItem): Map<String, Any?> = mapOf(
        "nombre" to v.nombre, "emoji" to v.emoji,
        "monedaCode" to v.monedaCode, "monedaSymbol" to v.monedaSymbol,
        "meta" to v.meta, "metaFecha" to v.metaFecha, "metaDesde" to v.metaDesde,
        "movs" to v.movs.map { m ->
            mapOf(
                "id" to m.id, "tipo" to m.tipo, "monto" to m.monto,
                "nota" to m.nota, "fecha" to m.fecha,
                "autorUid" to m.autorUid, "autorNombre" to m.autorNombre
            )
        },
        "miembros" to v.miembros.associate { mi ->
            mi.uid to mapOf("nombre" to mi.nombre, "compromiso" to mi.compromiso)
        },
        "miembrosUids" to v.miembros.map { it.uid }
    )

    @Suppress("UNCHECKED_CAST")
    private fun docAVaka(codigo: String, data: Map<String, Any?>): VakaItem {
        val movsRaw = data["movs"] as? List<Map<String, Any?>> ?: emptyList()
        val miembrosRaw = data["miembros"] as? Map<String, Map<String, Any?>> ?: emptyMap()
        return VakaItem(
            id = 0L, codigo = codigo,
            nombre = data["nombre"] as? String ?: "Vaka",
            emoji = data["emoji"] as? String ?: "🐷",
            monedaCode = data["monedaCode"] as? String ?: "COP",
            monedaSymbol = data["monedaSymbol"] as? String ?: "$",
            meta = (data["meta"] as? Number)?.toDouble() ?: 0.0,
            metaFecha = data["metaFecha"] as? String,
            metaDesde = data["metaDesde"] as? String,
            movs = movsRaw.map { m ->
                Mov(
                    id = (m["id"] as? Number)?.toLong() ?: 0L,
                    tipo = m["tipo"] as? String ?: "deposito",
                    monto = (m["monto"] as? Number)?.toDouble() ?: 0.0,
                    nota = m["nota"] as? String ?: "",
                    fecha = m["fecha"] as? String ?: hoy(),
                    autorUid = m["autorUid"] as? String ?: "",
                    autorNombre = m["autorNombre"] as? String ?: ""
                )
            },
            miembros = miembrosRaw.map { (uid, mm) ->
                Miembro(
                    uid = uid,
                    nombre = mm["nombre"] as? String ?: "Alguien",
                    compromiso = (mm["compromiso"] as? Number)?.toDouble() ?: 0.0
                )
            }
        )
    }

    fun crear(v: VakaItem, uid: String, nombre: String, onOk: (String) -> Unit, onError: () -> Unit) {
        val codigo = genCodigo()
        val compartida = v.copy(codigo = codigo, miembros = listOf(Miembro(uid, nombre, 0.0)))
        db.collection(COL).document(codigo).set(vakaAMapa(compartida))
            .addOnSuccessListener { onOk(codigo) }
            .addOnFailureListener { onError() }
    }

    fun guardar(v: VakaItem, onEstado: ((Boolean) -> Unit)? = null) {
        val c = v.codigo ?: return
        db.collection(COL).document(c).set(vakaAMapa(v))
            .addOnSuccessListener { onEstado?.invoke(true) }
            .addOnFailureListener { onEstado?.invoke(false) }
    }

    fun unirse(codigo: String, uid: String, nombre: String, onOk: () -> Unit, onError: (String) -> Unit) {
        val ref = db.collection(COL).document(codigo)
        ref.get()
            .addOnSuccessListener { d ->
                if (!d.exists()) {
                    onError("No encontramos una Vaka con ese código.")
                } else {
                    ref.update(
                        mapOf(
                            "miembros.$uid" to mapOf("nombre" to nombre, "compromiso" to 0.0),
                            "miembrosUids" to FieldValue.arrayUnion(uid)
                        )
                    ).addOnSuccessListener { onOk() }
                        .addOnFailureListener { onError("No se pudo unir. Intenta de nuevo.") }
                }
            }
            .addOnFailureListener { onError("Error de conexión. Revisa tu internet.") }
    }

    fun salir(codigo: String, uid: String) {
        val ref = db.collection(COL).document(codigo)
        ref.get().addOnSuccessListener { d ->
            @Suppress("UNCHECKED_CAST")
            val miembrosUids = d.get("miembrosUids") as? List<String> ?: emptyList()
            if (miembrosUids.size <= 1) {
                // Soy el único (o estaba huérfana): borrar la Vaka entera
                ref.delete()
            } else {
                ref.update(
                    mapOf(
                        "miembros.$uid" to FieldValue.delete(),
                        "miembrosUids" to FieldValue.arrayRemove(uid)
                    )
                )
            }
        }
    }

    fun setCompromiso(codigo: String, uid: String, monto: Double) {
        db.collection(COL).document(codigo).update("miembros.$uid.compromiso", monto)
    }

    fun escuchar(uid: String, onCambio: (List<VakaItem>) -> Unit): ListenerRegistration =
        db.collection(COL).whereArrayContains("miembrosUids", uid)
            .addSnapshotListener { snap, _ ->
                onCambio(snap?.documents?.mapNotNull { d ->
                    d.data?.let { docAVaka(d.id, it) }
                } ?: emptyList())
            }

    // ================== Perfiles y amigos ==================

    fun guardarPerfil(uid: String, nombre: String, correo: String, colorAvatar: String = "violeta") {
        val ref = db.collection(COL_USUARIOS).document(uid)
        ref.get().addOnSuccessListener { d ->
            val datos = mutableMapOf<String, Any>(
                "nombre" to nombre,
                "correo" to correo.trim().lowercase(),
                "colorAvatar" to colorAvatar
            )
            if (d.getString("codigo").isNullOrBlank()) {
                datos["codigo"] = generarCodigoUsuario()
            }
            ref.set(datos, SetOptions.merge())
            // Limpiar referencias inválidas: no puedo ser mi propio amigo ni tener
            // mi propio uid en mis solicitudes (residuos de bugs anteriores)
            ref.update(
                mapOf(
                    "amigos" to FieldValue.arrayRemove(uid),
                    "solicitudes" to FieldValue.arrayRemove(uid)
                )
            )
        }
    }

    fun borrarPerfil(uid: String, onTerminar: () -> Unit = {}) {
        db.collection(COL_USUARIOS).document(uid).delete()
            .addOnCompleteListener { onTerminar() }
    }

    /**
     * Guarda la foto de perfil del usuario (ya comprimida y codificada como Base64).
     * Pasa cadena vacía para quitar la foto.
     */
    fun guardarFotoPerfil(uid: String, fotoBase64: String, onTerminar: () -> Unit = {}) {
        db.collection(COL_USUARIOS).document(uid)
            .update("fotoBase64", fotoBase64)
            .addOnCompleteListener { onTerminar() }
    }

    /**
     * Limpieza completa al eliminar cuenta:
     * - Sale (o borra si quedaba solo) de cada Vaka compartida.
     * - Quita su UID de la lista de amigos/solicitudes de los demás usuarios.
     * - Borra su propio perfil.
     */
    fun borrarTodoDelUsuario(uid: String, onTerminar: () -> Unit = {}) {
        // 1) Sacarse de todas las Vakas donde sea miembro
        db.collection(COL).whereArrayContains("miembrosUids", uid).get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val miembrosUids = doc.get("miembrosUids") as? List<String> ?: emptyList()
                    if (miembrosUids.size <= 1) {
                        doc.reference.delete()
                    } else {
                        doc.reference.update(
                            mapOf(
                                "miembros.$uid" to FieldValue.delete(),
                                "miembrosUids" to FieldValue.arrayRemove(uid)
                            )
                        )
                    }
                }
                // 2) Limpiar referencias en perfiles de otros usuarios (amigos/solicitudes)
                db.collection(COL_USUARIOS).whereArrayContains("amigos", uid).get()
                    .addOnSuccessListener { s2 ->
                        s2.documents.forEach { d ->
                            d.reference.update("amigos", FieldValue.arrayRemove(uid))
                        }
                    }
                db.collection(COL_USUARIOS).whereArrayContains("solicitudes", uid).get()
                    .addOnSuccessListener { s3 ->
                        s3.documents.forEach { d ->
                            d.reference.update("solicitudes", FieldValue.arrayRemove(uid))
                        }
                    }
                // 3) Borrar el propio perfil
                db.collection(COL_USUARIOS).document(uid).delete()
                    .addOnCompleteListener { onTerminar() }
            }
            .addOnFailureListener {
                // Si falla la consulta de Vakas, al menos limpiamos el perfil
                db.collection(COL_USUARIOS).document(uid).delete()
                    .addOnCompleteListener { onTerminar() }
            }
    }

    fun escucharPerfil(
        uid: String,
        onCambio: (perfil: PerfilUsuario?, amigos: List<String>, solicitudes: List<String>) -> Unit
    ): ListenerRegistration =
        db.collection(COL_USUARIOS).document(uid).addSnapshotListener { d, _ ->
            @Suppress("UNCHECKED_CAST")
            val solicitudesEnviadas = d?.get("solicitudesEnviadas") as? List<String> ?: emptyList()
            val perfil = d?.let {
                PerfilUsuario(
                    it.id,
                    it.getString("nombre") ?: "",
                    it.getString("correo") ?: "",
                    it.getString("codigo") ?: "",
                    it.getString("colorAvatar") ?: "violeta",
                    it.getString("fotoBase64") ?: "",
                    solicitudesEnviadas
                )
            }
            onCambio(
                perfil,
                d?.get("amigos") as? List<String> ?: emptyList(),
                d?.get("solicitudes") as? List<String> ?: emptyList()
            )
        }

    fun cargarUsuarios(uids: List<String>, onOk: (List<PerfilUsuario>) -> Unit) {
        if (uids.isEmpty()) { onOk(emptyList()); return }
        val resultado = mutableListOf<PerfilUsuario>()
        val grupos = uids.chunked(10)
        var pendientes = grupos.size
        grupos.forEach { grupo ->
            db.collection(COL_USUARIOS)
                .whereIn(FieldPath.documentId(), grupo)
                .get()
                .addOnSuccessListener { snap ->
                    snap.documents.forEach { d ->
                        resultado.add(PerfilUsuario(
                            d.id,
                            d.getString("nombre") ?: "Alguien",
                            d.getString("correo") ?: "",
                            d.getString("codigo") ?: "",
                            d.getString("colorAvatar") ?: "violeta",
                            d.getString("fotoBase64") ?: ""
                        ))
                    }
                    if (--pendientes == 0) onOk(resultado)
                }
                .addOnFailureListener { if (--pendientes == 0) onOk(resultado) }
        }
    }

    fun buscarPorCodigo(codigo: String, onOk: (PerfilUsuario?) -> Unit) {
        val c = codigo.trim().uppercase()
        db.collection(COL_USUARIOS).whereEqualTo("codigo", c).limit(1).get()
            .addOnSuccessListener { snap ->
                val d = snap.documents.firstOrNull()
                onOk(d?.let {
                    PerfilUsuario(
                        it.id,
                        it.getString("nombre") ?: "Alguien",
                        it.getString("correo") ?: "",
                        it.getString("codigo") ?: "",
                        it.getString("colorAvatar") ?: "violeta",
                        it.getString("fotoBase64") ?: ""
                    )
                })
            }
            .addOnFailureListener { onOk(null) }
    }

    fun enviarSolicitud(miUid: String, paraUid: String, onOk: () -> Unit, onError: () -> Unit) {
        // Paso 1: agregar a las solicitudes recibidas del destinatario
        db.collection(COL_USUARIOS).document(paraUid)
            .update("solicitudes", FieldValue.arrayUnion(miUid))
            .addOnSuccessListener {
                // Paso 2: registrar en mis solicitudesEnviadas (mi propio doc)
                // Esto siempre está permitido (es mi doc).
                db.collection(COL_USUARIOS).document(miUid)
                    .update("solicitudesEnviadas", FieldValue.arrayUnion(paraUid))
                    .addOnSuccessListener { onOk() }
                    .addOnFailureListener {
                        // La solicitud quedó enviada (paso 1) pero no se registró en
                        // mis enviadas. No es crítico — solo no detectaríamos su rechazo.
                        // Reportamos OK porque el flujo principal funcionó.
                        onOk()
                    }
            }
            .addOnFailureListener { onError() }
    }

    /**
     * miUid (yo) acepta la solicitud que me envió otroUid.
     *
     * Flujo:
     *  Paso 1: actualizo MI doc — me agrego a otroUid como amigo y lo quito
     *          de mis solicitudes recibidas.
     *  Paso 2: actualizo el doc del OTRO — me agrego a sus amigos y me quito
     *          de SUS solicitudesEnviadas (porque ya no es pendiente).
     */
    fun aceptarSolicitud(miUid: String, otroUid: String, onError: (String) -> Unit = {}) {
        // Paso 1: actualizar mi propio doc
        db.collection(COL_USUARIOS).document(miUid).update(
            mapOf(
                "amigos" to FieldValue.arrayUnion(otroUid),
                "solicitudes" to FieldValue.arrayRemove(otroUid)
            )
        ).addOnSuccessListener {
            // Paso 2: actualizar el doc del otro (requiere Caso 3 y Caso 6 nuevo)
            db.collection(COL_USUARIOS).document(otroUid).update(
                mapOf(
                    "amigos" to FieldValue.arrayUnion(miUid),
                    "solicitudesEnviadas" to FieldValue.arrayRemove(miUid)
                )
            ).addOnFailureListener { e ->
                android.util.Log.e("VakaNube", "aceptarSolicitud: falla en doc del otro", e)
                onError(
                    "La amistad quedó solo en un lado. " +
                    "Publica las nuevas reglas de Firestore."
                )
            }
        }.addOnFailureListener { e ->
            android.util.Log.e("VakaNube", "aceptarSolicitud: falla en mi doc", e)
            onError("No se pudo aceptar: ${e.localizedMessage}")
        }
    }

    /**
     * miUid (yo) rechaza la solicitud que me envió otroUid.
     * Flujo:
     *  Paso 1: quitar otroUid de mis solicitudes recibidas (mi doc, siempre permitido)
     *  Paso 2: quitarme de las solicitudesEnviadas del otro (requiere Caso 6 nuevo)
     */
    fun rechazarSolicitud(miUid: String, otroUid: String) {
        db.collection(COL_USUARIOS).document(miUid)
            .update("solicitudes", FieldValue.arrayRemove(otroUid))
            .addOnSuccessListener {
                db.collection(COL_USUARIOS).document(otroUid)
                    .update("solicitudesEnviadas", FieldValue.arrayRemove(miUid))
                    .addOnFailureListener { e ->
                        android.util.Log.e("VakaNube", "rechazarSolicitud: limpieza en otro doc", e)
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("VakaNube", "rechazarSolicitud: mi doc", e)
            }
    }

    fun eliminarAmigo(miUid: String, otroUid: String) {
        db.collection(COL_USUARIOS).document(miUid).update("amigos", FieldValue.arrayRemove(otroUid))
            .addOnFailureListener { e ->
                android.util.Log.e("VakaNube", "eliminarAmigo: mi doc", e)
            }
        db.collection(COL_USUARIOS).document(otroUid).update("amigos", FieldValue.arrayRemove(miUid))
            .addOnFailureListener { e ->
                android.util.Log.e("VakaNube", "eliminarAmigo: doc del otro", e)
            }
    }

    fun invitarAVaka(codigo: String, amigo: PerfilUsuario, onOk: () -> Unit, onError: () -> Unit) {
        db.collection(COL).document(codigo).update(
            mapOf(
                "miembros.${amigo.uid}" to mapOf("nombre" to amigo.nombre, "compromiso" to 0.0),
                "miembrosUids" to FieldValue.arrayUnion(amigo.uid)
            )
        ).addOnSuccessListener { onOk() }.addOnFailureListener { onError() }
    }
}
