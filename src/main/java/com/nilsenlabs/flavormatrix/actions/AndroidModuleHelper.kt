package com.nilsenlabs.flavormatrix.actions

import com.android.tools.idea.gradle.project.model.AndroidModuleModel
import com.intellij.openapi.module.Module

interface AndroidModuleHelper {
    fun createDimensionTable(androidModules: List<AndroidModuleModel>, modules: Array<Module>): DimensionList
}