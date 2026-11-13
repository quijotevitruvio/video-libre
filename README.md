# TVFut 1.0

Navegador Android ligero para ver videos y transmisiones en vivo desde tus propios enlaces, con bloqueo de publicidad.

## APK

- **`TVFut-1.0.apk`** (2.6 MB) — versión release, lista para instalar.
- Paquete: `com.tvfut.app`
- Requisitos: Android 7.0 o superior (minSdk 24).

Instalación:

1. Copia `TVFut-1.0.apk` al teléfono.
2. Abre el archivo y permite instalar apps desconocidas para esa fuente.
3. Abre **TVFut**.

También se puede instalar por ADB:

```powershell
adb install -r TVFut-1.0.apk
```

## Qué hace

- **Lista de enlaces propia**: agregar, editar, eliminar, copiar y reproducir. Se guarda en el dispositivo.
- **Navegador interno (WebView)** con:
  - bloqueo de dominios publicitarios y de seguimiento,
  - limpieza de banners, capas y contenedores de anuncios,
  - bloqueo de ventanas emergentes y `window.open`,
  - pantalla completa para el video.
- **Detección de streams**: si la página carga un `.m3u8`, `.mpd` o `.mp4`, aparece el botón **Ver directo**.
- **Reproductor Media3/ExoPlayer**: HLS, DASH, MP4, pantalla completa, badge **EN VIVO** y controles táctiles.
- **Ajustes**: activar/desactivar el bloqueo y borrar caché/cookies.
- Al agregar un enlace, si el portapapeles contiene una URL, se rellena sola.

## Uso

1. Pulsa **+** y escribe un nombre y el enlace.
2. Toca el enlace:
   - si es una página, se abre en el navegador interno;
   - si es un archivo directo, se abre en el reproductor.
3. En una página con video, usa **Ver directo** para reproducir el stream sin cargar la web.
4. Mantén pulsado un enlace (o usa **⋮**) para editar, eliminar, copiar o reproducir directo.
5. En **Ajustes** (engranaje) puedes desactivar el bloqueo o borrar datos del navegador.

Demos incluidas: un MP4 y un HLS de prueba. Se pueden eliminar.

## Cómo funciona el bloqueo

- `app/src/main/assets/ad_hosts.txt`: lista de dominios. Se bloquean también sus subdominios. Puedes añadir más dominios, uno por línea.
- `app/src/main/assets/cosmetic.js`: oculta selectores conocidos de anuncios y observa el DOM para limpiar los que aparezcan después.
- `PlayerActivity.kt`: intercepta las peticiones del WebView y corta las que pertenecen a dominios bloqueados.

### Límites normales de este enfoque

- Los anuncios **insertados dentro del propio stream** (SSAI, cortes del servidor) no se pueden quitar sin romper el video.
- Algunos sitios detectan WebView, piden DRM o usan su propio reproductor y pueden no funcionar; en ese caso usa **Abrir en navegador**.
- Si un sitio sirve anuncios desde su mismo dominio, solo actúa la limpieza visual.
- El bloqueo es para uso personal; algunos sitios lo prohíben en sus términos.

## Compilar

Requisitos: JDK 17 y Android SDK 34.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

Salidas:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

La versión release de este proyecto está firmada con la clave de debug para poder instalarla directamente. Para publicar en Google Play hay que crear un keystore propio y configurar `signingConfigs`.

## Estructura

```text
app/src/main/
  java/com/tvfut/app/
    MainActivity.kt          Lista de enlaces y ajustes
    PlayerActivity.kt        WebView con adblock y detección de streams
    DirectPlayerActivity.kt  Reproductor Media3
    AdBlocker.kt             Dominios y script cosmético
    LinkRepository.kt        Persistencia local (JSON)
    LinkAdapter.kt           Lista RecyclerView
    StreamUtils.kt           Detección de URLs de video
  assets/ad_hosts.txt        Dominios bloqueados
  assets/cosmetic.js         Limpieza visual
  res/                       Layouts, tema oscuro e iconos
```

Configuración: `compileSdk 34`, `minSdk 24`, `targetSdk 34`, Kotlin 2.0.21, AGP 8.7.0.
