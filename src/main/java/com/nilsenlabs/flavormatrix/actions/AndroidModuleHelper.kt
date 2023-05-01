package com.nilsenlabs.flavormatrix.actions

import com.android.tools.idea.gradle.project.model.GradleAndroidModel
import com.intellij.openapi.module.Module

object AndroidModuleHelper {
    fun createDimensionTable(androidModules: List<GradleAndroidModel>, modules: Array<Module> ): DimensionList {
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
    private fun createMergedDimensionList(modules: List<GradleAndroidModel>): DimensionList {
        val dimensionList = DimensionList()
        for (module in modules) {
            val flavors = module.androidProject.multiVariantData?.productFlavors?.toList().orEmpty()
            for (flavorObj in flavors) {
                val flavor = flavorObj.productFlavor
                flavor.dimension?.let { dim ->
                    val flavorsForDimension = dimensionList.getOrCreateDimension(dim)
                    flavorsForDimension.addUniqueVariant(flavor.name)
                }
            }
        }
        return dimensionList
    }

    fun getBuildTypes(modules: Array<Module>): List<String> {
        val buildTypes = mutableSetOf<String>()
        for(module in modules) {

            val amod = GradleAndroidModel.get(module)
            System.out.println( "Selected variant for ${module.name}: ${amod?.selectedVariant?.displayName} -- ${amod?.selectedVariant?.buildType} -- ${amod?.selectedVariant?.productFlavors?.joinToString("*")}")
            buildTypes.addAll(amod?.buildTypeNames ?: emptyList())
        }
        return buildTypes.stream().toList()
    }
}


val Module.variantNames: Collection<String?>
    // Note: NDK part is untested
    get() = // NdkModuleModel.get(this)?.ndkModel?.allVariantAbis?.stream()?.map { it.displayName }?.toList() ?:
        GradleAndroidModel.get(this)?.variantNames ?: emptyList()

val Module.variantItems: ModuleBuildVariant
    get() = ModuleBuildVariant(name, variantNames.asSequence().filterNotNull().sorted().toList())

data class ModuleBuildVariant(val moduleName: String, val buildVariants: List<String>)
