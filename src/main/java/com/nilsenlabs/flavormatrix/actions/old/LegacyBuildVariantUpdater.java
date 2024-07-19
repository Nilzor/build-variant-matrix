//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.nilsenlabs.flavormatrix.actions.old;

import com.android.tools.idea.gradle.project.facet.ndk.NdkFacet;
import com.android.tools.idea.gradle.project.model.NdkModuleModel;
import com.android.tools.idea.gradle.project.model.VariantAbi;
import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker;
import com.android.tools.idea.gradle.project.sync.GradleSyncListener;
import com.android.tools.idea.gradle.project.sync.GradleSyncState;
import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker.Request;
import com.android.tools.idea.gradle.project.sync.GradleSyncStateHolder;
import com.android.tools.idea.gradle.project.sync.idea.AndroidGradleProjectResolver;
import com.android.tools.idea.gradle.project.sync.idea.GradleSyncExecutor;
import com.android.tools.idea.gradle.project.sync.idea.VariantAndAbi;
import com.android.tools.idea.gradle.project.sync.idea.VariantSwitcher;
import com.android.tools.idea.projectsystem.gradle.GradleProjectPath;
import com.google.wireless.android.sdk.stats.GradleSyncStats.Trigger;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ThreeState;
import com.intellij.util.containers.ContainerUtil;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

// Copied from android-studio-2021.2.1.13 \plugins\android\lib\android.jar\com\android\tools\idea\gradle\
public class LegacyBuildVariantUpdater {
    @NotNull
    private final List<BuildVariantSelectionChangeListener> mySelectionChangeListeners = ContainerUtil.createLockFreeCopyOnWriteList();


    public LegacyBuildVariantUpdater() {
    }

    void addSelectionChangeListener(@NotNull BuildVariantSelectionChangeListener listener) {
        this.mySelectionChangeListeners.add(listener);
    }

    public boolean updateSelectedBuildVariant(@NotNull Project project, @NotNull String moduleName, @NotNull String selectedBuildVariant) {
        Module moduleToUpdate = findModule(project, moduleName);
        if (moduleToUpdate == null) {
            logAndShowBuildVariantFailure(String.format("Cannot find module '%1$s'.", moduleName));
            return false;
        } else {
            NdkModuleModel ndkModuleModel = getNdkModelIfItHasNativeVariantAbis(moduleToUpdate);
            NdkFacet ndkFacet = NdkFacet.getInstance(moduleToUpdate);
            if (ndkModuleModel != null && ndkFacet != null) {
                VariantAbi newVariantAbi = resolveNewVariantAbi(ndkFacet, ndkModuleModel, selectedBuildVariant, (String)null);
                if (newVariantAbi == null) {
                    logAndShowBuildVariantFailure(String.format("Cannot find suitable ABI for native module '%1$s'.", moduleName));
                    return false;
                } else {
                    return this.updateSelectedVariant(project, moduleName, VariantAndAbi.fromVariantAbi(newVariantAbi));
                }
            } else {
                return this.updateSelectedVariant(project, moduleName, new VariantAndAbi(selectedBuildVariant, (String)null));
            }
        }
    }

    public boolean updateSelectedAbi(@NotNull Project project, @NotNull String moduleName, @NotNull String selectedAbiName) {
        Module moduleToUpdate = findModule(project, moduleName);
        if (moduleToUpdate == null) {
            logAndShowAbiNameFailure(String.format("Cannot find module '%1$s'.", moduleName));
            return false;
        } else {
            NdkModuleModel ndkModuleModel = getNdkModelIfItHasNativeVariantAbis(moduleToUpdate);
            NdkFacet ndkFacet = NdkFacet.getInstance(moduleToUpdate);
            if (ndkModuleModel != null && ndkFacet != null) {
                VariantAbi currentSelectedVariantAbi = ndkFacet.getSelectedVariantAbi();
                VariantAbi newVariantAbi;
                if (currentSelectedVariantAbi == null) {
                    newVariantAbi = null;
                } else {
                    newVariantAbi = resolveNewVariantAbi(ndkFacet, ndkModuleModel, currentSelectedVariantAbi.getVariant(), selectedAbiName);
                }

                if (newVariantAbi == null) {
                    logAndShowAbiNameFailure(String.format("Cannot find suitable ABI for native module '%1$s'.", moduleName));
                    return false;
                } else {
                    return this.updateSelectedVariant(project, moduleName, VariantAndAbi.fromVariantAbi(newVariantAbi));
                }
            } else {
                logAndShowAbiNameFailure(String.format("Cannot find native module model '%1$s'.", moduleName));
                return false;
            }
        }
    }

