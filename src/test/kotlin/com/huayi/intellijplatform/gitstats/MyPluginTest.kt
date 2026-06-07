package com.huayi.intellijplatform.gitstats

import com.huayi.intellijplatform.gitstats.components.RefreshButton
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import javax.swing.SwingUtilities

class MyPluginTest : BasePlatformTestCase() {

    fun testBundleLabels() {
        assertEquals("Refresh", MyBundle.message("refreshButtonLabel"))
        assertEquals("Mode:", MyBundle.message("settingDialogModeLabel"))
    }

    fun testRefreshButtonCanEnterLoadingStateDuringInitialization() = runOnEdt {
        val button = RefreshButton("Refresh", "Loading")

        button.startLoading()

        assertFalse(button.isEnabled)
        assertEquals("Loading", button.text)
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
