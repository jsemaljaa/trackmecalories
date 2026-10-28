package com.jsemalja.trackmecalories.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A small in-memory cache that parallel coroutines can share. With [maxSize], the oldest entry
 * (by first insertion) is dropped once it's exceeded.
 */
internal class MemoryCache<K, V>(private val maxSize: Int = Int.MAX_VALUE) {
    private val mutex = Mutex()
    private val map = LinkedHashMap<K, V>()

    suspend fun get(key: K): V? = mutex.withLock { map[key] }

    suspend fun put(key: K, value: V) = mutex.withLock {
        map[key] = value
        if (map.size > maxSize) map.remove(map.keys.first())
    }
}
