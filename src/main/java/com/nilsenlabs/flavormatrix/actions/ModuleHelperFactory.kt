package com.nilsenlabs.flavormatrix.actions

object ModuleHelperFactory {
    fun create() : AndroidModuleHelper {
        return LegacyAndroidModuleHelper()
    }
}