// 🚀 Plan Z definitivo: Servidor como Foreground Service inmortal

package com.marce.rocola

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat

class RocolaService : Service() {

    private var servidor: RocolaServidor? = null
    private val channelId = "rocola_channel"
    private val notificationId = 1
    private var rol: String = "desconocido"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        rol = intent?.getStringExtra("rol") ?: "desconocido"

        crearCanalDeNotificacion()
        iniciarEnPrimerPlano(rol)

        if (rol == "maestro") {
            if (servidor == null || servidor?.isAlive == false) {
                try {
                    servidor = RocolaServidor(this).apply { start() }
                    Toast.makeText(this, "✅ Servidor iniciado como Foreground", Toast.LENGTH_SHORT).show()
                    Log.i("RocolaService", "Servidor iniciado")
                } catch (e: Exception) {
                    Toast.makeText(this, "❌ Error al iniciar servidor: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("RocolaService", "Fallo al iniciar servidor", e)
                }
            } else {
                Log.w("RocolaService", "⚠️ El servidor ya estaba corriendo.")
            }
        }


        return START_NOT_STICKY

    }

    private fun crearCanalDeNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Rocola Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun iniciarEnPrimerPlano(rol: String) {
        val stopIntent = Intent(this, StopServiceReceiver::class.java).apply {
            putExtra("rol", rol)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val action = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Detener",
            pendingIntent
        ).build()

        val texto = when (rol) {
            "maestro" -> "🎵 Servidor activo para invitados"
            "invitado" -> "🧑‍🤝‍🧑 Conectado al DJ"
            else -> "Modo desconocido"
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Rocola Democrática")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(action)
            .setOngoing(false)
            .build()

        startForeground(notificationId, notification)
    }

    override fun onDestroy() {
        servidor?.stop()
        servidor = null
        Log.i("RocolaService", "Servidor detenido (si existía)")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }

        super.onDestroy()
        servidor?.playlistFile?.writeText("[]")
        Log.i("RocolaService", "🧹 Playlist reseteada al detener el servicio")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
class StopServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val rol = intent.getStringExtra("rol")
        Toast.makeText(context, "📛 STOP ($rol)", Toast.LENGTH_SHORT).show()
        Log.i("StopServiceReceiver", "📛 Recibido: $rol")

        val stopResult = context.stopService(Intent(context, RocolaService::class.java))
        Log.i("StopServiceReceiver", "¿Se detuvo? → $stopResult")
    }
}

