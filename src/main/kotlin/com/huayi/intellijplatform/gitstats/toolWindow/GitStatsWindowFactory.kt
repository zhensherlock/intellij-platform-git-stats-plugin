package com.huayi.intellijplatform.gitstats.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.services.GitStatsService
import javax.swing.JButton


class GitStatsWindowFactory : ToolWindowFactory {

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    private val contentFactory = ContentFactory.SERVICE.getInstance()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val gitStatsWindow = GitStatsWindow(toolWindow)
        val content = contentFactory.createContent(gitStatsWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class GitStatsWindow(toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<GitStatsService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            service.getUserStats()
            val label = JBLabel(MyBundle.message("randomLabel", "?"))

            add(label)
            add(JButton(MyBundle.message("shuffle")).apply {
                addActionListener {
                    label.text = MyBundle.message("randomLabel", service.getRandomNumber())
                }
            })
        }
    }
}
