package com.huayi.intellijplatform.gitstats.components.branchScope

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.models.BranchScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

object BranchScopeCustomRangeDialog {
    fun show(project: Project?, currentScope: BranchScope): BranchScope.CustomRevisionRange? {
        val initialValue = (currentScope as? BranchScope.CustomRevisionRange)?.revisionRange.orEmpty()
        val value = Messages.showInputDialog(
            project,
            MyBundle.message("branchScopeCustomDialogMessage"),
            MyBundle.message("branchScopeCustomDialogTitle"),
            null,
            initialValue,
            null
        ) ?: return null

        val scope = BranchScope.customRevisionRange(value)
        if (scope == null) {
            Messages.showErrorDialog(
                project,
                MyBundle.message("branchScopeInvalidCustomRange"),
                MyBundle.message("branchScopeCustomDialogTitle")
            )
        }
        return scope
    }
}
