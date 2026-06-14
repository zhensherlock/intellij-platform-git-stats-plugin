package com.huayi.intellijplatform.gitstats.components.filters

import com.huayi.intellijplatform.gitstats.MyBundle
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.KeyStroke

internal object PathSelectPopup {
    fun show(
        anchor: JComponent,
        currentPaths: String,
        applyPaths: (String) -> Unit,
        onClosed: () -> Unit
    ): JBPopup {
        var popup: JBPopup? = null
        val input = JBTextArea(currentPaths, TEXT_ROWS, TEXT_COLUMNS).apply {
            lineWrap = false
            border = JBUI.Borders.empty(6, 6)
            focusTraversalKeysEnabled = false
        }

        fun applyText() {
            val normalizedPaths = PathFilterPaths.parse(input.text).joinToString("\n")
            applyPaths(normalizedPaths)
            popup?.closeOk(null)
        }

        installKeyboardActions(input, ::applyText)

        val content = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.empty()
            add(JBScrollPane(input).apply {
                preferredSize = Dimension(JBUI.scale(360), JBUI.scale(110))
            }, BorderLayout.CENTER)
            add(JBLabel(MyBundle.message("filterPathsSelectHint")).apply {
                border = JBUI.Borders.empty(8, 10)
                foreground = UIUtil.getContextHelpForeground()
            }, BorderLayout.SOUTH)
        }

        val createdPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(content, input)
            .setRequestFocus(true)
            .setFocusable(true)
            .setCancelOnClickOutside(true)
            .setMinSize(Dimension(JBUI.scale(360), JBUI.scale(150)))
            .createPopup()

        popup = createdPopup
        createdPopup.setFinalRunnable(onClosed)
        createdPopup.showUnderneathOf(anchor)
        input.requestFocusInWindow()
        input.selectAll()
        return createdPopup
    }

    private fun installKeyboardActions(input: JBTextArea, applyText: () -> Unit) {
        input.inputMap.put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.META_DOWN_MASK),
            "gitStatsApplyPathSelection"
        )
        input.inputMap.put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
            "gitStatsApplyPathSelection"
        )
        input.actionMap.put("gitStatsApplyPathSelection", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                applyText()
            }
        })
    }

    private const val TEXT_ROWS = 4
    private const val TEXT_COLUMNS = 34
}
