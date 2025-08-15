# Build Variant Matrix Selector

A plugin for Android Studio making it easier to select variants / flavors

## Introduction

|Tired of this?|Try this!|
|---|---|
|![Android Studios way](meta/variant-hassle.gif)|![This plugin](meta/flavor-matrix.gif)|

This plugin replaces the variant selector in Android Studio with a popup that lets you select the
flavors in a matrix instead of using drop down lists. It's much more efficient when working on large projects
in Android Studio.

Here's what I didn't like about Android Studio's solution and why I chose to build this plugin:

- If you have multiple leaf modules, there was no
way to select the same flavor for all modules in one operation
- Drop downs lists the options inefficiently*
- Android Studio's conflict resolution didn't always work**


\*) While drop downs have to list A x B x C selections, where A, B and C are number of
flavors per dimension, radio buttons can do with A + B + C

 \**) Anecdotal, I haven't investigated deeply.

## Installation

**Requirements**: Android Studio Narwhal 2025.1.2 or newer (JDK 21 required).

### Option 1: From Plugin Marketplace
Go to File → Settings → Plugins, Search for plugin "Build Variant Matrix Selector"

![image](https://user-images.githubusercontent.com/990654/111862442-aa75bb80-8955-11eb-8e75-35352186242c.png)

### Option 2: Manual Installation
Download the latest release from:
- [JetBrains Plugin Repository](https://plugins.jetbrains.com/plugin/15632-build-variant-matrix-selector)
- [GitHub Releases](https://github.com/Nilzor/build-variant-matrix/releases)

Then: File → Settings → Plugins → Install Plugin from Disk → Select the downloaded `.zip` file

**Note**: After installation, restart Android Studio to activate the plugin.

## Usage

1. **Setup Hotkey**: Go to File → Settings → Keymap → Search for "Select Build Variant by Matrix" → Assign a keyboard shortcut
2. **Open Android Project**: Ensure your project has multiple flavor dimensions or build types
3. **Trigger Plugin**: Use your assigned hotkey or find the action in the menu
4. **Select Variants**: Use the matrix dialog to efficiently select variants across all modules

The plugin automatically detects Android modules and their available variants, supporting:
- Multiple flavor dimensions (e.g., abi, platform, sdk, buildType)
- Complex project structures with dependency models
- Both legacy and modern Android Gradle Plugin configurations

**Tip**: The plugin works best with projects that have consistent flavor dimensions across modules.

## Known limitations

- Not currently supporting modules that have incomplete dimension configuration.
   If you have flavor A, B & C for dimension X, you need to have flavor A, B & C in all modules.

## Acknowledgements

Thants to PandoraMedia's [variant-helper-plugin](https://github.com/PandoraMedia/variant-helper-plugin) for inspiration

---

## Version 1.5.1 Updates - Android Studio Compatibility & Enhancements

### Recent Changes & Challenges

This version addresses significant compatibility issues with modern Android Studio versions (particularly Android Studio Narwhal 2025.1.2) and introduces enhanced support for complex Android project structures.

#### **Major Challenges Resolved:**

1. **Reflection Access Violations**
   - **Issue**: Modern Android Studio versions have stricter security policies that prevent reflection access to Android Gradle Plugin models
   - **Solution**: Enhanced all reflection calls with `isAccessible = true` in `ReflectionAndroidModel.kt`
   - **Impact**: Plugin now works with security-hardened Android Studio environments

2. **Android Gradle Plugin Model Evolution** 
   - **Issue**: Modern Android projects use `GradleAndroidDependencyModelImpl` instead of legacy `GradleAndroidModelImpl`
   - **Solution**: Added dual-model support in `AndroidModuleHelper.kt` and `ReflectionAndroidModel.kt`
   - **Impact**: Compatible with both legacy and modern Android Gradle Plugin versions

3. **Complex Multi-Dimensional Projects**
   - **Issue**: Projects with multiple flavor dimensions (abi, platform, sdk, buildType) weren't properly detected
   - **Solution**: Enhanced dimension extraction logic to handle dependency models and complex variant structures
   - **Impact**: Successfully works with android-userland and similar complex projects

#### **Technical Approach:**

- **Reflection Layer**: Implemented robust reflection-based access with graceful fallbacks
- **Model Compatibility**: Added detection and support for multiple Android model types
- **Enhanced Logging**: Production-ready logging levels for debugging without noise
- **Error Handling**: Comprehensive exception handling for various Android Studio configurations

### Building, Testing & Running Locally

#### **Prerequisites**
- Android Studio Narwhal 2025.1.2 or newer
- JDK 21 (required by current IntelliJ Platform)
- Gradle 7.0 or newer

#### **Development Workflow**

Based on the development process, here's the recommended workflow for building and testing the plugin:

```bash
# 1. Clean build and package plugin
./gradlew clean buildPlugin

# 2. For development/testing with frequent iterations, use the rebuild script:
chmod +x rebuild.sh
./rebuild.sh
```

The `rebuild.sh` script performs these essential cleanup steps:
```bash
# Remove old logs
rm -rf ~/.cache/Google/AndroidStudio2025.1.2/log/idea.log
# Clear plugin sandbox
rm -rf build/idea-sandbox  
# Remove cached plugin
rm -rf ~/.cache/Google/AndroidStudio2025.1.2/plugins/build-variant-matrix.zip
# Clean build
./gradlew clean buildPlugin
```

#### **Testing & Debugging Process**

During development, you'll need to frequently reload the plugin and restart Android Studio. Here's the workflow:

1. **Build Plugin**:
   ```bash
   ./rebuild.sh
   ```

2. **Install Plugin**: 
   - Copy `build/distributions/build-variant-matrix-1.5.1.zip` to Android Studio
   - Or use: File → Settings → Plugins → Install from Disk

3. **Restart Android Studio** (required for plugin changes)

4. **Monitor Plugin Activity**:
   ```bash
   # Real-time monitoring
   tail -f ~/.cache/Google/AndroidStudio2025.1.2/log/idea.log | grep flavormatrix
   
   # Search plugin logs
   grep "com.nilsenlabs.flavormatrix" ~/.cache/Google/AndroidStudio2025.1.2/log/idea.log > plugin.log
   ```

5. **Test Plugin**:
   - Open Android project with multiple flavor dimensions
   - Use File → Settings → Keymap to assign hotkey to "Select Build Variant by Matrix"
   - Trigger plugin action and verify dialog appears

#### **Common Development Issues**

- **Plugin Not Loading**: Clear Android Studio caches and restart
- **Reflection Errors**: Check Android Studio logs for access violations
- **Model Detection Issues**: Enable debug logging to see which models are detected
- **Dialog Not Appearing**: Verify Android modules are properly detected in logs

#### **Build Artifacts**

After successful build:
- **Plugin ZIP**: `build/distributions/build-variant-matrix-1.5.1.zip`
- **Plugin JAR**: `build/libs/build-variant-matrix-1.5.1.jar`
- **Sandbox**: `build/idea-sandbox/` (for testing)

The plugin has been thoroughly tested with complex Android projects and modern Android Studio versions, ensuring robust compatibility across different development environments.