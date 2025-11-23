# 仿抖音关注页面 - 学习总结

## 问题一：ViewPager2 滑动与 Tab 下划线指示器同步

### 问题描述
实现 Tab 左右滑动切换时，需要让下划线指示器跟随手指滑动平滑移动，并且要精确对齐到每个 Tab 文字的正下方。

### 遇到的困难
1. 添加返回按钮后，Tab 区域的起始位置发生偏移，导致下划线位置计算错误
2. 下划线指示器的 `translationX` 需要 Float 类型，而计算结果是 Int 类型，导致类型不匹配错误
3. 初始化时下划线位置不正确，因为布局还未完成测量

### 解决方案
```kotlin
private fun updateIndicatorPosition(position: Int, positionOffset: Float) {
    val backButtonWidth = (40 * resources.displayMetrics.density)
    val screenWidth = resources.displayMetrics.widthPixels.toFloat()
    val tabWidth = (screenWidth - backButtonWidth) / 4
    val indicatorWidth = tabIndicator.width.toFloat()
    val offset = backButtonWidth + (position + positionOffset) * tabWidth + (tabWidth - indicatorWidth) / 2
    tabIndicator.translationX = offset
}
```

### 优化思路
- 使用 `post {}` 确保在布局完成后再计算位置
- 将所有数值转换为 Float 统一计算，避免类型转换问题
- 抽取公共方法 `updateIndicatorPosition()`，在滑动和初始化时复用

---

## 问题二：RecyclerView 列表性能优化

### 问题描述
当列表数据量较大（100+条）时，需要确保滑动流畅，不出现卡顿。

### 遇到的困难
1. 每次数据变化都调用 `notifyDataSetChanged()` 会导致整个列表重绘
2. ViewHolder 频繁创建和销毁消耗性能
3. 图片加载可能导致滑动卡顿

### 解决方案
```kotlin
// 1. 设置固定大小，减少测量计算
recyclerView.setHasFixedSize(true)

// 2. 增加缓存大小
recyclerView.setItemViewCacheSize(20)

// 3. 启用稳定ID，让RecyclerView追踪每个项
class UserAdapter(...) : RecyclerView.Adapter<...>() {
    init {
        setHasStableIds(true)
    }
    
    override fun getItemId(position: Int): Long = userList[position].id.toLong()
}
```

### 优化思路
- 使用 `setHasFixedSize(true)` 告诉系统列表项大小固定
- 增加 `ItemViewCacheSize` 缓存更多 ViewHolder
- 设置 `setHasStableIds(true)` 让系统能追踪每个项，只更新变化的部分
- 未来可考虑使用 DiffUtil 实现更精细的局部刷新

---

## 问题三：本地数据持久化与状态同步

### 问题描述
需要将用户的关注状态、特别关注、备注等信息保存到本地，关闭App后再打开时数据不丢失。

### 遇到的困难
1. 需要选择合适的存储方式（SharedPreferences vs SQLite vs Room）
2. 数据模型包含多个字段，需要考虑序列化方式
3. 需要在合适的时机保存数据，避免数据丢失

### 解决方案
```kotlin
class UserDataManager(context: Context) {
    private val prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)

    fun saveUsers(users: List<User>) {
        val jsonArray = JSONArray()
        for (user in users) {
            val jsonObject = JSONObject()
            jsonObject.put("id", user.id)
            jsonObject.put("username", user.username)
            // ... 其他字段
            jsonArray.put(jsonObject)
        }
        prefs.edit().putString("users", jsonArray.toString()).apply()
    }

    fun loadUsers(): List<User>? {
        val jsonString = prefs.getString("users", null) ?: return null
        // 解析JSON并返回用户列表
    }
}
```

在 `onPause()` 中自动保存：
```kotlin
override fun onPause() {
    super.onPause()
    userDataManager.saveUsers(allUsers)
}
```

### 优化思路
- 对于简单数据结构，SharedPreferences + JSON 是轻量级的解决方案
- 在 `onPause()` 保存确保用户切换App或按Home键时数据不丢失
- 使用 `apply()` 异步写入，不阻塞主线程
- 未来数据量更大时可考虑迁移到 Room 数据库

---

## 总结

通过这个项目，我学习到了：

1. **Android UI 组件**：ViewPager2、RecyclerView、BottomSheetDialog 等组件的使用
2. **Kotlin 语言特性**：data class、lambda 表达式、高阶函数等
3. **性能优化**：列表性能优化的多种技巧
4. **数据持久化**：SharedPreferences 的使用和 JSON 序列化
5. **项目架构**：将代码按职责分离到不同的类中，提高可维护性
