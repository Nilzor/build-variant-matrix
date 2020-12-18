package com.nilsenlabs.flavormatrix.actions

import com.android.tools.idea.gradle.project.model.AndroidModuleModel
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
        private val androidModules: List<AndroidModuleModel>,
        project: Project
) : DialogWrapper(project) {
    init {
        this.title = "Select variant"
        if (!SystemInfo.isMac) {
            setButtonsAlignment(SwingConstants.CENTER)
        }
        init()
    }

    override fun createCenterPanel(): JComponent? {
        return createLayouts()
     }

    /**
     * Create the flavor-dimension selection grid
     */
    private fun createLayouts() : JComponent {
        val container = JPanel(
            VerticalFlowLayout(VerticalFlowLayout.TOP or VerticalFlowLayout.LEFT, true, false)
        )
        container.maximumSize = java.awt.Dimension(500, 300)

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

            val dimTitle = JTextField(dimension.dimensionName)
            dimTitle.font = Font(dimTitle.font.name, Font.BOLD, dimTitle.font.size)
            dimTitle.isEditable = false
            dimTitle.background = Color(0,0,0,0)
            dimTitle.border = LineBorder(Color.BLACK,0)
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
        var text = ""
        var anyNull = false
        for (module in androidModules) {
            val variantName = dimensions.getSelectedVariantFor(module.moduleName)
            if (variantName == null) anyNull = true
            text += "${module.moduleName}: $variantName\n"
        }
        variantResultList.text = text
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