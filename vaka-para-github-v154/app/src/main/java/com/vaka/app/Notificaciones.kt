package com.vaka.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object Notificaciones {
    const val CANAL = "vaka_recordatorios"
    private const val TRABAJO = "vaka_recordatorio_diario"

    fun crearCanal(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val canal = NotificationChannel(
                CANAL,
                "Recordatorios de ahorro",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Hitos de progreso, fechas límite y ánimos para seguir ahorrando"
            }
            ctx.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(canal)
        }
    }

    /**
     * Muestra una notificación. Si se proporciona `pestanaDestino`, al tocarla
     * la app se abre directamente en esa pestaña ("amigos", "inicio", etc.).
     */
    fun mostrar(
        ctx: Context,
        titulo: String,
        texto: String,
        id: Int = 1,
        pestanaDestino: String? = null
    ) {
        // En Android 13+ se necesita el permiso de notificaciones
        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(
                ctx, android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(ctx, MainActivity::class.java).apply {
            // FLAG_ACTIVITY_SINGLE_TOP para no relanzar si ya está abierta,
            // FLAG_ACTIVITY_CLEAR_TOP para volver al inicio del stack.
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (pestanaDestino != null) {
                putExtra("pestana_destino", pestanaDestino)
            }
        }
        val pending = PendingIntent.getActivity(
            ctx,
            // request code único basado en el id de la notificación + destino,
            // para que cada notificación tenga su propio PendingIntent
            id + (pestanaDestino?.hashCode() ?: 0),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(ctx, CANAL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, notif)
    }

    /**
     * Notifica al usuario que alguien le envió una solicitud de amistad.
     * Al tocar la notificación, la app se abre en la pestaña "Amigos".
     */
    fun notificarSolicitudAmistad(ctx: Context, nombreEmisor: String, uidEmisor: String) {
        if (!Repo.loadNotif(ctx)) return
        crearCanal(ctx)
        val id = ("solicitud_$uidEmisor".hashCode() and 0x7FFFFFFF).coerceAtLeast(700)
        mostrar(
            ctx = ctx,
            titulo = "🤝 Nueva solicitud de amistad",
            texto = "$nombreEmisor quiere ser tu amigo en Vaka. Toca para responder.",
            id = id,
            pestanaDestino = "amigos"
        )
    }

    /**
     * Notifica al usuario que su posición en el ranking de una Vaka compartida cambió.
     *
     * @param tipo "subio_a_1" cuando pasa al primer lugar, "perdio_1" cuando lo destronan
     */
    fun notificarTopVaka(
        ctx: Context,
        nombreVaka: String,
        tipo: String,
        otroNombre: String,
        codigoVaka: String
    ) {
        if (!Repo.loadNotif(ctx)) return
        crearCanal(ctx)
        val id = ("top_${codigoVaka}_$tipo".hashCode() and 0x7FFFFFFF).coerceAtLeast(800)
        val (titulo, texto) = when (tipo) {
            "subio_a_1" -> "👑 ¡Eres el #1 en \"$nombreVaka\"!" to
                "Lideras el ranking de aportes del equipo. ¡Sigue así, máquina! 🔥"
            "perdio_1" -> "🥈 $otroNombre te pasó en \"$nombreVaka\"" to
                "Quedaste segundo en el ranking de aportes. ¿Te vas a quedar así? 😏"
            else -> "🏆 Cambio en el ranking de \"$nombreVaka\"" to
                "Hubo movimiento en el top del equipo."
        }
        mostrar(ctx = ctx, titulo = titulo, texto = texto, id = id)
    }

    /**
     * Notifica un movimiento propio (depósito o retiro) en cualquier Vaka.
     * Muestra el nombre de la Vaka, el monto y una frase aleatoria con humor.
     */
    fun notificarMovimientoPropio(
        ctx: Context,
        nombreVaka: String,
        tipo: String,
        monto: Double,
        monedaCode: String,
        monedaSymbol: String,
        idMov: Long
    ) {
        if (!Repo.loadNotif(ctx)) return
        crearCanal(ctx)

        val cantidad = formatea(monto, monedaCode, monedaSymbol)
        val esDeposito = tipo == "deposito"

        val titulo = if (esDeposito) {
            "Depósito registrado"
        } else {
            "Retiro registrado"
        }

        val descripcion = if (esDeposito) {
            "+$cantidad en \"$nombreVaka\""
        } else {
            "−$cantidad de \"$nombreVaka\""
        }

        mostrar(
            ctx,
            titulo,
            descripcion,
            id = ("propio_$idMov".hashCode() % Int.MAX_VALUE).coerceAtLeast(500)
        )
    }

    /**
     * Notifica un movimiento nuevo de un compañero en una Vaka compartida.
     * Usa un id basado en hash del id del movimiento para no notificar duplicados.
     */
    fun notificarMovimientoCompanero(
        ctx: Context,
        nombreVaka: String,
        autorNombre: String,
        tipo: String,
        monto: Double,
        monedaCode: String,
        monedaSymbol: String,
        idMov: Long
    ) {
        if (!Repo.loadNotif(ctx)) return
        val prefs = ctx.getSharedPreferences("vaka_prefs", Context.MODE_PRIVATE)
        val key = "notif_mov_$idMov"
        if (prefs.getBoolean(key, false)) return  // ya notificado
        prefs.edit().putBoolean(key, true).apply()
        crearCanal(ctx)
        val emoji = if (tipo == "deposito") "💸" else "📤"
        val accion = if (tipo == "deposito") "depositó" else "retiró"
        val cantidad = formatea(monto, monedaCode, monedaSymbol)
        mostrar(
            ctx,
            "$emoji ${autorNombre.ifBlank { "Un compañero" }} $accion en \"$nombreVaka\"",
            "$cantidad · Entra a Vaka para verlo en detalle.",
            id = (idMov % Int.MAX_VALUE).toInt().coerceAtLeast(100)
        )
    }

    /**
     * Comparación motivadora: si un compañero te lleva ventaja considerable
     * en una Vaka con meta, te incentiva (una vez al día por Vaka).
     */
    fun notificarComparacionEquipo(
        ctx: Context,
        nombreVaka: String,
        nombreLider: String,
        diferenciaPct: Int,
        codigoVaka: String
    ) {
        if (!Repo.loadNotif(ctx)) return
        val prefs = ctx.getSharedPreferences("vaka_prefs", Context.MODE_PRIVATE)
        val hoy = java.time.LocalDate.now().toString()
        val key = "notif_comp_${codigoVaka}_$hoy"
        if (prefs.getBoolean(key, false)) return
        prefs.edit().putBoolean(key, true).apply()
        crearCanal(ctx)
        mostrar(
            ctx,
            "💪 ${nombreLider} te lleva ventaja en \"$nombreVaka\"",
            "Te lleva un $diferenciaPct% más arriba en su parte. ¡Dale, un pequeño aporte y te pones al día!",
            id = ("comp_$codigoVaka".hashCode() % Int.MAX_VALUE).coerceAtLeast(200)
        )
    }

    /** Programa la revisión diaria de recordatorios (sobrevive reinicios de la app). */
    fun programar(ctx: Context) {
        val solicitud = PeriodicWorkRequestBuilder<RecordatorioWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(4, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            TRABAJO, ExistingPeriodicWorkPolicy.KEEP, solicitud
        )
    }

    fun cancelar(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(TRABAJO)
    }
}

/**
 * Trabajador diario: revisa las Vakas (privadas + el resumen de las compartidas)
 * y envía como máximo un recordatorio con esta prioridad:
 *   1. Fechas límite que se acercan (30, 14, 7, 3 y 1 día)
 *   2. Hitos de progreso alcanzados (25 %, 50 %, 75 %, 100 %)
 *   3. Inactividad (4+ días sin aportes)
 * Cada hito o umbral se notifica una sola vez por Vaka.
 */
class RecordatorioWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val c = applicationContext
        if (!Repo.loadNotif(c)) return Result.success()
        Notificaciones.crearCanal(c)
        val prefs = c.getSharedPreferences("vaka_prefs", Context.MODE_PRIVATE)

        val hoyD = LocalDate.now()

        // === GUARDIA: máximo 1 notificación por día ===
        // Si en este día ya enviamos una notificación (deadline, hito, reto,
        // inactividad o motivacional), no enviamos más. Solo notificaciones
        // críticas (deadlines de hoy) pueden saltar esta guardia.
        val yaEnviadoHoy = prefs.getString("notif_algo_hoy", "") == hoyD.toString()

        val resumenes = Repo.load(c).map { v ->
            ResumenVaka(
                clave = claveDe(v), nombre = v.nombre,
                meta = v.meta, metaFecha = v.metaFecha,
                total = v.total,
                ultima = v.movs.maxOfOrNull { it.fecha } ?: "",
                monedaCode = v.monedaCode, monedaSymbol = v.monedaSymbol
            )
        } + Repo.loadResumenCompartidas(c)

        if (resumenes.isEmpty()) {
            if (!yaEnviadoHoy) {
                Notificaciones.mostrar(
                    c, "Tu Vaka te espera",
                    "Crea tu primera Vaka y empieza a ahorrar hoy."
                )
                prefs.edit().putString("notif_algo_hoy", hoyD.toString()).apply()
            }
            return Result.success()
        }

        // ---- 1) Fechas límite que se acercan ----
        val umbrales = listOf(30L, 14L, 7L, 3L, 1L)
        for (r in resumenes) {
            val f = r.metaFecha ?: continue
            val fecha = try { LocalDate.parse(f) } catch (e: Exception) { continue }
            val dias = ChronoUnit.DAYS.between(hoyD, fecha)
            if (dias < 0 || (r.meta > 0 && r.total >= r.meta)) continue
            val umbral = umbrales.firstOrNull { dias <= it } ?: continue
            val key = "notif_plazo_${r.clave}"
            val ultimoUmbral = prefs.getLong(key, Long.MAX_VALUE)
            if (umbral < ultimoUmbral) {
                prefs.edit().putLong(key, umbral).apply()
                val cuando = if (dias <= 1) "¡es mañana!" else "es en $dias días"
                val falta = if (r.meta > 0)
                    "Aún te falta ${formatea(r.meta - r.total, r.monedaCode, r.monedaSymbol)} para la meta. "
                else ""
                Notificaciones.mostrar(
                    c, "⏰ \"${r.nombre}\" $cuando",
                    "${falta}¡No te quedes colgado! 💪", 2
                )
                prefs.edit().putString("notif_algo_hoy", hoyD.toString()).apply()
                return Result.success()
            }
        }

        // ---- 2) Hitos de progreso ----
        // Las notificaciones de hito tienen prioridad pero igual respetan el
        // límite de 1/día si ya hubo otra notificación. Solo se saltan la
        // guardia si son hitos críticos (meta cumplida 100%).
        for (r in resumenes) {
            if (r.meta <= 0) continue
            val pct = (r.total / r.meta * 100).toInt()
            val hito = when {
                pct >= 100 -> 100
                pct >= 75 -> 75
                pct >= 50 -> 50
                pct >= 25 -> 25
                else -> 0
            }
            if (hito == 0) continue
            val key = "notif_hito_${r.clave}"
            if (hito > prefs.getInt(key, 0)) {
                // Para hitos no críticos, respetar la guardia diaria
                if (hito < 100 && yaEnviadoHoy) {
                    prefs.edit().putInt(key, hito).apply()
                    continue
                }
                prefs.edit().putInt(key, hito).apply()
                val (titulo, texto) = when (hito) {
                    100 -> "🏆 ¡Meta cumplida en \"${r.nombre}\"!" to
                        "Lo lograste. Entra a Vaka y celebra tu logro 🎉"
                    75 -> "🚀 ¡Vas en el 75 % de \"${r.nombre}\"!" to
                        "Entraste a la recta final. Un empujón más y lo logras."
                    50 -> "🎉 ¡Ya vas en el 50 % de \"${r.nombre}\"!" to
                        "La mitad del camino recorrido. ¡Sigue así! 💪"
                    else -> "🌱 ¡25 % de \"${r.nombre}\" completado!" to
                        "Buen comienzo. La constancia es la clave del ahorro."
                }
                Notificaciones.mostrar(c, titulo, texto, 3)
                prefs.edit().putString("notif_algo_hoy", hoyD.toString()).apply()
                return Result.success()
            }
        }

        // ---- 3) Inactividad ----
        // Solo si todavía no enviamos nada hoy
        if (yaEnviadoHoy) return Result.success()
        val ultimaFecha = resumenes.mapNotNull { it.ultima.ifBlank { null } }.maxOrNull()
        if (ultimaFecha != null) {
            val dias = try {
                ChronoUnit.DAYS.between(LocalDate.parse(ultimaFecha), hoyD)
            } catch (e: Exception) { 0L }
            val key = "notif_inactivo"
            val yaHoy = prefs.getString(key, "") == hoyD.toString()
            if (dias >= 4 && !yaHoy) {
                prefs.edit().putString(key, hoyD.toString()).apply()
                Notificaciones.mostrar(
                    c, "🐷 Tus Vakas te extrañan",
                    "Hace $dias días que no haces un aporte. Hasta el más pequeño suma: ¡dales de comer!", 4
                )
                // Marcamos que ya se mandó algo hoy para no duplicar
                prefs.edit().putString("notif_algo_hoy", hoyD.toString()).apply()
                return Result.success()
            }
        }

        // ---- 3.5) Motivacional DIARIA ----
        // Si en este día aún no se ha mandado ninguna otra notificación
        // (deadlines, hitos, inactividad), enviamos un mensaje motivacional
        // variado para mantener la presencia diaria sin abrumar al usuario.
        val keyDia = "notif_motivacional_dia"
        val yaMotivacionalHoy = prefs.getString(keyDia, "") == hoyD.toString()
        val algoYaHoy = prefs.getString("notif_algo_hoy", "") == hoyD.toString()
        if (!yaMotivacionalHoy && !algoYaHoy) {
            prefs.edit()
                .putString(keyDia, hoyD.toString())
                .putString("notif_algo_hoy", hoyD.toString())
                .apply()

            // Frases rotativas (selección por día del año para variedad)
            val frasesMotivacionales = listOf(
                "💪 ¡Buen día! Un pequeño aporte hoy es un gran logro mañana." to "Vaka",
                "🌱 La constancia vence al talento. Hazlo simple: aporta lo que puedas." to "Tu Vaka te espera",
                "💎 Cada peso que ahorras te acerca a algo importante." to "Hora de aportar",
                "🚀 Un día, un aporte, una meta más cerca." to "¡Vamos por hoy!",
                "🎯 ¿Y si hoy te sorprendes con cuánto puedes ahorrar?" to "Reto del día",
                "✨ Recordatorio amable: tu yo del futuro te lo agradecerá." to "Hola desde Vaka",
                "🐄 Tu Vaka muge feliz cuando le agregas plata." to "Mu mu mu",
                "💰 Pequeñas gotas hacen océanos. ¿Tu aporte de hoy?" to "Vaka",
                "🔥 La racha de ahorro es como una llama: aliméntala todos los días." to "Mantén la racha",
                "📈 Hoy es un buen día para hacer crecer tus Vakas." to "Buenos días",
                "⏰ Tu próximo aporte está a un toque de distancia." to "Vaka pendiente",
                "🌟 Quien ahorra hoy, vive tranquilo mañana." to "Vaka del día",
                "☕ Ahorra el equivalente a un café. Es más fácil de lo que crees." to "Idea del día",
                "🎁 Imagina cuánto podrías regalar(te) en 6 meses si ahorras hoy." to "Visualízalo",
                "🐷 ¡Cocha pidiéndote moneditas!" to "Tu alcancía habla"
            )
            // Rota según el día del año para no repetirse mucho
            val idx = hoyD.dayOfYear % frasesMotivacionales.size
            val (texto, titulo) = frasesMotivacionales[idx]

            Notificaciones.mostrar(c, titulo, texto, 5)
            return Result.success()
        }

        // ---- 4) Reto semanal ----
        // Distinguimos 3 momentos:
        //   a) DOMINGO: recordatorio "último día para cumplir el reto"
        //   b) LUNES: notificación de resultado (cumplido o fallado)
        // Cada notificación se manda una sola vez por semana usando una clave
        // distinta en SharedPreferences.
        val vakasLocales = Repo.load(c)
        val moneda = resumenes.firstOrNull()
            ?: ResumenVaka("", "", 0.0, null, 0.0, "", "COP", "$")

        // Función auxiliar: suma los depósitos de las vakas locales en un rango
        fun depositosEntre(inicio: LocalDate, fin: LocalDate): Double {
            var t = 0.0
            for (v in vakasLocales) for (m in v.movs) {
                if (m.tipo != "deposito") continue
                val fm = try { LocalDate.parse(m.fecha) } catch (e: Exception) { continue }
                if (!fm.isBefore(inicio) && !fm.isAfter(fin)) t += m.monto
            }
            return t
        }

        // === a) DOMINGO: último día para cumplir el reto ===
        if (hoyD.dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            val keyDomingo = "notif_reto_recordatorio"
            val semanaActual = "${hoyD.year}-${hoyD.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())}"
            if (prefs.getString(keyDomingo, "") != semanaActual) {
                // Semana actual (lunes pasado a hoy) vs semana anterior
                val inicioActual = hoyD.minusDays(6)  // lunes
                val finActual = hoyD                  // domingo (hoy)
                val inicioAnterior = inicioActual.minusDays(7)
                val finAnterior = inicioActual.minusDays(1)

                val depActual = depositosEntre(inicioActual, finActual)
                val depAnterior = depositosEntre(inicioAnterior, finAnterior)

                prefs.edit().putString(keyDomingo, semanaActual).apply()

                // Solo notificamos si hay un reto activo (semana anterior tuvo aportes)
                if (depAnterior > 0) {
                    val (titulo, texto) = when {
                        depActual > depAnterior ->
                            "✅ Hoy cierra el reto y vas ganando" to
                                "Llevas ${formatea(depActual, moneda.monedaCode, moneda.monedaSymbol)} esta semana " +
                                "vs ${formatea(depAnterior, moneda.monedaCode, moneda.monedaSymbol)} de la anterior. " +
                                "¡Asegura el reto con un aporte más! 🔒"
                        else -> {
                            val faltante = depAnterior - depActual + 1.0
                            "⏰ Hoy es el último día del reto" to
                                "Te faltan ${formatea(faltante, moneda.monedaCode, moneda.monedaSymbol)} " +
                                "para superar los ${formatea(depAnterior, moneda.monedaCode, moneda.monedaSymbol)} de la semana pasada. " +
                                "¡Aún puedes lograrlo! 🔥"
                        }
                    }
                    Notificaciones.mostrar(c, titulo, texto, 6)
                    prefs.edit().putString("notif_algo_hoy", hoyD.toString()).apply()
                    return Result.success()
                }
            }
        }

        // === b) LUNES: resultado del reto de la semana que acaba de cerrar ===
        if (hoyD.dayOfWeek == java.time.DayOfWeek.MONDAY) {
            val keyLunes = "notif_reto_resultado"
            val semanaActual = "${hoyD.year}-${hoyD.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())}"
            if (prefs.getString(keyLunes, "") != semanaActual) {
                // Semana que acaba de cerrar (lunes pasado a domingo pasado)
                val inicioCerrada = hoyD.minusDays(7)
                val finCerrada = hoyD.minusDays(1)
                // Semana de comparación (2 lunes atrás a 2 domingos atrás)
                val inicioAnterior = inicioCerrada.minusDays(7)
                val finAnterior = inicioCerrada.minusDays(1)

                val depCerrada = depositosEntre(inicioCerrada, finCerrada)
                val depAnterior = depositosEntre(inicioAnterior, finAnterior)

                prefs.edit().putString(keyLunes, semanaActual).apply()

                val (titulo, texto) = when {
                    // No había nada que superar (primera semana o sin historial)
                    depAnterior == 0.0 -> {
                        if (depCerrada > 0) {
                            "🚀 Nueva semana, nuevo reto" to
                                "La semana pasada ahorraste ${formatea(depCerrada, moneda.monedaCode, moneda.monedaSymbol)}. " +
                                "Esta semana intenta superarte 💪"
                        } else {
                            "🌱 Tu Vaka te espera" to
                                "No hubo movimiento la semana pasada. Empieza esta con un aporte, " +
                                "por pequeño que sea. ¡La constancia es la clave!"
                        }
                    }
                    // Reto cumplido
                    depCerrada > depAnterior ->
                        "🏆 ¡Cumpliste tu reto semanal!" to
                            "Ahorraste ${formatea(depCerrada, moneda.monedaCode, moneda.monedaSymbol)} " +
                            "vs ${formatea(depAnterior, moneda.monedaCode, moneda.monedaSymbol)} de la anterior. " +
                            "¿Lo repetimos esta semana? 🔥"
                    // Reto fallado
                    else ->
                        "💪 No cumpliste el reto" to
                            "Ahorraste ${formatea(depCerrada, moneda.monedaCode, moneda.monedaSymbol)} " +
                            "vs ${formatea(depAnterior, moneda.monedaCode, moneda.monedaSymbol)} de la anterior. " +
                            "Esta semana es una nueva oportunidad 🌱"
                }
                Notificaciones.mostrar(c, titulo, texto, 5)
                return Result.success()
            }
        }

        return Result.success()
    }
}
