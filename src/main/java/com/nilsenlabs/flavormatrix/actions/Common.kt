package com.nilsenlabs.flavormatrix.actions

import com.intellij.openapi.diagnostic.Logger

fun getLog(): Logger {
    return Logger.getInstance(SelectBuildVariantMatrixAction::class.java)
}