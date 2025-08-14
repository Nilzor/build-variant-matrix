package com.nilsenlabs.flavormatrix.actions

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import java.lang.reflect.Method

object ReflectionAndroidModel {
    private val logger: Logger = getLog()
    private val modelClassName = "com.android.tools.idea.gradle.project.model.GradleAndroidModel"
    private var modelClass: Class<*>? = null
    private var getMethod: Method? = null

    private fun ensureLoaded() {
        if (modelClass != null) return
        try {
            modelClass = Class.forName(modelClassName)
            // find static get(Module) method
            getMethod = modelClass?.methods?.firstOrNull { m ->
                m.name == "get" &&
                m.parameterCount == 1 &&
                m.parameterTypes[0].name == Module::class.java.name &&
                java.lang.reflect.Modifier.isStatic(m.modifiers)
            }

            if (getMethod != null) {
                logger.debug("Found GradleAndroidModel.get method: ${getMethod!!}")
            } else {
                logger.warn("Could not find static get(Module) method in $modelClassName")
                // Log all available methods for debugging
                modelClass?.methods?.forEach { method ->
                    if (method.name == "get" || method.name.contains("get")) {
                        logger.warn("Available method: ${method.name}, params: ${method.parameterTypes.contentToString()}, static: ${java.lang.reflect.Modifier.isStatic(method.modifiers)}")
                    }
                }
            }
        } catch (t: Throwable) {
            logger.warn("Reflection: Failed to load $modelClassName: ${t.message}")
            modelClass = null
            getMethod = null
        }
    }

    fun getModel(module: Module): Any? {
        try {
            ensureLoaded()
            val gm = getMethod ?: return null
            val result = gm.invoke(null, module)

            // Validate that we got the correct type of model
            if (result != null) {
                val className = result::class.java.name
                logger.debug("Retrieved model of type: $className for module: ${module.name}")

                // Accept both main models and dependency models
                if (className.contains("GradleAndroidDependencyModel")) {
                    logger.debug("Found dependency model for module ${module.name}")
                } else if (className.contains("GradleAndroidModel")) {
                    logger.debug("Found main Android model for module ${module.name}")
                } else {
                    logger.debug("Found unknown model type for module ${module.name}: $className")
                }
            }

            return result
        } catch (t: Throwable) {
            logger.warn("Reflection: Failed to invoke GradleAndroidModel.get for module ${module.name}: ${t.message}")
            return null
        }
    }

    fun getModuleName(model: Any?): String? {
        if (model == null) return null
        try {
            val className = model::class.java.name

            // Try multiple method names for getting module name from both types of models
            val methodNames = listOf("getModuleName", "moduleName", "getName", "name")
            for (methodName in methodNames) {
                try {
                    val m = model::class.java.methods.firstOrNull {
                        it.name == methodName && it.parameterCount == 0
                    }
                    if (m != null) {
                        m.isAccessible = true
                        val result = m.invoke(model)?.toString()
                        if (result != null) {
                            logger.debug("Successfully got module name '$result' using method '$methodName' from $className")
                            return result
                        }
                    }
                } catch (methodException: Throwable) {
                    logger.debug("Method '$methodName' failed for $className: ${methodException.message}")
                    // Continue to next method
                }
            }

            // If we can't get a module name, try to extract it from the model itself
            // For dependency models, sometimes the information is in a different structure
            logger.debug("No accessible method found to get module name from model type: $className")
            return null
        } catch (t: Throwable) {
            logger.warn("Reflection: Failed to get moduleName: ${t.message}")
            return null
        }
    }

    fun getFilteredVariantNames(model: Any?): List<String> {
        if (model == null) return emptyList()
        try {
            val m = model::class.java.methods.firstOrNull { it.name == "getFilteredVariantNames" || it.name == "filteredVariantNames" }
            if (m != null) {
                m.isAccessible = true
                val res = m.invoke(model)
                return when (res) {
                    is Collection<*> -> res.filterIsInstance<String>()
                    is Array<*> -> res.filterIsInstance<String>()
                    else -> emptyList()
                }
            }
            return emptyList()
        } catch (t: Throwable) {
            logger.warn("Reflection: Failed to get filteredVariantNames: ${t.message}")
            return emptyList()
        }
    }

    fun getAndroidProject(model: Any?): Any? {
        if (model == null) return null
        try {
            val m = model::class.java.methods.firstOrNull { it.name == "getAndroidProject" || it.name == "androidProject" }
            if (m != null) {
                m.isAccessible = true  // Make method accessible
                return m.invoke(model)
            }
            return null
        } catch (t: Throwable) {
            logger.warn("Reflection: Failed to get androidProject: ${t.message}")
            return null
        }
    }

    fun getSelectedVariant(model: Any?): Any? {
        if (model == null) return null
        try {
            val m = model::class.java.methods.firstOrNull { it.name == "getSelectedVariant" || it.name == "selectedVariant" }
            if (m != null) {
                m.isAccessible = true  // Make method accessible
                return m.invoke(model)
            }
            return null
        } catch (t: Throwable) {
            logger.warn("Reflection: Failed to get selectedVariant: ${t.message}")
            return null
        }
    }

