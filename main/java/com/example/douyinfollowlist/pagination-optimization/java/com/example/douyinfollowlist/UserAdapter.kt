package com.example.douyinfollowlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.imageview.ShapeableImageView

class UserAdapter(
    private val userList: MutableList<User>,
    private val onMoreClick: (User, Int) -> Unit,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    // 优化后的Glide配置
    private val glideOptions = RequestOptions()
        .circleCrop() // 使用更高效的circleCrop替代transform
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC) // 让Glide自动选择最优策略
        .skipMemoryCache(false)
        .placeholder(R.drawable.default_avatar)
        .error(R.drawable.default_avatar)
        .override(144, 144) // 明确尺寸（48dp * 3倍密度）

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarImage: ShapeableImageView = itemView.findViewById(R.id.avatarImage)
        val usernameText: TextView = itemView.findViewById(R.id.usernameText)
        val vipIcon: ImageView = itemView.findViewById(R.id.vipIcon)
        val specialFollowTag: TextView = itemView.findViewById(R.id.specialFollowTag)
        val followButton: TextView = itemView.findViewById(R.id.followButton)
        val moreButton: ImageView = itemView.findViewById(R.id.moreButton)
        val userInfoLayout: LinearLayout = itemView.findViewById(R.id.userInfoLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        bindUser(holder, position, null)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            bindUser(holder, position, payloads[0] as? Map<*, *>)
        }
    }

    private fun bindUser(holder: UserViewHolder, position: Int, payload: Map<*, *>?) {
        val user = userList[position]

        // 局部更新
        if (payload != null) {
            payload["isFollowed"]?.let {
                updateFollowButton(holder, user)
            }
            payload["isSpecialFollow"]?.let {
                holder.specialFollowTag.visibility = if (user.isSpecialFollow) View.VISIBLE else View.GONE
            }
            payload["remark"]?.let {
                holder.usernameText.text = if (user.remark.isNotEmpty()) user.remark else user.username
            }
            return
        }

        // 完整更新
        // 加载头像（移除了预加载逻辑）
        Glide.with(holder.itemView.context)
            .load(user.avatarResId)
            .apply(glideOptions)
            .into(holder.avatarImage)

        holder.usernameText.text = if (user.remark.isNotEmpty()) {
            user.remark
        } else {
            user.username
        }

        holder.vipIcon.visibility = if (user.isVip) View.VISIBLE else View.GONE
        holder.specialFollowTag.visibility = if (user.isSpecialFollow) View.VISIBLE else View.GONE

        updateFollowButton(holder, user)

        holder.followButton.setOnClickListener {
            user.isFollowed = !user.isFollowed
            notifyItemChanged(position, mapOf("isFollowed" to user.isFollowed))
        }

        holder.moreButton.setOnClickListener {
            onMoreClick(user, position)
        }

        holder.avatarImage.setOnClickListener {
            onUserClick(user)
        }
        holder.userInfoLayout.setOnClickListener {
            onUserClick(user)
        }
    }

    private fun updateFollowButton(holder: UserViewHolder, user: User) {
        if (user.isFollowed) {
            holder.followButton.text = "已关注"
            holder.followButton.setTextColor(0xFF999999.toInt())
            holder.followButton.setBackgroundResource(R.drawable.button_followed)
        } else {
            holder.followButton.text = "关注"
            holder.followButton.setTextColor(0xFFFFFFFF.toInt())
            holder.followButton.setBackgroundResource(R.drawable.button_follow)
        }
    }

    override fun onViewRecycled(holder: UserViewHolder) {
        super.onViewRecycled(holder)
        Glide.with(holder.itemView.context).clear(holder.avatarImage)
    }

    override fun getItemCount(): Int = userList.size

    override fun getItemId(position: Int): Long = userList[position].id.toLong()

    init {
        setHasStableIds(true)
    }

    // 高性能更新方法：避免DiffUtil计算
    fun updateDataAsync(newList: List<User>, callback: () -> Unit = {}) {
        val oldSize = userList.size

        if (oldSize == 0) {
            // 首次加载，直接添加
            userList.addAll(newList)
            notifyItemRangeInserted(0, newList.size)
        } else if (newList.size > oldSize) {
            // 新增数据（分页加载）
            userList.clear()
            userList.addAll(newList)
            notifyItemRangeInserted(oldSize, newList.size - oldSize)
        } else {
            // 搜索/排序场景，全量替换
            userList.clear()
            userList.addAll(newList)
            notifyDataSetChanged()
        }

        callback()
    }
}