package com.huayi.intellijplatform.gitstats.services

import com.huayi.intellijplatform.gitstats.models.SettingModel
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.huayi.intellijplatform.gitstats.toolWindow.StatsTableModel
import com.huayi.intellijplatform.gitstats.utils.GitUtils
import com.huayi.intellijplatform.gitstats.utils.Utils
import java.text.SimpleDateFormat
import java.util.*

@Service(Service.Level.PROJECT)
class GitStatsService(p: Project) {
    private var project: Project

    init {
        project = p
    }

    fun getUserStats(startTime: Date, endTime: Date, settingModel: SettingModel): StatsTableModel {
        if (!Utils.checkDirectoryExists(project.basePath)) {
            return StatsTableModel(arrayOf(), arrayOf())
        }
        val gitUtils = GitUtils(project)
        val userStats = gitUtils.getUserStats(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTime),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endTime),
            settingModel
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
            arrayOf("Author", "Commits", "Lines Added", "Lines Deleted", "Modified Files")
        )
    }

    fun getTopSpeedUserStats(startTime: Date, endTime: Date, settingModel: SettingModel): StatsTableModel {
        if (!Utils.checkDirectoryExists(project.basePath)) {
            return StatsTableModel(arrayOf(), arrayOf())
        }
        val gitUtils = GitUtils(project)
        val userStats = gitUtils.getTopSpeedUserStats(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTime),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endTime),
            settingModel
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
            arrayOf("Author", "Lines Added", "Lines Deleted", "Modified Files")
        )
    }
}
