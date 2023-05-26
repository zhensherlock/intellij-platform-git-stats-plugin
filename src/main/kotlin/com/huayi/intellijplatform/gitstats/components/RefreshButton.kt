package com.huayi.intellijplatform.gitstats.components

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JButton
import javax.swing.SwingWorker


class RefreshButton(text: String?) : JButton(text), ActionListener {
    private var isLoading = false

    init {
        addActionListener(this)
    }

    override fun actionPerformed(e: ActionEvent) {
        isLoading = true
        isEnabled = false
        text = "Loading..."
        BackgroundTask().execute()
    }

    private inner class BackgroundTask : SwingWorker<String, String>() {
        override fun doInBackground(): String {
            return "null"
        }

        override fun done() {
            isLoading = false
            isEnabled = true
            text = "Click me"
        }
    }
}