package com.nilsenlabs.flavormatrix.actions

import com.android.tools.idea.gradle.project.model.GradleAndroidModel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleManager
import com.nilsenlabs.flavormatrix.actions.old.LegacyBuildVariantUpdater

class SelectBuildVariantMatrixAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val legacyUpdater = LegacyBuildVariantUpdater()
        val moduleManager = ModuleManager.getInstance(project)

        val androidModules = moduleManager.modules
            .map { GradleAndroidModel.get(it) }
            .filter { it?.moduleName != null }
            .map { it!! }
            .distinct()

        val dimensions = AndroidModuleHelper.createDimensionTable(androidModules, moduleManager.modules)

        val dialog = VariantSelectorDialog(dimensions, androidModules, project)
        if (dialog.showAndGet()) {
            // OK selected => Post variant selection back to Android Studio
            for (andModule in androidModules) {
                dimensions.getSelectedVariantFor(andModule.moduleName)?.let {vari ->
                    println("Found module ${andModule.moduleName}. Updating to variant $vari")
                    legacyUpdater.updateSelectedBuildVariant(project, andModule.moduleName, vari)
                }
            }
            LegacyBuildVariantUpdater.requestGradleSync(project)
        }
    }
}
