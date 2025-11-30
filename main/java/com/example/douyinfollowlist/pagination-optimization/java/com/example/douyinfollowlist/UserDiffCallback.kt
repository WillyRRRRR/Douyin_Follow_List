package com.example.douyinfollowlist

import androidx.recyclerview.widget.DiffUtil

/**
 * DiffUtil 回调类
 * 用于智能对比新旧列表的差异，只刷新变化的部分
 */
class UserDiffCallback(
    private val oldList: List<User>,
    private val newList: List<User>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    // 判断是否是同一个对象（通过ID判断）
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    // 判断内容是否相同
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldUser = oldList[oldItemPosition]
        val newUser = newList[newItemPosition]

        // 比较所有可能变化的字段
        return oldUser.username == newUser.username &&
                oldUser.isFollowed == newUser.isFollowed &&
                oldUser.isSpecialFollow == newUser.isSpecialFollow &&
                oldUser.remark == newUser.remark &&
                oldUser.isVip == newUser.isVip
    }

    // 当内容不同时，返回具体变化的字段（可选）
    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldUser = oldList[oldItemPosition]
        val newUser = newList[newItemPosition]

        // 返回变化的字段，用于局部更新
        val payload = mutableMapOf<String, Any>()

        if (oldUser.isFollowed != newUser.isFollowed) {
            payload["isFollowed"] = newUser.isFollowed
        }
        if (oldUser.isSpecialFollow != newUser.isSpecialFollow) {
            payload["isSpecialFollow"] = newUser.isSpecialFollow
        }
        if (oldUser.remark != newUser.remark) {
            payload["remark"] = newUser.remark
        }

        return if (payload.isEmpty()) null else payload
    }
}