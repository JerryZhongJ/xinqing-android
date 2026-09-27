#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
BT="$SDK/build-tools/34.0.0"
ANDROID="$SDK/platforms/android-34/android.jar"
mkdir -p build/classes build/dex build/generated
"$BT/aapt2" compile --dir res -o build/resources.zip
"$BT/aapt2" link -o build/base.apk --manifest AndroidManifest.xml --java build/generated -I "$ANDROID" build/resources.zip
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$ANDROID:$BT/core-lambda-stubs.jar" -d build/classes build/generated/cn/xinqing/journal/R.java src/cn/xinqing/journal/*.java
jar cf build/classes.jar -C build/classes .
"$BT/d8" --lib "$ANDROID" --min-api 26 --output build/dex build/classes.jar
cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q -u ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [ ! -f build/debug.keystore ] && [ -f .signing/debug.keystore ]; then
    cp .signing/debug.keystore build/debug.keystore
fi
if [ ! -f build/debug.keystore ]; then
    keytool -genkeypair -keystore build/debug.keystore -storepass android -keypass android -alias androiddebugkey -dname "CN=Android Debug" -keyalg RSA -keysize 2048 -validity 10000
fi
"$BT/apksigner" sign --ks build/debug.keystore --ks-pass pass:android --out build/xinqing.apk build/aligned.apk
"$BT/apksigner" verify build/xinqing.apk
printf 'APK: %s/build/xinqing.apk\n' "$PWD"
