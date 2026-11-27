package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.DeckLinkStateStore
import com.example.model.LogStatus
import com.example.model.UiNodeSummary

class DeckLinkAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: DeckLinkAccessibilityService? = null
            private set

        fun isConnected(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        DeckLinkStateStore.updateReadiness { it.copy(accessibilityConnected = true) }
        DeckLinkStateStore.logCommand(
            "local",
            "Remote Touch Ready",
            "Accessibility service connected",
            LogStatus.SYSTEM
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // On-demand node inspection and gesture dispatch; no passive scraping
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        DeckLinkStateStore.updateReadiness { it.copy(accessibilityConnected = false) }
    }

    fun performNormalizedTap(normX: Float, normY: Float, durationMs: Long = 70L): Boolean {
        val metrics = resources.displayMetrics
        val px = (normX.coerceIn(0f, 1f) * metrics.widthPixels).coerceIn(1f, metrics.widthPixels - 1f)
        val py = (normY.coerceIn(0f, 1f) * metrics.heightPixels).coerceIn(1f, metrics.heightPixels - 1f)
        val path = Path().apply { moveTo(px, py) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(30L, 1800L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun performNormalizedSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 260L
    ): Boolean {
        val metrics = resources.displayMetrics
        val x1 = (startX.coerceIn(0f, 1f) * metrics.widthPixels).coerceIn(1f, metrics.widthPixels - 1f)
        val y1 = (startY.coerceIn(0f, 1f) * metrics.heightPixels).coerceIn(1f, metrics.heightPixels - 1f)
        val x2 = (endX.coerceIn(0f, 1f) * metrics.widthPixels).coerceIn(1f, metrics.widthPixels - 1f)
        val y2 = (endY.coerceIn(0f, 1f) * metrics.heightPixels).coerceIn(1f, metrics.heightPixels - 1f)

        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(80L, 1500L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun performGlobalKey(actionName: String): Boolean {
        val actionId = when (actionName.lowercase()) {
            "back" -> GLOBAL_ACTION_BACK
            "home" -> GLOBAL_ACTION_HOME
            "recents" -> GLOBAL_ACTION_RECENTS
            "notifications" -> GLOBAL_ACTION_NOTIFICATIONS
            "quick_settings" -> GLOBAL_ACTION_QUICK_SETTINGS
            "power_dialog" -> GLOBAL_ACTION_POWER_DIALOG
            "lock_screen" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) GLOBAL_ACTION_LOCK_SCREEN else return false
            "screenshot" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) GLOBAL_ACTION_TAKE_SCREENSHOT else return false
            else -> return false
        }
        return performGlobalAction(actionId)
    }

    fun inputText(text: String, append: Boolean = true): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: findFirstEditable(root) ?: return false
        val existing = if (append) focused.text?.toString().orEmpty() else ""
        val combined = (existing + text).take(8000)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, combined)
        }
        val success = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (!success) {
            // Try paste if ACTION_SET_TEXT is unsupported by the target field
            return focused.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }
        return true
    }

    fun clearFocusedText(): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: findFirstEditable(root) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun inspectInteractiveNodes(limit: Int = 45): List<UiNodeSummary> {
        val root = rootInActiveWindow ?: return emptyList()
        val metrics = resources.displayMetrics
        val screenW = metrics.widthPixels.coerceAtLeast(1).toFloat()
        val screenH = metrics.heightPixels.coerceAtLeast(1).toFloat()
        val results = mutableListOf<UiNodeSummary>()
        collectNodes(root, screenW, screenH, results, limit)
        return results
    }

    fun clickInteractiveNode(index: Int): Boolean {
        val nodes = inspectInteractiveNodes(60)
        val target = nodes.firstOrNull { it.index == index } ?: return false
        return performNormalizedTap(target.centerX, target.centerY, 75L)
    }

    private fun collectNodes(
        node: AccessibilityNodeInfo?,
        screenW: Float,
        screenH: Float,
        out: MutableList<UiNodeSummary>,
        limit: Int
    ) {
        if (node == null || out.size >= limit) return
        val label = (node.text?.toString() ?: node.contentDescription?.toString()).orEmpty().trim()
        val isInteresting = (node.isClickable || node.isEditable) && label.isNotEmpty()
        if (isInteresting && node.isVisibleToUser) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 4 && rect.height() > 4) {
                val cx = (rect.exactCenterX() / screenW).coerceIn(0f, 1f)
                val cy = (rect.exactCenterY() / screenH).coerceIn(0f, 1f)
                out.add(
                    UiNodeSummary(
                        index = out.size,
                        label = label.take(64),
                        className = node.className?.toString()?.substringAfterLast('.') ?: "View",
                        clickable = node.isClickable,
                        editable = node.isEditable,
                        centerX = cx,
                        centerY = cy
                    )
                )
            }
        }
        for (i in 0 until node.childCount) {
            if (out.size >= limit) break
            collectNodes(node.getChild(i), screenW, screenH, out, limit)
        }
    }

    private fun findFirstEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable && node.isVisibleToUser) return node
        for (i in 0 until node.childCount) {
            val found = findFirstEditable(node.getChild(i))
            if (found != null) return found
        }
        return null
    }
}
