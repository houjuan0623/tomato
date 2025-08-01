package com.tomato.utils;

import com.tomato.nativeaccessibility.AccessibilityConfig;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;
import android.graphics.Rect;
import android.graphics.Path;
import android.accessibilityservice.GestureDescription;

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
}
