package com.vaka.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/** Tabla de tasas de cambio con USD como base. */
data class TablaTasas(val tasas: Map<String, Double>, val fecha: String)

object Tasas {
    private const val PREFS = "vaka_prefs"
    private const val KEY_JSON = "tasas_json"
    private const val KEY_FECHA = "tasas_fecha"

    // API gratuita de tasas de referencia diarias (sin necesidad de clave)
    private const val API = "https://open.er-api.com/v6/latest/USD"

    /**
     * Descarga las tasas del día. Si no hay internet,
     * devuelve las últimas guardadas en el teléfono.
     */
    suspend fun obtener(ctx: Context): TablaTasas? = withContext(Dispatchers.IO) {
        try {
            val texto = URL(API).readText()
            val o = JSONObject(texto)
            if (o.optString("result") == "success") {
                val rates = o.getJSONObject("rates")
                val fecha = o.optString("time_last_update_utc", "").take(16).ifBlank { hoy() }
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY_JSON, rates.toString())
                    .putString(KEY_FECHA, fecha)
                    .apply()
                return@withContext TablaTasas(parsear(rates), fecha)
            }
        } catch (e: Exception) {
            // sin conexión o error: caemos a la caché
        }
        cache(ctx)
    }

    /** Últimas tasas guardadas en el teléfono (para uso sin internet). */
    fun cache(ctx: Context): TablaTasas? {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val s = p.getString(KEY_JSON, null) ?: return null
        return try {
            TablaTasas(parsear(JSONObject(s)), p.getString(KEY_FECHA, "") ?: "")
        } catch (e: Exception) { null }
    }

    private fun parsear(rates: JSONObject): Map<String, Double> {
        val m = mutableMapOf<String, Double>()
        rates.keys().forEach { k -> m[k] = rates.optDouble(k, 0.0) }
        return m
    }

    /**
     * Convierte un monto entre dos monedas usando USD como puente.
     * Devuelve null si alguna de las monedas no tiene tasa (p. ej. monedas personalizadas).
     */
    fun convertir(tabla: TablaTasas, monto: Double, de: String, a: String): Double? {
        val rDe = tabla.tasas[de.trim().uppercase()] ?: return null
        val rA = tabla.tasas[a.trim().uppercase()] ?: return null
        if (rDe <= 0.0) return null
        return monto / rDe * rA
    }
}
