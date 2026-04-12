package com.melihcolpan.musclemap.rendering

import androidx.compose.ui.graphics.Path
import com.melihcolpan.musclemap.core.PathBuilder

class PathCache {
    private val cache = mutableMapOf<String, Path>()

    // Uses standard lock for thread-safety like Swift's NSLock
    private val lock = Any()

    fun path(svgPath: String, scale: Float, offsetX: Float, offsetY: Float): Path {
        val key = "${svgPath.hashCode()}-$scale-$offsetX-$offsetY"
        
        synchronized(lock) {
            val cached = cache[key]
            if (cached != null) return cached
        }
        
        val built = PathBuilder.buildPath(svgPath, scale, offsetX, offsetY)
        
        synchronized(lock) {
            cache[key] = built
        }
        
        return built
    }

    fun invalidate() {
        synchronized(lock) {
            cache.clear()
        }
    }
}
