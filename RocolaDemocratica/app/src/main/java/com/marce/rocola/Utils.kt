package com.marce.rocola

import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONTokener
import org.json.JSONObject
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import android.util.Log
import android.content.Intent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.snapshots.SnapshotStateList


// --------------------------------------------------------
// 🔁 YouTubeWebView (compartido)
// --------------------------------------------------------
@Composable
fun YouTubeWebView(
    modifier: Modifier = Modifier,
    webViewKey: Int,
    onWebViewReady: (WebView) -> Unit,
    onVideoDetectado: () -> Unit,
    context: Context
) {
    key(webViewKey) {
        AndroidView(
            factory = {
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webChromeClient = WebChromeClient()
                    loadUrl("https://m.youtube.com")

                    isVerticalScrollBarEnabled = true
                    overScrollMode = WebView.OVER_SCROLL_IF_CONTENT_SCROLLS
                    isNestedScrollingEnabled = true

                    onWebViewReady(this)

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            Log.d("YouTubeWebView", "URL cargada: $url")
                            if (url?.contains("/watch?v=") == true) {
                                onVideoDetectado()
                            }
                        }
                    }
                }
            },
            modifier = modifier
        )
    }
}

// --------------------------------------------------------
// 🎶 PlaylistView (compartido)
// --------------------------------------------------------
@Composable
fun PlaylistView(canciones: List<Cancion>, textoClaro: Color) {

    val index = canciones.indexOfFirst { !it.reproducida }
    val actualSonando = if (index > 0) canciones[index - 1] else null


    Column(modifier = Modifier.fillMaxSize()) {

        // 🎧 Barra "Ahora suena"
        actualSonando?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F4422)) // fondo verde oscuro suave
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "🎧 Ahora suena: ▶️ ${it.titulo}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // 📃 Lista de canciones
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(canciones) { cancion ->
                val esActual = cancion == actualSonando
                val estilo = when {
                    cancion.reproducida -> textoClaro.copy(alpha = 0.4f)
                    esActual -> Color.Green
                    else -> textoClaro
                }

                val icono = when {
                    esActual -> "▶️"
                    cancion.reproducida -> "✅"
                    else -> "🎵"
                }

                Text(
                    text = "$icono ${cancion.titulo}",
                    color = estilo,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )

                HorizontalDivider(
                    color = Color.DarkGray,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}




// --------------------------------------------------------
// 🟡 BotonesFlotantes (compartido)
// --------------------------------------------------------
@Composable
fun BotonesFlotantes(
    estadoAgregar: EstadoAgregar,
    onAgregar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .background(color = Color.Black, shape = RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.widthIn(min = 2.dp, max = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Button(
                    onClick = onAgregar,
                    enabled = estadoAgregar == EstadoAgregar.LISTO,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (estadoAgregar) {
                            EstadoAgregar.AGREGADA -> Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    when (estadoAgregar) {
                        EstadoAgregar.LISTO -> Text("Agregar", fontSize = 18.sp)
                        EstadoAgregar.ENVIANDO -> CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        EstadoAgregar.AGREGADA -> Text("✔️", fontSize = 18.sp)
                    }
                }

                Button(
                    onClick = onVolver,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Volver", fontSize = 18.sp)
                }
            }
        }
    }
}

// --------------------------------------------------------
// 🧠 obtenerInfoDesdeWebView (compartido)
// --------------------------------------------------------
fun obtenerInfoDesdeWebView(
    webView: WebView?,
    callback: (titulo: String, url: String) -> Unit
) {
    webView?.evaluateJavascript(
        """
        (function() {
            return JSON.stringify({
                titulo: document.title,
                url: window.location.href
            });
        })();
        """.trimIndent()
    ) { result ->
        try {
            val rawJson = result.removeSurrounding("\"").replace("\\", "")
            val json = JSONTokener(rawJson).nextValue() as JSONObject
            val titulo = json.getString("titulo").removeSuffix(" - YouTube").trim()
            //val url = json.getString("url")
            callback(titulo, json.getString("url")) // usar la URL real
        } catch (e: Exception) {
            callback("Video sin título", "")
        }
    }
}

// --------------------------------------------------------
// 🧠 Botón de Poderes para el Master
// --------------------------------------------------------
@Composable
fun BotonPoderes(
    onMostrarQR: () -> Unit,
    servidor: RocolaServidor?,
    canciones: SnapshotStateList<Cancion>,
    context: Context
) {
    var mostrarMenu by remember { mutableStateOf(false) }
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    Box {
        Button(
            onClick = { mostrarMenu = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFBB86FC),
                contentColor = Color.Black
            )
        ) {
            Text("\uD83E\uDE84 Poderes", fontSize = 18.sp)
        }

        DropdownMenu(
            expanded = mostrarMenu,
            onDismissRequest = { mostrarMenu = false },
            offset = DpOffset(x = 0.dp, y = (-115).dp)
        ) {
            // 🧹 Limpiar Playlist
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .shadow(8.dp, shape = RoundedCornerShape(8.dp))
                    .background(Color(0x802C2C2C), shape = RoundedCornerShape(8.dp))
                    .clickable {
                        mostrarConfirmacion = true
                        mostrarMenu = false
                    }
                    .padding(12.dp)
            ) {
                Text("🧹 Limpiar Playlist", color = Color.White)
            }

            // 📷 Mostrar QR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .shadow(8.dp, shape = RoundedCornerShape(8.dp))
                    .background(Color(0x802C2C2C), shape = RoundedCornerShape(8.dp))
                    .clickable {
                        onMostrarQR()
                        mostrarMenu = false
                    }
                    .padding(12.dp)
            ) {
                Text("📷 Mostrar QR", color = Color.White)
            }
        }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            confirmButton = {
                TextButton(onClick = {
                    servidor?.limpiarPlaylist()
                    canciones.clear()
                    canciones.addAll(GestorDePlaylist.obtenerPlaylist(context))
                    mostrarConfirmacion = false
                }) {
                    Text("Sí, limpiar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarConfirmacion = false
                }) {
                    Text("Cancelar")
                }
            },
            title = { Text("¿Limpiar Playlist?") },
            text = { Text("Esta acción eliminará todas las canciones.") },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

// FUNCION CORTO QUE PASA CANCIONES Y LARGO PARA SHARE

fun Modifier.clickYLongPress(
    context: Context,
    url: String,
    onClickNormal: () -> Unit
): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        detectTapGestures(
            onTap = {
                onClickNormal()
            },
            onLongPress = {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "🎶 Escuchá este tema: $url")
                    type = "text/plain"
                }
                val chooser = Intent.createChooser(intent, "Compartir canción")
                context.startActivity(chooser)
            }
        )
    }
)

