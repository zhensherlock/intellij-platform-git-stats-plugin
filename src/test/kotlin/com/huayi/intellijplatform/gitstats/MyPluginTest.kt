package com.huayi.intellijplatform.gitstats

import com.huayi.intellijplatform.gitstats.components.RefreshButton
import com.huayi.intellijplatform.gitstats.models.SettingModel
import com.huayi.intellijplatform.gitstats.services.GitStatsSettingsService
import com.huayi.intellijplatform.gitstats.services.GitStatsResult
import com.huayi.intellijplatform.gitstats.services.GitStatsService
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import java.util.Calendar
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

    fun testSettingsServiceNormalizesAndCopiesSettings() {
        val service = GitStatsSettingsService()

        service.updateSettings(
            SettingModel(
                mode = SettingModel.MODE_DETAILED,
                exclude = """
                    vendor
                    build\generated
                    vendor
                """.trimIndent()
            )
        )

        assertEquals(SettingModel.MODE_DETAILED, service.state.mode)
        assertEquals("vendor\nbuild/generated", service.state.exclude)

        val settings = service.getSettings()
        settings.exclude = "dist"

        assertEquals("vendor\nbuild/generated", service.state.exclude)
    }

    fun testSettingsServiceFallsBackToFastSummaryForUnknownMode() {
        val service = GitStatsSettingsService()

        service.loadState(SettingModel(mode = "Unknown", exclude = "dist"))

        assertEquals(SettingModel.MODE_FAST_SUMMARY, service.state.mode)
        assertEquals("dist", service.state.exclude)
    }

    fun testSettingsServiceIsAvailableFromProject() {
        assertNotNull(project.service<GitStatsSettingsService>())
    }

    fun testGitStatsServiceReturnsFailureForNonGitProject() {
        projectDirectory().mkdirs()

        val result = project.service<GitStatsService>().getTopSpeedUserStats(
            dateOf(2026, Calendar.JUNE, 1),
            dateOf(2026, Calendar.JUNE, 7),
            SettingModel()
        )

        assertTrue(result is GitStatsResult.Failure)
        assertEquals(MyBundle.message("stateNotGitRepositoryTitle"), (result as GitStatsResult.Failure).title)
    }

    fun testGitStatsServiceReturnsEmptyForGitProjectWithoutCommits() {
        projectDirectory().mkdirs()
        runGit("init")

        val result = project.service<GitStatsService>().getTopSpeedUserStats(
            dateOf(2026, Calendar.JUNE, 1),
            dateOf(2026, Calendar.JUNE, 7),
            SettingModel()
        )

        assertTrue(result is GitStatsResult.Empty)
        assertEquals(MyBundle.message("stateNoDataTitle"), (result as GitStatsResult.Empty).title)
        assertEquals(MyBundle.message("stateNoDataMessage"), result.message)
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

    private fun dateOf(year: Int, month: Int, day: Int) = Calendar.getInstance().apply {
        clear()
        set(year, month, day)
    }.time

    private fun runGit(vararg args: String) {
        val command = listOf(gitExecutable()) + args
        val process = ProcessBuilder(command)
            .directory(projectDirectory())
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        assertEquals(output, 0, process.waitFor())
    }

    private fun projectDirectory() = File(project.basePath!!)

    private fun gitExecutable(): String {
        return listOf("/usr/bin/git", "/opt/homebrew/bin/git")
            .firstOrNull { File(it).canExecute() }
            ?: "git"
    }
}
