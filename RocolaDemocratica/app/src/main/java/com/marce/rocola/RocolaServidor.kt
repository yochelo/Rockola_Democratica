package com.marce.rocola

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import java.security.MessageDigest


class RocolaServidor(
    private val context: Context,
    port: Int = 8080
) : NanoHTTPD(port) {

    val playlistFile = File(this.context.filesDir, "playlist.json")

    // 🔔 Bandera para avisar si hay cambios
    private var actualizacionPendiente: Boolean = false

    // 🔁 Memoria RAM
    private var playlistEnMemoria: String = "[]"

    init {
        if (!playlistFile.exists()) {
            playlistFile.writeText("[]")
        }
        playlistEnMemoria = playlistFile.readText()
        Log.i("RocolaServidor", "🆕 Playlist cargada en memoria")
    }

    override fun serve(session: IHTTPSession): Response {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "📡 recib: ${session.uri}", Toast.LENGTH_SHORT).show()
        }
        return when (session.uri) {
            "/ping" -> NanoHTTPD.newFixedLengthResponse("pong")
            "/agregar", "/playlist/agregar" -> manejarAgregar(session)
            "/playlist" -> manejarPlaylist()
            "/hayUpdate" -> manejarHayUpdate()
            else -> NanoHTTPD.newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                NanoHTTPD.MIME_PLAINTEXT,
                "Ruta no encontrada"
            )
        }
    }

    fun escribirPlaylistDesdeJson(json: String) {
        playlistFile.writeText(json)
        playlistEnMemoria = json
        actualizacionPendiente = true
        Log.i("RocolaServidor", "📥 Playlist sincronizada")
    }

    private fun manejarHayUpdate(): Response {
        val huboCambios = actualizacionPendiente
        actualizacionPendiente = false
        return NanoHTTPD.newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            "{\"cambio\": $huboCambios}"
        )
    }


    private fun manejarAgregar(session: IHTTPSession): Response {
        if (session.method != Method.POST) {
            return jsonError("Usá POST", Response.Status.METHOD_NOT_ALLOWED)
        }

        // 👀 Log IP
        val ip = session.remoteIpAddress
        Log.w("RocolaServidor", "📥 POST desde $ip a /agregar")

        val body = mutableMapOf<String, String>()
        try {
            session.parseBody(body)
        } catch (e: Exception) {
            return jsonError("💥 Error parseando body", Response.Status.INTERNAL_ERROR)
        }

        val postData = body["postData"] ?: session.inputStream.bufferedReader().readText()
        val json = JSONObject(postData)
        val titulo = json.getString("titulo")
        val url = json.getString("url")

        val nuevaJson = JSONObject().apply {
            put("titulo", titulo)
            put("url", url)
            put("reproducida", false)
        }

        val jsonArray = JSONArray(playlistEnMemoria)

        // ❗ Verificar duplicados
        val yaExiste = (0 until jsonArray.length()).any {
            jsonArray.getJSONObject(it).getString("url") == url
        }

        if (yaExiste) {
            return JSONObject().apply {
                put("status", "ok")
                put("mensaje", "\uD83E\uDD14 Esta canción ya estaba en la lista")
            }.let {
                NanoHTTPD.newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    it.toString()
                )
            }
        }


        // ⛔ Límite de canciones
        if (jsonArray.length() >= 50) {
            return jsonError("⛔ Límite de 50 canciones alcanzado", Response.Status.FORBIDDEN)
        }

        // ✅ Agregar canción
        jsonArray.put(nuevaJson)
        playlistEnMemoria = jsonArray.toString(2)
        playlistFile.writeText(playlistEnMemoria)
        actualizacionPendiente = true


        return JSONObject().apply {
            put("status", "ok")
            put("mensaje", "Canción agregada")
        }.let {
            NanoHTTPD.newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                it.toString()
            )
        }
    }


    fun manejarPlaylist(): NanoHTTPD.Response {
        return try {
            val rawText = playlistFile.readText()  // el contenido real de la playlist

            val hash = MessageDigest.getInstance("MD5")
                .digest(rawText.toByteArray())
                .joinToString("") { "%02x".format(it) }

            val responseJson = JSONObject().apply {
                put("hash", hash)
                put("canciones", JSONArray(rawText))
            }

            NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                "application/json",
                responseJson.toString()
            )
        } catch (e: Exception) {
            NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.INTERNAL_ERROR,
                "text/plain",
                "Error al leer la playlist: ${e.message}"
            )
        }
    }


    private fun jsonError(mensaje: String, status: Response.Status): Response {
        return JSONObject().apply {
            put("status", "error")
            put("mensaje", mensaje)
        }.let {
            NanoHTTPD.newFixedLengthResponse(status, "application/json", it.toString())
        }
    }

    // 🧹 Limpieza manual (por botón de poderes)
    fun limpiarPlaylist() {
        playlistEnMemoria = "[]"
        playlistFile.writeText(playlistEnMemoria)
        Log.i("RocolaServidor", "🧼 Playlist limpiada")
    }
}

