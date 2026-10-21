# Torre Abierta

App de Android para seguir vuelos en tiempo real y ver los itinerarios de los aeropuertos.

- Mapa satelital o de relieve con los aviones en vivo, combinando cuatro redes ADS-B comunitarias: adsb.lol, adsb.fi, airplanes.live y OpenSky.
- Ficha de cada vuelo con foto del avión, ruta, progreso, hora estimada de llegada, altitud y velocidad.
- Por aeropuerto: los aviones que llegan y salen en este momento (según el radar) y el itinerario oficial con AirLabs o AeroDataBox.

## Cómo está hecha

| Carpeta | Contenido |
|---|---|
| `web/template.html` | Toda la interfaz (HTML, CSS y JavaScript con Leaflet) |
| `web/build.js` | Genera la versión web y la que va dentro del APK |
| `android/` | Envoltorio nativo: una `WebView` y un puente HTTP en Java (`MainActivity.java`) que hace las peticiones desde Android para evitar bloqueos de CORS |
| `scripts/build-apk.sh` | Compila y firma el APK con el Android SDK |
| `.github/workflows/build-apk.yml` | Compila el APK automáticamente en GitHub Actions |

## Compilar en GitHub (recomendado)

Cada vez que subes cambios a la rama `main`, GitHub Actions compila el APK. Lo descargas en la pestaña **Actions**, dentro de la última ejecución, en *Artifacts*.

Para que el APK se instale encima de las versiones que ya tienes, configura la llave de firma una sola vez en **Settings → Secrets and variables → Actions → New repository secret**:

- `KEYSTORE_B64`: el contenido del archivo `torre-abierta.p12.base64.txt`
- `KEYSTORE_PASSWORD`: la contraseña de la llave

Sin esos secretos el APK se firma con una llave de prueba y tendrás que desinstalar la versión anterior antes de instalarlo.

## Compilar en tu computador

Necesitas Node.js 20, JDK 17 y el Android SDK (`platforms;android-34` y `build-tools;34.0.0`).

```bash
npm install
node web/build.js
export ANDROID_HOME=/ruta/a/Android/Sdk
export KEYSTORE_PASSWORD='tu-contraseña'   # y copia la llave a keystore/torre-abierta.p12
bash scripts/build-apk.sh
```

El APK queda en `dist/TorreAbierta.apk`.

## Claves de itinerarios

Las claves de AirLabs o AeroDataBox no van en el código. Cada usuario las pega en la app (botón de engranaje) y quedan guardadas solo en su teléfono.

## Fuentes de datos y licencias

- Posiciones ADS-B: [adsb.lol](https://adsb.lol) (ODbL), [adsb.fi](https://adsb.fi), [airplanes.live](https://airplanes.live), [OpenSky Network](https://opensky-network.org). Uso personal y no comercial.
- Rutas por indicativo: [adsbdb](https://www.adsbdb.com).
- Fotos: [Planespotters.net](https://www.planespotters.net), con crédito al fotógrafo.
- Mapas: [Sentinel-2 cloudless](https://s2maps.eu) y Terrain Light de EOX (gratis para uso no comercial con atribución), etiquetas © OpenStreetMap.
- Costas de respaldo: Natural Earth vía [world-atlas](https://github.com/topojson/world-atlas).
- Itinerarios: [AirLabs](https://airlabs.co) o [AeroDataBox](https://aerodatabox.com), con la clave de cada usuario.

Varias de estas fuentes solo permiten uso no comercial. Para publicar o vender la app hay que revisar y contratar las licencias correspondientes.
