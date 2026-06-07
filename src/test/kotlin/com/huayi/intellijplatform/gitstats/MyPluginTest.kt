package com.huayi.intellijplatform.gitstats

import com.huayi.intellijplatform.gitstats.components.RefreshButton
import com.huayi.intellijplatform.gitstats.models.SettingModel
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import javax.swing.SwingUtilities

class MyPluginTest : BasePlatformTestCase() {

    fun testBundleLabels() {
        assertEquals("Refresh", MyBundle.message("refreshButtonLabel"))
        assertEquals("Refreshing...", MyBundle.message("refreshButtonLoadingLabel"))
        assertEquals("Mode:", MyBundle.message("settingDialogModeLabel"))
        assertEquals("Add Path", MyBundle.message("settingDialogAddPathButton"))
    }

    fun testExcludePathsAreNormalized() {
        val paths = SettingModel.parseExcludePaths(
            """
            vendor
              dist
            build\generated
            vendor

            """.trimIndent()
        )

        assertEquals(listOf("vendor", "dist", "build/generated"), paths)
    }

    fun testRefreshButtonCanEnterLoadingStateDuringInitialization() = runOnEdt {
        val button = RefreshButton("Refresh", "Refreshing...")

        button.startLoading()

        assertFalse(button.isEnabled)
        assertEquals("Refreshing...", button.text)
        assertNotNull(button.disabledIcon)

        button.stopLoading()

        assertTrue(button.isEnabled)
        assertEquals("Refresh", button.text)
    }

    private fun runOnEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait(action)
        }
    }
}
