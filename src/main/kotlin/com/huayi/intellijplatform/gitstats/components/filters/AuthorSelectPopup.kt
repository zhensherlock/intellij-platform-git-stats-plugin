package com.huayi.intellijplatform.gitstats.components.filters

import com.huayi.intellijplatform.gitstats.MyBundle
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.math.min

internal object AuthorSelectPopup {
    fun show(
        anchor: JComponent,
        authors: List<String>,
        currentAuthor: String,
        applyAuthor: (String) -> Unit,
        onClosed: () -> Unit
    ): JBPopup? {
        if (authors.isEmpty()) {
            return null
        }

        val input = JBTextArea(currentAuthor, TEXT_ROWS, TEXT_COLUMNS).apply {
            lineWrap = false
            border = JBUI.Borders.empty(6, 6)
            focusTraversalKeysEnabled = false
        }
        val listModel = DefaultListModel<String>()
        val suggestionList = JBList(listModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            visibleRowCount = MAX_VISIBLE_ROWS
            cellRenderer = AuthorRenderer { activeToken(input.text, input.caretPosition) }
            emptyText.text = MyBundle.message("filterAuthorNoUsers")
        }
        val suggestionScrollPane = JBScrollPane(suggestionList).apply {
            border = JBUI.Borders.emptyTop(1)
            preferredSize = Dimension(JBUI.scale(360), JBUI.scale(suggestionHeight(authors.size)))
        }
        var popup: JBPopup? = null

        fun refreshSuggestions() {
            val token = activeToken(input.text, input.caretPosition)
            if (token.isBlank()) {
                listModel.clear()
                suggestionList.clearSelection()
                setSuggestionMenuVisible(suggestionScrollPane, popup, false)
                input.putClientProperty("JComponent.outline", null)
                return
            }

            val matches = authors
                .filter { it.contains(token, ignoreCase = true) }
                .take(MAX_SUGGESTIONS)
            listModel.clear()
            matches.forEach { listModel.addElement(it) }
            if (!listModel.isEmpty) {
                suggestionList.selectedIndex = 0
            }
            setSuggestionMenuVisible(suggestionScrollPane, popup, true)
            input.putClientProperty("JComponent.outline", null)
        }

        fun applyText() {
            val resolvedAuthors = resolveAuthors(input.text, authors)
                ?: suggestionList.selectedValue?.let { selected ->
                    val replacement = replaceActiveToken(input.text, input.caretPosition, selected)
                    input.text = replacement.text
                    input.caretPosition = replacement.caretPosition.coerceIn(0, input.text.length)
                    resolveAuthors(replacement.text, authors)
                }
                ?: emptyList()
            if (resolvedAuthors.isNotEmpty()) {
                applyAuthor(resolvedAuthors.joinToString("\n"))
                popup?.closeOk(null)
            } else {
                input.putClientProperty("JComponent.outline", "error")
                input.requestFocusInWindow()
                input.repaint()
            }
        }

        fun completeFromSuggestion(appendNewLine: Boolean = false) {
            val selectedValue = suggestionList.selectedValue
            if (selectedValue == null) {
                if (appendNewLine) {
                    val replacement = insertNewLine(input.text, input.caretPosition)
                    input.text = replacement.text
                    input.caretPosition = replacement.caretPosition.coerceIn(0, input.text.length)
                    refreshSuggestions()
                }
                return
            }

            val replacement = if (appendNewLine) {
                replaceActiveTokenAndAppendNewLine(input.text, input.caretPosition, selectedValue)
            } else {
                replaceActiveToken(input.text, input.caretPosition, selectedValue)
            }
            input.text = replacement.text
            input.caretPosition = replacement.caretPosition.coerceIn(0, input.text.length)
            refreshSuggestions()
        }

        input.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = refreshSuggestions()
            override fun removeUpdate(e: DocumentEvent) = refreshSuggestions()
            override fun changedUpdate(e: DocumentEvent) = refreshSuggestions()
        })
        input.addCaretListener {
            refreshSuggestions()
        }
        suggestionList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount >= 2) {
                    completeFromSuggestion()
                    applyText()
                } else {
                    completeFromSuggestion()
                }
            }
        })
        installKeyboardActions(
            input = input,
            suggestionList = suggestionList,
            completeFromSuggestion = { completeFromSuggestion(false) },
            completeFromSuggestionAndNewLine = { completeFromSuggestion(true) },
            applyText = ::applyText
        )

        val content = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.empty()
            add(input, BorderLayout.NORTH)
            add(suggestionScrollPane, BorderLayout.CENTER)
            add(JBLabel(MyBundle.message("filterAuthorSelectHint")).apply {
                border = JBUI.Borders.empty(8, 10)
                foreground = UIUtil.getContextHelpForeground()
            }, BorderLayout.SOUTH)
        }
        refreshSuggestions()

        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(content, input)
            .setRequestFocus(true)
            .setFocusable(true)
            .setCancelOnClickOutside(true)
            .setMinSize(Dimension(JBUI.scale(360), JBUI.scale(160)))
            .createPopup()

        popup.setFinalRunnable(onClosed)
        popup.showUnderneathOf(anchor)
        input.requestFocusInWindow()
        input.selectAll()
        return popup
    }

    private fun activeToken(text: String, caretPosition: Int): String {
        val range = activeTokenRange(text, caretPosition)
        return text.substring(range.start, range.end)
    }

    private fun replaceActiveToken(text: String, caretPosition: Int, replacement: String): TokenReplacement {
        val range = activeTokenRange(text, caretPosition)
        val replacedText = text.take(range.start) + replacement + text.drop(range.end)
        return TokenReplacement(replacedText, range.start + replacement.length)
    }

    private fun replaceActiveTokenAndAppendNewLine(
        text: String,
        caretPosition: Int,
        replacement: String
    ): TokenReplacement {
        val tokenReplacement = replaceActiveToken(text, caretPosition, replacement)
        return insertNewLineAfter(tokenReplacement.text, tokenReplacement.caretPosition)
    }

    private fun insertNewLineAfter(text: String, caretPosition: Int): TokenReplacement {
        val caret = caretPosition.coerceIn(0, text.length)
        val remainingText = text.drop(caret)
        val newlinePrefix = remainingText.takeWhile { it == ' ' || it == '\t' }
        val nextNonBlankIndex = caret + newlinePrefix.length
        if (text.getOrNull(nextNonBlankIndex) == '\n') {
            return TokenReplacement(text, nextNonBlankIndex + 1)
        }

        val pipeSeparator = PIPE_SEPARATOR_PATTERN.find(remainingText)
        if (pipeSeparator != null) {
            val afterSeparator = caret + pipeSeparator.range.last + 1
            return TokenReplacement(text.take(caret) + "\n" + text.drop(afterSeparator), caret + 1)
        }

        return insertNewLine(text, caret)
    }

    private fun insertNewLine(text: String, caretPosition: Int): TokenReplacement {
        val caret = caretPosition.coerceIn(0, text.length)
        return TokenReplacement(text.take(caret) + "\n" + text.drop(caret), caret + 1)
    }

    private fun activeTokenRange(text: String, caretPosition: Int): TokenRange {
        val caret = caretPosition.coerceIn(0, text.length)
        val previousSeparator = if (caret == 0) {
            -1
        } else {
            maxOf(text.lastIndexOf('|', caret - 1), text.lastIndexOf('\n', caret - 1))
        }
        val nextSeparators = listOf(text.indexOf('|', caret), text.indexOf('\n', caret)).filter { it >= 0 }
        val rawStart = previousSeparator + 1
        val rawEnd = nextSeparators.minOrNull() ?: text.length
        val start = text.indexOfFirstNonWhitespace(rawStart, rawEnd)
        val end = text.indexAfterLastNonWhitespace(start, rawEnd)
        return TokenRange(start, end)
    }

    private fun String.indexOfFirstNonWhitespace(start: Int, end: Int): Int {
        var index = start
        while (index < end && this[index].isWhitespace()) {
            index++
        }
        return index
    }

    private fun String.indexAfterLastNonWhitespace(start: Int, end: Int): Int {
        var index = end
        while (index > start && this[index - 1].isWhitespace()) {
            index--
        }
        return index
    }

    private fun resolveAuthors(text: String, authors: List<String>): List<String>? {
        val tokens = text
            .split('|', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        if (tokens.isEmpty()) {
            return emptyList()
        }
        return tokens.map { token ->
            authors.firstOrNull { it.equals(token, ignoreCase = false) }
                ?: authors.firstOrNull { it.equals(token, ignoreCase = true) }
                ?: return null
        }
    }

    private fun installKeyboardActions(
        input: JBTextArea,
        suggestionList: JBList<String>,
        completeFromSuggestion: () -> Unit,
        completeFromSuggestionAndNewLine: () -> Unit,
        applyText: () -> Unit
    ) {
        input.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "gitStatsNextAuthorSuggestion")
        input.actionMap.put("gitStatsNextAuthorSuggestion", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                moveSelection(suggestionList, 1)
            }
        })
        input.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "gitStatsPreviousAuthorSuggestion")
        input.actionMap.put("gitStatsPreviousAuthorSuggestion", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                moveSelection(suggestionList, -1)
            }
        })
        input.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "gitStatsCompleteAuthorSuggestion")
        input.actionMap.put("gitStatsCompleteAuthorSuggestion", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                completeFromSuggestion()
            }
        })
        input.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "gitStatsCompleteAuthorAndNewLine")
        input.actionMap.put("gitStatsCompleteAuthorAndNewLine", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                completeFromSuggestionAndNewLine()
            }
        })
        input.inputMap.put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.META_DOWN_MASK),
            "gitStatsApplyAuthorSelection"
        )
        input.inputMap.put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
            "gitStatsApplyAuthorSelection"
        )
        input.actionMap.put("gitStatsApplyAuthorSelection", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                applyText()
            }
        })
    }

    private fun moveSelection(list: JBList<String>, delta: Int) {
        if (list.model.size == 0) return
        val currentIndex = list.selectedIndex.takeIf { it >= 0 } ?: 0
        val nextIndex = (currentIndex + delta).coerceIn(0, list.model.size - 1)
        list.selectedIndex = nextIndex
        list.ensureIndexIsVisible(nextIndex)
    }

    private fun setSuggestionMenuVisible(
        suggestionScrollPane: JBScrollPane,
        popup: JBPopup?,
        visible: Boolean
    ) {
        if (suggestionScrollPane.isVisible == visible) return
        suggestionScrollPane.isVisible = visible
        suggestionScrollPane.parent?.revalidate()
        suggestionScrollPane.parent?.repaint()
        popup?.pack(true, true)
    }

    private fun suggestionHeight(authorCount: Int): Int {
        return min(authorCount.coerceAtLeast(1), MAX_VISIBLE_ROWS) * SUGGESTION_ROW_HEIGHT
    }

    private class AuthorRenderer(
        private val query: () -> String
    ) : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
            val author = value as? String ?: return component
            component.text = highlightedName(author, query())
            component.border = JBUI.Borders.empty(4, 8)
            return component
        }

        private fun highlightedName(name: String, query: String): String {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isEmpty()) {
                return StringUtil.escapeXmlEntities(name)
            }
            val startIndex = name.indexOf(normalizedQuery, ignoreCase = true)
            if (startIndex < 0) {
                return StringUtil.escapeXmlEntities(name)
            }
            val endIndex = startIndex + normalizedQuery.length
            return "<html>" +
                StringUtil.escapeXmlEntities(name.substring(0, startIndex)) +
                "<font color=\"$MATCH_COLOR\">" +
                StringUtil.escapeXmlEntities(name.substring(startIndex, endIndex)) +
                "</font>" +
                StringUtil.escapeXmlEntities(name.substring(endIndex)) +
                "</html>"
        }
    }

    private const val TEXT_ROWS = 2
    private const val TEXT_COLUMNS = 34
    private const val MAX_VISIBLE_ROWS = 5
    private const val MAX_SUGGESTIONS = 20
    private const val SUGGESTION_ROW_HEIGHT = 32
    private const val MATCH_COLOR = "#589DF6"
    private val PIPE_SEPARATOR_PATTERN = Regex("""^\s*\|\s*""")

    private data class TokenReplacement(val text: String, val caretPosition: Int)
    private data class TokenRange(val start: Int, val end: Int)
}
