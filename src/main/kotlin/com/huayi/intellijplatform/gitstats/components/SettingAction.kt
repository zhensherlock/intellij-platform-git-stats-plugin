package com.huayi.intellijplatform.gitstats.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.jetbrains.annotations.Nls
import java.util.function.Supplier

class SettingAction(text: @Nls String, defaultMode: String, private val onSelectedModeChanged: (String) -> Unit) :
    DumbAwareAction(Supplier { text }, AllIcons.General.Settings) {
    private var selectedMode: String = defaultMode
        set(value) {
            field = value
            onSelectedModeChanged.invoke(value)
        }

    override fun actionPerformed(e: AnActionEvent) {
        val dialogWrapper = SettingDialogWrapper(selectedMode)
        dialogWrapper.showAndGet()
        if (dialogWrapper.isOK) {
            selectedMode = dialogWrapper.selectedMode
        }
    }
}