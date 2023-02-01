package com.nilsenlabs.flavormatrix.actions

import com.android.tools.idea.gradle.project.model.GradleAndroidModel
import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker
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

        val moduleNameMap: Map<String, Module> = moduleManager.modules.map {
            Pair<String, Module>(it.name, it)
        }.toMap()

        println("Module name map has these names: " + moduleNameMap.keys.joinToString(", "))
        println("Module name map has ${moduleNameMap.keys.size} elems")

        val androidModules = moduleManager.modules
            .map { GradleAndroidModel.get(it) }
            .filter { it?.moduleName != null }
            .map { it!! }
            .distinct()

        println("androidModules has ${androidModules.size} elems")

        val dimensions = AndroidModuleHelper.createDimensionTable(androidModules, moduleManager.modules)

        val dialog = VariantSelectorDialog(dimensions, androidModules, project)
        if (dialog.showAndGet()) {
            // OK selected => Post variant selection back to Android Studio
            for (andModule in androidModules) {
                val variant = dimensions.getSelectedVariantFor(andModule.moduleName)
                val stdModule = moduleNameMap[andModule.moduleName] ?: continue
                println("Found module ${stdModule.name}. Updating to variant $variant")
                if (variant != null) updater.updateSelectedBuildVariant(stdModule, variant)
            }
        }
    }
}
