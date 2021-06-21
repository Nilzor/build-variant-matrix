package com.nilsenlabs.flavormatrix.actions

import com.android.tools.idea.gradle.project.model.AndroidModuleModel
import com.android.tools.idea.gradle.project.model.NdkModuleModel
import com.intellij.openapi.module.Module
import kotlin.streams.toList

class LegacyAndroidModuleHelper : AndroidModuleHelper {
    override fun createDimensionTable(androidModules: List<AndroidModuleModel>, modules: Array<Module> ): DimensionList {
        val dimensions = createMergedDimensionList(androidModules)
        addBuildTypes(modules, dimensions)
        dimensions.createOrderedDimensionMaps(androidModules)
        dimensions.selectFrom(androidModules)
        dimensions.deselectDuplicates()
        return dimensions
    }

    private fun addBuildTypes(modules: Array<Module>, dimensions: DimensionList) {
        dimensions.dimensions.add(
                Dimension(DimensionList.BUILD_TYPE_NAME).also { dim ->
                    getBuildTypes(modules).forEach {
                        dim.addUniqueVariant(it)
                    }
                }
        )
    }

    /**
     * Fills the list with a merged set of dimensions for all modules
     */
    private fun createMergedDimensionList(modules: List<AndroidModuleModel>): DimensionList {
        val dimensionList = DimensionList()
        for (module in modules) {
            val flavors = module.productFlavors.toList()
            for (flavorStr in flavors) {
                val flavor = module.findProductFlavor(flavorStr)?.productFlavor ?: continue
                val flavorsForDimension = dimensionList.getOrCreateDimension(flavor.dimension)
                flavorsForDimension.addUniqueVariant(flavor.name)
            }
        }
        return dimensionList
    }

    private fun getBuildTypes(modules: Array<Module>): List<String> {
        val buildTypes = mutableSetOf<String>()
        for(module in modules) {

            val amod = AndroidModuleModel.get(module)
            System.out.println( "Selected variant for ${module.name}: ${amod?.selectedVariant?.displayName} -- ${amod?.selectedVariant?.buildType} -- ${amod?.selectedVariant?.productFlavors?.joinToString("*")}")
            buildTypes.addAll(amod?.buildTypeNames ?: emptyList())
        }
        return buildTypes.stream().toList()
    }
}