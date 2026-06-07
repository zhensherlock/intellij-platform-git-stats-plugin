package com.huayi.intellijplatform.gitstats.components

import com.intellij.icons.AllIcons
import javax.swing.JButton


class RefreshButton(
    private val refreshLabel: String,
    private val loadingLabel: String,
) : JButton(refreshLabel, REFRESH_ICON) {
    private var isLoading = false

    fun startLoading() {
        if (isLoading) return

        isLoading = true
        text = loadingLabel
        icon = LOADING_ICON
        disabledIcon = LOADING_ICON
        isEnabled = false
        accessibleContext?.accessibleName = loadingLabel
    }

    fun stopLoading() {
        isLoading = false
        isEnabled = true
        text = refreshLabel
        icon = REFRESH_ICON
        disabledIcon = null
        accessibleContext?.accessibleName = refreshLabel
    }

    private companion object {
        private val REFRESH_ICON = AllIcons.Actions.Refresh
        private val LOADING_ICON = AllIcons.Process.Step_1
    }
}
