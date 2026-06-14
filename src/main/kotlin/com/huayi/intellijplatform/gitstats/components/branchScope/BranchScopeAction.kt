package com.huayi.intellijplatform.gitstats.components.branchScope

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.components.filters.GitLogFilterChip
import com.huayi.intellijplatform.gitstats.models.BranchInfo
import com.huayi.intellijplatform.gitstats.models.BranchScope
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import javax.swing.JComponent

class BranchScopeAction(
    private val project: Project,
    initialScope: BranchScope = BranchScope.CurrentBranch,
    private val onScopeChanged: (BranchScope) -> Unit
) : DumbAwareAction(), CustomComponentAction {
    private var branchInfo = BranchInfo()
    private var selectedScope: BranchScope = initialScope
    private val components = mutableSetOf<GitLogFilterChip>()
    private var currentPopup: JBPopup? = null

    init {
        updateTemplatePresentation()
    }

    fun setBranchInfo(value: BranchInfo) {
        branchInfo = value
        updateTemplatePresentation()
        refreshComponents()
    }

    fun getSelectedScope(): BranchScope = selectedScope

    fun getScopeTooltip(): String = BranchScopePresentation.tooltip(selectedScope, branchInfo)

    override fun update(e: AnActionEvent) {
        e.presentation.text = BranchScopePresentation.toolbarText(selectedScope)
        e.presentation.description = BranchScopePresentation.tooltip(selectedScope, branchInfo)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        return GitLogFilterChip(
            onOpenPopup = ::showPopup,
            onClear = ::resetScope
        ).also {
            components.add(it)
            it.update(createChipModel())
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val component = e.inputEvent?.component as? JComponent ?: components.firstOrNull() ?: return
        showPopup(component)
    }

    fun refreshPresentation(presentation: Presentation) {
        presentation.text = BranchScopePresentation.toolbarText(selectedScope)
        presentation.description = BranchScopePresentation.tooltip(selectedScope, branchInfo)
    }

    fun resetScope() {
        applyScope(BranchScope.CurrentBranch)
    }

    private fun showPopup(component: JComponent) {
        currentPopup?.cancel()
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            null,
            BranchScopePopupActionGroupFactory(
                branchInfo = branchInfo,
                selectedScope = { selectedScope },
                applyScope = ::applyScope,
                requestBranchSelection = { showBranchSelectionPopup(component) },
                requestCustomRange = ::showCustomRangeDialog
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

    private fun showBranchSelectionPopup(component: JComponent) {
        closePopup()
        currentPopup = BranchScopeSelectPopup.show(
            anchor = component,
            branchInfo = branchInfo,
            selectedScope = selectedScope,
            applyScope = ::applyScope
        ) {
            currentPopup = null
        }
    }

    private fun applyScope(scope: BranchScope) {
        selectedScope = scope
        updateTemplatePresentation()
        refreshComponents()
        onScopeChanged(scope)
        closePopup()
    }

    private fun updateTemplatePresentation() {
        templatePresentation.text = BranchScopePresentation.toolbarText(selectedScope)
        templatePresentation.description = BranchScopePresentation.tooltip(selectedScope, branchInfo)
    }

    private fun refreshComponents() {
        components.removeIf { it.parent == null }
        components.forEach { it.update(createChipModel()) }
    }

    private fun closePopup() {
        currentPopup?.closeOk(null)
        currentPopup = null
    }

    private fun createChipModel(): GitLogFilterChip.Model {
        val active = BranchScopePresentation.isActive(selectedScope)
        return GitLogFilterChip.Model(
            label = BranchScopePresentation.buttonText(),
            value = if (active) BranchScopePresentation.activeValueText(selectedScope) else null,
            tooltip = BranchScopePresentation.tooltip(selectedScope, branchInfo),
            clearTooltip = MyBundle.message("branchScopeClearTooltip")
        )
    }

    private fun showCustomRangeDialog() {
        closePopup()
        BranchScopeCustomRangeDialog.show(project, selectedScope)?.let { scope ->
            applyScope(scope)
        }
    }
}
