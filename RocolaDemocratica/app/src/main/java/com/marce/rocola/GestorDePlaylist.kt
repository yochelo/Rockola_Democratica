package com.marce.rocola

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import android.util.Log

data class Cancion(
    val titulo: String,
    val url: String,
    val reproducida: Boolean = false,
    val enReproduccion: Boolean = false
)


/**
 * Gestor de la playlist persistente en JSON.
 * - Controla estado de reproducción y persistencia.
 * - Evita duplicados.
 * - Administra reproducción, pausa, reinicio.
 */
object GestorDePlaylist {

    private fun getArchivo(context: Context): File {
        return File(context.filesDir, "playlist.json")
    }

    fun obtenerPlaylist(context: Context): MutableList<Cancion> {
        val archivo = getArchivo(context)
        if (!archivo.exists()) return mutableListOf()

        val texto = archivo.readText()
        val jsonArray = JSONArray(texto)
        val lista = mutableListOf<Cancion>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            lista.add(
                Cancion(
                    titulo = obj.getString("titulo"),
                    url = obj.getString("url"),
                    reproducida = obj.optBoolean("reproducida", false),
                    enReproduccion = obj.optBoolean("enReproduccion", false)

            )
            )
        }

        return lista
    }

    fun guardarPlaylist(
        context: Context,
        canciones: List<Cancion>,
        servidor: RocolaServidor? = null
    ) {
        val jsonArray = JSONArray()
        for (cancion in canciones) {
            val obj = JSONObject().apply {
                put("titulo", cancion.titulo)
                put("url", cancion.url)
                put("reproducida", cancion.reproducida)
                put("enReproduccion", cancion.enReproduccion)
            }
            jsonArray.put(obj)
        }


        val jsonFinal = jsonArray.toString(2)

        // 💾 Escribimos en archivo
        getArchivo(context).writeText(jsonFinal)

        // 🔄 Actualizamos la playlist en RAM del servidor (si se lo pasaron)
        servidor?.escribirPlaylistDesdeJson(jsonFinal)

        Log.i("GestorDePlaylist", "🎼 Playlist guardada (${canciones.size} canciones)")
    }


    fun agregarCancion(context: Context, nueva: Cancion) {
        val lista = obtenerPlaylist(context)

        val yaExiste = lista.any { it.url == nueva.url }
        if (!yaExiste) {
            lista.add(nueva)
            guardarPlaylist(context, lista)
        }
    }

    /**
     * Cambia el estado de reproducción a la canción con `url` especificado.
     * Todas las anteriores quedan como reproducidas, la actual sonando.
     */
    fun forzarReproducir(context: Context, url: String) {
        val playlistFile = File(context.filesDir, "playlist.json")
        if (!playlistFile.exists()) return

        val raw = playlistFile.readText()
        val lista = JSONArray(raw)
        var reproducida = true

        for (i in 0 until lista.length()) {
            val cancion = lista.getJSONObject(i)
            cancion.put("reproducida", reproducida)

            if (cancion.getString("url") == url) {
                reproducida = false
            }
        }

        playlistFile.writeText(lista.toString())
    }

    fun marcarComoEnReproduccion(context: Context, url: String, servidor: RocolaServidor? = null) {
        val playlistFile = File(context.filesDir, "playlist.json")
        if (!playlistFile.exists()) return

        val raw = playlistFile.readText()
        val lista = JSONArray(raw)

        for (i in 0 until lista.length()) {
            val cancion = lista.getJSONObject(i)
            val enReproduccion = cancion.getString("url") == url
            cancion.put("enReproduccion", enReproduccion)
        }

        val jsonFinal = lista.toString(2)
        playlistFile.writeText(jsonFinal)
        servidor?.escribirPlaylistDesdeJson(jsonFinal)
    }




}

