package com.nilsenlabs.flavormatrix.actions


class DimensionList {
    companion object {
        val BUILD_TYPE_NAME = "buildType"

        /** Splits the flavor-buildtype-string into individual parts. e.g. qaArm64Debug -> [qa, arm64, debug] */
        fun flavorsFromVariant(variantName: String): List<String> {
            // Enhanced parsing for complex variant names like "qaArm64Debug" or "prodFreeArm32Release"
            val flavors = variantName
                .split(Regex("(?=[A-Z])"))
                .filter { it.isNotEmpty() }
                .map { it.lowercase() }
            return flavors
        }
    }

    /**
     * All dimensions from all modules merged
     */
    val dimensions = mutableListOf<Dimension>()

    /**
     * Ordered list of dimensions per module
     */
    val moduleOrderedDimensionMap = mutableMapOf<String, MutableList<Dimension>>()

    fun getOrCreateDimension(dimensionName: String): Dimension {
        return dimensions.firstOrNull { it.dimensionName == dimensionName} ?: Dimension(dimensionName).also {
            dimensions.add(it)
        }
    }

    fun getDimensionForFlavor(flavorName: String, selected: Boolean): Dimension? {
        val flavorSelectable = SelectableString(flavorName, selected)
        return dimensions.firstOrNull { it.flavors.contains(flavorSelectable) }
    }

    /** Select currently selected flavors from reflection-based android models */
    fun selectFrom(androidModels: List<Any?>) {
        for (model in androidModels) {
            try {
                val selectedVariant = ReflectionAndroidModel.getSelectedVariant(model)
                val flavors = ReflectionAndroidModel.getSelectedVariantProductFlavors(selectedVariant)
                for (selectedFlavor in flavors) {
                    getFlavorByName(selectedFlavor)?.isSelected = true
                }
                val selectedBuild = ReflectionAndroidModel.getSelectedVariantBuildType(selectedVariant)
                getDimensionByName(BUILD_TYPE_NAME)?.flavors?.firstOrNull { it.title == selectedBuild }?.isSelected = true
            } catch (e: Exception) {
                // Log warning but continue with other modules
                getLog().warn("Failed to select flavors for a module: ${e.message}")
            }
        }
    }

    /** Generates a selectable variant string for the given module based on the selected items
     * Enhanced for complex multi-dimension scenarios
     */
    fun getSelectedVariantFor(moduleName: String): String? {
        val dimensionList: MutableList<Dimension> = moduleOrderedDimensionMap[moduleName] ?: return null
        try {
            val selectedFlavors = mutableListOf<String>()

            // Collect selected flavors from each dimension in order
            for (dimension in dimensionList) {
                val selectedFlavor = dimension.flavors.firstOrNull { it.isSelected }
                if (selectedFlavor != null) {
                    selectedFlavors.add(selectedFlavor.title)
                } else {
                    // If any dimension has no selection, variant is incomplete
                    getLog().warn("No flavor selected for dimension '${dimension.dimensionName}' in module '$moduleName')")
                    return null
                }
            }

            // Construct variant name: first flavor lowercase, rest capitalized
            val variantString = selectedFlavors.mapIndexed { index, flavor ->
                if (index == 0) {
                    flavor.lowercase()
                } else {
                    flavor.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase() else char.toString()
                    }
                }
            }.joinToString("")

            getLog().info("Constructed variant for module '$moduleName': $variantString (from flavors: ${selectedFlavors.joinToString(", ")})")
            return variantString

        } catch (ex: NoSuchElementException) {
            getLog().warn("Could not construct variant for module '$moduleName' - missing selected flavors")
            return null
        } catch (ex: Exception) {
            getLog().warn("Failed to construct variant for module '$moduleName': ${ex.message}")
            return null
        }
    }

    /** Make a map of Module => Ordered List Of Dimensions, where order
     * matches what Android Studio lists when string concatenating
     * Enhanced for complex multi-dimension scenarios including ABI
     */
    fun createOrderedDimensionMaps(models: List<Any?>) {
        for (model in models) {
            try {
                val androidProject = ReflectionAndroidModel.getAndroidProject(model)
                val basicVariants = ReflectionAndroidModel.getBasicVariants(androidProject)
                val firstVariant = basicVariants.firstOrNull()
                val firstVariantName = ReflectionAndroidModel.getVariantName(firstVariant)
                if (firstVariantName != null) {
                    val orderedFlavors = flavorsFromVariant(firstVariantName)
                    val moduleName = ReflectionAndroidModel.getModuleName(model) ?: ""
                    val dimensionsForFlavor = mutableListOf<Dimension>()
                    moduleOrderedDimensionMap[moduleName] = dimensionsForFlavor

                    getLog().debug("Creating ordered dimension map for module $moduleName")
                    getLog().debug("First variant name: '$firstVariantName' -> ordered flavors: ${orderedFlavors.joinToString(", ")}")

                    for (flavor in orderedFlavors) {
                        val dimension = getDimensionForFlavor(flavor, false)
                        if (dimension != null) {
                            dimensionsForFlavor.add(dimension)
                            getLog().debug("  Added dimension '${dimension.dimensionName}' for flavor '$flavor'")
                        } else {
                            getLog().debug("  Warning: Could not find dimension for flavor '$flavor' in variant '$firstVariantName'")
                        }
                    }

                    // Validate that we have the expected number of dimensions
                    val expectedDimensionCount = orderedFlavors.size
                    val actualDimensionCount = dimensionsForFlavor.size
                    if (expectedDimensionCount != actualDimensionCount) {
                        getLog().warn("Dimension count mismatch for module $moduleName. Expected: $expectedDimensionCount, Got: $actualDimensionCount")
                    }
                }
            } catch (e: Exception) {
                getLog().warn("Failed to create ordered dimension map for a module: ${e.message}")
            }
        }
    }

    private fun getDimensionByName(name: String): Dimension? {
        return dimensions.firstOrNull { it.dimensionName == name }
    }

    private fun getFlavorByName(name: String): SelectableString? {
        return dimensions
            .flatMap { it.flavors }
            .firstOrNull { it.title == name }
    }

    fun deselectDuplicates() {
        for (dimension in dimensions) {
            val selectedFlavors = dimension.flavors.count { it.isSelected }
            if (selectedFlavors > 1) {
                dimension.flavors.forEach { it.isSelected = false }
            }
        }
    }
}

data class Dimension(
    val dimensionName: String,
    val flavors: MutableList<SelectableString> = mutableListOf()
) {
    fun addUniqueVariant(variant: String) {
        val selString = SelectableString(variant, false)
        if (!flavors.contains(selString)) flavors.add(selString)
    }

    fun selectUniqueFlavor(text: String) {
        flavors.forEach { it.isSelected = false }
        flavors.firstOrNull { it.title == text }?.isSelected = true
    }
}

data class SelectableString(
    val title: String,
    var isSelected: Boolean = false
)