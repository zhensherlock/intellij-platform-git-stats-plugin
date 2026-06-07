package com.huayi.intellijplatform.gitstats.toolWindow

import com.huayi.intellijplatform.gitstats.MyBundle
import java.text.NumberFormat
import java.util.regex.Pattern
import javax.swing.JTable
import javax.swing.RowFilter
import javax.swing.RowSorter
import javax.swing.SortOrder
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableRowSorter

internal object StatsTableSupport {
    fun createSummaryText(model: StatsTableModel, rows: List<StatsTableRow>): String {
        if (rows.isEmpty()) {
            return MyBundle.message("statsSummaryNoRows")
        }
        val formatter = NumberFormat.getIntegerInstance()
        val authors = formatter.format(rows.size)
        val addedLines = formatter.format(rows.sumOf { it.addedLines })
        val deletedLines = formatter.format(rows.sumOf { it.deletedLines })
        val modifiedFiles = formatter.format(rows.sumOf { it.modifiedFileCount })
        if (model.hasColumn(StatsTableColumnKind.COMMITS)) {
            return MyBundle.message(
                "statsSummaryDetailed",
                authors,
                formatter.format(rows.sumOf { it.commitCount ?: 0 }),
                addedLines,
                deletedLines,
                modifiedFiles
            )
        }
        return MyBundle.message(
            "statsSummaryFast",
            authors,
            addedLines,
            deletedLines,
            modifiedFiles
        )
    }

    fun visibleRows(table: JTable, model: StatsTableModel): List<StatsTableRow> {
        return (0 until table.rowCount).map { viewRow ->
            model.rowAt(table.convertRowIndexToModel(viewRow))
        }
    }

    fun createAuthorFilter(
        model: StatsTableModel,
        authorFilter: String
    ): RowFilter<StatsTableModel, Int>? {
        if (authorFilter.isBlank()) {
            return null
        }
        val authorColumn = model.columnIndex(StatsTableColumnKind.AUTHOR)
        if (authorColumn < 0) {
            return null
        }
        val pattern = Pattern.compile(Pattern.quote(authorFilter), Pattern.CASE_INSENSITIVE)
        return object : RowFilter<StatsTableModel, Int>() {
            override fun include(entry: Entry<out StatsTableModel, out Int>): Boolean {
                return pattern.matcher(entry.getStringValue(authorColumn)).find()
            }
        }
    }

    fun applyDefaultSort(sorter: TableRowSorter<StatsTableModel>, model: StatsTableModel) {
        val defaultColumn = if (model.hasColumn(StatsTableColumnKind.COMMITS)) {
            model.columnIndex(StatsTableColumnKind.COMMITS)
        } else {
            model.columnIndex(StatsTableColumnKind.LINES_ADDED)
        }
        if (defaultColumn >= 0) {
            sorter.sortKeys = listOf(RowSorter.SortKey(defaultColumn, SortOrder.DESCENDING))
        }
    }

    fun configureColumns(table: JTable, model: StatsTableModel) {
        val numericRenderer = DefaultTableCellRenderer().apply {
            horizontalAlignment = SwingConstants.RIGHT
        }
        model.columns.forEachIndexed { modelIndex, column ->
            val viewIndex = table.convertColumnIndexToView(modelIndex)
            if (viewIndex < 0) return@forEachIndexed
            val tableColumn = table.columnModel.getColumn(viewIndex)
            when (column.kind) {
                StatsTableColumnKind.AUTHOR -> {
                    tableColumn.minWidth = 160
                    tableColumn.preferredWidth = 260
                }
                StatsTableColumnKind.COMMITS -> {
                    tableColumn.minWidth = 80
                    tableColumn.preferredWidth = 90
                    tableColumn.cellRenderer = numericRenderer
                }
                StatsTableColumnKind.LINES_ADDED,
                StatsTableColumnKind.LINES_DELETED,
                StatsTableColumnKind.MODIFIED_FILES -> {
                    tableColumn.minWidth = 110
                    tableColumn.preferredWidth = 130
                    tableColumn.cellRenderer = numericRenderer
                }
            }
        }
    }
}
