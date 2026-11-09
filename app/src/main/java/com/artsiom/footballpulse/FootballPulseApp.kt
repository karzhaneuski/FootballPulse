package com.artsiom.footballpulse

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache

class FootballPulseApp : Application(), ImageLoaderFactory {
    val container: AppContainer by lazy { AppContainer() }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(SvgDecoder.Factory()) }
        .memoryCache { MemoryCache.Builder(this).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .build()
        }
        .build()
}
