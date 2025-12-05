#!/bin/bash

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="com.example.additioapp"
ACTIVITY=".MainActivity"

echo "Building APK..."
./gradlew assembleDebug

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

if [ ! -f "$APK_PATH" ]; then
    echo "APK not found at $APK_PATH."
    exit 1
fi

echo "Installing APK..."
~/bin/adb install -r "$APK_PATH"

echo "Done! App installed."
