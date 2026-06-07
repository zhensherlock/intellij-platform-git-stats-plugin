package com.huayi.intellijplatform.gitstats

import com.huayi.intellijplatform.gitstats.components.RefreshButton
import com.huayi.intellijplatform.gitstats.models.SettingModel
import com.huayi.intellijplatform.gitstats.models.StatsMode
import com.huayi.intellijplatform.gitstats.services.GitStatsSettingsService
import com.huayi.intellijplatform.gitstats.services.GitStatsResult
import com.huayi.intellijplatform.gitstats.services.GitStatsService
import com.huayi.intellijplatform.gitstats.toolWindow.TableSnapshot
import com.huayi.intellijplatform.gitstats.toolWindow.TableTextFormatters
import com.huayi.intellijplatform.gitstats.toolWindow.StatsTableColumn
import com.huayi.intellijplatform.gitstats.toolWindow.StatsTableColumnKind
import com.huayi.intellijplatform.gitstats.toolWindow.StatsTableModel
import com.huayi.intellijplatform.gitstats.toolWindow.StatsTableRow
import com.huayi.intellijplatform.gitstats.utils.DateRanges
import com.huayi.intellijplatform.gitstats.utils.GitLogCommandBuilder
import com.huayi.intellijplatform.gitstats.utils.GitLogParser
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
        assertEquals("Fast Summary", MyBundle.message("settingsModeFastSummary"))
        assertEquals("Detailed", MyBundle.message("settingsModeDetailed"))
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

    fun testSettingsServiceNormalizesLegacyModeLabels() {
        val service = GitStatsSettingsService()

        service.loadState(SettingModel(mode = StatsMode.DETAILED.legacyLabel, exclude = "dist"))

        assertEquals(StatsMode.DETAILED.id, service.state.mode)
        assertEquals(StatsMode.DETAILED, service.state.statsMode())
    }

    fun testSettingsServiceIsAvailableFromProject() {
        assertNotNull(project.service<GitStatsSettingsService>())
    }

    fun testStatsTableModelUsesTypedNumericValues() {
        val model = StatsTableModel(
            listOf(
                StatsTableRow(
                    author = "Ada",
                    commitCount = 3,
                    addedLines = 120,
                    deletedLines = 40,
                    modifiedFileCount = 8
                )
            ),
            listOf(
                StatsTableColumn(StatsTableColumnKind.AUTHOR, "Author", String::class.java),
                StatsTableColumn(StatsTableColumnKind.COMMITS, "Commits", Int::class.javaObjectType),
                StatsTableColumn(StatsTableColumnKind.LINES_ADDED, "Lines Added", Int::class.javaObjectType)
            )
        )

        assertEquals(String::class.java, model.getColumnClass(0))
        assertEquals(Int::class.javaObjectType, model.getColumnClass(1))
        assertEquals("Ada", model.getValueAt(0, 0))
        assertEquals(3, model.getValueAt(0, 1))
        assertEquals(120, model.getValueAt(0, 2))
        assertFalse(model.isCellEditable(0, 0))
    }

    fun testGitLogCommandBuilderUsesExcludePathspecs() {
        val command = GitLogCommandBuilder("/usr/bin/git").fastSummaryCommand(
            "2026-06-01 00:00:00",
            "2026-06-07 23:59:59",
            listOf("vendor", "build/generated")
        )

        assertTrue(command.containsAll(listOf("--", ".", ":(exclude)vendor", ":(exclude)build/generated")))
        assertEquals("/usr/bin/git", command.first())
        assertEquals("log", command[1])
    }

    fun testGitLogParserParsesFastSummaryOutput() {
        val stats = GitLogParser.parseFastSummary(
            """
            Ada
            10	2	src/A.kt
            -	-	assets/image.png
            Lin
            3	1	README.md
            """.trimIndent()
        )

        assertEquals(2, stats.size)
        assertEquals("Ada", stats[0].author)
        assertEquals(10, stats[0].addedLines)
        assertEquals(2, stats[0].deletedLines)
        assertEquals(2, stats[0].modifiedFileCount)
        assertEquals("Lin", stats[1].author)
    }

    fun testGitLogParserParsesDetailedOutput() {
        val separator = "\u001F"
        val stats = GitLogParser.parseDetailed(
            """
            ${separator}abc123${separator}2026-06-01 10:00:00 +0800${separator}Ada
            12	4	src/A.kt
            ${separator}def456${separator}2026-06-02 10:00:00 +0800${separator}Ada
            1	0	src/B.kt
            """.trimIndent()
        )

        assertEquals(1, stats.size)
        assertEquals("Ada", stats[0].author)
        assertEquals(2, stats[0].commitCount)
        assertEquals(13, stats[0].addedLines)
        assertEquals(4, stats[0].deletedLines)
        assertEquals(2, stats[0].modifiedFileCount)
        assertEquals("abc123", stats[0].commits[0].hash)
        assertEquals("src/A.kt", stats[0].commits[0].files[0].fileName)
    }

    fun testDateRangesParseAndOrderRange() {
        val range = DateRanges.parseDateRange("2026-06-07 - 2026-06-01")

        assertNotNull(range)
        assertEquals("2026-06-01", DateRanges.formatDate(range!!.first))
        assertEquals("2026-06-07", DateRanges.formatDate(range.second))
        assertNull(DateRanges.parseDateRange("2026-02-31 - 2026-03-01"))
    }

    fun testTableTextFormattersEscapeCsvAndNormalizeTsv() {
        val snapshot = TableSnapshot(
            headers = listOf("Author", "Note"),
            rows = listOf(
                listOf("Ada", "comma,value"),
                listOf("Lin", "quote \"value\""),
                listOf("Grace", "line\nbreak")
            )
        )

        assertEquals(
            "Author,Note\n" +
                "Ada,\"comma,value\"\n" +
                "Lin,\"quote \"\"value\"\"\"\n" +
                "Grace,\"line\nbreak\"",
            TableTextFormatters.toCsv(snapshot)
        )
        assertEquals(
            "Author\tNote\nAda\tcomma,value\nLin\tquote \"value\"\nGrace\tline break",
            TableTextFormatters.toTsv(snapshot)
        )
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
