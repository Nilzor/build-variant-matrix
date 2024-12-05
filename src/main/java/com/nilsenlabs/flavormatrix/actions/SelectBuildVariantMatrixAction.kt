package com.nilsenlabs.flavormatrix.actions

import com.android.tools.idea.gradle.project.model.GradleAndroidModel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.nilsenlabs.flavormatrix.actions.old.LegacyBuildVariantUpdater

class SelectBuildVariantMatrixAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val legacyUpdater = LegacyBuildVariantUpdater()
        val moduleManager = ModuleManager.getInstance(project)

        val unfilteredModuleList = moduleManager.modules
            .map { module ->
                GradleAndroidModel.get(module)?.let { Pair(it, module) }
            }
            .filterNotNull()

        getLog().info("Module list before filtering:")
        for (pair in unfilteredModuleList) {
            getLog().info("AndroidModule: ${pair.first.moduleName} -> PlainModule: ${pair.second.name}")
        }

        val androidModulePairs: Map<GradleAndroidModel?, Module> = unfilteredModuleList
            .filter { it.first.moduleName.lowercase() == it.second.name.lowercase() }
            .distinct()
            .toMap()
        val androidModules = androidModulePairs.keys.filterNotNull()

        getLog().info("Module list after filtering:")
        for (pair in androidModulePairs) {
            getLog().info("AndroidModule: ${pair.key?.moduleName} -> PlainModule: ${pair.value.name}")
        }

        val dimensions = AndroidModuleHelper.createDimensionTable(androidModules, moduleManager.modules)

        getLog().info("Staring variant update...")
        val dialog = VariantSelectorDialog(dimensions, androidModules, project)
        if (dialog.showAndGet()) {
            // OK selected => Post variant selection back to Android Studio
            for (modulePair in androidModulePairs) {
                val andModule = modulePair.key ?: continue // it being null should not happen
                dimensions.getSelectedVariantFor(andModule.moduleName)?.let { vari ->
                    getLog().info("Module: ${andModule.moduleName}. Updating to variant $vari")
                    legacyUpdater.updateSelectedBuildVariant(project, modulePair.value, vari)
                }
            }
            LegacyBuildVariantUpdater.requestGradleSync(project)
        }
    }
}
