package com.example.douyinfollowlist

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule

/**
 * Glide 全局配置
 * 优化图片加载性能和内存使用
 */
@GlideModule
class GlideConfiguration : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // 配置内存缓存大小：20MB
        val memoryCacheSizeBytes = 1024 * 1024 * 20
        builder.setMemoryCache(LruResourceCache(memoryCacheSizeBytes.toLong()))

        // 配置磁盘缓存大小：100MB
        val diskCacheSizeBytes = 1024 * 1024 * 100
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, diskCacheSizeBytes.toLong()))
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false // 禁用清单解析，提升初始化速度
    }
}