package com.huayi.intellijplatform.gitstats.toolWindow

import javax.swing.table.DefaultTableModel

class StatsTableModel(data: Array<Array<String>>, columnNames: Array<String>) : DefaultTableModel(data, columnNames) {
    override fun isCellEditable(row: Int, column: Int): Boolean {
        return false
    }
}