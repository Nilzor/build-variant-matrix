package com.nilsenlabs.flavormatrix.actions

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.SystemInfo
import java.awt.Color
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagConstraints.WEST
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.border.LineBorder


class VariantSelectorDialog(
        private val dimensions: DimensionList,
        private val androidModules: List<Any?>,
        project: Project
) : DialogWrapper(project) {
    init {
        this.title = "Select variant"
        // Note: setButtonsAlignment() is deprecated and not essential for functionality
        // if (!SystemInfo.isMac) {
        //     setButtonsAlignment(SwingConstants.CENTER)
        // }
        init()
    }

    override fun createCenterPanel(): JComponent? {
        return createLayouts()
     }

    /**
     * Create the flavor-dimension selection grid
     * Enhanced for better display of complex dimension configurations
     */
    private fun createLayouts() : JComponent {
        val container = JPanel(
            VerticalFlowLayout(VerticalFlowLayout.TOP or VerticalFlowLayout.LEFT, true, false)
        )
        container.maximumSize = java.awt.Dimension(600, 400) // Slightly larger for complex configs

        val selectorContainer = JPanel(GridBagLayout())
        container.add(selectorContainer)
        val moduleListTextArea = JTextArea()

        for ((x, dimension) in dimensions.dimensions.withIndex()) {

            val constForLabel = GridBagConstraints().apply {
                gridx = x
                gridy = 0
                ipady = 8
                ipadx = 16
                anchor = WEST
            }

            // Enhanced dimension title with flavor count for clarity
            val dimensionTitle = "${dimension.dimensionName} (${dimension.flavors.size})"
            val dimTitle = JTextField(dimensionTitle)
            dimTitle.font = Font(dimTitle.font.name, Font.BOLD, dimTitle.font.size)
            dimTitle.isEditable = false
            dimTitle.background = Color(0,0,0,0)
            dimTitle.border = LineBorder(Color.BLACK,0)
            dimTitle.toolTipText = "Dimension: ${dimension.dimensionName} with ${dimension.flavors.size} flavors"
            selectorContainer.add(dimTitle, constForLabel)

            val rbGroup = ButtonGroup()

            for ((y, flavor) in dimension.flavors.withIndex()) {
                val constForButton = GridBagConstraints().apply {
                    gridx = x
                    gridy = y + 1
                    anchor = WEST
                    ipadx = 12
                    ipady = 8
                }

                val flavorView = JRadioButton(flavor.title)
                flavorView.isSelected = flavor.isSelected
                flavorView.toolTipText = "Select ${flavor.title} for ${dimension.dimensionName} dimension"
                selectorContainer.add(flavorView, constForButton)
                rbGroup.add(flavorView)
                flavorView.addActionListener {
                    onRadioButtonChecked(dimension, flavorView, moduleListTextArea)
                }
            }
        }
        val allFlavorsSelected = populateVariantResult(moduleListTextArea)
        enableButtons(allFlavorsSelected)
        container.add(moduleListTextArea)

        selectorContainer.doLayout()
        return container
    }

    private fun populateVariantResult(variantResultList: JTextArea): Boolean {
        var text = "Selected Variants:\n"
        text += "================\n"
        var anyNull = false
        var completeVariants = 0
        
        for (module in androidModules) {
            try {
                val moduleName = try {
                    ReflectionAndroidModel.getModuleName(module) ?: "Unknown Module"
                } catch (error: Throwable) {
                    "Unknown Module"
                }
                
                val variantName = dimensions.getSelectedVariantFor(moduleName)
                if (variantName == null) {
                    anyNull = true
                    text += "$moduleName: [INCOMPLETE - select all dimensions]\n"
                } else {
                    completeVariants++
                    text += "$moduleName: $variantName\n"
                }
            } catch (e: Exception) {
                anyNull = true
                text += "Error accessing module: ${e.message}\n"
            }
        }
        
        text += "\nStatus: $completeVariants/${androidModules.size} modules ready"
        if (anyNull) {
            text += "\n\nPlease select a flavor for each dimension to enable OK button."
        }
        
        variantResultList.text = text
        variantResultList.isEditable = false
        variantResultList.caretPosition = 0 // Scroll to top
        return !anyNull
    }

    /**
     * Update the data model instantly each time a selection is made
     */
    private fun onRadioButtonChecked(dimension: Dimension, button: JRadioButton, variantResultList: JTextArea) {
        if (button.isSelected) {
            dimension.selectUniqueFlavor(button.text)
        }
        val allFlavorsSelected = populateVariantResult(variantResultList)
        enableButtons(allFlavorsSelected)
    }

    fun enableButtons(allFlavorsSelected: Boolean) {
        isOKActionEnabled = allFlavorsSelected
    }
}