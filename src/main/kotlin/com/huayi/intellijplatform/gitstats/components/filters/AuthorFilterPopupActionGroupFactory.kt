package com.huayi.intellijplatform.gitstats.components.filters

import com.huayi.intellijplatform.gitstats.MyBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction

internal class AuthorFilterPopupActionGroupFactory(
    private val authors: List<String>,
    private val applyAuthor: (String) -> Unit,
    private val requestAuthorSelection: () -> Unit
) {
    fun create(): DefaultActionGroup {
        return DefaultActionGroup().apply {
            add(SelectAuthorAction())
            addSeparator()
            if (authors.isEmpty()) {
                add(DisabledTextAction(MyBundle.message("filterAuthorNoUsers")))
            } else {
                authors.forEach { author ->
                    add(AuthorAction(author))
                }
            }
        }
    }

    private inner class SelectAuthorAction : DumbAwareAction(MyBundle.message("filterAuthorSelectAction")) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = authors.isNotEmpty()
        }

        override fun actionPerformed(e: AnActionEvent) {
            requestAuthorSelection()
        }
    }

    private inner class AuthorAction(
        private val author: String
    ) : DumbAwareAction(author) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) {
            applyAuthor(author)
        }
    }

    private class DisabledTextAction(text: String) : DumbAwareAction(text) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = false
        }

        override fun actionPerformed(e: AnActionEvent) = Unit
    }
}
