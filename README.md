# 🎶 Rocola Democrática

Una app colaborativa hecha en Kotlin, donde varios celulares pueden sumar canciones a una lista común.  
Solo uno —el **Maestro**— se conecta al parlante y reproduce la lista en orden.

> ✨ Ideal para fiestas, juntadas o cualquier evento donde nadie quiere pelearse por el Bluetooth.

---

## 🚀 ¿Cómo funciona?

### 🔁 Flujo general de uso:

1. 📶 **Conectarse a la misma red Wi-Fi**
   - Tanto el Maestro como los Invitados deben estar en la misma red local (LAN).

2. 👑 **El Maestro inicia la app**
   - Elige el rol **Maestro**
   - Se activa un **Foreground Service** que mantiene el servidor HTTP funcionando incluso si se minimiza o bloquea la pantalla.
   - Se crea automáticamente una lista de canciones en su almacenamiento interno.
   - Puede mostrar un **código QR** con su IP local, para que los invitados se conecten fácilmente.

3. 🙋 **Los Invitados abren la app**
   - Eligen el rol **Invitado**
   - Usan la cámara para escanear el QR del Maestro.
   - Acceden a una WebView con YouTube embebido para buscar canciones y agregarlas a la cola compartida.
   
4. 🧠 **Reproducción en el Maestro**
   - El maestro reproduce la lista en YouTube nativo, no embebido.
   - YouTube sigue sonando incluso con la pantalla apagada o la app minimizada.
   - 🔔 Se recomienda tener **YouTube Premium** en el Maestro para evitar publicidades.

5. 📱 **Desde la barra de notificaciones**
   - Se puede ver el estado de conexión y detener la sesión (tanto para Maestro como Invitado).

---

## 🎬 Limitaciones actuales

- ❌ **No hay autoplay** al finalizar un video.
   - No es posible tomar el control de la notificación de YouTube o detectar directamente cuándo termina una canción.
   - Si alguien tiene una idea creativa para resolverlo, ¡los pull requests son bienvenidos!

---

## 📂 Estructura de carpetas

```plaintext
app/src/main/java/com/marce/rocola/
├── MainActivity.kt        → Pantalla de inicio (elegir rol)
├── ModoMaestro.kt         → WebView con YouTube + controles de lista
├── ModoInvitado.kt        → Permite sumar canciones como invitado (YouTube embebido)
├── RocolaServidor.kt      → Servidor HTTP embebido (modo LAN)
├── ScanQrView.kt          → Lector QR para conectarse rápido al Maestro
├── ColaDeCanciones.kt     → Clase que maneja la lista (cola) de temas
├── PlaylistVisual.kt      → Visualización de la lista para todos
└── util/                  → Funciones auxiliares y helpers
```

---

## ⚙️ Requisitos para compilar

- Android Studio Flamingo o superior
- SDK 33+
- Kotlin
- Internet solo para compilar la primera vez (usa librerías locales)

---

## 💡 Ideas futuras

- ✅ Reproducción desde YouTube
- ✅ Escaneo QR con CameraX
- ✅ Historial de temas
- ⬜ Autoplay al finalizar canción (ésta es la pieza que falta)
- ⬜ Votación de canciones
- ⬜ Reordenamiento democrático

---

## 🙌 Hecho con amor por Marce

¡Pull requests y sugerencias bienvenidas!  
Si lo usás en una fiesta, mandame una foto 😄
