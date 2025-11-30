package com.example.douyinfollowlist

import kotlinx.coroutines.delay

/**
 * 模拟服务端
 * 负责分页返回数据，每次10条
 */
object MockServer {
    private const val TOTAL_COUNT = 1000 // 总关注数 1000
    private const val PAGE_SIZE = 10     // 每次返回 10 条

    // 模拟的头像资源池
    private val avatarResIds = listOf(
        R.drawable.head1, R.drawable.head2, R.drawable.head3,
        R.drawable.head4, R.drawable.head5, R.drawable.head6,
        R.drawable.head7, R.drawable.head8, R.drawable.head9
    )

    private val baseNames = listOf("周杰伦", "王心凌", "张韶涵", "林俊杰", "五月天", "薛之谦", "邓紫棋", "刘德华", "杨幂")

    /**
     * 模拟网络请求获取用户列表
     * suspend 关键字表示这是一个挂起函数，需要在协程中运行
     */
    suspend fun getUsers(pageIndex: Int): List<User> {
        // 首页快速返回，后续页面正常延迟
        if (pageIndex == 1) {
            delay(100) // 首页只延迟100ms
        } else {
            delay(500) // 后续页延迟500ms
        }
        // 1. 模拟网络延迟 (500ms - 1000ms)，让加载过程更真实
        delay(800)

        val startId = (pageIndex - 1) * PAGE_SIZE
        if (startId >= TOTAL_COUNT) {
            return emptyList() // 超过总数，返回空
        }

        val users = mutableListOf<User>()
        // 每次只生成 10 条数据
        for (i in 0 until PAGE_SIZE) {
            val currentId = startId + i
            if (currentId >= TOTAL_COUNT) break // 防止越界

            val baseIndex = currentId % baseNames.size
            users.add(
                User(
                    id = currentId,
                    username = "${baseNames[baseIndex]} #${currentId + 1}", //加上编号方便观察
                    douyinId = "DY_${10000 + currentId}",
                    avatarResId = avatarResIds[baseIndex],
                    isVip = (currentId % 3 == 0),
                    followTime = System.currentTimeMillis() - (currentId * 3600000L)
                )
            )
        }
        return users
    }
}