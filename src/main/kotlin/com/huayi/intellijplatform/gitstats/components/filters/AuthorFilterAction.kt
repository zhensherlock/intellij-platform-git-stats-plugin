package com.huayi.intellijplatform.gitstats.components.filters

import com.huayi.intellijplatform.gitstats.MyBundle
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import javax.swing.JComponent

class AuthorFilterAction(
    private val onAuthorChanged: (String) -> Unit
) : DumbAwareAction(), CustomComponentAction {
    private val components = mutableSetOf<GitLogFilterChip>()
    private var availableAuthors = emptyList<String>()
    private var currentAuthor = ""
    private var currentPopup: JBPopup? = null

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.text = toolbarText()
        e.presentation.description = tooltip()
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        return GitLogFilterChip(
            onOpenPopup = ::showPopup,
            onClear = { setAuthor("") }
        ).also {
            components.add(it)
            it.update(createChipModel())
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val component = e.inputEvent?.component as? JComponent ?: components.firstOrNull() ?: return
        showPopup(component)
    }

    fun setAvailableAuthors(authors: List<String>) {
        availableAuthors = authors
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    private fun showPopup(component: JComponent) {
        currentPopup?.cancel()
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            null,
            AuthorFilterPopupActionGroupFactory(
                authors = availableAuthors,
                applyAuthor = ::setAuthor,
                requestAuthorSelection = { showAuthorSelectionPopup(component) }
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

    private fun showAuthorSelectionPopup(component: JComponent) {
        closePopup()
        currentPopup = AuthorSelectPopup.show(
            anchor = component,
            authors = availableAuthors,
            currentAuthor = currentAuthor,
            applyAuthor = ::setAuthor
        ) {
            currentPopup = null
        }
    }

    private fun setAuthor(author: String) {
        currentAuthor = author.trim()
        refreshComponents()
        onAuthorChanged(currentAuthor)
        closePopup()
    }

    private fun refreshComponents() {
        components.removeIf { it.parent == null }
        components.forEach { it.update(createChipModel()) }
    }

    private fun createChipModel(): GitLogFilterChip.Model {
        val value = currentAuthor.takeIf { it.isNotBlank() }?.let(::shorten)
        return GitLogFilterChip.Model(
            label = MyBundle.message("filterAuthorButtonLabel"),
            value = value,
            tooltip = tooltip(),
            clearTooltip = MyBundle.message("filterAuthorClearTooltip")
        )
    }

    private fun toolbarText(): String {
        return currentAuthor.takeIf { it.isNotBlank() }
            ?.let { "${MyBundle.message("filterAuthorButtonLabel")}: ${shorten(it)}" }
            ?: MyBundle.message("filterAuthorButtonLabel")
    }

    private fun tooltip(): String {
        return currentAuthor.takeIf { it.isNotBlank() }
            ?.let { MyBundle.message("filterAuthorTooltip", displayText(it, Int.MAX_VALUE)) }
            ?: MyBundle.message("filterAuthorButtonLabel")
    }

    private fun shorten(text: String, maxLength: Int = 24): String {
        return displayText(text, maxLength)
    }

    private fun displayText(text: String, maxLength: Int): String {
        val normalized = text
            .split('|', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" | ")
            .ifEmpty { text }
        return if (normalized.length <= maxLength) normalized else normalized.take(maxLength - 3) + "..."
    }

    private fun closePopup() {
        currentPopup?.closeOk(null)
        currentPopup = null
    }
}
