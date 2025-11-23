package com.example.douyinfollowlist

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabIndicator: View
    private lateinit var tabs: List<TextView>

    // 数据存储管理
    private lateinit var userDataManager: UserDataManager

    // 所有用户数据
    private val allUsers = mutableListOf<User>()
    // 当前显示的用户
    private val displayUsers = mutableListOf<User>()

    // 排序方式
    private var sortDescending = true

    // 关注页面的组件引用
    private var followAdapter: UserAdapter? = null
    private var searchEditText: EditText? = null
    private var followCountText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化数据存储管理
        userDataManager = UserDataManager(this)

        // 尝试从本地加载数据，如果没有则初始化模拟数据
        val savedUsers = userDataManager.loadUsers()
        if (savedUsers != null) {
            allUsers.addAll(savedUsers)
        } else {
            initMockData()
        }

        // 绑定Tab
        val tabMutual = findViewById<TextView>(R.id.tabMutual)
        val tabFollow = findViewById<TextView>(R.id.tabFollow)
        val tabFans = findViewById<TextView>(R.id.tabFans)
        val tabFriends = findViewById<TextView>(R.id.tabFriends)
        tabs = listOf(tabMutual, tabFollow, tabFans, tabFriends)

        tabIndicator = findViewById(R.id.tabIndicator)
        viewPager = findViewById(R.id.viewPager)

        // 设置ViewPager适配器
        viewPager.adapter = TabPagerAdapter()

        // ViewPager滑动监听
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabStyle(position)
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                updateIndicatorPosition(position, positionOffset)
            }
        })

        // Tab点击监听
        tabs.forEachIndexed { index, tab ->
            tab.setOnClickListener {
                viewPager.currentItem = index
            }
        }

        // 默认选中"关注"Tab（第二个）
        viewPager.setCurrentItem(1, false)
        updateTabStyle(1)

        // 初始化下划线位置
        tabIndicator.post {
            updateIndicatorPosition(1, 0f)
        }

        // 设置返回按钮
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    // 更新下划线位置
    private fun updateIndicatorPosition(position: Int, positionOffset: Float) {
        val backButtonWidth = (40 * resources.displayMetrics.density)
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val tabWidth = (screenWidth - backButtonWidth) / 4
        val indicatorWidth = tabIndicator.width.toFloat()
        val offset = backButtonWidth + (position + positionOffset) * tabWidth + (tabWidth - indicatorWidth) / 2
        tabIndicator.translationX = offset
    }

    // 设置返回按钮
    private fun setupBackButton() {
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }
    }

    // 更新Tab样式
    private fun updateTabStyle(selectedIndex: Int) {
        tabs.forEachIndexed { index, tab ->
            if (index == selectedIndex) {
                tab.setTextColor(0xFF000000.toInt())
            } else {
                tab.setTextColor(0xFF999999.toInt())
            }
        }
    }

    // 初始化模拟数据（大量数据测试版）
    private fun initMockData() {
        val baseNames = listOf("周杰伦", "王心凌", "张韶涵", "林俊杰", "五月天", "薛之谦", "邓紫棋", "刘德华", "杨幂")
        val baseDouyinIds = listOf("Jay_Chou", "CyndiWang905", "AngelaZhang", "JJLin", "Mayday", "JokerXue", "GEM", "AndyLau", "YangMi")
        val avatars = listOf(
            R.drawable.head1, R.drawable.head2, R.drawable.head3,
            R.drawable.head4, R.drawable.head5, R.drawable.head6,
            R.drawable.head7, R.drawable.head8, R.drawable.head9
        )

        // 生成10000个用户数据
        val totalUsers = 10000

        for (i in 0 until totalUsers) {
            val baseIndex = i % baseNames.size
            val suffix = if (i < baseNames.size) "" else "_${i / baseNames.size}"

            allUsers.add(
                User(
                    id = i,
                    username = "${baseNames[baseIndex]}$suffix",
                    douyinId = "${baseDouyinIds[baseIndex]}$suffix",
                    avatarResId = avatars[baseIndex],
                    isVip = (i % 3 != 0), // 约2/3的用户是VIP
                    followTime = System.currentTimeMillis() - (i * 3600000L) // 每人间隔1小时
                )
            )
        }
    }

    // 更新显示列表
    private fun updateDisplayList(searchText: String = "") {
        displayUsers.clear()

        val filtered = if (searchText.isEmpty()) {
            allUsers.toList()
        } else {
            allUsers.filter {
                it.username.contains(searchText, ignoreCase = true) ||
                        it.remark.contains(searchText, ignoreCase = true) ||
                        it.douyinId.contains(searchText, ignoreCase = true)
            }
        }

        val sorted = if (sortDescending) {
            filtered.sortedByDescending { it.followTime }
        } else {
            filtered.sortedBy { it.followTime }
        }

        displayUsers.addAll(sorted)
        followAdapter?.notifyDataSetChanged()
        followCountText?.text = "我的关注 (${allUsers.size}人)"
    }

    // 显示底部弹窗
    private fun showBottomSheet(user: User, position: Int) {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_user, null)
        dialog.setContentView(view)

        // 绑定新增的视图
        val sheetUserName = view.findViewById<TextView>(R.id.sheetUserName)
        val sheetDouyinId = view.findViewById<TextView>(R.id.sheetDouyinId)
        val closeButton = view.findViewById<ImageView>(R.id.closeButton)
        val copyButton = view.findViewById<ImageView>(R.id.copyButton)
        val specialSwitch = view.findViewById<Switch>(R.id.specialFollowSwitch)
        val remarkLayout = view.findViewById<View>(R.id.remarkLayout)
        val unfollowButton = view.findViewById<View>(R.id.unfollowButton)

        // 设置用户名（有备注显示备注，没有备注显示用户名）
        sheetUserName.text = if (user.remark.isNotEmpty()) user.remark else user.username

        // 设置抖音号（有备注时显示名字+抖音号，没有备注时只显示抖音号）
        sheetDouyinId.text = if (user.remark.isNotEmpty()) {
            "名字: ${user.username} | 抖音号: ${user.douyinId}"
        } else {
            "抖音号: ${user.douyinId}"
        }

        // 关闭按钮
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // 复制抖音号
        copyButton.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("抖音号", user.douyinId)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "已复制抖音号", Toast.LENGTH_SHORT).show()
        }

        // 特别关注开关
        specialSwitch.isChecked = user.isSpecialFollow
        specialSwitch.setOnCheckedChangeListener { _, isChecked ->
            user.isSpecialFollow = isChecked
            followAdapter?.notifyDataSetChanged()
            val msg = if (isChecked) "已设为特别关注" else "已取消特别关注"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        // 设置备注
        remarkLayout.setOnClickListener {
            dialog.dismiss()
            showRemarkDialog(user, position)
        }

        // 取消关注
        unfollowButton.setOnClickListener {
            user.isFollowed = false
            updateDisplayList(searchEditText?.text.toString() ?: "")
            dialog.dismiss()
            Toast.makeText(this, "已取消关注 ${user.username}", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    // 显示设置备注对话框
    private fun showRemarkDialog(user: User, position: Int) {
        val editText = EditText(this)
        editText.setText(user.remark)
        editText.hint = "请输入备注名"

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("设置备注")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                user.remark = editText.text.toString()
                followAdapter?.notifyItemChanged(position)
                Toast.makeText(this, "备注已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ViewPager适配器
    inner class TabPagerAdapter : RecyclerView.Adapter<TabPagerAdapter.PageViewHolder>() {

        inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = when (viewType) {
                1 -> createFollowPage(parent)  // 关注页面
                else -> createEmptyPage(parent, viewType)  // 其他空页面
            }
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            // 数据绑定在创建时已完成
        }

        override fun getItemCount(): Int = 4

        override fun getItemViewType(position: Int): Int = position

        // 创建关注页面
        private fun createFollowPage(parent: ViewGroup): View {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_follow, parent, false)

            val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
            searchEditText = view.findViewById(R.id.searchEditText)
            followCountText = view.findViewById(R.id.followCountText)
            val sortButton = view.findViewById<TextView>(R.id.sortButton)
            val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)

            recyclerView.layoutManager = LinearLayoutManager(parent.context)
            recyclerView.setHasFixedSize(true)
            recyclerView.setItemViewCacheSize(20)
            followAdapter = UserAdapter(displayUsers,
                onMoreClick = { user, position -> showBottomSheet(user, position) },
                onUserClick = { user -> Toast.makeText(this@MainActivity, "已选中{${user.username}}", Toast.LENGTH_SHORT).show() }
            )
            recyclerView.adapter = followAdapter

            updateDisplayList()

            searchEditText?.addTextChangedListener { text ->
                updateDisplayList(text.toString())
            }

            sortButton.setOnClickListener {
                sortDescending = !sortDescending
                sortButton.text = if (sortDescending) "按时间排序 ▼" else "按时间排序 ▲"
                updateDisplayList(searchEditText?.text.toString() ?: "")
            }

            swipeRefreshLayout.setColorSchemeColors(0xFFFE2C55.toInt())
            swipeRefreshLayout.setOnRefreshListener {
                Handler(Looper.getMainLooper()).postDelayed({
                    allUsers.forEach { it.isFollowed = true }
                    updateDisplayList(searchEditText?.text.toString() ?: "")
                    swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(this@MainActivity, "刷新成功", Toast.LENGTH_SHORT).show()
                }, 1000)
            }

            return view
        }

        // 创建空页面
        private fun createEmptyPage(parent: ViewGroup, position: Int): View {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_empty, parent, false)

            val emptyTitle = view.findViewById<TextView>(R.id.emptyTitle)
            val emptySubtitle = view.findViewById<TextView>(R.id.emptySubtitle)

            when (position) {
                0 -> emptyTitle.text = "暂无互关"
                2 -> emptyTitle.text = "暂无粉丝"
                3 -> {
                    emptyTitle.text = "暂无朋友"
                    emptySubtitle.text = "互相关注的人已迁移到「我-互关」下"
                    emptySubtitle.visibility = View.VISIBLE
                }
            }

            return view
        }
    }

    // App暂停时保存数据
    override fun onPause() {
        super.onPause()
        userDataManager.saveUsers(allUsers)
    }
}