    fun getSelectedVariantProductFlavors(selectedVariant: Any?): List<String> {
        if (selectedVariant == null) return emptyList()
        try {
            val m = selectedVariant::class.java.methods.firstOrNull { it.name == "getProductFlavors" || it.name == "productFlavors" }
            if (m != null) {
                m.isAccessible = true
                val res = m.invoke(selectedVariant)
                return when (res) {
                    is Collection<*> -> res.mapNotNull { it?.toString() }
                    is Array<*> -> res.mapNotNull { it?.toString() }
                    else -> emptyList()
                }
            }
            return emptyList()
        } catch (t: Throwable) {
            logger.warn("Reflection: Failed to get productFlavors: ${t.message}")
            return emptyList()
        }
    }

    fun getSelectedVariantBuildType(selectedVariant: Any?): String? {
        if (selectedVariant == null) return null
        try {
            val m = selectedVariant::class.java.methods.firstOrNull { it.name == "getBuildType" || it.name == "buildType" }
            if (m != null) {
                m.isAccessible = true
                return m.invoke(selectedVariant)?.toString()
            }
            return null
        } catch (t: Throwable) {
            logger.warn("Reflection: Failed to get buildType from selectedVariant: ${t.message}")
            return null
        }
    }

    fun getBasicVariants(androidProject: Any?): List<Any> {
        if (androidProject == null) return emptyList()
        try {
            val m = androidProject::class.java.methods.firstOrNull { it.name == "getBasicVariants" || it.name == "basicVariants" }
            if (m != null) {
                m.isAccessible = true
                val res = m.invoke(androidProject)
                return when (res) {
                    is Collection<*> -> res.filterNotNull()
                    is Array<*> -> res.filterNotNull()
                    else -> emptyList()
                }
            }
            return emptyList()
        } catch (t: Throwable) {
            logger.warn("Reflection: Failed to get basicVariants: ${t.message}")
            return emptyList()
        }
    }

    fun getBuildTypeNames(model: Any?): List<String> {
        if (model == null) return emptyList()
        try {
            val m = model::class.java.methods.firstOrNull { it.name == "getBuildTypeNames" || it.name == "buildTypeNames" }
            if (m != null) {
                m.isAccessible = true
                val res = m.invoke(model)
                return when (res) {
                    is Collection<*> -> res.filterIsInstance<String>()
                    is Array<*> -> res.filterIsInstance<String>()
                    else -> emptyList()
                }
            }
            return emptyList()
        } catch (t: Throwable) {
            logger.warn("Reflection: Failed to get buildTypeNames: ${t.message}")
            return emptyList()
        }
    }

    /**
     * Extract flavor name and dimension from a Variant-like object using reflection.
     * Returns a list of pairs: (flavorName, dimensionName)
     */
    fun getProductFlavorsFromVariant(variant: Any?): List<Pair<String?, String?>> {
        if (variant == null) return emptyList()
        try {
            val m = variant::class.java.methods.firstOrNull { it.name == "getProductFlavors" || it.name == "productFlavors" }
            if (m != null) {
                m.isAccessible = true
                val res = m.invoke(variant) ?: return emptyList()
                val flavors = when (res) {
                    is Collection<*> -> res.filterNotNull()
                    is Array<*> -> res.filterNotNull()
                    else -> emptyList<Any>()
                }
                val out = mutableListOf<Pair<String?, String?>>()
                for (f in flavors) {
                    try {
                        val nameMethod = f::class.java.methods.firstOrNull { it.name == "getName" || it.name == "name" }
                        val dimMethod = f::class.java.methods.firstOrNull { it.name == "getDimension" || it.name == "dimension" }
                        nameMethod?.isAccessible = true
                        dimMethod?.isAccessible = true
                        val fname = nameMethod?.invoke(f)?.toString()
                        val fdim = dimMethod?.invoke(f)?.toString()
                        out.add(Pair(fname, fdim))
                    } catch (inner: Throwable) {
                        logger.warn("Reflection: Failed to read productFlavor fields: ${inner.message}")
                    }
                }
                return out
            }
            return emptyList()
        } catch (t: Throwable) {
            logger.warn("Reflection: Failed to get productFlavors from variant: ${t.message}")
            return emptyList()
        }
    }

    // New: get the name of a variant-like object
    fun getVariantName(variant: Any?): String? {
        if (variant == null) return null
        try {
            val m = variant::class.java.methods.firstOrNull { it.name == "getName" || it.name == "name" }
            if (m != null) {
                m.isAccessible = true
                return m.invoke(variant)?.toString()
            }
            return null
        } catch (t: Throwable) {
            logger.warn("Reflection: Failed to get variant name: ${t.message}")
            return null
        }
    }
}
