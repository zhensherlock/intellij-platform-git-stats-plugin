package com.huayi.intellijplatform.gitstats.components.filters

import com.huayi.intellijplatform.gitstats.MyBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.DumbAwareAction

class RefreshStatsAction(
    private val onRefreshRequested: (RefreshStatsAction) -> Unit
) : DumbAwareAction(
    MyBundle.message("refreshButtonLabel"),
    MyBundle.message("refreshButtonLabel"),
    REFRESH_ICON
) {
    private var loading = false

    val isLoading: Boolean
        get() = loading

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        requestRefresh()
    }

    override fun update(e: AnActionEvent) {
        applyState(e.presentation)
    }

    fun requestRefresh() {
        if (!loading) {
            onRefreshRequested(this)
        }
    }

    fun startLoading() {
        loading = true
    }

    fun stopLoading() {
        loading = false
    }

    private fun applyState(presentation: Presentation) {
        presentation.text = if (loading) {
            MyBundle.message("refreshButtonLoadingLabel")
        } else {
            MyBundle.message("refreshButtonLabel")
        }
        presentation.description = presentation.text
        presentation.icon = if (loading) LOADING_ICON else REFRESH_ICON
        presentation.isEnabled = !loading
    }

    private companion object {
        private val REFRESH_ICON = AllIcons.Actions.Refresh
        private val LOADING_ICON = AllIcons.Process.Step_1
    }
}
