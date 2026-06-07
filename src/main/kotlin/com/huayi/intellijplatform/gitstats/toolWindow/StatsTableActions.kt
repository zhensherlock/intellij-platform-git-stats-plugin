package com.huayi.intellijplatform.gitstats.toolWindow

import com.huayi.intellijplatform.gitstats.MyBundle
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.AbstractAction
import javax.swing.JTable
import javax.swing.KeyStroke

internal object StatsTableActions {
    private const val COPY_SELECTED_ACTION = "copySelectedStatsRows"

    fun install(table: JTable, project: Project) {
        table.inputMap.put(
            KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx),
            COPY_SELECTED_ACTION
        )
        table.actionMap.put(COPY_SELECTED_ACTION, object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                copyRows(table, project, selectedOnly = true)
            }
        })

        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                showPopup(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                showPopup(e)
            }

            private fun showPopup(e: MouseEvent) {
                if (!e.isPopupTrigger) return
                val row = table.rowAtPoint(e.point)
                if (row >= 0 && !table.isRowSelected(row)) {
                    table.setRowSelectionInterval(row, row)
                }
                createPopup(table, project).show(RelativePoint(e))
            }
        })
    }

    private fun createPopup(table: JTable, project: Project) =
        JBPopupFactory.getInstance().createActionGroupPopup(
            null,
            DefaultActionGroup().apply {
                add(CopySelectedRowsAction(table, project))
                add(CopyAllRowsAction(table, project))
                addSeparator()
                add(ExportCsvAction(table, project))
            },
            DataManager.getInstance().getDataContext(table),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
        )

    private class CopySelectedRowsAction(
        private val table: JTable,
        private val project: Project
    ) : DumbAwareAction(MyBundle.message("tableCopySelected")) {
        override fun actionPerformed(e: AnActionEvent) {
            copyRows(table, project, selectedOnly = true)
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = table.selectedRowCount > 0
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    }

    private class CopyAllRowsAction(
        private val table: JTable,
        private val project: Project
    ) : DumbAwareAction(MyBundle.message("tableCopyAll")) {
        override fun actionPerformed(e: AnActionEvent) {
            copyRows(table, project, selectedOnly = false)
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = table.rowCount > 0
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    }

    private class ExportCsvAction(
        private val table: JTable,
        private val project: Project
    ) : DumbAwareAction(MyBundle.message("tableExportCsv")) {
        override fun actionPerformed(e: AnActionEvent) {
            exportCsv(table, project)
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = table.rowCount > 0
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    }

    private data class TableSnapshot(
        val headers: List<String>,
        val rows: List<List<Any>>
    )

    private fun tableSnapshot(table: JTable, selectedOnly: Boolean): TableSnapshot {
        val model = table.model as? StatsTableModel ?: return TableSnapshot(emptyList(), emptyList())
        if (model.columnCount == 0) {
            return TableSnapshot(emptyList(), emptyList())
        }
        val viewRows = if (selectedOnly) {
            table.selectedRows.toList().sorted()
        } else {
            (0 until table.rowCount).toList()
        }
        val viewColumns = (0 until table.columnCount).toList()
        val headers = viewColumns.map { viewColumn -> table.getColumnName(viewColumn) }
        val rows = viewRows.map { viewRow ->
            val modelRow = table.convertRowIndexToModel(viewRow)
            viewColumns.map { viewColumn ->
                val modelColumn = table.convertColumnIndexToModel(viewColumn)
                model.getValueAt(modelRow, modelColumn)
            }
        }
        return TableSnapshot(headers, rows)
    }

    private fun copyRows(table: JTable, project: Project, selectedOnly: Boolean) {
        val snapshot = tableSnapshot(table, selectedOnly)
        if (snapshot.rows.isEmpty()) {
            Messages.showInfoMessage(
                project,
                MyBundle.message("tableNoRowsToCopy"),
                MyBundle.message("name")
            )
            return
        }
        Toolkit.getDefaultToolkit()
            .systemClipboard
            .setContents(StringSelection(toTsv(snapshot)), null)
    }

    private fun exportCsv(table: JTable, project: Project) {
        val snapshot = tableSnapshot(table, selectedOnly = false)
        if (snapshot.rows.isEmpty()) {
            Messages.showInfoMessage(
                project,
                MyBundle.message("tableNoRowsToExport"),
                MyBundle.message("name")
            )
            return
        }
        val targetFile = chooseCsvTarget(project) ?: return
        runCatching {
            targetFile.writeText(toCsv(snapshot), Charsets.UTF_8)
        }.onFailure {
            Messages.showErrorDialog(
                project,
                MyBundle.message("exportCsvFailed", it.message ?: it::class.java.simpleName),
                MyBundle.message("name")
            )
        }.onSuccess {
            Messages.showInfoMessage(
                project,
                MyBundle.message("exportCsvSuccess", targetFile.absolutePath),
                MyBundle.message("name")
            )
        }
    }

    private fun chooseCsvTarget(project: Project): File? {
        val descriptor = FileSaverDescriptor(
            MyBundle.message("exportCsvDialogTitle"),
            MyBundle.message("exportCsvDialogDescription"),
            "csv"
        )
        val wrapper = FileChooserFactory.getInstance()
            .createSaveFileDialog(descriptor, project)
            .save(baseDirectory(project), MyBundle.message("exportCsvDefaultFileName"))
            ?: return null
        return ensureCsvExtension(wrapper.file)
    }

    private fun baseDirectory(project: Project): VirtualFile? {
        return project.basePath?.let { LocalFileSystem.getInstance().refreshAndFindFileByPath(it) }
    }

    private fun ensureCsvExtension(file: File): File {
        if (file.name.endsWith(".csv", ignoreCase = true)) {
            return file
        }
        val parent = file.parentFile
        return if (parent == null) {
            File("${file.path}.csv")
        } else {
            File(parent, "${file.name}.csv")
        }
    }

    private fun toTsv(snapshot: TableSnapshot): String {
        return (listOf(snapshot.headers) + snapshot.rows)
            .joinToString("\n") { row ->
                row.joinToString("\t") { value ->
                    value.toString().replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
                }
            }
    }

    private fun toCsv(snapshot: TableSnapshot): String {
        return (listOf(snapshot.headers) + snapshot.rows)
            .joinToString("\n") { row ->
                row.joinToString(",") { value -> csvEscape(value.toString()) }
            }
    }

    private fun csvEscape(value: String): String {
        val normalized = value.replace("\r\n", "\n").replace('\r', '\n')
        val escaped = normalized.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '"' || it == '\n' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
