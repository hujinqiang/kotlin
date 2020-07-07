/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.builder

import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilder
import org.jetbrains.kotlin.gradle.model.Parcelize
import org.jetbrains.kotlin.gradle.model.impl.ParcelizeImpl

/**
 * [ToolingModelBuilder] for [Parcelize] models.
 * This model builder is registered for Parcelize Gradle sub-plugin.
 */
class KotlinParcelizeModelBuilder : ToolingModelBuilder {
    override fun canBuild(modelName: String): Boolean {
        return modelName == Parcelize::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any? {
        if (modelName == Parcelize::class.java.name) {
            return ParcelizeImpl(project.name)
        }

        return null
    }
}