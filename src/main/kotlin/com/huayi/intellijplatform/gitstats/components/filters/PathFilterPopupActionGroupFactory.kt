package com.huayi.intellijplatform.gitstats.components.filters

import com.huayi.intellijplatform.gitstats.MyBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction

internal class PathFilterPopupActionGroupFactory(
    private val requestPathSelection: () -> Unit,
    private val requestTreeSelection: () -> Unit,
    private val isTreeSelectionAvailable: () -> Boolean
) {
    fun create(): DefaultActionGroup {
        return DefaultActionGroup().apply {
            add(SelectPathAction())
            add(SelectInTreeAction())
        }
    }

    private inner class SelectPathAction : DumbAwareAction(MyBundle.message("filterPathsSelectAction")) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) {
            requestPathSelection()
        }
    }

    private inner class SelectInTreeAction : DumbAwareAction(MyBundle.message("filterPathsSelectInTreeAction")) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = isTreeSelectionAvailable()
        }

        override fun actionPerformed(e: AnActionEvent) {
            requestTreeSelection()
        }
    }
}
