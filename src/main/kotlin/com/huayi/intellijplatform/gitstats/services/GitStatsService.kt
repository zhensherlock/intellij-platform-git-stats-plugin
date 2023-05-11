package com.huayi.intellijplatform.gitstats.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.utils.GitUtil

@Service(Service.Level.PROJECT)
class GitStatsService(p: Project) {
    private var project: Project

    init {
        project = p
        thisLogger().info(MyBundle.message("projectService", project.name))
        thisLogger().info(project.projectFilePath)
        thisLogger().info(project.basePath)
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    fun getRandomNumber() = (1..100).random()

    fun getUserStats() {
        val startDate = "2023-05-07T00:00:00"
        val endDate = "2023-05-13T23:59:59"
        val gitUtil = GitUtil("/Users/sunzhenxuan/work/qcc/code/qcc_pro/pro-front")
        val gitStatsResult = gitUtil.getUserStats(startDate, endDate)
        for (commitStats in gitStatsResult) {
            println("${commitStats.author} committed ${commitStats.commitCount} times, added ${commitStats.addedLines} lines, deleted ${commitStats.deletedLines} lines, modified ${commitStats.modifiedFileCount} files")
        }
    }
}
