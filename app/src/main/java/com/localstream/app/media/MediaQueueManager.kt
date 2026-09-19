package com.localstream.app.media

import com.localstream.app.domain.model.MediaFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections

class MediaQueueManager {

    private val _queue = MutableStateFlow<List<MediaFile>>(emptyList())
    val queue: StateFlow<List<MediaFile>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    val currentItem: MediaFile?
        get() {
            val idx = _currentIndex.value
            val list = _queue.value
            return if (idx in list.indices) list[idx] else null
        }

    fun setQueue(items: List<MediaFile>, startIndex: Int = 0) {
        _queue.value = items
        _currentIndex.value = startIndex.coerceIn(-1, items.size - 1)
    }

    fun addToQueue(item: MediaFile) {
        val updated = _queue.value.toMutableList().apply { add(item) }
        _queue.value = updated
        if (_currentIndex.value == -1) {
            _currentIndex.value = 0
        }
    }

    fun removeFromQueue(index: Int) {
        val current = _queue.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _queue.value = current

            val activeIdx = _currentIndex.value
            when {
                current.isEmpty() -> _currentIndex.value = -1
                index < activeIdx -> _currentIndex.value = activeIdx - 1
                index == activeIdx -> _currentIndex.value = activeIdx.coerceAtMost(current.size - 1)
            }
        }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val current = _queue.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            Collections.swap(current, fromIndex, toIndex)
            _queue.value = current

            val active = _currentIndex.value
            if (active == fromIndex) {
                _currentIndex.value = toIndex
            } else if (active == toIndex) {
                _currentIndex.value = fromIndex
            }
        }
    }

    fun next(): MediaFile? {
        val list = _queue.value
        val nextIdx = _currentIndex.value + 1
        return if (nextIdx in list.indices) {
            _currentIndex.value = nextIdx
            list[nextIdx]
        } else {
            null
        }
    }

    fun previous(): MediaFile? {
        val list = _queue.value
        val prevIdx = _currentIndex.value - 1
        return if (prevIdx in list.indices) {
            _currentIndex.value = prevIdx
            list[prevIdx]
        } else {
            null
        }
    }

    fun jumpToIndex(index: Int): MediaFile? {
        val list = _queue.value
        return if (index in list.indices) {
            _currentIndex.value = index
            list[index]
        } else {
            null
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentIndex.value = -1
    }

    fun hasNext(): Boolean = _currentIndex.value + 1 < _queue.value.size
    fun hasPrevious(): Boolean = _currentIndex.value > 0
}
