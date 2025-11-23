package com.example.douyinfollowlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView

class UserAdapter(
    private val userList: MutableList<User>,
    private val onMoreClick: (User, Int) -> Unit,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

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
        val user = userList[position]

        // 设置头像
        holder.avatarImage.setImageResource(user.avatarResId)

        // 设置用户名（如果有备注显示备注）
        holder.usernameText.text = if (user.remark.isNotEmpty()) {
            user.remark
        } else {
            user.username
        }

        // 设置VIP图标
        holder.vipIcon.visibility = if (user.isVip) View.VISIBLE else View.GONE

        // 设置特别关注标签
        holder.specialFollowTag.visibility = if (user.isSpecialFollow) View.VISIBLE else View.GONE

        // 设置关注按钮状态
        if (user.isFollowed) {
            holder.followButton.text = "已关注"
            holder.followButton.setTextColor(0xFF999999.toInt())
            holder.followButton.setBackgroundResource(R.drawable.button_followed)
        } else {
            holder.followButton.text = "关注"
            holder.followButton.setTextColor(0xFFFFFFFF.toInt())
            holder.followButton.setBackgroundResource(R.drawable.button_follow)
        }

        // 点击关注按钮
        holder.followButton.setOnClickListener {
            user.isFollowed = !user.isFollowed
            notifyItemChanged(position)
        }

        // 点击更多按钮
        holder.moreButton.setOnClickListener {
            onMoreClick(user, position)
        }

        // 点击头像或用户名
        holder.avatarImage.setOnClickListener {
            onUserClick(user)
        }
        holder.userInfoLayout.setOnClickListener {
            onUserClick(user)
        }
    }

    override fun getItemCount(): Int = userList.size

    // 获取Item的唯一ID，提升RecyclerView性能
    override fun getItemId(position: Int): Long = userList[position].id.toLong()

    init {
        setHasStableIds(true)
    }
}