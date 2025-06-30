package com.marce.rocola

import androidx.compose.runtime.*

@Composable
fun ModoInvitadoRouter() {
    var ip by remember { mutableStateOf<String?>(null) }

    if (ip == null) {

        PantallaSeleccionIP(onIpObtenida = { ip = it })
    } else {
        val ipLimpia = ip!!
            .removePrefix("http://")
            .removeSuffix("/")
            .replace(":8080/playlist", "")

        PantallaModoInvitado(ip = ipLimpia)
    }

}
