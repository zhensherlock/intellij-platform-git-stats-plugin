package com.huayi.intellijplatform.gitstats.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.utils.GitUtil
import com.huayi.intellijplatform.gitstats.utils.UserStats

@Service(Service.Level.PROJECT)
class GitStatsService(p: Project) {
    private var project: Project

    init {
        project = p
        thisLogger().info(MyBundle.message("projectService", project.name))
        thisLogger().info(project.projectFilePath)
        thisLogger().info(project.basePath)
    }

    fun getRandomNumber() = (1..100).random()

    fun getUserStats(startDate: String, endDate: String): Array<UserStats> {
        val gitUtil = GitUtil("/Users/sunzhenxuan/work/qcc/code/qcc_pro/pro-front")
        return gitUtil.getUserStats(startDate, endDate)
    }
}
