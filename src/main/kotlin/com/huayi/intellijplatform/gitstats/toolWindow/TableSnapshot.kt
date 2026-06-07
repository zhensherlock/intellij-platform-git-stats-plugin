package com.huayi.intellijplatform.gitstats.toolWindow

import javax.swing.JTable

internal data class TableSnapshot(
    val headers: List<String>,
    val rows: List<List<Any>>
)

internal object TableSnapshots {
    fun from(table: JTable, selectedOnly: Boolean): TableSnapshot {
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
}

internal object TableTextFormatters {
    fun toTsv(snapshot: TableSnapshot): String {
        return (listOf(snapshot.headers) + snapshot.rows)
            .joinToString("\n") { row ->
                row.joinToString("\t") { value ->
                    value.toString().replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
                }
            }
    }

    fun toCsv(snapshot: TableSnapshot): String {
        return (listOf(snapshot.headers) + snapshot.rows)
            .joinToString("\n") { row ->
                row.joinToString(",") { value -> csvEscape(value.toString()) }
            }
    }

    internal fun csvEscape(value: String): String {
        val normalized = value.replace("\r\n", "\n").replace('\r', '\n')
        val escaped = normalized.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '"' || it == '\n' }) {
            "\"$escaped\""
        } else {
            escaped
        }
    }
}
