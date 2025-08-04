package com.tomato.utils;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.Bundle;
import android.util.Log;
import android.graphics.Rect;
import android.graphics.Path;
import android.accessibilityservice.GestureDescription;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public class AccessibilityActionUtils {

    private static final String TAG = AccessibilityConfig.TAG + ".ActionUtils";

    /**
     * 尝试点击指定的节点。
     * 优先使用 ACTION_CLICK，如果失败或节点不可直接点击，则回退到手势模拟点击。
     * 点击前会检查节点是否可见且启用。
     *
     * @param service     AccessibilityService 实例，用于 dispatchGesture。
     * @param targetNode  要点击的目标节点。此节点应由调用者在使用后回收。
     * @return 如果点击操作被认为成功发起了，则返回 true；否则返回 false。
     */
    public static boolean performClick(AccessibilityService service, AccessibilityNodeInfo targetNode){
        if (targetNode == null) {
            Log.w(TAG, "performClick: Target node is null.");
            return false;
        }

        // 关键检查：即使 NodeCondition 可能已检查，操作前再次确认
        if (!targetNode.isVisibleToUser()) {
            Log.w(TAG, "performClick: Target node (ID: " + targetNode.getViewIdResourceName() + ") is not visible to user. Click cancelled.");
            return false;
        }
        if (!targetNode.isEnabled()) {
            Log.w(TAG, "performClick: Target node (ID: " + targetNode.getViewIdResourceName() + ") is not enabled. Click cancelled.");
            return false;
        }
        // 1. 尝试 ACTION_CLICK
        if (targetNode.isClickable()) {
            Log.i(TAG, "performClick: Node (ID: " + targetNode.getViewIdResourceName() + ") is clickable, attempting ACTION_CLICK.");
            if (targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                Log.i(TAG, "performClick: ACTION_CLICK successful on node (ID: " + targetNode.getViewIdResourceName() + ").");
                return true;
            } else {
                Log.w(TAG, "performClick: ACTION_CLICK failed on node (ID: " + targetNode.getViewIdResourceName() + "). Falling back to gesture.");
            }
        } else {
            Log.i(TAG, "performClick: Node (ID: " + targetNode.getViewIdResourceName() + ") is not directly clickable (isClickable=false). Attempting gesture click.");
        }
        // 2. 如果 ACTION_CLICK 失败或不可点击，回退到手势模拟点击
        Rect bounds = new Rect();
        targetNode.getBoundsInScreen(bounds);

        if (bounds.width() <= 0 || bounds.height() <= 0) { // 使用 <= 0 更严谨
            Log.e(TAG, "performClick: Node (ID: " + targetNode.getViewIdResourceName() + ") has invalid bounds for gesture click: " + bounds);
            return false;
        }

        Log.i(TAG, "performClick: Performing gesture click on node (ID: " + targetNode.getViewIdResourceName() + ") at center (" + bounds.centerX() + ", " + bounds.centerY() + ").");
        return clickByGesture(service, bounds.centerX(), bounds.centerY(), null);
    }

    /**
     * 在指定屏幕坐标执行模拟手势点击。
     *
     * @param service  AccessibilityService 实例，用于 dispatchGesture。
     * @param x        X坐标。
     * @param y        Y坐标。
     * @param callback 可选的手势结果回调。
     * @return 如果手势成功派发则返回 true，否则返回 false。
     */
    public static boolean clickByGesture(AccessibilityService service, int x, int y, AccessibilityService.GestureResultCallback callback) {
        Log.i(TAG, "clickByGesture: Performing gesture click at (" + x + ", " + y + ").");
        Path path = new Path();
        path.moveTo(x, y);

        // 短暂按压模拟点击，50ms 可能适合大多数情况
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 50);
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder().addStroke(stroke);

        AccessibilityService.GestureResultCallback localCallback = callback != null ? callback : new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                Log.i(TAG, "clickByGesture: Gesture completed at (" + x + ", " + y + ").");
            }
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                Log.w(TAG, "clickByGesture: Gesture cancelled at (" + x + ", " + y + ").");
            }
        };
        return service.dispatchGesture(gestureBuilder.build(), localCallback, null);
    }

    /**
     * 执行滑动（swipe）手势。
     *
     * @param service     AccessibilityService 实例。
     * @param startX      起始X坐标。
     * @param startY      起始Y坐标。
     * @param endX        结束X坐标。
     * @param endY        结束Y坐标。
     * @param durationMs  滑动持续时间（毫秒）。
     * @param callback    可选的手势结果回调。
     * @return 如果手势成功派发则返回 true，否则返回 false。
     */
    public static boolean performSwipe(AccessibilityService service,
                                       int startX, int startY,
                                       int endX, int endY,
                                       long durationMs,
                                       AccessibilityService.GestureResultCallback callback) {
        if (durationMs <= 0) {
            Log.w(TAG, "performSwipe: Duration must be positive. Got " + durationMs);
            return false;
        }
        Log.i(TAG, "performSwipe: From (" + startX + "," + startY + ") to (" + endX + "," + endY + ") in " + durationMs + "ms.");

        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);

        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, durationMs);
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder().addStroke(stroke);

        AccessibilityService.GestureResultCallback localCallback = callback != null ? callback : new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                Log.i(TAG, "performSwipe: Gesture completed.");
            }
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                Log.w(TAG, "performSwipe: Gesture cancelled.");
            }
        };
        return service.dispatchGesture(gestureBuilder.build(), localCallback, null);
    }

    /**
     * [新增功能] 在指定的可编辑节点（如 EditText）中输入文本。
     * 此方法会先尝试为节点获取焦点，然后执行设置文本的操作。
     *
     * @param targetNode  目标输入框节点。此节点应由调用者在使用后回收。
     * @param textToInput 要输入的文本。
     * @return 如果设置文本的操作成功发起，则返回 true；否则返回 false。
     */
    public static boolean performInput(AccessibilityService service, AccessibilityNodeInfo targetNode, String textToInput) {
        if (targetNode == null) {
            Log.w(TAG, "performInput: 目标节点为空。");
            return false;
        }
        if (textToInput == null) {
            Log.w(TAG, "performInput: 将要输入的文本为空。");
            return false;
        }
        // 步骤 1: 验证节点是否可编辑
        if (!targetNode.isEditable()) {
            Log.w(TAG, "performInput: Node (ID: " + targetNode.getViewIdResourceName() + ") is not editable. Input cancelled.");
            return false;
        }

        // 步骤 2: 尝试获取焦点，使用三级回退策略 (FOCUS -> CLICK -> GESTURE)
        boolean focusActionInitiated = targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        if (!focusActionInitiated) {
            Log.d(TAG, "performInput: ACTION_FOCUS failed, trying ACTION_CLICK...");
            focusActionInitiated = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        if (!focusActionInitiated) {
            Log.d(TAG, "performInput: ACTION_CLICK failed, trying gesture click as a last resort...");
            Rect bounds = new Rect();
            targetNode.getBoundsInScreen(bounds);
            if (bounds.width() > 0 && bounds.height() > 0) {
                // 注意: 手势是异步的。这是一种最大努力的尝试。
                // 返回值仅表示手势是否成功派发，不保证焦点已立即获取。
                focusActionInitiated = clickByGesture(service, bounds.centerX(), bounds.centerY(), null);
            } else {
                Log.w(TAG, "performInput: Node has invalid bounds for gesture click, skipping gesture.");
            }
        }

        if (!focusActionInitiated) {
            Log.e(TAG, "performInput: Failed to initiate focus action via FOCUS, CLICK, or GESTURE.");
            return false;
        }

        Log.i(TAG, "performInput: Focus action initiated for node (ID: " + targetNode.getViewIdResourceName() + "). Proceeding to set text.");

        // 步骤 3: 准备参数并执行 ACTION_SET_TEXT
        // 注意：如果焦点是通过异步手势获取的，这里可能会因为UI尚未响应而失败。
        // 在实际应用中，可能需要在手势后加入短暂延迟或更复杂的事件监听逻辑
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToInput);

        if (targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
            Log.i(TAG, "performInput: ACTION_SET_TEXT successful on node (ID: " + targetNode.getViewIdResourceName() + ").");
            return true;
        }

        // [备选方案] 如果 ACTION_SET_TEXT 失败，尝试使用剪贴板粘贴
        Log.w(TAG, "performInput: ACTION_SET_TEXT failed. Falling back to clipboard paste.");
        ClipboardManager clipboard = (ClipboardManager) service.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Log.e(TAG, "performInput: Could not get ClipboardManager.");
            return false;
        }
        // 保存当前剪贴板内容，以便后续恢复
        ClipData originalClip = clipboard.getPrimaryClip();
        try {
            // 设置新的剪贴板内容
            ClipData clip = ClipData.newPlainText("text_to_paste", textToInput);
            clipboard.setPrimaryClip(clip);

            // 执行粘贴操作
            if (targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)) {
                Log.i(TAG, "performInput: ACTION_PASTE successful.");
                return true;
            } else {
                Log.e(TAG, "performInput: ACTION_PASTE failed.");
                return false;
            }
        } finally {
            // 无论成功与否，都尝试恢复原始剪贴板内容
            if (originalClip != null) {
                clipboard.setPrimaryClip(originalClip);
                Log.d(TAG, "performInput: Original clipboard content restored.");
            }
        }
    }
    
    /**
     * [新增功能] 在屏幕上执行一次通用的向上滑动（从下到上），用于滚动列表。
     * 这是当 ACTION_SCROLL_FORWARD 失败时的备用方案。
     *
     * @param service AccessibilityService 实例，用于获取屏幕尺寸和派发手势。
     * @return 如果手势成功派发则返回 true，否则返回 false。
     */
    public static boolean performGenericSwipeUp(AccessibilityService service) {
        // 获取屏幕尺寸
        android.util.DisplayMetrics displayMetrics = service.getResources().getDisplayMetrics();
        int screenHeight = displayMetrics.heightPixels;
        int screenWidth = displayMetrics.widthPixels;

        // 定义滑动的起始和结束点
        // 从屏幕 80% 高度的中心位置滑动到 20% 高度的中心位置
        int startX = screenWidth / 2;
        int startY = (int) (screenHeight * 0.8);
        int endX = screenWidth / 2;
        int endY = (int) (screenHeight * 0.2);
        long durationMs = 350; // 500毫秒的滑动时间

        Log.i(TAG, "performGenericSwipeUp: Performing generic swipe from (" + startX + "," + startY + ") to (" + endX + "," + endY + ")");
        return performSwipe(service, startX
                , startY, endX, endY, durationMs, null);
    }

    /**
     * [新增功能] 在屏幕上执行一次通用的向左滑动（从右到左），用于翻页。
     *
     * @param service AccessibilityService 实例，用于获取屏幕尺寸和派发手势。
     * @return 如果手势成功派发则返回 true，否则返回 false。
     */
    public static boolean performGenericSwipeLeft(AccessibilityService service) {
        // 获取屏幕尺寸
        android.util.DisplayMetrics displayMetrics = service.getResources().getDisplayMetrics();
        int screenHeight = displayMetrics.heightPixels;
        int screenWidth = displayMetrics.widthPixels;

        // 定义滑动的起始和结束点
        // 从屏幕 80% 宽度的中心位置滑动到 20% 宽度的中心位置
        // 为了避免触发边缘区域的特殊功能，我们将滑动范围稍微向内收缩
        int startX = (int) (screenWidth * 0.85);
        int startY = screenHeight / 2;
        int endX = (int) (screenWidth * 0.15);
        int endY = screenHeight / 2 + 5; // 增加一个微小的垂直位移，模拟更自然的手势
        // 持续时间是关键。过长（像拖动）或过短（像点击）都可能无效。
        // 500ms 是一个比较典型的“轻拂”手势时长，可以根据目标应用调整。
        long durationMs = 500;

        Log.i(TAG, "performGenericSwipeLeft: Performing generic swipe from (" + startX + "," + startY + ") to (" + endX + "," + endY + ")");
        return performSwipe(service, startX, startY, endX, endY, durationMs, null);
    }
}
