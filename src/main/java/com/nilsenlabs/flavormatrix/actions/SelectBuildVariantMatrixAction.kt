package com.nilsenlabs.flavormatrix.actions

import com.android.tools.idea.gradle.project.model.GradleAndroidModel
import com.android.tools.idea.gradle.variant.view.BuildVariantUpdater
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.Module
import com.nilsenlabs.flavormatrix.actions.old.OldBuildVariantUpdater

class SelectBuildVariantMatrixAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val updater = BuildVariantUpdater.getInstance(project)
        //val updater = MyBuildVariantUpdater(project)
        val oldUpdater = OldBuildVariantUpdater()
        val moduleManager = ModuleManager.getInstance(project)

        val moduleNameMap: Map<String, Module> = moduleManager.modules.map {
            Pair<String, Module>(it.name, it)
        }.toMap()

        println("Module name map has these names: " + moduleNameMap.keys.joinToString("\n"))
        println("Module name map has ${moduleNameMap.keys.size} elems")

        val androidModules1 = moduleManager.modules
            .map { GradleAndroidModel.get(it) }
            .filter { it?.moduleName != null }
            .map { it!! }

        val reverseMap = mutableMapOf<GradleAndroidModel, MutableList<Module>>()
        for (module in moduleManager.modules) {
            val gradleModule = GradleAndroidModel.get(module) ?: continue
            val list = reverseMap[gradleModule] ?: mutableListOf<Module>().also { reverseMap[gradleModule] = it }
            list.add(module)
        }

        for ((key, value) in reverseMap) {
            println("GradleModule ${key.moduleName} maps to: [${value.joinToString()}]")
        }

        /*  .map { Pair(it, ) }
        .filter { it.second?.moduleName != null }
        .toMap()*/

        val androidModules = androidModules1.distinct()
        println("Before/After distinct: ${androidModules1.size}, ${androidModules.size}")

        val theMap = moduleManager.modules
            .map { Pair(it, GradleAndroidModel.get(it)) }
            .filter { it.second?.moduleName != null }
            .toMap()

        println("Themap size: ${theMap.size}")

        val dimensions = AndroidModuleHelper.createDimensionTable(androidModules, moduleManager.modules)

        val dialog = VariantSelectorDialog(dimensions, androidModules, project)
        if (dialog.showAndGet()) {
            // OK selected => Post variant selection back to Android Studio
            for (andModule in androidModules) {
                /*reverseMap[andModule]?.let { stdModuleList ->
                    stdModuleList.sortedBy { it.name.length }.firstOrNull()?.let { stdModule ->
                        val variant = dimensions.getSelectedVariantFor(andModule.moduleName)
                        //val stdModule = moduleNameMap[andModule.moduleName] ?: continue
                        println("Found module ${stdModule.name} . Updating to variant $variant")
                        if (variant != null) updater.updateSelectedBuildVariant(stdModule, variant)
                    }
                }*/
                dimensions.getSelectedVariantFor(andModule.moduleName)?.let {vari ->
                    println("Found module ${andModule.moduleName}. Updating to variant $vari")
                    oldUpdater.updateSelectedBuildVariant(project, andModule.moduleName, vari)
                }
            }
        }
    }
}
