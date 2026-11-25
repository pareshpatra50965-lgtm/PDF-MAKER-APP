package com.example.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

class DragDropListState(
    val lazyListState: LazyListState,
    private val keyPrefix: String,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit
) {
    var draggingItemKey by mutableStateOf<Any?>(null)
        private set

    var draggingItemOffset by mutableFloatStateOf(0f)
        private set

    fun onDragStart(itemKey: Any) {
        draggingItemKey = itemKey
        draggingItemOffset = 0f
    }

    fun onDrag(deltaY: Float) {
        val activeKey = draggingItemKey ?: return
        draggingItemOffset += deltaY

        val currentItemInfo = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == activeKey } ?: return

        val currentCenter = currentItemInfo.offset + draggingItemOffset + currentItemInfo.size / 2f
        val draggableItems = lazyListState.layoutInfo.visibleItemsInfo
            .filter { (it.key as? String)?.startsWith(keyPrefix) == true }

        val currentPosInDraggable = draggableItems.indexOfFirst { it.key == activeKey }
        if (currentPosInDraggable == -1) return

        val targetItem: LazyListItemInfo? = when {
            draggingItemOffset > 0f && currentPosInDraggable < draggableItems.lastIndex -> {
                val next = draggableItems[currentPosInDraggable + 1]
                val nextMid = next.offset + next.size / 2f
                if (currentCenter > nextMid) next else null
            }
            draggingItemOffset < 0f && currentPosInDraggable > 0 -> {
                val prev = draggableItems[currentPosInDraggable - 1]
                val prevMid = prev.offset + prev.size / 2f
                if (currentCenter < prevMid) prev else null
            }
            else -> null
        }

        if (targetItem != null) {
            val fromIndex = extractIndex(currentItemInfo)
            val toIndex = extractIndex(targetItem)
            if (fromIndex != null && toIndex != null) {
                val offsetDelta = targetItem.offset - currentItemInfo.offset
                onMove(fromIndex, toIndex)
                draggingItemOffset -= offsetDelta
            }
        }
    }

    fun onDragEnd() {
        draggingItemKey = null
        draggingItemOffset = 0f
    }

    private fun extractIndex(itemInfo: LazyListItemInfo): Int? {
        val firstDraggableLazyIndex = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { (it.key as? String)?.startsWith(keyPrefix) == true }
            ?.let { firstVisible ->
                // Look up index offset via contentType or prefix index; instead we store index via map lookup below
                null
            }
        return firstDraggableLazyIndex
    }
}

class IndexedDragDropState(
    val lazyListState: LazyListState,
    private val keyPrefix: String,
    private val indexForKey: (Any) -> Int,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit
) {
    var draggingItemKey by mutableStateOf<Any?>(null)
        private set

    var draggingItemOffset by mutableFloatStateOf(0f)
        private set

    fun onDragStart(itemKey: Any) {
        draggingItemKey = itemKey
        draggingItemOffset = 0f
    }

    fun onDrag(deltaY: Float) {
        val activeKey = draggingItemKey ?: return
        draggingItemOffset += deltaY

        val currentItemInfo = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == activeKey } ?: return

        val currentCenter = currentItemInfo.offset + draggingItemOffset + currentItemInfo.size / 2f
        val draggableItems = lazyListState.layoutInfo.visibleItemsInfo
            .filter { (it.key as? String)?.startsWith(keyPrefix) == true }

        val pos = draggableItems.indexOfFirst { it.key == activeKey }
        if (pos == -1) return

        val target = when {
            draggingItemOffset > 0f && pos < draggableItems.lastIndex -> {
                val next = draggableItems[pos + 1]
                if (currentCenter > next.offset + next.size / 2f) next else null
            }
            draggingItemOffset < 0f && pos > 0 -> {
                val prev = draggableItems[pos - 1]
                if (currentCenter < prev.offset + prev.size / 2f) prev else null
            }
            else -> null
        }

        if (target != null) {
            val fromIndex = indexForKey(activeKey)
            val toIndex = indexForKey(target.key)
            if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
                val delta = target.offset - currentItemInfo.offset
                onMove(fromIndex, toIndex)
                draggingItemOffset -= delta
            }
        }
    }

    fun onDragEnd() {
        draggingItemKey = null
        draggingItemOffset = 0f
    }
}

@Composable
fun rememberIndexedDragDropState(
    lazyListState: LazyListState,
    keyPrefix: String,
    indexForKey: (Any) -> Int,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
): IndexedDragDropState {
    return remember(lazyListState) {
        IndexedDragDropState(
            lazyListState = lazyListState,
            keyPrefix = keyPrefix,
            indexForKey = indexForKey,
            onMove = onMove
        )
    }
}

fun Modifier.dragHandleReorder(
    itemKey: Any,
    state: IndexedDragDropState
): Modifier = this.pointerInput(itemKey, state) {
    detectDragGestures(
        onDragStart = { state.onDragStart(itemKey) },
        onDragEnd = { state.onDragEnd() },
        onDragCancel = { state.onDragEnd() },
        onDrag = { change, dragAmount ->
            change.consume()
            state.onDrag(dragAmount.y)
        }
    )
}

fun Modifier.longPressReorder(
    itemKey: Any,
    state: IndexedDragDropState
): Modifier = this.pointerInput(itemKey, state) {
    detectDragGesturesAfterLongPress(
        onDragStart = { state.onDragStart(itemKey) },
        onDragEnd = { state.onDragEnd() },
        onDragCancel = { state.onDragEnd() },
        onDrag = { change, dragAmount ->
            change.consume()
            state.onDrag(dragAmount.y)
        }
    )
}
