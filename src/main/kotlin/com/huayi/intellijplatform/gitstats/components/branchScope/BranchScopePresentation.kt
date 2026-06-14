package com.huayi.intellijplatform.gitstats.components.branchScope

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.models.BranchInfo
import com.huayi.intellijplatform.gitstats.models.BranchScope

object BranchScopePresentation {
    fun buttonText(): String = MyBundle.message("filterScopeButtonLabel")

    fun isActive(scope: BranchScope): Boolean = scope != BranchScope.CurrentBranch

    fun toolbarText(scope: BranchScope): String {
        return if (isActive(scope)) {
            "${buttonText()}: ${activeValueText(scope)}"
        } else {
            buttonText()
        }
    }

    fun activeValueText(scope: BranchScope): String = shorten(text(scope))

    fun text(scope: BranchScope): String {
        return when (scope) {
            BranchScope.CurrentBranch -> MyBundle.message("branchScopeCurrent")
            BranchScope.Head -> MyBundle.message("branchScopeHead")
            BranchScope.AllLocalBranches -> MyBundle.message("branchScopeAllLocal")
            is BranchScope.SelectedBranch -> scope.branchName
            is BranchScope.SelectedBranches -> scope.branches.joinToString(BRANCH_JOINER) { it.branchName }
            is BranchScope.CustomRevisionRange -> scope.revisionRange
        }
    }

    fun tooltip(scope: BranchScope, branchInfo: BranchInfo): String {
        return when (scope) {
            BranchScope.CurrentBranch -> branchInfo.currentBranch
                ?.let { MyBundle.message("branchScopeCurrentTooltip", it) }
                ?: MyBundle.message("branchScopeCurrent")

            BranchScope.Head -> MyBundle.message("branchScopeHeadTooltip")
            BranchScope.AllLocalBranches -> MyBundle.message("branchScopeAllLocal")
            is BranchScope.SelectedBranch -> MyBundle.message("branchScopeSelectedTooltip", scope.branchName)
            is BranchScope.SelectedBranches -> MyBundle.message(
                "branchScopeSelectedBranchesTooltip",
                scope.branches.joinToString(BRANCH_JOINER) { it.branchName }
            )
            is BranchScope.CustomRevisionRange -> MyBundle.message("branchScopeCustomTooltip", scope.revisionRange)
        }
    }

    private fun shorten(text: String, maxLength: Int = 24): String {
        return if (text.length <= maxLength) text else text.take(maxLength - 3) + "..."
    }

    private const val BRANCH_JOINER = " | "
}
