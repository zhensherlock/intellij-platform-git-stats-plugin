package com.huayi.intellijplatform.gitstats.components

import com.huayi.intellijplatform.gitstats.models.SettingModel
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.jetbrains.annotations.Nls
import java.util.function.Supplier

class SettingAction(text: @Nls String, defaultSettingModel: SettingModel, private val onSettingChanged: (SettingModel) -> Unit) :
    DumbAwareAction(Supplier { text }, AllIcons.General.Settings) {
    private var settingModel: SettingModel = defaultSettingModel
        set(value) {
            field = value
            onSettingChanged.invoke(value)
        }

    override fun actionPerformed(e: AnActionEvent) {
        val dialogWrapper = SettingDialogWrapper(settingModel)
        dialogWrapper.showAndGet()
        if (dialogWrapper.isOK) {
            settingModel = dialogWrapper.settingModel
        }
    }
}