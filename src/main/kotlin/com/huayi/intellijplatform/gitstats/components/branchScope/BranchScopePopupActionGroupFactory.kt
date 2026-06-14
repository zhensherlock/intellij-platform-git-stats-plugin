package com.huayi.intellijplatform.gitstats.components.branchScope

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.models.BranchInfo
import com.huayi.intellijplatform.gitstats.models.BranchRef
import com.huayi.intellijplatform.gitstats.models.BranchScope
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction

internal class BranchScopePopupActionGroupFactory(
    private val branchInfo: BranchInfo,
    private val selectedScope: () -> BranchScope,
    private val applyScope: (BranchScope) -> Unit,
    private val requestBranchSelection: () -> Unit,
    private val requestCustomRange: () -> Unit
) {
    fun create(): DefaultActionGroup {
        return DefaultActionGroup().apply {
            add(SelectBranchAction())
            add(createRecentBranchesGroup())
            addSeparator()
            add(ScopeAction(MyBundle.message("branchScopeHead"), BranchScope.Head))
            add(ScopeAction(MyBundle.message("branchScopeAllLocal"), BranchScope.AllLocalBranches))
            addSeparator()
            addRemoteBranchGroups()
            add(createLocalBranchesGroup())
            addSeparator()
            add(CustomRangeAction())
        }
    }

    private fun createRecentBranchesGroup(): DefaultActionGroup {
        val recentBranches = listOfNotNull(
            (selectedScope() as? BranchScope.SelectedBranch)?.branchName,
            branchInfo.currentBranch
        ).distinct()

        return DefaultActionGroup(MyBundle.message("branchScopeRecent"), true).apply {
            if (recentBranches.isEmpty()) {
                add(DisabledTextAction(MyBundle.message("branchScopeNoBranches")))
            } else {
                recentBranches.forEach { branchName ->
                    BranchScope.selectedBranch(branchRefForName(branchName))?.let { scope ->
                        add(ScopeAction(branchName, scope))
                    }
                }
            }
        }
    }

    private fun DefaultActionGroup.addRemoteBranchGroups() {
        branchInfo.remoteBranches
            .groupBy { remoteName(it) }
            .toSortedMap()
            .forEach { (remoteName, branches) ->
                add(createRemoteBranchesGroup(remoteName, branches))
            }
    }

    private fun createRemoteBranchesGroup(remoteName: String, branches: List<String>): DefaultActionGroup {
        return DefaultActionGroup("$remoteName/...", true).apply {
            branches.sorted().forEach { branchName ->
                BranchScope.selectedBranch(BranchRef.remote(branchName))?.let { scope ->
                    add(ScopeAction(branchName.removePrefix("$remoteName/"), scope))
                }
            }
        }
    }

    private fun createLocalBranchesGroup(): DefaultActionGroup {
        return DefaultActionGroup(MyBundle.message("branchScopeLocalBranches"), true).apply {
            if (branchInfo.localBranches.isEmpty()) {
                add(DisabledTextAction(MyBundle.message("branchScopeNoBranches")))
            } else {
                branchInfo.localBranches.forEach { branchName ->
                    BranchScope.selectedBranch(BranchRef.local(branchName))?.let { scope ->
                        add(ScopeAction(branchName, scope))
                    }
                }
            }
        }
    }

    private inner class ScopeAction(
        text: String,
        private val scope: BranchScope
    ) : DumbAwareAction(text) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) {
            applyScope(scope)
        }
    }

    private inner class SelectBranchAction : DumbAwareAction(MyBundle.message("branchScopeSelectAction")) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = branchInfo.selectableBranches.isNotEmpty()
        }

        override fun actionPerformed(e: AnActionEvent) {
            requestBranchSelection()
        }
    }

    private inner class CustomRangeAction : DumbAwareAction(MyBundle.message("branchScopeCustomAction")) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) {
            requestCustomRange()
        }
    }

    private class DisabledTextAction(text: String) : DumbAwareAction(text) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = false
        }

        override fun actionPerformed(e: AnActionEvent) = Unit
    }

    private companion object {
        private fun remoteName(branchName: String): String {
            return branchName.substringBefore('/', branchName)
        }
    }

    private fun branchRefForName(branchName: String): BranchRef {
        return if (branchInfo.remoteBranches.contains(branchName)) {
            BranchRef.remote(branchName)
        } else {
            BranchRef.local(branchName)
        }
    }
}
