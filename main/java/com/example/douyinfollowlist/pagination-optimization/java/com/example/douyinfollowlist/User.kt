package com.example.douyinfollowlist

data class User(
    val id: Int,                    // 用户唯一ID
    val username: String,           // 用户名
    val douyinId: String = "",      // 抖音号
    val avatarResId: Int,           // 头像资源ID
    val isVip: Boolean = false,     // 是否是VIP
    var isFollowed: Boolean = true, // 是否已关注
    var isSpecialFollow: Boolean = false,  // 是否特别关注
    var remark: String = "",        // 备注名
    val followTime: Long = System.currentTimeMillis(),  // 关注时间
    val signature: String? = null,           // 个性签名，可为空
    val isFollowedBack: Boolean = false
)
