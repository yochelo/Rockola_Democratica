package com.marce.rocola

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.*
import android.os.Handler
import android.os.Looper

class YouTubeAutoplayService : NotificationListenerService() {

    private var ultimoEstado = ""
    private var ultimoTitulo = ""
    private var esperandoSiguiente = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.i("YouTubeAutoplay", "🎵 Servicio iniciado")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let { procesarNotificacion(it) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.let { procesarNotificacion(it) }
    }

    private fun procesarNotificacion(sbn: StatusBarNotification) {
        // Solo procesar notificaciones de YouTube
        if (sbn.packageName != "com.google.android.youtube") return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val titulo = extras.getString("android.title") ?: ""
        val texto = extras.getString("android.text") ?: ""

        Log.d("YouTubeAutoplay", "📱 Notificación: título='$titulo', texto='$texto'")

        // Detectar cuando la música se pausa/termina
        val estaPausado = texto.contains("Pausado", ignoreCase = true) ||
                texto.contains("Paused", ignoreCase = true) ||
                titulo.isEmpty() && texto.isEmpty()

        val estaReproduciendo = texto.contains("Reproduciendo", ignoreCase = true) ||
                texto.contains("Playing", ignoreCase = true) ||
                (!estaPausado && titulo.isNotEmpty())

        val estadoActual = when {
            estaReproduciendo -> "reproduciendo"
            estaPausado -> "pausado"
            else -> "desconocido"
        }

        // Detectar cambio de estado
        if (estadoActual != ultimoEstado) {
            Log.i("YouTubeAutoplay", "🔄 Cambio de estado: $ultimoEstado → $estadoActual")

            when {
                ultimoEstado == "reproduciendo" && estadoActual == "pausado" -> {
                    // La música se pausó, podría haber terminado
                    Log.i("YouTubeAutoplay", "⏸️ Música pausada, iniciando timer...")
                    iniciarTimerAutoplay()
                }

                ultimoEstado == "pausado" && estadoActual == "reproduciendo" -> {
                    // Se reanudó, cancelar autoplay
                    Log.i("YouTubeAutoplay", "▶️ Música reanudada, cancelando autoplay")
                    cancelarTimerAutoplay()
                }
            }

            ultimoEstado = estadoActual
            ultimoTitulo = titulo
        }
    }

    private fun iniciarTimerAutoplay() {
        cancelarTimerAutoplay() // Cancelar timer anterior si existe

        timerJob = scope.launch {
            delay(3000) // Esperar 3 segundos para confirmar que realmente terminó

            // Verificar que no se haya reanudado
            if (ultimoEstado == "pausado" && !esperandoSiguiente) {
                Log.i("YouTubeAutoplay", "🎯 Ejecutando autoplay...")
                esperandoSiguiente = true
                reproducirSiguienteCancion()
            }
        }
    }

    private fun cancelarTimerAutoplay() {
        timerJob?.cancel()
        timerJob = null
        esperandoSiguiente = false
    }

    private fun reproducirSiguienteCancion() {
        try {
            val canciones = GestorDePlaylist.obtenerPlaylist(this)
            val siguienteIndex = canciones.indexOfFirst { !it.reproducida }

            if (siguienteIndex >= 0) {
                val siguienteCancion = canciones[siguienteIndex]

                Log.i("YouTubeAutoplay", "🎵 Reproduciendo siguiente: ${siguienteCancion.titulo}")

                // Marcar como en reproducción
                GestorDePlaylist.forzarReproducir(this, siguienteCancion.url)

                // Abrir en YouTube
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(siguienteCancion.url)).apply {
                    setPackage("com.google.android.youtube")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                startActivity(intent)

                // Reset del flag después de un delay
                Handler(Looper.getMainLooper()).postDelayed({
                    esperandoSiguiente = false
                }, 5000)

            } else {
                Log.i("YouTubeAutoplay", "✅ No hay más canciones en la playlist")
                esperandoSiguiente = false
            }

        } catch (e: Exception) {
            Log.e("YouTubeAutoplay", "❌ Error reproduciendo siguiente canción: ${e.message}")
            esperandoSiguiente = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        Log.i("YouTubeAutoplay", "🛑 Servicio detenido")
    }
}