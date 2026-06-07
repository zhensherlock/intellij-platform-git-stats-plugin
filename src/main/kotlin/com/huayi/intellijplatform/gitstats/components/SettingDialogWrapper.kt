package com.huayi.intellijplatform.gitstats.components

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.models.SettingModel
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent


class SettingDialogWrapper(defaultSettingModel: SettingModel) : DialogWrapper(true) {
    var settingModel: SettingModel = defaultSettingModel
    private lateinit var modeComboBox: ComboBox<String>
    private lateinit var excludeField: JBTextField
    init {
        title = "Git Stats Settings"
        init()
    }
    override fun createCenterPanel(): JComponent {
        val dialogPanel = JBPanel<JBPanel<*>>(GridBagLayout()).apply {
            preferredSize = Dimension(380, 72)
        }

        modeComboBox = ComboBox<String>().apply {
            addItem(SettingModel.MODE_FAST_SUMMARY)
            addItem(SettingModel.MODE_DETAILED)
            selectedItem = settingModel.mode
        }
        excludeField = JBTextField().apply {
            text = settingModel.exclude
        }

        dialogPanel.add(JBLabel(MyBundle.message("settingDialogModeLabel", "")), labelConstraints(0))
        dialogPanel.add(modeComboBox, fieldConstraints(0))
        dialogPanel.add(JBLabel(MyBundle.message("settingDialogExcludeLabel", "")), labelConstraints(1))
        dialogPanel.add(excludeField, fieldConstraints(1))

        return dialogPanel
    }

    private fun labelConstraints(row: Int) = GridBagConstraints().apply {
        gridx = 0
        gridy = row
        anchor = GridBagConstraints.LINE_END
        insets = Insets(0, 0, rowBottomInset(row), 8)
    }

    private fun fieldConstraints(row: Int) = GridBagConstraints().apply {
        gridx = 1
        gridy = row
        weightx = 1.0
        fill = GridBagConstraints.HORIZONTAL
        insets = Insets(0, 0, rowBottomInset(row), 0)
    }

    private fun rowBottomInset(row: Int) = if (row == 0) 8 else 0

    override fun doOKAction() {
        settingModel.mode = modeComboBox.selectedItem as String
        settingModel.exclude = excludeField.text
        super.doOKAction()
    }
}
