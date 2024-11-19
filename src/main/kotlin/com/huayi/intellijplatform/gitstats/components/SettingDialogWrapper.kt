package com.huayi.intellijplatform.gitstats.components

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.models.SettingModel
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.BoxLayout
import javax.swing.JComponent


class SettingDialogWrapper(defaultSettingModel: SettingModel) : DialogWrapper(true) {
    var settingModel: SettingModel = defaultSettingModel
    private lateinit var modeComboBox: ComboBox<String>
    private lateinit var excludeField: JBTextField
    init {
        title = "Git Stats Setting"
        init()
    }
    override fun createCenterPanel(): JComponent {
        val dialogPanel = JBPanel<JBPanel<*>>().apply {
            layout = GridLayout(2, 1, 0, 5)
            preferredSize = Dimension(260, 60)
        }
        val modeFieldPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(JBLabel(MyBundle.message("settingDialogModeLabel", "")).apply {
                preferredSize = Dimension(60, 30)
            })
            modeComboBox = ComboBox<String>().apply {
                addItem("Top-speed")
                addItem("Advanced")
                selectedItem = settingModel.mode
            }
            add(modeComboBox)
        }
        dialogPanel.add(modeFieldPanel)

        val excludeFieldPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(JBLabel(MyBundle.message("settingDialogExcludeLabel", "")).apply {
                preferredSize = Dimension(60, 30)
            })
            excludeField = JBTextField().apply {
                text = settingModel.exclude
            }
            add(excludeField)
        }
        dialogPanel.add(excludeFieldPanel)

        return dialogPanel
    }

    override fun doOKAction() {
        settingModel.mode = modeComboBox.selectedItem as String
        settingModel.exclude = excludeField.text
        super.doOKAction()
    }
}