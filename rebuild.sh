#!/bin/bash
# rebuild.sh - Script to clean and rebuild the Build Variant Matrix plugin for Android Studio

rm -rf ~/.cache/Google/AndroidStudio2025.1.2/log/idea.log
rm -rf build/idea-sandbox
rm -rf ~/.cache/Google/AndroidStudio2025.1.2/plugins/build-variant-matrix.zip
clear
./gradlew clean buildPlugin