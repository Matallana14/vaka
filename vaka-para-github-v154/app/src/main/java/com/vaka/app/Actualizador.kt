package com.vaka.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sistema simple de actualización over-the-air:
 *  - Consulta el endpoint público de GitHub Releases (https://api.github.com/repos/{owner}/{repo}/releases/latest)
 *  - Compara la versión publicada con la versión instalada (BuildConfig.VERSION_NAME)
 *  - Si hay versión nueva: descarga el APK al directorio interno de la app
 *    y abre el instalador nativo de Android
 *
 *  Para que esto funcione, cada vez que subas una release nueva en GitHub:
 *   1. Sube el archivo .apk como "asset" del release.
 *   2. Pon como nombre del tag de la release la versión, por ejemplo: v1.1, v1.2, etc.
 *   3. La nota del release puede contener el changelog (la app lo muestra al usuario).
 */
object Actualizador {

    // === CONFIGURA AQUÍ tu repositorio de GitHub ===
    private const val GH_OWNER = "Matallana14"
    private const val GH_REPO = "vaka-releases"
    // Si el repo es PRIVADO, pega aquí tu token personal de GitHub.
    // Si el repo es PÚBLICO, déjalo vacío ("").
    // Ver instrucciones en el README sobre cómo generar el token.
    private const val GH_TOKEN = ""

    data class Resultado(
        val hayActualizacion: Boolean,
        val versionRemota: String = "",
        val versionLocal: String = "",
        val notas: String = "",
        val urlApk: String = "",
        val error: String? = null,
    )

    /** Consulta GitHub y dice si hay versión nueva. */
    fun buscar(ctx: Context, onResultado: (Resultado) -> Unit) {
        Thread {
            try {
                // Usamos /releases (lista) en vez de /releases/latest porque este último
                // a veces no devuelve datos para repos privados o tiene problemas con
                // la marca "Latest". La lista siempre devuelve todas, ordenadas de la
                // más reciente a la más antigua.
                val url = URL("https://api.github.com/repos/$GH_OWNER/$GH_REPO/releases?per_page=10")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "Vaka-App")
                    if (GH_TOKEN.isNotBlank()) {
                        setRequestProperty("Authorization", "Bearer $GH_TOKEN")
                    }
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                val codigo = conn.responseCode
                if (codigo == 404) {
                    onResultado(Resultado(
                        hayActualizacion = false,
                        versionLocal = versionInstalada(ctx)
                    ))
                    return@Thread
                }
                if (codigo == 401 || codigo == 403) {
                    onResultado(Resultado(false, error = "Error: el token de GitHub no tiene permisos o expiró."))
                    return@Thread
                }
                if (codigo != 200) {
                    onResultado(Resultado(false, error = "Error: el servidor respondió $codigo."))
                    return@Thread
                }
                val texto = conn.inputStream.bufferedReader().use { it.readText() }

                val arr = org.json.JSONArray(texto)
                if (arr.length() == 0) {
                    // No hay releases publicadas
                    onResultado(Resultado(
                        hayActualizacion = false,
                        versionLocal = versionInstalada(ctx)
                    ))
                    return@Thread
                }

                // Buscamos la primera release que NO sea pre-release ni draft, y que
                // tenga al menos un APK adjunto.
                var releaseElegida: JSONObject? = null
                for (i in 0 until arr.length()) {
                    val r = arr.getJSONObject(i)
                    if (r.optBoolean("draft", false)) continue
                    if (r.optBoolean("prerelease", false)) continue
                    releaseElegida = r
                    break
                }
                if (releaseElegida == null) releaseElegida = arr.getJSONObject(0)

                val tag = releaseElegida.optString("tag_name", "")
                    .removePrefix("v").removePrefix("V").trim()
                val notas = releaseElegida.optString("body", "")

                // Buscar el primer asset que termine en .apk
                val assets = releaseElegida.optJSONArray("assets")
                var apkUrl = ""
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        val nombre = a.optString("name", "")
                        if (nombre.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = if (GH_TOKEN.isNotBlank()) {
                                a.optString("url", "")
                            } else {
                                a.optString("browser_download_url", "")
                            }
                            break
                        }
                    }
                }
                if (apkUrl.isBlank()) {
                    onResultado(Resultado(false, versionRemota = tag, error = "Error: la release no tiene un APK adjunto."))
                    return@Thread
                }
                val local = versionInstalada(ctx)
                val hayNueva = esVersionMayor(tag, local)
                onResultado(Resultado(
                    hayActualizacion = hayNueva,
                    versionRemota = tag,
                    versionLocal = local,
                    notas = notas,
                    urlApk = apkUrl
                ))
            } catch (e: Exception) {
                android.util.Log.e("VakaOTA", "Error al buscar", e)
                onResultado(Resultado(false, error = "Error: sin conexión (${e.javaClass.simpleName})."))
            }
        }.start()
    }

    /** Descarga el APK y lanza el instalador. onProgreso recibe 0..100. */
    fun descargarEInstalar(
        ctx: Context,
        urlApk: String,
        onProgreso: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val url = URL(urlApk)
                val conn0 = (url.openConnection() as HttpURLConnection).apply {
                    // No seguimos redirect automáticamente para no filtrar el token al CDN externo
                    instanceFollowRedirects = false
                    connectTimeout = 20000
                    readTimeout = 60000
                    if (GH_TOKEN.isNotBlank()) {
                        setRequestProperty("Authorization", "Bearer $GH_TOKEN")
                        setRequestProperty("Accept", "application/octet-stream")
                    }
                    setRequestProperty("User-Agent", "Vaka-App")
                }
                // GitHub responde con 302 y "Location" apuntando al CDN real.
                // Seguimos ese redirect SIN el token (el CDN ya trae firma temporal).
                val conn: HttpURLConnection = if (conn0.responseCode in 300..399) {
                    val redir = conn0.getHeaderField("Location")
                    conn0.disconnect()
                    (URL(redir).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = true
                        connectTimeout = 20000
                        readTimeout = 60000
                        setRequestProperty("User-Agent", "Vaka-App")
                    }
                } else {
                    conn0
                }
                val total = conn.contentLength
                val carpeta = File(ctx.getExternalFilesDir(null), "updates")
                carpeta.mkdirs()
                val archivo = File(carpeta, "vaka-update.apk")
                if (archivo.exists()) archivo.delete()

                conn.inputStream.use { entrada ->
                    FileOutputStream(archivo).use { salida ->
                        val buffer = ByteArray(8 * 1024)
                        var leidos = 0L
                        var n: Int
                        var ultimoPct = -1
                        while (entrada.read(buffer).also { n = it } > 0) {
                            salida.write(buffer, 0, n)
                            leidos += n
                            if (total > 0) {
                                val pct = ((leidos * 100) / total).toInt()
                                if (pct != ultimoPct) {
                                    ultimoPct = pct
                                    onProgreso(pct)
                                }
                            }
                        }
                    }
                }
                onProgreso(100)
                lanzarInstalador(ctx, archivo)
            } catch (e: Exception) {
                onError("Error al descargar: ${e.javaClass.simpleName}")
            }
        }.start()
    }

    private fun lanzarInstalador(ctx: Context, archivo: File) {
        val uri: Uri = FileProvider.getUriForFile(
            ctx, "${ctx.packageName}.fileprovider", archivo
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        ctx.startActivity(intent)
    }

    private fun versionInstalada(ctx: Context): String =
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0"
        } catch (e: Exception) { "0" }

    /** Compara "1.2.0" > "1.1.5" devolviendo true si remota > local. */
    private fun esVersionMayor(remota: String, local: String): Boolean {
        val r = remota.trim().split(".", "-", "_", " ").mapNotNull { it.trim().toIntOrNull() }
        val l = local.trim().split(".", "-", "_", " ").mapNotNull { it.trim().toIntOrNull() }
        val n = maxOf(r.size, l.size)
        for (i in 0 until n) {
            val ri = r.getOrNull(i) ?: 0
            val li = l.getOrNull(i) ?: 0
            if (ri > li) return true
            if (ri < li) return false
        }
        return false
    }
}
