package com.nilsenlabs.flavormatrix.actions

import com.android.tools.idea.gradle.project.model.GradleAndroidModel
import kotlin.streams.toList

class DimensionList {
    companion object {
        val BUILD_TYPE_NAME = "buildType"

        fun flavorsFromVariant(variantName: String): List<String> {
            return variantName
                    .split(Regex("(?=[A-Z])"))
                    .toList()
                    .map { it.toLowerCase() }
        }
    }

    /**
     * All dimensions from all modulese merged
     */
    val dimensions = mutableListOf<Dimension>()

    /**
     * Ordered list om dimensions per module
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

    fun selectFrom(androidModules: List<GradleAndroidModel>) {
        for (module in androidModules) {
            for (selectedFlavor in module.selectedVariant.productFlavors) {
                getFlavorByName(selectedFlavor)?.isSelected = true
            }
            val selectedBuild = module.selectedVariant.buildType
            getDimensionByName(BUILD_TYPE_NAME)?.flavors?.
                firstOrNull { it.title == selectedBuild }?.isSelected = true

        }
    }

    /** Generates a selectable variant string for the given module based on the selected items */
    fun getSelectedVariantFor(moduleName: String): String? {
        val dimensionList: MutableList<Dimension> = moduleOrderedDimensionMap[moduleName] ?: return null
        try {
            val variantString = dimensionList.joinToString("") {
                it.flavors
                    .first { it.isSelected }
                    .title.capitalize() // Lowercase first char
            }
            return variantString[0].toLowerCase().toString() + variantString.subSequence(1, variantString.length)
        } catch (ex: NoSuchElementException) {
            return null
        }
    }

    /** Make a map of Module => Ordered List Of Dimensions, where order
     * matches what Android Studio lists when string concatenating
     */
    fun createOrderedDimensionMaps(modules: List<GradleAndroidModel>) {
        for (module in modules) {
            val variantList = module.androidProject.basicVariants.stream().toList()
             variantList.firstOrNull()?.let { firstVariant ->
                // The (first) named variant is always sorted the same way we need to sort the output
                // e.g. "alphaBravoCharlie" means the dimension for "alpha" always must come first
                val orderedFlavors = flavorsFromVariant(firstVariant.name)
                val dimensionsForFlavor = mutableListOf<Dimension>()
                moduleOrderedDimensionMap[module.moduleName] = dimensionsForFlavor
                for (flavor in orderedFlavors) {
                    getDimensionForFlavor(flavor, false)?.let { dimension ->
                        dimensionsForFlavor.add(dimension)
                    }
                }
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