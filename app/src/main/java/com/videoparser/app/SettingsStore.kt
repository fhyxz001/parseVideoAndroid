package com.videoparser.app

import android.content.Context

/** 用户设置持久化，目前仅用于保存可自定义的解析服务器地址 */
object SettingsStore {

    private const val PREFS_NAME = "video_parser_settings"
    private const val KEY_SERVER = "server_base_url"

    fun getServer(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SERVER, VideoApi.DEFAULT_SERVER) ?: VideoApi.DEFAULT_SERVER
    }

    fun setServer(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVER, value.trim().trimEnd('/'))
            .apply()
    }
}
