package com.huayi.intellijplatform.gitstats.components.filters

import com.huayi.intellijplatform.gitstats.MyBundle
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Paths
import javax.swing.JComponent

class PathFilterAction(
    private val project: Project,
    private val onPathsChanged: (List<String>) -> Unit
) : DumbAwareAction(), CustomComponentAction {
    private val components = mutableSetOf<GitLogFilterChip>()
    private var currentPaths = emptyList<String>()
    private var currentPopup: JBPopup? = null

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.text = toolbarText()
        e.presentation.description = tooltip()
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        return GitLogFilterChip(
            onOpenPopup = ::showPopup,
            onClear = { setPaths(emptyList()) }
        ).also {
            components.add(it)
            it.update(createChipModel())
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val component = e.inputEvent?.component as? JComponent ?: components.firstOrNull() ?: return
        showPopup(component)
    }

    private fun showPopup(component: JComponent) {
        currentPopup?.cancel()
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            null,
            PathFilterPopupActionGroupFactory(
                requestPathSelection = { showPathSelectionPopup(component) },
                requestTreeSelection = { showPathTreeChooser(component) },
                isTreeSelectionAvailable = { baseDirectory() != null }
            ).create(),
            DataManager.getInstance().getDataContext(component),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true,
            {
                currentPopup = null
            },
            -1
        )
        currentPopup = popup
        popup.showUnderneathOf(component)
    }

    private fun showPathSelectionPopup(component: JComponent) {
        closePopup()
        currentPopup = PathSelectPopup.show(
            anchor = component,
            currentPaths = currentPaths.joinToString("\n"),
            applyPaths = { setPaths(PathFilterPaths.parse(it)) }
        ) {
            currentPopup = null
        }
    }

    private fun showPathTreeChooser(component: JComponent) {
        closePopup()
        val baseDirectory = baseDirectory() ?: return
        val descriptor = FileChooserDescriptor(
            true,
            true,
            false,
            false,
            false,
            true
        ).withTitle(MyBundle.message("filterPathsTreeDialogTitle"))
            .withDescription(MyBundle.message("filterPathsTreeDialogDescription"))
            .withRoots(baseDirectory)
            .withTreeRootVisible(true)
            .withShowHiddenFiles(true)
            .withHideIgnored(false)
        descriptor.setForcedToUseIdeaFileChooser(true)

        val selectedFiles = FileChooser.chooseFiles(descriptor, component, project, baseDirectory)
        val selectedPaths = selectedFiles.mapNotNull { projectRelativePath(baseDirectory, it) }
        if (selectedPaths.isNotEmpty()) {
            setPaths(selectedPaths)
        }
    }

    private fun setPaths(paths: List<String>) {
        currentPaths = PathFilterPaths.normalize(paths)
        refreshComponents()
        onPathsChanged(currentPaths)
        closePopup()
    }

    private fun refreshComponents() {
        components.removeIf { it.parent == null }
        components.forEach { it.update(createChipModel()) }
    }

    private fun createChipModel(): GitLogFilterChip.Model {
        val value = currentPaths.takeIf { it.isNotEmpty() }?.let(::shorten)
        return GitLogFilterChip.Model(
            label = MyBundle.message("filterPathsButtonLabel"),
            value = value,
            tooltip = tooltip(),
            clearTooltip = MyBundle.message("filterPathsClearTooltip")
        )
    }

    private fun toolbarText(): String {
        return currentPaths.takeIf { it.isNotEmpty() }
            ?.let { "${MyBundle.message("filterPathsButtonLabel")}: ${shorten(it)}" }
            ?: MyBundle.message("filterPathsButtonLabel")
    }

    private fun tooltip(): String {
        return currentPaths.takeIf { it.isNotEmpty() }
            ?.let { MyBundle.message("filterPathsTooltip", displayText(it, Int.MAX_VALUE)) }
            ?: MyBundle.message("filterPathsEmptyTooltip")
    }

    private fun shorten(paths: List<String>, maxLength: Int = 24): String {
        return displayText(paths, maxLength)
    }

    private fun displayText(paths: List<String>, maxLength: Int): String {
        val normalized = paths.joinToString(" | ")
        return if (normalized.length <= maxLength) normalized else normalized.take(maxLength - 3) + "..."
    }

    private fun closePopup() {
        currentPopup?.closeOk(null)
        currentPopup = null
    }

    private fun baseDirectory(): VirtualFile? {
        return project.basePath?.let { LocalFileSystem.getInstance().refreshAndFindFileByPath(it) }
    }

    private fun projectRelativePath(baseDirectory: VirtualFile, selectedFile: VirtualFile): String? {
        val basePath = Paths.get(baseDirectory.path).toAbsolutePath().normalize()
        val selectedPath = Paths.get(selectedFile.path).toAbsolutePath().normalize()
        if (!selectedPath.startsWith(basePath)) {
            return null
        }
        return basePath.relativize(selectedPath)
            .toString()
            .replace('\\', '/')
            .ifBlank { "." }
    }
}
