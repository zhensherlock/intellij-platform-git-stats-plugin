package com.huayi.intellijplatform.gitstats.components.filters

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.ui.components.JBBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

internal class GitLogFilterChip(
    private val onOpenPopup: (JComponent) -> Unit,
    private val onClear: () -> Unit
) : JBPanel<GitLogFilterChip>(FlowLayout(FlowLayout.LEFT, 0, 0)) {
    private var hovered = false
    private var label: JBLabel? = null
    private var valueLabel: JBLabel? = null
    private val openMouseListener = object : MouseAdapter() {
        override fun mouseEntered(e: MouseEvent) {
            setHovered(true)
        }

        override fun mouseExited(e: MouseEvent) {
            updateHoverFromExit(e)
        }

        override fun mouseClicked(e: MouseEvent) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                e.consume()
                onOpenPopup(this@GitLogFilterChip)
            }
        }
    }

    init {
        isOpaque = false
        border = JBUI.Borders.empty(3, 6)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(openMouseListener)
    }

    fun update(model: Model) {
        removeAll()
        label = null
        valueLabel = null

        val filterLabel = JBLabel(if (model.value == null) model.label else "${model.label}: ")
        label = filterLabel
        add(filterLabel)
        installOpenHandler(filterLabel)

        if (model.value == null) {
            add(JBBox.createHorizontalStrut(4))
            val arrow = JBLabel(ComboBoxAction.getArrowIcon(true), SwingConstants.CENTER)
            add(arrow)
            installOpenHandler(arrow)
        } else {
            val value = JBLabel(model.value).apply {
                font = font.deriveFont(java.awt.Font.BOLD)
            }
            valueLabel = value
            add(value)
            installOpenHandler(value)
            add(JBBox.createHorizontalStrut(5))
            add(createClearLabel(model.clearTooltip))
        }

        toolTipText = model.tooltip
        updateTextColors()
        revalidate()
        repaint()
    }

    override fun getPreferredSize(): Dimension {
        val size = super.getPreferredSize()
        return Dimension(size.width, JBUI.scale(CHIP_HEIGHT))
    }

    override fun getMinimumSize(): Dimension {
        val size = super.getMinimumSize()
        return Dimension(size.width, JBUI.scale(CHIP_HEIGHT))
    }

    private fun createClearLabel(tooltip: String?): JBLabel {
        return JBLabel(AllIcons.Actions.Close, SwingConstants.CENTER).apply {
            toolTipText = tooltip
            border = JBUI.Borders.empty(1, 1)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    icon = AllIcons.Actions.CloseHovered
                    setHovered(true)
                }

                override fun mouseExited(e: MouseEvent) {
                    icon = AllIcons.Actions.Close
                    updateHoverFromExit(e)
                }

                override fun mouseClicked(e: MouseEvent) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        e.consume()
                        onClear()
                    }
                }
            })
        }
    }

    private fun installOpenHandler(component: JComponent) {
        component.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        component.addMouseListener(openMouseListener)
    }

    private fun setHovered(value: Boolean) {
        if (hovered != value) {
            hovered = value
            updateTextColors()
            repaint()
        }
    }

    private fun updateTextColors() {
        val hoverColor = UIUtil.getActiveTextColor()
        label?.foreground = if (hovered) hoverColor else UIUtil.getContextHelpForeground()
        valueLabel?.foreground = if (hovered) hoverColor else UIUtil.getLabelForeground()
    }

    private fun updateHoverFromExit(e: MouseEvent) {
        val point = SwingUtilities.convertPoint(e.component, e.point, this)
        setHovered(contains(point))
    }

    data class Model(
        val label: String,
        val value: String?,
        val tooltip: String?,
        val clearTooltip: String?
    )

    private companion object {
        private const val CHIP_HEIGHT = 28
    }
}
