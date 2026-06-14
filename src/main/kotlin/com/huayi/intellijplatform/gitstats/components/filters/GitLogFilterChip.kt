package com.huayi.intellijplatform.gitstats.components.filters

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.ui.components.JBBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Container
import java.awt.Cursor
import java.awt.Dimension
import java.awt.LayoutManager
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

internal class GitLogFilterChip(
    private val onOpenPopup: (JComponent) -> Unit,
    private val onClear: () -> Unit
) : JBPanel<GitLogFilterChip>(CenteredRowLayout { JBUI.scale(CHIP_HEIGHT) }) {
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
                        setHovered(false)
                        icon = AllIcons.Actions.Close
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

private class CenteredRowLayout(
    private val preferredHeight: () -> Int
) : LayoutManager {
    override fun addLayoutComponent(name: String?, component: java.awt.Component?) = Unit

    override fun removeLayoutComponent(component: java.awt.Component?) = Unit

    override fun preferredLayoutSize(parent: Container): Dimension = layoutSize(parent, usePreferredSize = true)

    override fun minimumLayoutSize(parent: Container): Dimension = layoutSize(parent, usePreferredSize = false)

    override fun layoutContainer(parent: Container) {
        synchronized(parent.treeLock) {
            val insets = parent.insets
            val contentHeight = parent.height - insets.top - insets.bottom
            var x = insets.left
            parent.components
                .filter { it.isVisible }
                .forEach { component ->
                    val size = component.preferredSize
                    val y = insets.top + ((contentHeight - size.height) / 2).coerceAtLeast(0)
                    component.setBounds(x, y, size.width, size.height)
                    x += size.width
                }
        }
    }

    private fun layoutSize(parent: Container, usePreferredSize: Boolean): Dimension {
        synchronized(parent.treeLock) {
            val insets = parent.insets
            val width = parent.components
                .filter { it.isVisible }
                .sumOf { component ->
                    if (usePreferredSize) component.preferredSize.width else component.minimumSize.width
                }
            return Dimension(insets.left + width + insets.right, preferredHeight())
        }
    }
}
