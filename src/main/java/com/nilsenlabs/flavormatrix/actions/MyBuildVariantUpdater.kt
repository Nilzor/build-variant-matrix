package com.nilsenlabs.flavormatrix.actions

import com.android.tools.idea.flags.StudioFlags
import com.android.tools.idea.gradle.project.sync.*
import com.android.tools.idea.gradle.project.sync.idea.*
import com.android.tools.idea.gradle.variant.view.BuildVariantUpdater
import com.google.wireless.android.sdk.stats.GradleSyncStats
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.ThreeState
import org.jetbrains.kotlin.idea.gradleJava.compilerPlugin.AbstractAnnotationBasedCompilerPluginGradleImportHandler
import org.jetbrains.kotlin.idea.gradleJava.configuration.GradleProjectImportHandler
import org.jetbrains.plugins.gradle.util.GradleConstants

class MyBuildVariantUpdater(private val myProject: Project) {
    fun updateSelectedBuildVariant(moduleToUpdate: Module, selectedBuildVariant: String) {
        try {
            val m2u = moduleToUpdate.getModuleIdForSyncRequest()
            updateSelectedVariant(
                moduleToUpdate,
                SwitchVariantRequest(m2u, selectedBuildVariant, null as String?)
            )
        } catch (ex: java.lang.IllegalStateException) {
            System.out.println("WARN: ModuleToUpdate not found for ${moduleToUpdate.name}: $ex")
        }
    }

    fun updateSelectedVariant(moduleToUpdate: Module, variantAndAbi: SwitchVariantRequest) {
        val data = ProjectDataManager.getInstance()
            .getExternalProjectData(this.myProject, GradleConstants.SYSTEM_ID, this.myProject.getBasePath()!!)
        val variantProjectDataNode =
            if (StudioFlags.GRADLE_SYNC_ENABLE_CACHED_VARIANTS.get() as Boolean && data != null) findVariantProjectData(
                moduleToUpdate,
                variantAndAbi,
                data
            ) else null
        if (GradleSyncState.getInstance(this.myProject).isSyncNeeded() == ThreeState.YES) {
            println("XXXX Alpha")
            requestGradleSync(this.myProject, variantAndAbi)
        } else if (data != null && variantProjectDataNode != null) {
            data.findAndSetupSelectedCachedVariantData(variantProjectDataNode)
            disableKotlinCompilerPluginImportHandlers(this.myProject)
            variantProjectDataNode.restoreKotlinUserDataFromDataNodes()
            setupCachedVariant(this.myProject, variantProjectDataNode)
            println("XXXX Bravo")
        } else {
            AndroidGradleProjectResolver.saveCurrentlySyncedVariantsForReuse(this.myProject)
            requestGradleSync(this.myProject, variantAndAbi)
            println("XXXX CHarlie ${variantAndAbi.moduleId} - ${variantAndAbi.variantName} - ${variantAndAbi.abi}")
        }
    }

    private fun setupCachedVariant(project: Project, variantData: DataNode<ProjectData>) {
        val application = ApplicationManager.getApplication()
        val task: Backgroundable = object : Backgroundable(project, "Setting up Project", false) {
            override fun run(indicator: ProgressIndicator) {
                switchVariant(project, variantData)
                GradleSyncStateHolder.getInstance(project).syncSkipped(null as GradleSyncListener?)
            }
        }
        if (application.isUnitTestMode) {
            task.run(EmptyProgressIndicator())
        } else {
            ProgressManager.getInstance()
                .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
        }
    }

    private fun disableKotlinCompilerPluginImportHandlers(project: Project) {
        val importHandlerEP = project.extensionArea.getExtensionPoint(GradleProjectImportHandler.extensionPointName)
        val var2: Iterator<*> = importHandlerEP.extensionList.iterator()
        while (var2.hasNext()) {
            val importHandler = var2.next() as GradleProjectImportHandler
            if (importHandler is AbstractAnnotationBasedCompilerPluginGradleImportHandler<*>) {
                importHandlerEP.unregisterExtension(importHandler.javaClass)
            }
        }
    }

    private fun requestGradleSync(project: Project, requestedVariantChange: SwitchVariantRequest) {
        //val request = GradleSyncInvoker.Request(GradleSyncStats.Trigger.TRIGGER_VARIANT_SELECTION_CHANGED_BY_USER, requestedVariantChange)
        val request = GradleSyncInvoker.Request(GradleSyncStats.Trigger.TRIGGER_VARIANT_FIRST_MARKER, requestedVariantChange)
        GradleSyncInvoker.getInstance().requestProjectSync(project, request, mySyncListener)
    }

    val mySyncListener = object : GradleSyncListener {

    }
}