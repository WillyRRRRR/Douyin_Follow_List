# 仿抖音关注页面 App

## 项目简介

这是一个仿抖音关注页面的 Android 应用，实现了关注列表的展示、交互功能以及本地数据持久化存储。

## 功能特性

### 核心功能
- **Tab 切换**：支持点击和左右滑动切换（互关、关注、粉丝、朋友）
- **关注列表**：展示用户头像（圆形）、用户名、VIP标识、关注状态
- **搜索功能**：支持按用户名或备注搜索
- **排序功能**：支持按关注时间正序/倒序排序
- **下拉刷新**：支持下拉刷新列表数据

### 交互功能
- **关注/取消关注**：点击按钮切换关注状态
- **特别关注**：设置特别关注后显示标签
- **设置备注**：为用户添加备注名
- **用户详情**：点击"..."弹出底部设置面板
- **复制抖音号**：支持一键复制用户抖音号

### 数据存储
- **本地持久化**：使用 SharedPreferences 存储用户数据
- **状态保持**：关闭App后数据不丢失

## 技术栈

- **开发语言**：Kotlin
- **最低SDK**：Android 7.0 (API 24)
- **目标SDK**：Android 14 (API 34)
- **主要组件**：
  - ViewPager2 - 实现页面滑动切换
  - RecyclerView - 高性能列表展示
  - SwipeRefreshLayout - 下拉刷新
  - BottomSheetDialog - 底部弹窗
  - ShapeableImageView - 圆形头像
  - SharedPreferences - 本地数据存储

## 项目结构

```
app/src/main/
├── java/com/example/douyinfollowlist/
│   ├── MainActivity.kt      # 主界面，包含Tab切换、ViewPager逻辑
│   ├── User.kt              # 用户数据类
│   ├── UserAdapter.kt       # RecyclerView适配器
│   └── UserDataManager.kt   # 本地数据存储管理
│
└── res/
    ├── layout/
    │   ├── activity_main.xml       # 主界面布局
    │   ├── fragment_follow.xml     # 关注页面布局
    │   ├── fragment_empty.xml      # 空页面布局
    │   ├── item_user.xml           # 用户列表项布局
    │   └── bottom_sheet_user.xml   # 底部弹窗布局
    │
    ├── drawable/                    # 图标和背景资源
    └── values/
        └── themes.xml              # 主题和样式
```

## 核心代码说明

### 1. 数据模型 (User.kt)
```kotlin
data class User(
    val id: Int,
    val username: String,
    val douyinId: String,
    val avatarResId: Int,
    val isVip: Boolean,
    var isFollowed: Boolean,
    var isSpecialFollow: Boolean,
    var remark: String,
    val followTime: Long
)
```

### 2. 列表适配器 (UserAdapter.kt)
- 使用 ViewHolder 模式优化性能
- 设置 `setHasStableIds(true)` 提升刷新效率
- 支持点击事件回调

### 3. 数据持久化 (UserDataManager.kt)
- 使用 SharedPreferences 存储 JSON 格式数据
- 支持保存、读取、清除操作

## 运行说明

1. 使用 Android Studio 打开项目
2. 等待 Gradle 同步完成
3. 连接设备或启动模拟器
4. 点击 Run 运行项目

## 截图展示

（在此添加App截图）

## 作者

（你的名字）

## 许可证

MIT License
