package com.nilsenlabs.flavormatrix.actions

import com.android.tools.idea.gradle.project.model.GradleAndroidModel
import com.android.tools.idea.gradle.variant.view.BuildVariantUpdater
import com.android.tools.idea.gradle.variant.view.update
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleManager

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

        val dimensions = AndroidModuleHelper.createDimensionTable(androidModules, moduleManager.modules)

        val dialog = VariantSelectorDialog(dimensions, androidModules, project)
        if (dialog.showAndGet()) {
            // OK selected => Post variant selection back to Android Studio
            for (module in androidModules) {
                val variant = dimensions.getSelectedVariantFor(module.moduleName)
                if (variant != null) updater.update(project, module.moduleName, variant)
            }
        }
    }
}
