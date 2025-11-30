package com.example.douyinfollowlist

import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabIndicator: View
    private lateinit var tabs: List<TextView>

    private lateinit var userDataManager: UserDataManager

    private val allUsers = mutableListOf<User>()
    private val displayUsers = mutableListOf<User>()

    private var sortDescending = true

    private var followAdapter: UserAdapter? = null
    private var searchEditText: EditText? = null
    private var followCountText: TextView? = null
    private var loadingProgressBar: ProgressBar? = null

    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false

    private var updateJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userDataManager = UserDataManager(this)

        val tabMutual = findViewById<TextView>(R.id.tabMutual)
        val tabFollow = findViewById<TextView>(R.id.tabFollow)
        val tabFans = findViewById<TextView>(R.id.tabFans)
        val tabFriends = findViewById<TextView>(R.id.tabFriends)
        tabs = listOf(tabMutual, tabFollow, tabFans, tabFriends)

        tabIndicator = findViewById(R.id.tabIndicator)
        viewPager = findViewById(R.id.viewPager)

        viewPager.adapter = TabPagerAdapter()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabStyle(position)
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                updateIndicatorPosition(position, positionOffset)
            }
        })

        tabs.forEachIndexed { index, tab ->
            tab.setOnClickListener {
                viewPager.currentItem = index
            }
        }

        viewPager.setCurrentItem(1, false)
        updateTabStyle(1)

        tabIndicator.post {
            updateIndicatorPosition(1, 0f)
        }

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    /**
     * 核心功能：分页加载更多用户
     * 优化点：
     * 1. 在协程中异步执行，不阻塞主线程
     * 2. 防止重复加载
     * 3. 显示加载状态
     */
    private fun loadMoreUsers() {
        if (isLoading || isLastPage) return

        isLoading = true
        loadingProgressBar?.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // 在IO线程请求数据
                val newUsers = withContext(Dispatchers.IO) {
                    MockServer.getUsers(currentPage)
                }

                if (newUsers.isNotEmpty()) {
                    allUsers.addAll(newUsers)
                    updateDisplayList(searchEditText?.text.toString() ?: "")
                    currentPage++
                } else {
                    isLastPage = true
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "没有更多内容了", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "加载失败", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isLoading = false
                withContext(Dispatchers.Main) {
                    loadingProgressBar?.visibility = View.GONE
                }
            }
        }
    }

    /**
     * 核心优化：使用协程+DiffUtil更新列表
     * 优化点：
     * 1. 耗时的过滤和排序操作在IO线程执行
     * 2. 使用DiffUtil智能刷新，只更新变化的部分
     * 3. 支持取消上一次未完成的更新任务
     */
    private fun updateDisplayList(searchText: String = "") {
        // 取消之前的更新任务
        updateJob?.cancel()

        updateJob = lifecycleScope.launch {
            // 在Default线程进行耗时的过滤和排序
            val filteredAndSorted = withContext(Dispatchers.Default) {
                // 1. 过滤
                val filtered = if (searchText.isEmpty()) {
                    allUsers.toList()
                } else {
                    allUsers.filter {
                        it.username.contains(searchText, ignoreCase = true) ||
                                it.remark.contains(searchText, ignoreCase = true) ||
                                it.douyinId.contains(searchText, ignoreCase = true)
                    }
                }

                // 2. 排序
                if (sortDescending) {
                    filtered.sortedByDescending { it.followTime }
                } else {
                    filtered.sortedBy { it.followTime }
                }
            }

            // 回到主线程更新UI
            withContext(Dispatchers.Main) {
                // 使用DiffUtil智能更新，而不是notifyDataSetChanged
                followAdapter?.updateDataAsync(filteredAndSorted) {
                    followCountText?.text = "我的关注 (${allUsers.size}人)"
                }
            }
        }
    }

    private fun updateIndicatorPosition(position: Int, positionOffset: Float) {
        val backButtonWidth = (40 * resources.displayMetrics.density)
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val tabWidth = (screenWidth - backButtonWidth) / 4
        val indicatorWidth = tabIndicator.width.toFloat()
        val offset = backButtonWidth + (position + positionOffset) * tabWidth + (tabWidth - indicatorWidth) / 2
        tabIndicator.translationX = offset
    }

    private fun updateTabStyle(selectedIndex: Int) {
        tabs.forEachIndexed { index, tab ->
            if (index == selectedIndex) {
                tab.setTextColor(0xFF000000.toInt())
            } else {
                tab.setTextColor(0xFF999999.toInt())
            }
        }
    }

    private fun showBottomSheet(user: User, position: Int) {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_user, null)
        dialog.setContentView(view)

        val sheetUserName = view.findViewById<TextView>(R.id.sheetUserName)
        val sheetDouyinId = view.findViewById<TextView>(R.id.sheetDouyinId)
        val closeButton = view.findViewById<ImageView>(R.id.closeButton)
        val copyButton = view.findViewById<ImageView>(R.id.copyButton)
        val specialSwitch = view.findViewById<Switch>(R.id.specialFollowSwitch)
        val remarkLayout = view.findViewById<View>(R.id.remarkLayout)
        val unfollowButton = view.findViewById<View>(R.id.unfollowButton)

        sheetUserName.text = if (user.remark.isNotEmpty()) user.remark else user.username

        sheetDouyinId.text = if (user.remark.isNotEmpty()) {
            "名字: ${user.username} | 抖音号: ${user.douyinId}"
        } else {
            "抖音号: ${user.douyinId}"
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        copyButton.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("抖音号", user.douyinId)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "已复制抖音号", Toast.LENGTH_SHORT).show()
        }

        specialSwitch.isChecked = user.isSpecialFollow
        specialSwitch.setOnCheckedChangeListener { _, isChecked ->
            user.isSpecialFollow = isChecked
            // 使用局部刷新
            val index = displayUsers.indexOfFirst { it.id == user.id }
            if (index != -1) {
                followAdapter?.notifyItemChanged(index, mapOf("isSpecialFollow" to isChecked))
            }
            val msg = if (isChecked) "已设为特别关注" else "已取消特别关注"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        remarkLayout.setOnClickListener {
            dialog.dismiss()
            showRemarkDialog(user, position)
        }

        unfollowButton.setOnClickListener {
            user.isFollowed = false
            updateDisplayList(searchEditText?.text.toString() ?: "")
            dialog.dismiss()
            Toast.makeText(this, "已取消关注 ${user.username}", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun showRemarkDialog(user: User, position: Int) {
        val editText = EditText(this)
        editText.setText(user.remark)
        editText.hint = "请输入备注名"

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("设置备注")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                user.remark = editText.text.toString()
                // 使用局部刷新
                val index = displayUsers.indexOfFirst { it.id == user.id }
                if (index != -1) {
                    followAdapter?.notifyItemChanged(index, mapOf("remark" to user.remark))
                }
                Toast.makeText(this, "备注已保存", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    inner class TabPagerAdapter : RecyclerView.Adapter<TabPagerAdapter.PageViewHolder>() {

        inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = when (viewType) {
                1 -> createFollowPage(parent)
                else -> createEmptyPage(parent, viewType)
            }
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {}

        override fun getItemCount(): Int = 4

        override fun getItemViewType(position: Int): Int = position

        private fun createFollowPage(parent: ViewGroup): View {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_follow, parent, false)

            val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
            searchEditText = view.findViewById(R.id.searchEditText)
            followCountText = view.findViewById(R.id.followCountText)
            val sortButton = view.findViewById<TextView>(R.id.sortButton)
            val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)

            // 添加底部加载指示器（需要在布局中添加）
            loadingProgressBar = view.findViewById(R.id.loadingProgressBar)

            // RecyclerView 优化配置
            val layoutManager = LinearLayoutManager(parent.context)
            recyclerView.layoutManager = layoutManager
            recyclerView.setHasFixedSize(true)
            recyclerView.setItemViewCacheSize(20) // 增加ViewHolder缓存


            // 设置RecyclerView的缓冲区大小，提前加载更多item
            recyclerView.recycledViewPool.setMaxRecycledViews(0, 20)

            followAdapter = UserAdapter(displayUsers,
                onMoreClick = { user, position -> showBottomSheet(user, position) },
                onUserClick = { user -> Toast.makeText(this@MainActivity, "已选中{${user.username}}", Toast.LENGTH_SHORT).show() }
            )
            recyclerView.adapter = followAdapter

            // 滚动监听：实现上拉加载更多
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    // dy > 0 表示向下滚动
                    if (dy > 0) {
                        val visibleItemCount = layoutManager.childCount
                        val totalItemCount = layoutManager.itemCount
                        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                        // 提前2个item就触发加载，提升用户体验
                        if (!isLoading && !isLastPage) {
                            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                                loadMoreUsers()
                            }
                        }
                    }
                }
            })

            // 初始加载第一页
            if (allUsers.isEmpty()) {
                loadMoreUsers()
            } else {
                updateDisplayList()
            }

            // 搜索功能
            searchEditText?.addTextChangedListener { text ->
                updateDisplayList(text.toString())
            }

            // 排序功能
            sortButton.setOnClickListener {
                sortDescending = !sortDescending
                sortButton.text = if (sortDescending) "按时间排序 ▼" else "按时间排序 ▲"
                updateDisplayList(searchEditText?.text.toString() ?: "")
            }

            // 下拉刷新
            swipeRefreshLayout.setColorSchemeColors(0xFFFE2C55.toInt())
            swipeRefreshLayout.setOnRefreshListener {
                lifecycleScope.launch {
                    try {
                        // 重置状态
                        allUsers.clear()
                        displayUsers.clear()
                        currentPage = 1
                        isLastPage = false

                        // 重新加载第一页
                        val newUsers = withContext(Dispatchers.IO) {
                            MockServer.getUsers(1)
                        }

                        allUsers.addAll(newUsers)
                        currentPage++

                        updateDisplayList(searchEditText?.text.toString() ?: "")

                        Toast.makeText(this@MainActivity, "刷新成功", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@MainActivity, "刷新失败", Toast.LENGTH_SHORT).show()
                    } finally {
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            }

            return view
        }

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

    override fun onPause() {
        super.onPause()
        // 只保存前100条数据，避免存储过大
        userDataManager.saveUsers(allUsers.take(100))
    }

    override fun onDestroy() {
        super.onDestroy()
        // 取消所有协程任务
        updateJob?.cancel()
    }
}