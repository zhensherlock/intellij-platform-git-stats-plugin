package com.huayi.intellijplatform.gitstats.components

import com.huayi.intellijplatform.gitstats.MyBundle
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.BoxLayout
import javax.swing.JComponent


class SettingDialogWrapper : DialogWrapper(true) {
    init {
        title = "Git Stats Setting"
        init()
    }
    override fun createCenterPanel(): JComponent {
        val dialogPanel = JBPanel<JBPanel<*>>().apply {
            layout = GridLayout(1, 1)
            preferredSize = Dimension(260, 30)
        }
        val modeFieldPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(JBLabel(MyBundle.message("settingDialogModeLabel", "")).apply {
                preferredSize = Dimension(50, 30)
            })
            add(ComboBox<String>().apply {
                addItem("Top-speed")
                addItem("Advanced")
            })
        }
        dialogPanel.add(modeFieldPanel)
        return dialogPanel
    }
}