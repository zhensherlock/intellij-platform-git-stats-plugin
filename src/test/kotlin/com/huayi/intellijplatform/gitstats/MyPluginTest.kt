package com.huayi.intellijplatform.gitstats

import com.huayi.intellijplatform.gitstats.components.branchScope.BranchScopePresentation
import com.huayi.intellijplatform.gitstats.components.branchScope.BranchScopeSelectPopup
import com.huayi.intellijplatform.gitstats.components.filters.DateRangePopupActionGroupFactory
import com.huayi.intellijplatform.gitstats.components.filters.GitLogFilterChip
import com.huayi.intellijplatform.gitstats.components.filters.RefreshStatsAction
import com.huayi.intellijplatform.gitstats.models.BranchInfo
import com.huayi.intellijplatform.gitstats.models.BranchRefType
import com.huayi.intellijplatform.gitstats.models.BranchScope
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
import com.huayi.intellijplatform.gitstats.toolWindow.StatsTableSupport
import com.huayi.intellijplatform.gitstats.utils.DateRanges
import com.huayi.intellijplatform.gitstats.utils.GitLogCommandBuilder
import com.huayi.intellijplatform.gitstats.utils.GitLogParser
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import java.awt.event.MouseEvent
import java.io.File
import java.util.Calendar
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.table.TableRowSorter

class MyPluginTest : BasePlatformTestCase() {

