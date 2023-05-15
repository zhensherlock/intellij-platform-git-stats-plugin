package com.huayi.intellijplatform.gitstats.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.huayi.intellijplatform.gitstats.MyBundle
import com.huayi.intellijplatform.gitstats.toolWindow.StatsTableModel
import com.huayi.intellijplatform.gitstats.utils.GitUtil
import com.huayi.intellijplatform.gitstats.utils.Utils
import java.text.SimpleDateFormat
import java.util.*

@Service(Service.Level.PROJECT)
class GitStatsService(p: Project) {
    private var project: Project

    init {
        project = p
//        thisLogger().info(MyBundle.message("projectService", project.name))
    }

//    fun getRandomNumber() = (1..100).random()

    fun getUserStats(startTime: Date, endTime: Date): StatsTableModel {
        if (!Utils.checkDirectoryExists(project.basePath)) {
            return StatsTableModel(arrayOf(), arrayOf())
        }
        val gitUtil = GitUtil(project.basePath as String)
        val userStats = gitUtil.getUserStats(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTime),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endTime)
        )
        val data = userStats.map { item ->
            arrayOf(
                item.author,
                item.commitCount.toString(),
                item.addedLines.toString(),
                item.deletedLines.toString(),
                item.modifiedFileCount.toString()
            )
        }.toTypedArray()
        return StatsTableModel(
            data,
            arrayOf("Author", "CommitCount", "AddedLines", "DeletedLines", "ModifiedFileCount")
        )
    }

    fun getTopSpeedUserStats(startTime: Date, endTime: Date): StatsTableModel {
        if (!Utils.checkDirectoryExists(project.basePath)) {
            return StatsTableModel(arrayOf(), arrayOf())
        }
        val gitUtil = GitUtil(project.basePath as String)
        val userStats = gitUtil.getTopSpeedUserStats(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTime),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endTime)
        )
        val data = userStats.map { item ->
            arrayOf(
                item.author,
                item.addedLines.toString(),
                item.deletedLines.toString(),
                item.modifiedFileCount.toString()
            )
        }.toTypedArray()
        return StatsTableModel(
            data,
            arrayOf("Author", "AddedLines", "DeletedLines", "ModifiedFileCount")
        )
    }
}