    private boolean updateSelectedVariant(@NotNull Project project, @NotNull String moduleName, @NotNull VariantAndAbi variantAndAbi) {
        Module module = findModule(project, moduleName);
        if (module == null) {
            logAndShowBuildVariantFailure(String.format("Cannot find module '%1$s'.", moduleName));
            return false;
        } else if (!findAndUpdateAffectedFacets(module, variantAndAbi)) {
            return false;
        } else {
            ExternalProjectInfo data = ProjectDataManager.getInstance().getExternalProjectData(project, GradleConstants.SYSTEM_ID, project.getBasePath());
            // Commented out by Nilzor due to broken API in Electric Eel
            //Map<GradleProjectPath, VariantAndAbi> variantsExpectedAfterSwitch = (Boolean)StudioFlags.GRADLE_SYNC_ENABLE_CACHED_VARIANTS.get() ? VariantSwitcher.computeExpectedVariantsAfterSwitch(module, variantAndAbi, data) : null;
            Map<GradleProjectPath, VariantAndAbi> variantsExpectedAfterSwitch = null;
            Runnable invokeVariantSelectionChangeListeners = () -> {
                Iterator var1 = this.mySelectionChangeListeners.iterator();

                while(var1.hasNext()) {
                    BuildVariantSelectionChangeListener listener = (BuildVariantSelectionChangeListener)var1.next();
                    listener.selectionChanged();
                }

            };
            if (GradleSyncState.getInstance(project).isSyncNeeded().equals(ThreeState.YES)) {
                //requestGradleSync(project, module, invokeVariantSelectionChangeListeners);
                return true;
            } else {
                if (variantsExpectedAfterSwitch != null) {
                    DataNode<ProjectData> variantProjectDataNode = VariantSwitcher.findAndSetupSelectedCachedVariantData(data, variantsExpectedAfterSwitch);
                    if (variantProjectDataNode != null) {
                        setupCachedVariant(project, variantProjectDataNode, invokeVariantSelectionChangeListeners);
                        return true;
                    }
                }

                AndroidGradleProjectResolver.saveCurrentlySyncedVariantsForReuse(project);
                //requestGradleSync(project, module, invokeVariantSelectionChangeListeners);
                return true;
            }
        }
    }

    private static boolean findAndUpdateAffectedFacets(@NotNull Module moduleToUpdate, @NotNull VariantAndAbi variantToSelect) {
        AndroidFacet androidFacet = AndroidFacet.getInstance(moduleToUpdate);
        if (androidFacet == null) {
            throw new IllegalStateException(String.format("Cannot update the selected build variant. Module: %s Variant: %s", moduleToUpdate, variantToSelect));
        } else {
            NdkFacet ndkFacet = NdkFacet.getInstance(moduleToUpdate);
            VariantAbi selectedVariantAbi = ndkFacet != null ? ndkFacet.getSelectedVariantAbi() : null;
            String selectedAbi = selectedVariantAbi != null ? selectedVariantAbi.getAbi() : null;
            if (Objects.equals(variantToSelect.getVariant(), androidFacet.getProperties().SELECTED_BUILD_VARIANT) && Objects.equals(variantToSelect.getAbi(), selectedAbi)) {
                return false;
            } else {
                String variantName = variantToSelect.getVariant();
                if (ndkFacet != null) {
                    NdkModuleModel ndkModuleModel = getNdkModelIfItHasNativeVariantAbis(ndkFacet);
                    if (ndkModuleModel != null) {
                        VariantAbi variantAbiToSelect = variantToSelect.toVariantAbi();
                        if (variantAbiToSelect != null) {
                            ndkFacet.setSelectedVariantAbi(variantAbiToSelect);
                        }
                    }
                }

                androidFacet.getProperties().SELECTED_BUILD_VARIANT = variantName;
                return true;
            }
        }
    }

    @Nullable
    private static VariantAbi resolveNewVariantAbi(@NotNull NdkFacet ndkFacet, @NotNull NdkModuleModel ndkModel, @NotNull String newVariant, @Nullable String userSelectedAbi) {
        VariantAbi selectedVariantAbi;
        if (userSelectedAbi != null) {
            selectedVariantAbi = new VariantAbi(newVariant, userSelectedAbi);
            if (ndkModel.getAllVariantAbis().contains(selectedVariantAbi)) {
                return selectedVariantAbi;
            }
        }

        selectedVariantAbi = ndkFacet.getSelectedVariantAbi();
        if (selectedVariantAbi == null) {
            return null;
        } else {
            String existingAbi = selectedVariantAbi.getAbi();
            return new VariantAbi(newVariant, existingAbi);
        }
    }

