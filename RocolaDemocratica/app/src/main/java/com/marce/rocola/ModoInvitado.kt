package com.marce.rocola

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import android.widget.Toast
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.net.URL


@Composable
fun PantallaModoInvitado(ip: String) {
    val fondoOscuro = Color(0xFF121212)
    val context = LocalContext.current
    val textoClaro = Color.White
    var estaPrevisualizando by remember { mutableStateOf(false) }
    val canciones = remember { mutableStateListOf<Cancion>() }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var webViewKey by remember { mutableIntStateOf(0) }
    var yaAgregada by remember { mutableStateOf(false) }
    var ultimaListaHash by remember { mutableStateOf("") }
    var estaExpandido by remember { mutableStateOf(false) }
    val ipDelMaestro = remember { mutableStateOf("") }


    LaunchedEffect(ipDelMaestro.value) {
        if (ipDelMaestro.value.isBlank()) return@LaunchedEffect

        while (true) {
            delay(3000)

            try {
                val respuesta = URL("http://${ipDelMaestro.value}:8080/hayUpdate").readText()
                val cambio = JSONObject(respuesta).optBoolean("cambio", false)

                if (cambio) {
                    val raw = URL("http://${ipDelMaestro.value}:8080/playlist").readText()
                    val json = JSONObject(raw)
                    val cancionesJson = json.getJSONArray("canciones")
                    val nuevas = mutableListOf<Cancion>()

                    for (i in 0 until cancionesJson.length()) {
                        val obj = cancionesJson.getJSONObject(i)
                        nuevas.add(
                            Cancion(
                                titulo = obj.getString("titulo"),
                                url = obj.getString("url"),
                                reproducida = obj.optBoolean("reproducida", false),
                                enReproduccion = obj.optBoolean("enReproduccion", false)
                            )
                        )
                    }

                    canciones.clear()
                    canciones.addAll(nuevas)
                }

            } catch (e: Exception) {
                // Toast o log si querés
            }
        }
    }

    // 🧼 Al ingresar por primera vez, limpiar lo viejo y clonar el estado   actual del Maestro
    LaunchedEffect(key1 = "fetchInicial") {
        canciones.clear() // elimina basura de sesiones anteriores
        ultimaListaHash = ""

        obtenerPlaylistRemotaConHash(context, ip) { nuevoHash, lista ->
            if (nuevoHash != ultimaListaHash) {
                ultimaListaHash = nuevoHash
                canciones.clear()
                canciones.addAll(lista)
            }
        }

    }

    var estadoAgregar by remember { mutableStateOf(EstadoAgregar.LISTO) }

    // 🔁 Obtener la playlist
    var lastKnownUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val actual = webViewRef?.url ?: ""

            if (actual != lastKnownUrl) {
                lastKnownUrl = actual
            }

            val esVideo = actual.contains("/watch?v=")

            if (esVideo && !estaPrevisualizando) {
                estaPrevisualizando = true
                estadoAgregar = EstadoAgregar.LISTO // 🔁 reactiva el botón al tocar una nueva canción
            }


            if (!esVideo && estaPrevisualizando) {
                estaPrevisualizando = false
                estadoAgregar = EstadoAgregar.LISTO // 🔁 reactiva el botón al salir del video
            }


            delay(500)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (!estaPrevisualizando) {
                obtenerPlaylistRemotaConHash(context, ip) { hash, lista ->
                    if (hash != ultimaListaHash) {
                        ultimaListaHash = hash
                        canciones.clear()
                        canciones.addAll(lista)
                    }
                }
            }
            delay(8000)
        }
    }

    BackHandler {
        if (estaPrevisualizando) {
            estaPrevisualizando = false
            webViewRef?.loadUrl("https://m.youtube.com")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fondoOscuro)
            ) {
                Text(
                    text = "Playlist 🎷🎶",
                    color = textoClaro,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .statusBarsPadding()
                )

                ListaDeReproduccion(
                    canciones = canciones,
                    context = context
                )


                HorizontalDivider(
                    color = Color.Gray,
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // WebView renderizado encima pero pegado abajo
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter) // 👈 Lo mueve visualmente abajo
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable { estaExpandido = !estaExpandido }
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (estaExpandido)
                            "\uD83D\uDC49      TOCA para minimizar      ⬇\uFE0F "
                        else
                            "\uD83D\uDC49   º      TOCA para MAXIMIZAR      ⬆\uFE0F ",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                YouTubeWebView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (estaExpandido || estaPrevisualizando) 500.dp else 80.dp),
                    webViewKey = webViewKey,
                    onWebViewReady = { webViewRef = it },
                    onVideoDetectado = {
                        estaPrevisualizando = true
                        estaExpandido = true
                    },
                    context = context
                )
            }
        }


        if (estaPrevisualizando) {
            BotonesFlotantes(
                estadoAgregar = estadoAgregar,
                onAgregar = {
                    obtenerInfoDesdeWebView(webViewRef) { titulo, url ->
                        estadoAgregar = EstadoAgregar.ENVIANDO

                        enviarCancionAlMaestro(context, titulo, url, ip) { exito, mensaje ->
                            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()

                            if (exito && !mensaje.contains("ya estaba", ignoreCase = true)) {
                                obtenerPlaylistRemotaConHash(context, ip) { nuevoHash, lista ->
                                    if (nuevoHash != ultimaListaHash) {
                                        ultimaListaHash = nuevoHash
                                        canciones.clear()
                                        canciones.addAll(lista)
                                    }
                                    estadoAgregar = EstadoAgregar.AGREGADA
                                }
                            } else if (!exito) {
                                estadoAgregar = EstadoAgregar.LISTO
                                Toast.makeText(
                                    context,
                                    "❌ Error al enviar al Maestro",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                estadoAgregar = EstadoAgregar.AGREGADA
                            }
                        }
                    }
                }
                ,
                onVolver = {
                    estaPrevisualizando = false
                    yaAgregada = false
                    webViewKey++
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .zIndex(10f)
            )
        }

    }
}
