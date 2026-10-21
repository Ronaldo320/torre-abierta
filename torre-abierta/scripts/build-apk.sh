#!/usr/bin/env bash
# Compila y firma dist/TorreAbierta.apk con las herramientas del Android SDK.
# Requisitos: Android SDK (build-tools y platforms;android-34), JDK 17+, zip.
#
# Firma (elige una):
#   - Variables KEYSTORE_B64 (keystore en base64) y KEYSTORE_PASSWORD   -> usado por GitHub Actions
#   - Archivo local keystore/torre-abierta.p12 y variable KEYSTORE_PASSWORD
#   - Si no hay ninguna, se crea una llave de prueba (no se podrá instalar encima de versiones firmadas con la llave real)
set -euo pipefail
cd "$(dirname "$0")/.."

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ]; then echo "Define ANDROID_HOME con la ruta del Android SDK"; exit 1; fi
BT="${BUILD_TOOLS:-$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)}"
JAR="${ANDROID_JAR:-$SDK/platforms/android-34/android.jar}"
[ -f "$JAR" ] || { echo "Falta $JAR. Instálalo con: sdkmanager \"platforms;android-34\""; exit 1; }
[ -f android/assets/index.html ] || { echo "Primero ejecuta: npm install && node web/build.js"; exit 1; }

OUT=build/apk
rm -rf "$OUT"; mkdir -p "$OUT/classes" dist

echo "1/5 Recursos"
"$BT/aapt2" compile --dir android/res -o "$OUT/res.zip"
"$BT/aapt2" link -I "$JAR" --manifest android/AndroidManifest.xml \
  --min-sdk-version 24 --target-sdk-version 34 -A android/assets -o "$OUT/base.apk" "$OUT/res.zip"

echo "2/5 Java"
javac -source 8 -target 8 -bootclasspath "$JAR" -cp "$JAR" -Xlint:-options -d "$OUT/classes" $(find android/src -name '*.java')

echo "3/5 Dex"
"$BT/d8" --release --min-api 24 --lib "$JAR" --output "$OUT" $(find "$OUT/classes" -name '*.class')
cp "$OUT/base.apk" "$OUT/unaligned.apk"
(cd "$OUT" && zip -q -j unaligned.apk classes.dex)

echo "4/5 Alineación"
"$BT/zipalign" -f -p 4 "$OUT/unaligned.apk" "$OUT/aligned.apk"

echo "5/5 Firma"
KS="$OUT/release.p12"
if [ -n "${KEYSTORE_B64:-}" ]; then
  echo "$KEYSTORE_B64" | base64 -d > "$KS"
elif [ -f keystore/torre-abierta.p12 ]; then
  cp keystore/torre-abierta.p12 "$KS"
else
  echo "AVISO: no hay llave de firma; se usa una llave de prueba."
  KEYSTORE_PASSWORD="prueba123"
  keytool -genkeypair -keystore "$KS" -storetype PKCS12 -storepass "$KEYSTORE_PASSWORD" -alias torreabierta \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Torre Abierta prueba" >/dev/null 2>&1
fi
KS_PASS="${KEYSTORE_PASSWORD:?Falta KEYSTORE_PASSWORD}" "$BT/apksigner" sign --ks "$KS" --ks-type PKCS12 \
  --ks-pass env:KS_PASS --ks-key-alias torreabierta --out dist/TorreAbierta.apk "$OUT/aligned.apk"
"$BT/apksigner" verify dist/TorreAbierta.apk
rm -f "$KS"
echo "APK listo: dist/TorreAbierta.apk"
