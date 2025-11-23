package com.example.douyinfollowlist

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class UserDataManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)

    // 保存所有用户数据
    fun saveUsers(users: List<User>) {
        val jsonArray = JSONArray()

        for (user in users) {
            val jsonObject = JSONObject()
            jsonObject.put("id", user.id)
            jsonObject.put("username", user.username)
            jsonObject.put("douyinId", user.douyinId)
            jsonObject.put("avatarResId", user.avatarResId)
            jsonObject.put("isVip", user.isVip)
            jsonObject.put("isFollowed", user.isFollowed)
            jsonObject.put("isSpecialFollow", user.isSpecialFollow)
            jsonObject.put("remark", user.remark)
            jsonObject.put("followTime", user.followTime)
            jsonArray.put(jsonObject)
        }

        prefs.edit().putString("users", jsonArray.toString()).apply()
    }

    // 读取所有用户数据
    fun loadUsers(): List<User>? {
        val jsonString = prefs.getString("users", null) ?: return null

        val users = mutableListOf<User>()
        val jsonArray = JSONArray(jsonString)

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            users.add(
                User(
                    id = jsonObject.getInt("id"),
                    username = jsonObject.getString("username"),
                    douyinId = jsonObject.getString("douyinId"),
                    avatarResId = jsonObject.getInt("avatarResId"),
                    isVip = jsonObject.getBoolean("isVip"),
                    isFollowed = jsonObject.getBoolean("isFollowed"),
                    isSpecialFollow = jsonObject.getBoolean("isSpecialFollow"),
                    remark = jsonObject.getString("remark"),
                    followTime = jsonObject.getLong("followTime")
                )
            )
        }

        return users
    }

    // 清除所有数据
    fun clearData() {
        prefs.edit().clear().apply()
    }
}