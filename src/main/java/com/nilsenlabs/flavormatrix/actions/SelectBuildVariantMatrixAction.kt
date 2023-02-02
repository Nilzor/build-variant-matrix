package com.nilsenlabs.flavormatrix.actions

import com.android.tools.idea.gradle.project.model.GradleAndroidModel
import com.android.tools.idea.gradle.variant.view.BuildVariantUpdater
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.Module

class SelectBuildVariantMatrixAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val updater = BuildVariantUpdater.getInstance(project)
        val moduleManager = ModuleManager.getInstance(project)

        val androidModules = moduleManager.modules
            .map { GradleAndroidModel.get(it) }
            .filter { it?.moduleName != null }
            .map { it!! }
            .distinct()

        val reverseMap = mutableMapOf<GradleAndroidModel, MutableList<Module>>()
        for (module in moduleManager.modules) {
            val gradleModule = GradleAndroidModel.get(module) ?: continue
            val list = reverseMap[gradleModule] ?: mutableListOf<Module>().also { reverseMap[gradleModule] = it }
            list.add(module)
        }

        for ((key, value) in reverseMap) {
            println("GradleModule ${key.moduleName} maps to: [${value.joinToString()}]")
        }

        val dimensions = AndroidModuleHelper.createDimensionTable(androidModules, moduleManager.modules)

        val dialog = VariantSelectorDialog(dimensions, androidModules, project)
        if (dialog.showAndGet()) {
            // OK selected => Post variant selection back to Android Studio
            for (andModule in androidModules) {
                reverseMap[andModule]?.let { stdModuleList ->
                    // There are 4 Android modules per module. Non-postfixed, and postfixed with
                    // "unitTest", "androidTest" and "main". The non-postfixed is the one we want
                    stdModuleList.sortedBy { it.name.length }.firstOrNull()?.let { stdModule ->
                        val variant = dimensions.getSelectedVariantFor(andModule.moduleName)
                        println("Found module ${stdModule.name} . Updating to variant $variant")
                        if (variant != null) updater.updateSelectedBuildVariant(stdModule, variant)
                    }
                }
            }
        }
    }
}
