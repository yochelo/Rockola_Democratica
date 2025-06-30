package com.marce.rocola

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.marce.rocola.ui.theme.MyApplicationTheme
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 👇 PEDIMOS EL PERMISO PARA NOTIFICACIONES SI ES ANDROID 13+
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
        setContent {
            MyApplicationTheme {
                PantallaSelectorDeRol()
            }
        }
    }
}

@Composable
fun PantallaSelectorDeRol() {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // <-- fondo negro real
    ) {
        // Fondo con la imagen de la tapa
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            visible = true
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(durationMillis = 3500))
        ) {
            Image(
                painter = painterResource(id = R.drawable.rocola_portada),
                contentDescription = "Fondo Rocola",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }


        // Botones superpuestos abajo
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Button(
                    onClick = {
                        val ctx = context.applicationContext
                        val intent = Intent(ctx, RocolaService::class.java).apply {
                            putExtra("rol", "maestro")
                        }
                        ContextCompat.startForegroundService(ctx, intent)

                        val servidor = RocolaServidor(context)

                        (context as? ComponentActivity)?.setContent {
                            MyApplicationTheme {
                                PantallaModoMaestro(servidor = servidor)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD7BFFF),
                        contentColor = Color.Black
                    )
                ) {
                    Text("🎸 Soy el DJ")
                }

                Button(
                    onClick = {
                        val ctx = context.applicationContext
                        val intent = Intent(ctx, RocolaService::class.java).apply {
                            putExtra("rol", "invitado")
                        }
                        ContextCompat.startForegroundService(ctx, intent)

                        (context as? ComponentActivity)?.setContent {
                            MyApplicationTheme {
                                ModoInvitadoRouter()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD7BFFF),
                        contentColor = Color.Black
                    )
                ) {
                    Text("🤘 Público VIP")
                }
            }
        }
    }
}
