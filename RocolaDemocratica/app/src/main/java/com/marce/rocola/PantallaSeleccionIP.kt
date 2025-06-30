package com.marce.rocola
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale

@Composable
fun PantallaSeleccionIP(onIpObtenida: (String) -> Unit) {
    var escaneando by remember { mutableStateOf(false) }

    if (escaneando) {
        ScanQrView { ipLeida ->
            onIpObtenida(ipLeida.trim())
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Imagen de fondo
            Image(
                painter = painterResource(id = R.drawable.fondo_vinilos),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Contenido encima
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { escaneando = true }) {
                    Text("📷 Escaneá el QR del DJ")
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
