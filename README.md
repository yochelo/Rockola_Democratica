# 🎶 Rocola Democrática

Una app colaborativa hecha en Kotlin, donde varios celulares pueden sumar canciones a una lista común.  
Solo uno —el **Maestro**— se conecta al parlante y reproduce la lista en orden.

> ✨ Ideal para fiestas, juntadas o cualquier evento donde nadie quiere pelearse por el Bluetooth.

---

## 🚀 ¿Cómo funciona?

- Al iniciar la app, se elige un rol:
  - 👑 **Maestro**: se encarga de reproducir y compartir la IP con los invitados.
  - 🙋 **Invitado**: escanea el QR o pone la IP y agrega canciones.

### 🎥 Reproducción de canciones:

- Los **invitados** tienen un YouTube embebido para buscar y agregar canciones a la lista.
- El **Maestro** reproduce los temas directamente en la app mediante el navegador nativo de YouTube.
  - Esto permite que la música **siga sonando aunque se bloquee el teléfono** o se minimice la app.
  - 🔔 **Sugerencia**: lo ideal es que el Maestro tenga **YouTube Premium** para evitar publicidades entre canciones.

- La lista de canciones es compartida y en tiempo real.
- El servidor corre en el dispositivo Maestro, usando HTTP local.
- No requiere conexión a internet ni APIs externas (salvo para acceder a YouTube).

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

- ✅ Reproducción desde WebView
- ✅ Escaneo QR con CameraX
- ⬜ Votación de canciones
- ⬜ Reordenamiento democrático
- ⬜ Historial de temas
- ⬜ Versión en español e inglés

---

## 🙌 Hecho con amor por Marce

¡Pull requests y sugerencias bienvenidas!  
Si lo usás en una fiesta, mandame una foto 😄
