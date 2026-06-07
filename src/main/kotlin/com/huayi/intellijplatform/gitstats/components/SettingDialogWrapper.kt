package com.huayi.intellijplatform.gitstats.components

import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.models.SettingModel
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent


class SettingDialogWrapper(defaultSettingModel: SettingModel) : DialogWrapper(true) {
    var settingModel: SettingModel = defaultSettingModel
    private lateinit var modeComboBox: ComboBox<String>
    private lateinit var excludePathsPanel: JBPanel<JBPanel<*>>
    private val excludePathFields = mutableListOf<JBTextField>()

    init {
        title = "Git Stats Settings"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val dialogPanel = JBPanel<JBPanel<*>>(GridBagLayout()).apply {
            preferredSize = Dimension(460, 235)
        }

        modeComboBox = ComboBox<String>().apply {
            addItem(SettingModel.MODE_FAST_SUMMARY)
            addItem(SettingModel.MODE_DETAILED)
            selectedItem = settingModel.mode
        }

        excludePathFields.clear()
        excludePathsPanel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(4, 4, 0, 4)
        }
        settingModel.excludePaths().forEach { addExcludePathRow(it) }

        dialogPanel.add(JBLabel(MyBundle.message("settingDialogModeLabel", "")), labelConstraints(0))
        dialogPanel.add(modeComboBox, fieldConstraints(0))
        dialogPanel.add(JBLabel(MyBundle.message("settingDialogExcludeLabel", "")), labelConstraints(1))
        dialogPanel.add(createExcludePathsPanel(), fieldConstraints(1))

        return dialogPanel
    }

    private fun createExcludePathsPanel(): JComponent {
        val scrollPane = JBScrollPane(excludePathsPanel).apply {
            preferredSize = Dimension(320, 120)
            border = BorderFactory.createEtchedBorder()
        }
        val helpLabel = JBLabel(MyBundle.message("settingDialogExcludeHelp", "")).apply {
            foreground = UIUtil.getContextHelpForeground()
        }
        val addButton = JButton(MyBundle.message("settingDialogAddPathButton", ""), AllIcons.General.Add).apply {
            toolTipText = MyBundle.message("settingDialogAddPathTooltip", "")
            addActionListener {
                val field = addExcludePathRow()
                excludePathsPanel.revalidate()
                excludePathsPanel.repaint()
                field.requestFocusInWindow()
            }
        }

        return JBPanel<JBPanel<*>>(BorderLayout(0, 6)).apply {
            add(scrollPane, BorderLayout.CENTER)
            add(
                JBPanel<JBPanel<*>>(BorderLayout()).apply {
                    add(addButton, BorderLayout.WEST)
                    add(helpLabel, BorderLayout.SOUTH)
                },
                BorderLayout.SOUTH
            )
        }
    }

    private fun addExcludePathRow(path: String = ""): JBTextField {
        val field = JBTextField(path).apply {
            emptyText.text = MyBundle.message("settingDialogExcludePathPlaceholder", "")
        }
        val row = JBPanel<JBPanel<*>>(GridBagLayout()).apply {
            maximumSize = Dimension(Int.MAX_VALUE, 32)
            border = BorderFactory.createEmptyBorder(0, 0, 4, 0)
        }
        val removeButton = JButton(AllIcons.General.Remove).apply {
            toolTipText = MyBundle.message("settingDialogRemovePathTooltip", "")
            preferredSize = Dimension(28, 28)
            minimumSize = preferredSize
            maximumSize = preferredSize
            isFocusable = false
            isContentAreaFilled = false
            isBorderPainted = false
            addActionListener {
                excludePathFields.remove(field)
                excludePathsPanel.remove(row)
                excludePathsPanel.revalidate()
                excludePathsPanel.repaint()
            }
        }

        row.add(field, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(0, 0, 0, 6)
        })
        row.add(removeButton, GridBagConstraints().apply {
            gridx = 1
            gridy = 0
        })
        excludePathFields.add(field)
        excludePathsPanel.add(row)
        return field
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
        settingModel.exclude = excludePathFields
            .map { it.text.trim().replace('\\', '/') }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("\n")
        super.doOKAction()
    }
}
