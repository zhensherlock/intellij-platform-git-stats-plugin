package com.huayi.intellijplatform.gitstats

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MyPluginTest : BasePlatformTestCase() {

    fun testBundleLabels() {
        assertEquals("Refresh", MyBundle.message("refreshButtonLabel"))
        assertEquals("Mode:", MyBundle.message("settingDialogModeLabel"))
    }
}
