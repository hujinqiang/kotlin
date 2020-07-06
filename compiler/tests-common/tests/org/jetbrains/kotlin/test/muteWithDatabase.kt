/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import junit.framework.TestCase
import org.jetbrains.kotlin.test.mutes.*
import org.junit.internal.runners.statements.InvokeMethod
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters
import org.junit.runners.parameterized.ParametersRunnerFactory
import org.junit.runners.parameterized.TestWithParameters

private val SKIP_MUTED_TESTS = java.lang.Boolean.getBoolean("org.jetbrains.kotlin.skip.muted.tests")

private fun isMutedInDatabase(testClass: Class<*>, methodKey: String): Boolean {
    val mutedTest = mutedSet.mutedTest(testClass, methodKey)
    return mutedTest != null && (if (SKIP_MUTED_TESTS) !mutedTest.hasFailFile else mutedTest.isFlaky)
}

private fun isMutedInDatabase(testCase: TestCase): Boolean {
    return isMutedInDatabase(testCase.javaClass, testCase.name)
}

private fun isMutedInDatabaseWithLog(testClass: Class<*>, methodKey: String): Boolean {
    return if (isMutedInDatabase(testClass, methodKey)) {
        System.err.println(mutedMessage(testClass, methodKey))
        true
    } else {
        false
    }
}

internal fun wrapWithMuteInDatabase(testCase: TestCase, f: () -> Unit): (() -> Unit)? {
    if (isMutedInDatabase(testCase)) {
        return {
            System.err.println(mutedMessage(testCase))
        }
    }

    val mutedTest = getMutedTest(testCase.javaClass, testCase.name)
    if (mutedTest != null && !mutedTest.hasFailFile) {
        return {
            val testKey = testKey(testCase)
            invertMutedTestResultWithLog(f, testKey)
        }
    } else {
        val doAutoMute = DO_AUTO_MUTE ?: return null
        return {
            try {
                f()
            } catch (e: Throwable) {
                doAutoMute.muteTest(testKey(testCase))
                throw e
            }
        }
    }
}

private fun testKey(klass: Class<*>, methodKey: String) = "${klass.canonicalName}.$methodKey"
private fun testKey(testCase: TestCase) = testKey(testCase::class.java, testCase.name)

private fun mutedMessage(klass: Class<*>, methodKey: String) = "MUTED TEST: ${testKey(klass, methodKey)}"
private fun mutedMessage(testCase: TestCase) = mutedMessage(testCase::class.java, testCase.name)

class RunnerFactoryWithMuteInDatabase : ParametersRunnerFactory {
    override fun createRunnerForTestWithParameters(testWithParameters: TestWithParameters?): Runner {
        return object : BlockJUnit4ClassRunnerWithParameters(testWithParameters) {
            override fun isIgnored(child: FrameworkMethod): Boolean {
                return super.isIgnored(child) || isIgnoredInDatabaseWithLog(child, name)
            }

            override fun runChild(method: FrameworkMethod, notifier: RunNotifier) {
                notifier.withAutoMuteListener(method.declaringClass, parametrizedMethodKey(method, name)) {
                    super.runChild(method, notifier)
                }
            }

            override fun methodInvoker(method: FrameworkMethod, test: Any?): Statement {
                return object : InvokeMethod(method, test) {
                    override fun evaluate() {
                        val methodClass = method.declaringClass
                        val methodKey = parametrizedMethodKey(method, name)
                        val mutedTest = getMutedTest(methodClass, methodKey) ?: getMutedTest(methodClass, method.method.name)
                        if (mutedTest != null && !mutedTest.hasFailFile) {
                            val testKey = testKey(methodClass, methodKey)
                            invertMutedTestResultWithLog({ super.evaluate() }, testKey)
                            return
                        }
                        super.evaluate()
                    }
                }
            }
        }
    }
}

private fun invertMutedTestResultWithLog(f: () -> Unit, testKey: String) {
    var isTestGreen = true
    try {
        f()
    } catch (e: Throwable) {
        println("MUTED TEST STILL FAILS: $testKey")
        isTestGreen = false
    }

    if (isTestGreen) {
        System.err.println("SUCCESS RESULT OF MUTED TEST: $testKey")
        throw Exception("Muted non-flaky test $testKey finished successfully. Please remove it from csv file")
    }
}

private fun parametrizedMethodKey(child: FrameworkMethod, parametersName: String): String {
    return child.method.name + parametersName
}

private inline fun RunNotifier.withAutoMuteListener(
    declaredClass: Class<*>,
    methodKey: String,
    crossinline run: () -> Unit,
) {
    val doAutoMute = DO_AUTO_MUTE
    if (doAutoMute != null) {
        val autoMuteListener = object : RunListener() {
            override fun testFailure(failure: Failure) {
                doAutoMute.muteTest(testKey(declaredClass, methodKey))
                super.testFailure(failure)
            }
        }

        try {
            addListener(autoMuteListener)
            run()
        } finally {
            removeListener(autoMuteListener)
        }
    } else {
        run()
    }
}

private fun isIgnoredInDatabaseWithLog(child: FrameworkMethod, parametersName: String): Boolean {
    val methodWithParametersKey = parametrizedMethodKey(child, parametersName)

    return isMutedInDatabaseWithLog(child.declaringClass, child.name)
            || isMutedInDatabaseWithLog(child.declaringClass, methodWithParametersKey)
}

fun TestCase.runTest(test: () -> Unit) {
    (wrapWithMuteInDatabase(this, test) ?: test).invoke()
}

annotation class WithMutedInDatabaseRunTest
