package com.huayi.intellijplatform.gitstats.services

import com.huayi.intellijplatform.gitstats.models.SettingModel
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

@Service(Service.Level.PROJECT)
@State(
    name = "GitStatsSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class GitStatsSettingsService : PersistentStateComponent<SettingModel> {
    private var state = SettingModel()

    override fun getState(): SettingModel = state

    override fun loadState(state: SettingModel) {
        this.state = state.normalized()
    }

    fun getSettings(): SettingModel = state.copy()

    fun updateSettings(settingModel: SettingModel) {
        state = settingModel.normalized()
    }

    private fun SettingModel.normalized(): SettingModel {
        val normalizedMode = when (mode) {
            SettingModel.MODE_FAST_SUMMARY,
            SettingModel.MODE_DETAILED -> mode
            else -> SettingModel.MODE_FAST_SUMMARY
        }
        return copy(
            mode = normalizedMode,
            exclude = SettingModel.parseExcludePaths(exclude).joinToString("\n")
        )
    }
}
