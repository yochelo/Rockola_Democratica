package com.marce.rocola

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import android.content.Context
import android.content.Intent
import android.net.Uri


@Composable
fun ListaDeReproduccion(
    canciones: List<Cancion>,
    context: Context,

) {
    val fondoOscuro = Color(0xFF121212)
    val textoActivo = Color.White
    val textoReproducida = Color(0xFF888888)

    val index = canciones.indexOfFirst { !it.reproducida }
    val actual = if (index > 0) canciones[index - 1] else null



    LazyColumn(
        modifier = Modifier
            .background(fondoOscuro)
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(canciones.size) { index ->
            val item = canciones[index]
            val estaReproduciendo = item == actual

            val colorTexto = when {
                estaReproduciendo -> Color(0xFF00FF00)
                item.reproducida -> textoReproducida
                else -> textoActivo
            }

            val tamanoTexto = when {
                estaReproduciendo -> 20.sp // 🎵 Actual → +25%
                else -> 16.sp
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickYLongPress(
                        context = context,
                        url = item.url,
                        onClickNormal = {
                            GestorDePlaylist.forzarReproducir(context, item.url)

                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                            intent.setPackage("com.google.android.youtube")
                            context.startActivity(intent)
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (estaReproduciendo) {
                    Text(
                        text = "",
                        fontSize = tamanoTexto,
                        color = Color(0xFF00FF00), // Verde fuerte — lo podés ajustar
                        modifier = Modifier.padding(end = 4.dp)
                    )

                }
    
                Text(
                    text = "${if (estaReproduciendo) "" else if (item.reproducida) "✓" else "🎵"} ${item.titulo}",
                    color = colorTexto,
                    fontSize = tamanoTexto
                )
            }

        }
    }
}
