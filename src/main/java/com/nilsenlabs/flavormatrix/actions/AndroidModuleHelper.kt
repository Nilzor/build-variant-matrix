package com.nilsenlabs.flavormatrix.actions

import com.intellij.openapi.module.Module

object AndroidModuleHelper {
    fun createDimensionTable(androidModels: List<Any?>, modules: Array<Module>): DimensionList {
        val dimensions = createMergedDimensionList(androidModels)
        addBuildTypes(modules, dimensions)
        dimensions.createOrderedDimensionMaps(androidModels)
        dimensions.selectFrom(androidModels)
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
     * Fills the list with a merged set of dimensions for all models
     * Uses reflection-based access to support multiple IDE versions
     */
    private fun createMergedDimensionList(models: List<Any?>): DimensionList {
        val dimensionList = DimensionList()
        
        getLog().debug("Starting dimension analysis for ${models.size} models...")
        
        for ((index, model) in models.withIndex()) {
            try {
                getLog().debug("Processing model $index: ${model?.javaClass?.name}")
                
                // Try the dependency model approach first for better compatibility
                if (model?.javaClass?.name?.contains("GradleAndroidDependencyModel") == true) {
                    getLog().debug("Attempting to extract dimensions from dependency model...")
                    
                    // Try to get flavor dimensions directly from the dependency model
                    try {
                        val flavorsByDimensionMethod = model.javaClass.methods.firstOrNull { 
                            it.name == "getProductFlavorNamesByFlavorDimension" 
                        }
                        if (flavorsByDimensionMethod != null) {
                            flavorsByDimensionMethod.isAccessible = true
                            val result = flavorsByDimensionMethod.invoke(model)
                            
                            if (result is Map<*, *> && result.isNotEmpty()) {
                                getLog().debug("Found flavor dimensions in dependency model: $result")
                                for ((dimensionName, flavorList) in result) {
                                    if (dimensionName is String && flavorList is Collection<*>) {
                                        val dimension = dimensionList.getOrCreateDimension(dimensionName)
                                        for (flavor in flavorList) {
                                            if (flavor is String) {
                                                dimension.addUniqueVariant(flavor)
                                                getLog().debug("Added flavor '$flavor' to dimension '$dimensionName'")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        getLog().debug("Failed to extract flavors from dependency model: ${e.message}")
                    }
                }
                
                // Also try the original approach for compatibility
                val androidProject = ReflectionAndroidModel.getAndroidProject(model)
                val basicVariants = ReflectionAndroidModel.getBasicVariants(androidProject)
                
                // For each variant, extract product flavor name/dimension pairs
                for (variant in basicVariants) {
                    val flavorPairs = ReflectionAndroidModel.getProductFlavorsFromVariant(variant)
                    
                    for ((fname, fdim) in flavorPairs) {
                        if (fdim != null && fname != null) {
                            val flavorsForDimension = dimensionList.getOrCreateDimension(fdim)
                            flavorsForDimension.addUniqueVariant(fname)
                            if (fdim.lowercase() in listOf("abi", "arch", "architecture")) {
                                getLog().debug("Detected ABI/Architecture dimension: '$fdim' with flavor: '$fname'")
                            }
                        } else if (fname != null) {
                            // dimension missing for this flavor
                            getLog().warn("Flavor '$fname' has no dimension specified - this may cause variant selection issues")
                        }
                    }
                }

                val dimensionNames = dimensionList.dimensions.map { it.dimensionName }.distinct()
                if (dimensionNames.isNotEmpty()) {
                    getLog().debug("Model $index contributed dimensions: ${dimensionNames.joinToString(", ")}")
                }
            } catch (e: Exception) {
                getLog().warn("Failed to process model $index for dimensions: ${e.message}")
            }
        }

        val allDimensions = dimensionList.dimensions.map { "${it.dimensionName} (${it.flavors.size} flavors)" }
        getLog().debug("Total dimensions detected: ${allDimensions.joinToString(", ")}")
        return dimensionList
    }

    fun getBuildTypes(modules: Array<Module>): List<String> {
        val buildTypes = mutableSetOf<String>()
        for (module in modules) {
            try {
                val model = try {
                    ReflectionAndroidModel.getModel(module)
                } catch (error: Throwable) {
                    getLog().warn("Reflection error when getting GradleAndroidModel for module ${module.name}: ${error.message}")
                    null
                }
                buildTypes.addAll(ReflectionAndroidModel.getBuildTypeNames(model))
            } catch (e: Exception) {
                getLog().warn("Failed to get build types for module ${module.name}: ${e.message}")
            }
        }
        val list = buildTypes.toList()
        getLog().debug("Build type list resolved after iterating ${modules.size} modules: $list")
        return list
    }
}


val Module.variantNames: Collection<String?>
    // Note: NDK part is untested
    get() = try {
        val model = ReflectionAndroidModel.getModel(this)
        ReflectionAndroidModel.getFilteredVariantNames(model)
    } catch (e: Exception) {
        getLog().warn("Failed to get variant names for module ${this.name}: ${e.message}")
        emptyList()
    }

val Module.variantItems: ModuleBuildVariant
    get() = ModuleBuildVariant(name, variantNames.asSequence().filterNotNull().sorted().toList())

data class ModuleBuildVariant(val moduleName: String, val buildVariants: List<String>)