    fun testBundleLabels() {
        assertEquals("Refresh", MyBundle.message("refreshButtonLabel"))
        assertEquals("Refreshing...", MyBundle.message("refreshButtonLoadingLabel"))
        assertEquals("Branch:", MyBundle.message("filterScopeLabel"))
        assertEquals("Branch", MyBundle.message("filterScopeButtonLabel"))
        assertEquals("User", MyBundle.message("filterAuthorButtonLabel"))
        assertEquals("No date range filter", MyBundle.message("filterDateEmptyTooltip"))
        assertEquals("Select...", MyBundle.message("datePickerSelectButton"))
        assertEquals("Current Branch", MyBundle.message("branchScopeCurrent"))
        assertEquals("Custom Revision Range", MyBundle.message("branchScopeCustom"))
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

    fun testVisibleRowsIgnoresStaleSorterIndexesDuringModelSwap() = runOnEdt {
        val columns = listOf(
            StatsTableColumn(StatsTableColumnKind.AUTHOR, "Author", String::class.java),
            StatsTableColumn(StatsTableColumnKind.LINES_ADDED, "Lines Added", Int::class.javaObjectType)
        )
        val oldModel = StatsTableModel(
            listOf(
                StatsTableRow("Ada", addedLines = 3, deletedLines = 1, modifiedFileCount = 1),
                StatsTableRow("Lin", addedLines = 2, deletedLines = 1, modifiedFileCount = 1),
                StatsTableRow("Grace", addedLines = 1, deletedLines = 0, modifiedFileCount = 1)
            ),
            columns
        )
        val newModel = StatsTableModel(
            listOf(
                StatsTableRow("Ada", addedLines = 3, deletedLines = 1, modifiedFileCount = 1),
                StatsTableRow("Lin", addedLines = 2, deletedLines = 1, modifiedFileCount = 1)
            ),
            columns
        )
        val table = JTable(oldModel).apply {
            rowSorter = TableRowSorter(oldModel)
        }

        val visibleRows = StatsTableSupport.visibleRows(table, newModel)

        assertEquals(2, visibleRows.size)
        assertEquals(listOf("Ada", "Lin"), visibleRows.map { it.author })
    }

    fun testAuthorFilterMatchesAnySeparatedUser() = runOnEdt {
        val model = StatsTableModel(
            listOf(
                StatsTableRow("Ada", addedLines = 3, deletedLines = 1, modifiedFileCount = 1),
                StatsTableRow("Lin", addedLines = 2, deletedLines = 1, modifiedFileCount = 1),
                StatsTableRow("Grace", addedLines = 1, deletedLines = 0, modifiedFileCount = 1),
                StatsTableRow("Adalyn", addedLines = 1, deletedLines = 0, modifiedFileCount = 1)
            ),
            listOf(
                StatsTableColumn(StatsTableColumnKind.AUTHOR, "Author", String::class.java),
                StatsTableColumn(StatsTableColumnKind.LINES_ADDED, "Lines Added", Int::class.javaObjectType)
            )
        )
        val filter = StatsTableSupport.createAuthorFilter(model, "ada | Lin")
        val table = JTable(model)
        table.rowSorter = TableRowSorter(model).apply {
            rowFilter = filter
        }

        assertNotNull(filter)
        assertEquals(
            listOf("Ada", "Lin"),
            StatsTableSupport.visibleRows(table, model).map { it.author }
        )
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

    fun testGitLogCommandBuilderOmitsDateOptionsForAllTime() {
        val command = GitLogCommandBuilder("/usr/bin/git").fastSummaryCommand(
            null,
            null,
            emptyList()
        )

        assertFalse(command.any { it.startsWith("--since=") })
        assertFalse(command.any { it.startsWith("--until=") })
        assertTrue(command.containsAll(listOf("/usr/bin/git", "log", "--format=%aN", "--numstat", "--", ".")))
    }

    fun testDateRangePopupContainsCustomSelectionAndPresets() {
        val popup = DateRangePopupActionGroupFactory(
            applyPreset = {},
            requestCustomRange = {}
        ).create()

        val actionTexts = popup.childActionsOrStubs
            .mapNotNull { it.templateText }

        assertEquals(
            listOf("Select...", "This Week", "Last 7 Days", "This Month"),
            actionTexts
        )
    }

    fun testFilterChipKeepsLabelVerticalPositionWhenValueChanges() = runOnEdt {
        val chip = GitLogFilterChip(
            onOpenPopup = {},
            onClear = {}
        )

        chip.update(GitLogFilterChip.Model("Date", null, null, null))
        chip.setSize(chip.preferredSize)
        chip.doLayout()
        val emptyLabelY = chip.firstFilterLabelY("Date")

        chip.update(GitLogFilterChip.Model("Date", "This Week", null, null))
        chip.setSize(chip.preferredSize)
        chip.doLayout()
        val selectedLabelY = chip.firstFilterLabelY("Date")

        assertEquals(emptyLabelY, selectedLabelY)
    }

    fun testFilterChipClearingResetsHoveredLabelColor() = runOnEdt {
        lateinit var chip: GitLogFilterChip
        chip = GitLogFilterChip(
            onOpenPopup = {},
            onClear = { chip.update(GitLogFilterChip.Model("Date", null, null, null)) }
        )

        chip.update(GitLogFilterChip.Model("Date", "This Week", null, null))
        val clearLabel = chip.components
            .filterIsInstance<JLabel>()
            .first { it.icon != null }

        dispatchMouseEvent(clearLabel, MouseEvent.MOUSE_ENTERED)
        assertTrue(chip.isHovered())

        dispatchMouseEvent(clearLabel, MouseEvent.MOUSE_CLICKED)

        assertFalse(chip.isHovered())
        assertEquals(UIUtil.getContextHelpForeground(), chip.firstFilterLabel("Date").foreground)
    }

    fun testGitLogCommandBuilderUsesBranchScopeBeforePathspecs() {
        val selectedBranchCommand = GitLogCommandBuilder("/usr/bin/git").detailedCommand(
            "2026-06-01 00:00:00",
            "2026-06-07 23:59:59",
            emptyList(),
            BranchScope.selectedBranch("feature/login")!!
        )
        val selectedBranchRevisionIndex = selectedBranchCommand.indexOf("refs/heads/feature/login")
        val selectedBranchPathspecIndex = selectedBranchCommand.indexOf("--")

        assertTrue(selectedBranchRevisionIndex > 0)
        assertTrue(selectedBranchRevisionIndex < selectedBranchPathspecIndex)

        val remoteBranch = BranchInfo(
            localBranches = listOf("master"),
            remoteBranches = listOf("origin/master")
        ).selectableBranches.first { it.type == BranchRefType.REMOTE }
        val remoteBranchCommand = GitLogCommandBuilder("/usr/bin/git").detailedCommand(
            "2026-06-01 00:00:00",
            "2026-06-07 23:59:59",
            emptyList(),
            BranchScope.selectedBranch(remoteBranch)!!
        )
        val remoteBranchRevisionIndex = remoteBranchCommand.indexOf("refs/remotes/origin/master")
        val remoteBranchPathspecIndex = remoteBranchCommand.indexOf("--")

        assertTrue(remoteBranchRevisionIndex > 0)
        assertTrue(remoteBranchRevisionIndex < remoteBranchPathspecIndex)

        val multiBranchScope = BranchScope.selectedBranches(
            BranchInfo(
                localBranches = listOf("main"),
                remoteBranches = listOf("origin/main")
            ).selectableBranches
        )!!
        val multiBranchCommand = GitLogCommandBuilder("/usr/bin/git").fastSummaryCommand(
            "2026-06-01 00:00:00",
            "2026-06-07 23:59:59",
            emptyList(),
            multiBranchScope
        )
        val localMainIndex = multiBranchCommand.indexOf("refs/heads/main")
        val remoteMainIndex = multiBranchCommand.indexOf("refs/remotes/origin/main")
        val multiBranchPathspecIndex = multiBranchCommand.indexOf("--")

        assertTrue(localMainIndex > 0)
        assertTrue(remoteMainIndex > 0)
        assertTrue(localMainIndex < multiBranchPathspecIndex)
        assertTrue(remoteMainIndex < multiBranchPathspecIndex)

        val headCommand = GitLogCommandBuilder("/usr/bin/git").fastSummaryCommand(
            "2026-06-01 00:00:00",
            "2026-06-07 23:59:59",
            emptyList(),
            BranchScope.Head
        )
        val headIndex = headCommand.indexOf("HEAD")
        val headPathspecIndex = headCommand.indexOf("--")

        assertTrue(headIndex > 0)
        assertTrue(headIndex < headPathspecIndex)

        val allLocalBranchesCommand = GitLogCommandBuilder("/usr/bin/git").fastSummaryCommand(
            "2026-06-01 00:00:00",
            "2026-06-07 23:59:59",
            emptyList(),
            BranchScope.AllLocalBranches
        )
        val allBranchesIndex = allLocalBranchesCommand.indexOf("--branches")
        val allBranchesPathspecIndex = allLocalBranchesCommand.indexOf("--")

        assertTrue(allBranchesIndex > 0)
        assertTrue(allBranchesIndex < allBranchesPathspecIndex)

        val customRangeCommand = GitLogCommandBuilder("/usr/bin/git").fastSummaryCommand(
            "2026-06-01 00:00:00",
            "2026-06-07 23:59:59",
            emptyList(),
            BranchScope.customRevisionRange("main..feature/login")!!
        )
        val customRangeIndex = customRangeCommand.indexOf("main..feature/login")
        val customRangePathspecIndex = customRangeCommand.indexOf("--")

        assertTrue(customRangeIndex > 0)
        assertTrue(customRangeIndex < customRangePathspecIndex)
    }

    fun testBranchScopeNormalizesAndValidatesCustomRevisionRange() {
        val scope = BranchScope.customRevisionRange(" main..feature/login ")

        assertNotNull(scope)
        assertEquals("main..feature/login", scope!!.revisionRange)
        assertNull(BranchScope.customRevisionRange(""))
        assertNull(BranchScope.customRevisionRange("main feature"))
        assertNull(BranchScope.customRevisionRange("--"))
        assertNull(BranchScope.customRevisionRange("--all"))
        assertNull(BranchScope.customRevisionRange("main\nfeature"))
    }

    fun testBranchScopePresentationUsesShortToolbarTextAndFullTooltip() {
        val scope = BranchScope.customRevisionRange("main..feature/some-very-long-branch-name")!!

        assertEquals("Branch", BranchScopePresentation.buttonText())
        assertEquals("Branch", BranchScopePresentation.toolbarText(BranchScope.CurrentBranch))
        assertEquals("Branch: HEAD", BranchScopePresentation.toolbarText(BranchScope.Head))
        assertEquals(
            "Branch: main | origin/main",
            BranchScopePresentation.toolbarText(
                BranchScope.selectedBranches(
                    BranchInfo(
                        localBranches = listOf("main"),
                        remoteBranches = listOf("origin/main")
                    ).selectableBranches
                )!!
            )
        )
        assertEquals("main..feature/some-ve...", BranchScopePresentation.activeValueText(scope))
        assertEquals("Branch: main..feature/some-ve...", BranchScopePresentation.toolbarText(scope))
        assertEquals(
            "Custom revision range: main..feature/some-very-long-branch-name",
            BranchScopePresentation.tooltip(scope, BranchInfo())
        )
    }

    fun testBranchScopeSelectPopupUsesTokenNearCaret() {
        val text = "dev | origin/feature/husky"
        val devCaret = text.indexOf("dev") + 2
        val originCaret = text.indexOf("feature") + 3

        assertEquals("dev", BranchScopeSelectPopup.activeToken(text, devCaret))
        assertEquals("origin/feature/husky", BranchScopeSelectPopup.activeToken(text, originCaret))
        assertEquals("", BranchScopeSelectPopup.activeToken("dev\n", "dev\n".length))

        val replacement = BranchScopeSelectPopup.replaceActiveToken(text, devCaret, "develop")

        assertEquals("develop | origin/feature/husky", replacement.text)
        assertEquals("develop".length, replacement.caretPosition)

        val enterReplacement = BranchScopeSelectPopup.replaceActiveTokenAndAppendNewLine("ma", 2, "main")

        assertEquals("main\n", enterReplacement.text)
        assertEquals("main\n".length, enterReplacement.caretPosition)

        val middleEnterReplacement = BranchScopeSelectPopup.replaceActiveTokenAndAppendNewLine(text, devCaret, "develop")

        assertEquals("develop\norigin/feature/husky", middleEnterReplacement.text)
        assertEquals("develop\n".length, middleEnterReplacement.caretPosition)

        val blankLineReplacement = BranchScopeSelectPopup.insertNewLine("dev\n", "dev\n".length)

        assertEquals("dev\n\n", blankLineReplacement.text)
        assertEquals("dev\n\n".length, blankLineReplacement.caretPosition)
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

    fun testRefreshStatsActionCanEnterLoadingStateDuringInitialization() = runOnEdt {
        var refreshRequests = 0
        val action = RefreshStatsAction {
            refreshRequests++
        }

        action.requestRefresh()

        assertEquals(1, refreshRequests)

        action.startLoading()

        assertTrue(action.isLoading)

        action.requestRefresh()

        assertEquals(1, refreshRequests)

        action.stopLoading()

        assertFalse(action.isLoading)
    }

    private fun runOnEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait(action)
        }
    }

    private fun GitLogFilterChip.firstFilterLabelY(text: String): Int {
        return firstFilterLabel(text).y
    }

    private fun GitLogFilterChip.firstFilterLabel(text: String): JLabel {
        return components
            .filterIsInstance<JLabel>()
            .first { it.text?.startsWith(text) == true }
    }

    private fun dispatchMouseEvent(label: JLabel, id: Int) {
        val event = MouseEvent(label, id, System.currentTimeMillis(), 0, 1, 1, 1, false, MouseEvent.BUTTON1)
        label.mouseListeners.forEach { listener ->
            when (id) {
                MouseEvent.MOUSE_ENTERED -> listener.mouseEntered(event)
                MouseEvent.MOUSE_CLICKED -> listener.mouseClicked(event)
            }
        }
    }

    private fun GitLogFilterChip.isHovered(): Boolean {
        val field = GitLogFilterChip::class.java.getDeclaredField("hovered")
        field.isAccessible = true
        return field.getBoolean(this)
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
