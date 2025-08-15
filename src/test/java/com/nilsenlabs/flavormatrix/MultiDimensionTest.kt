package com.nilsenlabs.flavormatrix

import com.nilsenlabs.flavormatrix.actions.DimensionList
import org.junit.Test
import org.junit.Assert.*

class MultiDimensionTest {

    @Test
    fun testComplexVariantNameParsing() {
        // Test parsing of complex variant names like your android-userland project might have
        val testCases = mapOf(
            "qaArm64Debug" to listOf("qa", "arm64", "debug"),
            "prodFreeArm32Release" to listOf("prod", "free", "arm32", "release"),
            "devPaidArm64Debug" to listOf("dev", "paid", "arm64", "debug"),
            "qaDebug" to listOf("qa", "debug"),
            "simpleRelease" to listOf("simple", "release")
        )

        testCases.forEach { (variantName, expectedFlavors) ->
            val actualFlavors = DimensionList.flavorsFromVariant(variantName)
            assertEquals(
                "Failed for variant: $variantName", 
                expectedFlavors, 
                actualFlavors
            )
        }
    }

    @Test
    fun testDimensionCreationAndOrdering() {
        val dimensionList = DimensionList()
        
        // Simulate creating dimensions like your android-userland project
        val environmentDimension = dimensionList.getOrCreateDimension("environment")
        environmentDimension.addUniqueVariant("qa")
        environmentDimension.addUniqueVariant("prod")
        environmentDimension.addUniqueVariant("dev")
        
        val abiDimension = dimensionList.getOrCreateDimension("abi")
        abiDimension.addUniqueVariant("arm32")
        abiDimension.addUniqueVariant("arm64")
        
        val paymentDimension = dimensionList.getOrCreateDimension("paymentmodel")
        paymentDimension.addUniqueVariant("free")
        paymentDimension.addUniqueVariant("paid")
        
        // Test that dimensions are created correctly
        assertEquals(3, dimensionList.dimensions.size)
        
        // Test unique variant addition
        environmentDimension.addUniqueVariant("qa") // Should not duplicate
        assertEquals(3, environmentDimension.flavors.size)
        
        // Test dimension retrieval
        assertNotNull(dimensionList.getDimensionForFlavor("arm64", false))
        assertNotNull(dimensionList.getDimensionForFlavor("paid", false))
    }

    @Test
    fun testVariantConstruction() {
        val dimensionList = DimensionList()
        
        // Create test dimensions
        val envDim = dimensionList.getOrCreateDimension("environment")
        envDim.addUniqueVariant("qa")
        envDim.addUniqueVariant("prod")
        
        val abiDim = dimensionList.getOrCreateDimension("abi") 
        abiDim.addUniqueVariant("arm32")
        abiDim.addUniqueVariant("arm64")
        
        // Simulate ordered dimension map for a module
        val testModuleName = "app"
        dimensionList.moduleOrderedDimensionMap[testModuleName] = mutableListOf(envDim, abiDim)
        
        // Select flavors
        envDim.selectUniqueFlavor("qa")
        abiDim.selectUniqueFlavor("arm64")
        
        // Test variant construction
        val variant = dimensionList.getSelectedVariantFor(testModuleName)
        assertEquals("qaArm64", variant)
        
        // Test with different selection
        envDim.selectUniqueFlavor("prod")
        abiDim.selectUniqueFlavor("arm32")
        
        val variant2 = dimensionList.getSelectedVariantFor(testModuleName)
        assertEquals("prodArm32", variant2)
    }

    @Test
    fun testIncompleteVariantSelection() {
        val dimensionList = DimensionList()
        
        val envDim = dimensionList.getOrCreateDimension("environment")
        envDim.addUniqueVariant("qa")
        envDim.addUniqueVariant("prod")
        
        val abiDim = dimensionList.getOrCreateDimension("abi")
        abiDim.addUniqueVariant("arm32")
        abiDim.addUniqueVariant("arm64")
        
        val testModuleName = "app"
        dimensionList.moduleOrderedDimensionMap[testModuleName] = mutableListOf(envDim, abiDim)
        
        // Only select one dimension
        envDim.selectUniqueFlavor("qa")
        // Don't select ABI dimension
        
        // Should return null for incomplete selection
        val variant = dimensionList.getSelectedVariantFor(testModuleName)
        assertNull("Should return null when not all dimensions are selected", variant)
    }

    @Test
    fun testDuplicateDeselection() {
        val dimensionList = DimensionList()
        
        val envDim = dimensionList.getOrCreateDimension("environment")
        envDim.addUniqueVariant("qa")
        envDim.addUniqueVariant("prod")
        
        // Artificially select multiple flavors (shouldn't happen in normal UI flow)
        envDim.flavors[0].isSelected = true
        envDim.flavors[1].isSelected = true
        
        dimensionList.deselectDuplicates()
        
        // Should deselect all when multiple were selected
        assertTrue("All flavors should be deselected when duplicates found", 
                   envDim.flavors.none { it.isSelected })
    }
}
