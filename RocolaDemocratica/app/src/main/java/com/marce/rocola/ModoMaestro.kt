package com.marce.rocola

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import android.content.Context
import androidx.compose.ui.zIndex
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.foundation.clickable
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.unit.sp
import android.provider.Settings
import android.content.ComponentName


// --------------------------------------------------------
// 🧠 FUNCION QR generadora
// --------------------------------------------------------
fun generarQrDesdeTexto(texto: String, tamano: Int = 600): Bitmap {
    val bitMatrix: BitMatrix = MultiFormatWriter().encode(
        texto,
        BarcodeFormat.QR_CODE,
        tamano,
        tamano
    )

    val bitmap = Bitmap.createBitmap(tamano, tamano, Bitmap.Config.RGB_565)
    for (x in 0 until tamano) {
        for (y in 0 until tamano) {
            bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}

// Función para verificar si el servicio está habilitado
fun isNotificationServiceEnabled(context: Context): Boolean {
    val packageName = context.packageName
    val flat = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )

    if (flat != null && flat.isNotEmpty()) {
        val names = flat.split(":")
        return names.any {
            ComponentName.unflattenFromString(it)?.packageName == packageName
        }
    }
    return false
}

// Función para abrir la configuración de acceso a notificaciones
fun openNotificationListenerSettings(context: Context) {
    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

@Composable
fun PantallaModoMaestro(servidor: RocolaServidor?) {
    val context = LocalContext.current
    var mostrarQr by remember { mutableStateOf(true) }
    var autoplayHabilitado by remember { mutableStateOf(false) }
    var mostrarDialogoAutoplay by remember { mutableStateOf(false) }

    val canciones = remember { mutableStateListOf<Cancion>() }
    val ip = obtenerIpLocal()
    val url = "http://$ip:8080/playlist"

    LaunchedEffect(Unit) {
        autoplayHabilitado = isNotificationServiceEnabled(context)
    }

    BackHandler(enabled = mostrarQr) {
        mostrarQr = false
    }

    // Diálogo para habilitar autoplay
    if (mostrarDialogoAutoplay) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoAutoplay = false },
            confirmButton = {
                TextButton(onClick = {
                    openNotificationListenerSettings(context)
                    mostrarDialogoAutoplay = false
                }) {
                    Text("Habilitar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoAutoplay = false
                }) {
                    Text("Después")
                }
            },
            title = { Text("🎵 Autoplay Disponible") },
            text = {
                Text("¿Querés habilitar el autoplay automático? Las canciones pasarán solas cuando terminen.\n\nBuscá 'Rocola Autoplay' en la lista y activalo.")
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (mostrarQr) {
        // Mostrar QR con indicador de autoplay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🎧 Modo Maestro", style = MaterialTheme.typography.titleLarge)

                // Indicador de autoplay
                Text(
                    text = if (autoplayHabilitado) "🤖 AUTO" else "👤 MANUAL",
                    color = if (autoplayHabilitado) Color.Green else Color(0xFFFFA500),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(
                            color = if (autoplayHabilitado) Color.Green.copy(alpha = 0.2f)
                            else Color(0xFFFFA500).copy(alpha = 0.2f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .clickable {
                            if (!autoplayHabilitado) {
                                mostrarDialogoAutoplay = true
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val qrBitmap = generarQrDesdeTexto(url)
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "Código QR con IP del Maestro",
                modifier = Modifier.size(300.dp)
            )
            Text("📡 Escaneá este QR para ver la playlist")
            Text("🌐 IP: $ip", color = Color.Gray)

            if (!autoplayHabilitado) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { mostrarDialogoAutoplay = true }) {
                    Text("🤖 Habilitar autoplay (opcional)", color = Color(0xFFFFA500))
                }
            }
        }

    } else {
        // Playlist con botón de poderes
        val urlActual = remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            while (true) {
                delay(2000)
                canciones.clear()
                canciones.addAll(GestorDePlaylist.obtenerPlaylist(context))

                // Actualizar estado del autoplay
                autoplayHabilitado = isNotificationServiceEnabled(context)
            }
        }

        MostrarPlaylistMaestro(
            canciones = canciones,
            onLimpiar = { servidor?.playlistFile?.writeText("[]") },
            onMostrarQR = { mostrarQr = true },
            context = context,
            urlActual = urlActual,
            servidor = servidor,
            autoplayHabilitado = autoplayHabilitado,
            onHabilitarAutoplay = { mostrarDialogoAutoplay = true }
        )
    }
}

@Composable
fun MostrarPlaylistMaestro(
    canciones: SnapshotStateList<Cancion>,
    onLimpiar: () -> Unit,
    onMostrarQR: () -> Unit,
    context: Context,
    urlActual: MutableState<String>,
    servidor: RocolaServidor?,
    autoplayHabilitado: Boolean,
    onHabilitarAutoplay: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        val index = canciones.indexOfFirst { !it.reproducida }
        val actual = if (index > 0) canciones[index - 1] else null

        Column(modifier = Modifier.fillMaxSize()) {
            // Header con indicador de autoplay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎸🎶 Playlist",
                    fontSize = 24.sp,
                    color = Color.White
                )

                // Indicador de autoplay
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(
                            color = if (autoplayHabilitado) Color.Green.copy(alpha = 0.2f)
                            else Color(0xFFFFA500).copy(alpha = 0.2f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            if (!autoplayHabilitado) onHabilitarAutoplay()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (autoplayHabilitado) "🤖" else "👤",
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (autoplayHabilitado) "AUTO" else "MANUAL",
                        color = if (autoplayHabilitado) Color.Green else Color(0xFFFFA500),
                        fontSize = 10.sp
                    )
                }
            }

            // Lista de canciones
            canciones.forEach { cancion ->
                val indexActual = canciones.indexOfFirst { !it.reproducida }
                val cancionActual = if (indexActual > 0) canciones[indexActual - 1] else null
                val estaReproduciendo = cancion == cancionActual

                val textoColor = when {
                    estaReproduciendo -> Color(0xFF00FF00)
                    cancion.reproducida -> Color(0xFFAAAAAA)
                    else -> Color.White
                }

                val tamanoTexto = if (estaReproduciendo) 20.sp else 16.sp
                val emoji = when {
                    estaReproduciendo -> ""  // sin ícono como en backup
                    cancion.reproducida -> "✓"
                    else -> "🎵"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            urlActual.value = cancion.url
                            GestorDePlaylist.forzarReproducir(context, cancion.url)

                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cancion.url))
                            intent.setPackage("com.google.android.youtube")
                            context.startActivity(intent)
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$emoji ${cancion.titulo}",
                        color = textoColor,
                        fontSize = tamanoTexto
                    )
                }
            }

            // Mensaje informativo si no hay autoplay
            if (!autoplayHabilitado && canciones.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(
                            Color(0xFFFFA500).copy(alpha = 0.1f),
                            androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .clickable { onHabilitarAutoplay() }
                        .padding(12.dp)
                ) {
                    Text(
                        text = "💡 Tocá aquí para habilitar autoplay y que las canciones pasen automáticamente",
                        color = Color(0xFFFFA500),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Botón de poderes arriba a la derecha
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .zIndex(999f)
        ) {
            BotonPoderes(
                onMostrarQR = onMostrarQR,
                servidor = servidor,
                canciones = canciones,
                context = context
            )
        }
    }
}