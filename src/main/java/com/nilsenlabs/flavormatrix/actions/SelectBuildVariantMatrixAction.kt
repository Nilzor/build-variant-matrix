package com.nilsenlabs.flavormatrix.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.nilsenlabs.flavormatrix.actions.old.LegacyBuildVariantUpdater

class SelectBuildVariantMatrixAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        try {
            val legacyUpdater = LegacyBuildVariantUpdater()
            val moduleManager = ModuleManager.getInstance(project)

            getLog().info("Starting Build Variant Matrix Selector for project: ${project.name}")
            getLog().debug("Total modules in project: ${moduleManager.modules.size}")

            // Log all modules for debugging
            moduleManager.modules.forEach { module ->
                getLog().debug("Found module: ${module.name} (type: ${module::class.java.simpleName})")
            }

            val unfilteredModuleList = moduleManager.modules
                .mapNotNull { module ->
                    try {
                        // Use reflection-based model retrieval to avoid linking issues
                        val gradleModel = try {
                            ReflectionAndroidModel.getModel(module)
                        } catch (error: Throwable) {
                            getLog().warn("Reflection getModel error for module ${module.name}: ${error.message}")
                            null
                        }

                        // If we got a model, include it; otherwise log why we're skipping
                        if (gradleModel != null) {
                            getLog().debug("Including module ${module.name} with model type: ${gradleModel::class.java.name}")
                            Pair(gradleModel, module)
                        } else {
                            getLog().debug("Skipping module ${module.name} - no valid Android model found")
                            null
                        }
                    } catch (e: Exception) {
                        getLog().warn("Failed to get GradleAndroidModel for module ${module.name}: ${e.message}")
                        null
                    }
                }

            getLog().debug("Module list before filtering:")
            for (pair in unfilteredModuleList) {
                try {
                    val moduleName = ReflectionAndroidModel.getModuleName(pair.first)
                    if (moduleName != null) {
                        getLog().debug("AndroidModule: $moduleName -> PlainModule: ${pair.second.name}")
                    } else {
                        getLog().debug("AndroidModule: <null> -> PlainModule: ${pair.second.name} (skipping)")
                    }
                } catch (error: Throwable) {
                    getLog().warn("Reflection error when accessing module properties: ${error.message}")
                }
            }

            val androidModulePairs: Map<Any?, Module> = unfilteredModuleList
                .mapNotNull { pair ->
                    try {
                        // Safely check if module names match using reflection
                        val androidModuleName = try {
                            ReflectionAndroidModel.getModuleName(pair.first)
                        } catch (error: Throwable) {
                            getLog().warn("Reflection error when accessing moduleName: ${error.message}")
                            null
                        }

                        // Skip modules where we couldn't get a valid name
                        if (androidModuleName.isNullOrBlank()) {
                            getLog().debug("Skipping module pair with null/blank Android module name for ${pair.second.name}")
                            return@mapNotNull null
                        }

                        if (androidModuleName.lowercase() == pair.second.name.lowercase()) {
                            getLog().debug("Module name match: '$androidModuleName' == '${pair.second.name}'")
                            pair
                        } else {
                            getLog().debug("Module name mismatch: '$androidModuleName' != '${pair.second.name}'")
                            null
                        }
                    } catch (e: Exception) {
                        getLog().warn("Failed to filter module pair: ${e.message}")
                        null
                    }
                }
                .distinct()
                .toMap()
            val androidModules = androidModulePairs.keys.filterNotNull()

            if (androidModules.isEmpty()) {
                getLog().warn("No Android modules found in the project. This might be due to compatibility issues.")
                return
            }

            getLog().debug("Module list after filtering:")
            for (pair in androidModulePairs) {
                try {
                    val moduleName = ReflectionAndroidModel.getModuleName(pair.key)
                    if (moduleName != null) {
                        getLog().debug("AndroidModule: $moduleName -> PlainModule: ${pair.value.name}")
                    } else {
                        getLog().debug("AndroidModule: <null> -> PlainModule: ${pair.value.name}")
                    }
                } catch (error: Throwable) {
                    getLog().warn("Reflection error when accessing filtered module properties: ${error.message}")
                }
            }

            val dimensions = AndroidModuleHelper.createDimensionTable(androidModules, moduleManager.modules)

            getLog().info("Staring variant update...")
            val dialog = VariantSelectorDialog(dimensions, androidModules, project)
            if (dialog.showAndGet()) {
                // OK selected => Post variant selection back to Android Studio
                for (modulePair in androidModulePairs) {
                    val andModule = modulePair.key ?: continue // it being null should not happen
                    try {
                        val moduleName = ReflectionAndroidModel.getModuleName(andModule) ?: continue

                        dimensions.getSelectedVariantFor(moduleName)?.let { vari ->
                            getLog().info("Module: $moduleName. Updating to variant $vari")
                            legacyUpdater.updateSelectedBuildVariant(project, modulePair.value, vari)
                        }
                    } catch (e: Exception) {
                        getLog().warn("Failed to update variant for module: ${e.message}")
                    }
                }
                LegacyBuildVariantUpdater.requestGradleSync(project)
            }
        } catch (e: Exception) {
            getLog().error("An error occurred while trying to show the build variant matrix selector", e)
        }
    }
}
