# Build Variant Matrix Plugin — Developer Notes

## Project overview

**Build Variant Matrix Selector** is an IntelliJ/Android Studio plugin (Kotlin) that replaces the default dropdown-based build variant selector with a matrix dialog. It lets developers select all flavor dimensions simultaneously and applies the selection across all modules at once.

- **Plugin ID**: `com.nilsenlabs.flavormatrix`
- **Published at**: JetBrains Plugin Marketplace (plugin 15632)
- **License**: Apache 2.0
- **Current version**: Check `/build.gradle`

### Key source files

| Path | Purpose |
|------|---------|
| `build.gradle` | Build config — IDE version targeting, plugin version, changelog |
| `src/main/resources/META-INF/plugin.xml` | Plugin descriptor — `<idea-version>` compatibility range |
| `src/main/java/.../actions/SelectBuildVariantMatrixAction.kt` | Main action entry point |
| `src/main/java/.../actions/VariantSelectorDialog.kt` | Swing matrix dialog UI |
| `src/main/java/.../actions/ReflectionAndroidModel.kt` | Reflection-based AGP model access (handles both legacy and modern AGP) |
| `src/main/java/.../actions/DimensionList.kt` | Flavor dimension and variant name parsing |
| `local.properties` | **Not committed** — local Android Studio installation path override |

### Build

Requires a JDK matching the version bundled with the target Android Studio (found at `<as-install>/jbr`), and a local AS installation matching `targetIdeVersion`.

```bash
./gradlew buildPlugin        # produces build/distributions/build-variant-matrix-<version>.zip
./gradlew test               # run unit tests
```

Set your local AS path in `local.properties` (not committed):
```
localStudioPath=D:/coding/android-studio-quail
```

After switching AS versions, also delete stale library references in **Project Structure > Libraries** (platform-images.jar, properties.jar, smali.jar).

### Publishing

Manual — no CI/CD. Build the plugin ZIP and upload to the JetBrains Marketplace.

---

## New Android Studio version support procedure

When a new Android Studio version is released, do the following to update the plugin.

### 1. Find the new build number

Look up the exact build number for the new AS version. Format is `AAA.NNNNN.NNN` (e.g. `261.23567.138` for Quail 2026.1.1). Check the [Android Studio releases page](https://developer.android.com/studio/archive) or the JetBrains platform build page.

### 2. Install the new Android Studio locally

Download and install the new version. Note the installation folder path (e.g. `D:\coding\android-studio-quail`).

### 3. Update `build.gradle`

Three changes:

**a) Update the comment and `targetIdeVersion`:**
```groovy
// Below: Android Studio <Name> <Num> | <Year>.<Major>.<Minor> - Current version for compatibility
def targetIdeVersion = "<new-build-number>"
```

**b) Bump the plugin version** (minor bump for new AS support, e.g. `1.8.0` → `1.9.0`):
```groovy
version '1.9.0'
```

**c) Add a changelog entry** in `patchPluginXml.changeNotes`:
```html
<li>1.9.0 Support for Android Studio <Name> (<Year>.<Major>.<Minor>)</li>
```

**d) Optionally update `defaultLocalStudioPath`** if you want to change the default local path hint (not strictly required since it's overridden via `local.properties`):
```groovy
def defaultLocalStudioPath = 'D:\\coding\\android-studio-<codename>'
```

**e) Check if the JDK version changed** — if the new AS ships with a newer JBR than the previous one, two things are needed:

1. Update `javaVersion` in `build.gradle` to match (class version 65=Java 21, 66=22, 67=23, 68=24, 69=Java 25):
```groovy
def javaVersion = 25
```

2. Point the Gradle daemon at the new JDK — add/update in `~/.gradle/gradle.properties` (user-level, **not** committed):
```
org.gradle.java.home=D:\\coding\\android-studio-<codename>\\jbr
```
Then kill any running daemon before rebuilding: `./gradlew --stop`

> Note: `javaVersion` only controls the *compilation* toolchain. If the Gradle daemon itself runs on an older JDK it will fail to load the AS jars at dependency-resolution time with `class file has wrong version X, should be Y`.

**f) Consider whether to bump `org.jetbrains.kotlin.jvm`** — compare what Kotlin version ships with the new AS and update if needed.

### 4. ~~Update `plugin.xml`~~ — not needed

`since-build` and `until-build` are derived automatically from `targetIdeVersion` by the `patchPluginXml` task in `build.gradle`. Do **not** edit `plugin.xml` for this — updating `targetIdeVersion` is sufficient.

### 5. Point local build at the new installation

In `local.properties` (not committed), set:
```
localStudioPath=D:/coding/android-studio-<codename>
```

Also delete stale library references in **Project Structure > Libraries** that still point to old AS jars (platform-images.jar, properties.jar, smali.jar etc.).

### 6. Build and verify

```bash
./gradlew buildPlugin
```

Fix any compilation errors — API changes between AS versions occasionally require code fixes.

### 7. Commit

```
Update build.gradle against AS <Name>. Set version to <X.Y.0>
```

### Example: Quail 2026.1.1 (build `261.23567.138`)

| File | Change |
|------|--------|
| `build.gradle` | `targetIdeVersion = "261.23567.138"`, `version '1.8.0'`, added changelog entry |
| `plugin.xml` | `since-build="261.23567" until-build="261.*"` |
