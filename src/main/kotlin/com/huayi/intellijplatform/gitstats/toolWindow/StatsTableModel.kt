package com.huayi.intellijplatform.gitstats.toolWindow

import javax.swing.table.AbstractTableModel

enum class StatsTableColumnKind {
    AUTHOR,
    COMMITS,
    LINES_ADDED,
    LINES_DELETED,
    MODIFIED_FILES
}

data class StatsTableColumn(
    val kind: StatsTableColumnKind,
    val title: String,
    val columnClass: Class<*>
)

data class StatsTableRow(
    val author: String,
    val commitCount: Int? = null,
    val addedLines: Int,
    val deletedLines: Int,
    val modifiedFileCount: Int
)

class StatsTableModel(
    val rows: List<StatsTableRow>,
    val columns: List<StatsTableColumn>
) : AbstractTableModel() {
    override fun getRowCount(): Int = rows.size

    override fun getColumnCount(): Int = columns.size

    override fun getColumnName(column: Int): String = columns[column].title

    override fun getColumnClass(columnIndex: Int): Class<*> = columns[columnIndex].columnClass

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val row = rows[rowIndex]
        return when (columns[columnIndex].kind) {
            StatsTableColumnKind.AUTHOR -> row.author
            StatsTableColumnKind.COMMITS -> row.commitCount ?: 0
            StatsTableColumnKind.LINES_ADDED -> row.addedLines
            StatsTableColumnKind.LINES_DELETED -> row.deletedLines
            StatsTableColumnKind.MODIFIED_FILES -> row.modifiedFileCount
        }
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false

    fun rowAt(rowIndex: Int): StatsTableRow = rows[rowIndex]

    fun columnIndex(kind: StatsTableColumnKind): Int = columns.indexOfFirst { it.kind == kind }

    fun hasColumn(kind: StatsTableColumnKind): Boolean = columnIndex(kind) >= 0

    companion object {
        fun empty(): StatsTableModel = StatsTableModel(emptyList(), emptyList())
    }
}
