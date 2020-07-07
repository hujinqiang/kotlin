/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pacelize.ide.test

import org.jetbrains.kotlin.checkers.AbstractPsiCheckerTest
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractParcelizeCheckerTest : AbstractPsiCheckerTest() {
    override fun setUp() {
        super.setUp()

        val androidSdk = KotlinTestUtils.findAndroidSdk()
        val androidJarDir = File(androidSdk, "platforms")
            .listFiles().orEmpty()
            .sortedBy { it.name }
            .first { it.name.startsWith("android-") }

        ConfigLibraryUtil.addLibrary(module, "androidJar", androidJarDir.absolutePath, arrayOf("android.jar"))
        ConfigLibraryUtil.addLibrary(module, "androidExtensionsRuntime", "dist/kotlinc/lib", arrayOf("parcelize-runtime.jar"))
    }

    override fun tearDown() {
        ConfigLibraryUtil.removeLibrary(module, "androidJar")
        ConfigLibraryUtil.removeLibrary(module, "androidExtensionsRuntime")

        super.tearDown()
    }
}