    @NotNull
    private static GradleSyncListener getSyncListener(@NotNull Runnable variantSelectionChangeListeners) {
        return new GradleSyncListener() {
            public void syncFailed(@NotNull Project project, @NotNull String errorMessage) {
                AndroidGradleProjectResolver.clearVariantsSavedForReuse(project);
                variantSelectionChangeListeners.run();
            }

            public void syncSucceeded(@NotNull Project project) {
                AndroidGradleProjectResolver.clearVariantsSavedForReuse(project);
                variantSelectionChangeListeners.run();
            }

            public void syncSkipped(@NotNull Project project) {
                if (project.getUserData(GradleSyncExecutor.ALWAYS_SKIP_SYNC) == null) {
                    throw new IllegalStateException("Sync cannot complete with syncSkipped result when switching variants.");
                } else {
                    AndroidGradleProjectResolver.clearVariantsSavedForReuse(project);
                    variantSelectionChangeListeners.run();
                }
            }
        };
    }

    private static void requestGradleSync(@NotNull Project project, @NotNull Module module, @NotNull Runnable variantSelectionChangeListeners) {
        // Commented out by be, Nilzor because of broken APIs. Unsure of consequences.
        /*
        String moduleId = AndroidGradleProjectResolver.getModuleIdForModule(module);
        if (moduleId != null) {
            project.putUserData(MODULE_WITH_BUILD_VARIANT_SWITCHED_FROM_UI, moduleId);
        }
        */

        Request request = new Request(Trigger.TRIGGER_VARIANT_SELECTION_CHANGED_BY_USER);
        GradleSyncInvoker.getInstance().requestProjectSync(project, request, getSyncListener(variantSelectionChangeListeners));
    }

    public static void requestGradleSync(@NotNull Project project) {
        Request request = new Request(Trigger.TRIGGER_VARIANT_SELECTION_CHANGED_BY_USER);
        GradleSyncInvoker.getInstance().requestProjectSync(project, request, getSyncListener(new Runnable() {
            @Override
            public void run() {

            }
        }) );
    }

    private static void setupCachedVariant(@NotNull Project project, @NotNull DataNode<ProjectData> variantData, @NotNull Runnable variantSelectionChangeListeners) {
        final Application application = ApplicationManager.getApplication();
        Backgroundable task = new Backgroundable(project, "Setting up Project", false) {
            public void run(@NotNull ProgressIndicator indicator) {
                LegacyBuildVariantUpdater.getLog().info("Starting setup of cached variant");
                VariantSwitcher.switchVariant(project, variantData);

                GradleSyncStateHolder.getInstance(project).syncSkipped(null);
                // Commented out by Nilzor due to broken API in Electric Eel. Function not in use anyhow:
                //GradleSyncState.getInstance(project).syncSkipped((GradleSyncListener)null);
                if (application.isUnitTestMode()) {
                    variantSelectionChangeListeners.run();
                } else {
                    application.invokeLater(variantSelectionChangeListeners);
                }

                LegacyBuildVariantUpdater.getLog().info("Finished setup of cached variant");
            }
        };
        if (application.isUnitTestMode()) {
            task.run(new EmptyProgressIndicator());
        } else {
            ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, new BackgroundableProcessIndicator(task));
        }

    }

    @Nullable
    private static Module findModule(@NotNull Project project, @NotNull String moduleName) {
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        return moduleManager.findModuleByName(moduleName);
    }

    @Nullable
    private static NdkModuleModel getNdkModelIfItHasNativeVariantAbis(@NotNull NdkFacet facet) {
        NdkModuleModel ndkModuleModel = NdkModuleModel.get(facet);
        if (ndkModuleModel == null) {
            logAndShowBuildVariantFailure(String.format("Cannot find NativeAndroidProject for module '%1$s'.", facet.getModule().getName()));
            return null;
        } else {
            return ndkModuleModel.getAllVariantAbis().isEmpty() ? null : ndkModuleModel;
        }
    }

    @Nullable
    private static NdkModuleModel getNdkModelIfItHasNativeVariantAbis(@NotNull Module module) {
        NdkModuleModel ndkModuleModel = NdkModuleModel.get(module);
        if (ndkModuleModel == null) {
            return null;
        } else {
            return ndkModuleModel.getAllVariantAbis().isEmpty() ? null : ndkModuleModel;
        }
    }

    private static void logAndShowBuildVariantFailure(@NotNull String reason) {
        String prefix = "Unable to select build variant:\n";
        String msg = prefix + reason;
        getLog().error(msg);
        msg = msg + ".\n\nConsult IDE log for more details (Help | Show Log)";
        Messages.showErrorDialog(msg, "Error");
    }

    private static void logAndShowAbiNameFailure(@NotNull String reason) {
        String prefix = "Unable to select ABI:\n";
        String msg = prefix + reason;
        getLog().error(msg);
        msg = msg + ".\n\nConsult IDE log for more details (Help | Show Log)";
        Messages.showErrorDialog(msg, "Error");
    }

    @NotNull
    private static Logger getLog() {
        return Logger.getInstance(LegacyBuildVariantUpdater.class);
    }
}
