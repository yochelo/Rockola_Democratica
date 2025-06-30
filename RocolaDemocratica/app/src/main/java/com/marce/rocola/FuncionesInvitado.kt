package com.marce.rocola

import android.content.Context
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface


fun obtenerIpLocal(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in interfaces) {
            val addresses = intf.inetAddresses
            for (addr in addresses) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress ?: "127.0.0.1"
                }
            }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
    return "127.0.0.1"
}

fun enviarCancionAlMaestro(
    context: Context,
    titulo: String,
    url: String,
    maestroIp: String,
    onResultado: (Boolean, String) -> Unit // 👈 ahora recibe mensaje también
) {
    val requestQueue = Volley.newRequestQueue(context)

    val jsonBody = JSONObject().apply {
        put("titulo", titulo)
        put("url", url)
    }

    val ipLimpia = maestroIp.removePrefix("http://").removeSuffix("/")
    val urlFinal = "http://$ipLimpia:8080/playlist/agregar"

    val request = object : StringRequest(
        Method.POST, urlFinal,
        { response ->
            val json = JSONObject(response)
            val mensaje = json.optString("mensaje", "🎵 Enviado")
            onResultado(true, mensaje)
        },
        { error ->
            onResultado(false, "❌ Error al enviar: ${error.message}")
        }
    ) {
        override fun getBodyContentType(): String = "application/json; charset=utf-8"
        override fun getBody(): ByteArray = jsonBody.toString().toByteArray(Charsets.UTF_8)
    }

    requestQueue.add(request)
}


fun obtenerPlaylistRemotaConHash(
    context: Context,
    ip: String,
    onResultado: (hash: String, canciones: List<Cancion>) -> Unit
) {
    val requestQueue = Volley.newRequestQueue(context)
    val url = "http://${ip.removePrefix("http://").removeSuffix("/")}:8080/playlist"

    val request = StringRequest(
        Request.Method.GET,
        url,
        { response ->
            try {
                val json = JSONObject(response)
                val hash = json.getString("hash")
                val jsonArray = json.getJSONArray("canciones")

                val lista = mutableListOf<Cancion>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val cancion = Cancion(
                        titulo = obj.getString("titulo"),
                        url = obj.getString("url"),
                        reproducida = obj.optBoolean("reproducida", false),
                        enReproduccion = obj.optBoolean("enReproduccion", false)
                    )
                    lista.add(cancion) // 💥 ESTO FALTABA
                }


                onResultado(hash, lista)

            } catch (e: Exception) {
                Toast.makeText(context, "💥 Error parseando JSON", Toast.LENGTH_SHORT).show()
            }
        },
        {
            Toast.makeText(context, "❌ No se pudo obtener playlist", Toast.LENGTH_SHORT).show()
        }
    )

    requestQueue.add(request)
}


// Paso 1: Enum para el estado del botón
enum class EstadoAgregar {
    LISTO,
    ENVIANDO,
    AGREGADA
}
