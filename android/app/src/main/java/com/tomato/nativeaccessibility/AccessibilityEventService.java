package com.tomato.nativeaccessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * 核心的无障碍服务类。
 * 当用户授权后，此服务会在后台运行，监听指定应用的界面变化。
 *
 * [已修改]
 */
public class AccessibilityEventService extends AccessibilityService {

    // 状态标志位：防止在同一个界面上重复点击
    private boolean hasClickedOnThisScreen = false;

    // 使用 Handler 来处理延迟操作，避免阻塞主线程
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * 当服务成功连接时被调用。
     */
    @Override
    protected void onServiceConnected() {
        Log.i(AccessibilityConfig.TAG, "无障碍服务已连接。");
    }

    /**
     * 当系统检测到符合我们配置的无障碍事件时，此方法会被调用。
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(AccessibilityConfig.TAG, "I am here;;;;");
        if (event == null || event.getPackageName() == null) {
            return;
        }

        // 如果事件不是来自目标应用
        if (!AccessibilityConfig.TARGET_PACKAGE_NAME.equals(event.getPackageName().toString())) {
            // 如果用户之前在目标应用内，现在离开了，就重置状态
            if (hasClickedOnThisScreen) {
                Log.d(AccessibilityConfig.TAG, "用户离开目标应用，重置所有状态。");
                hasClickedOnThisScreen = false;
                // 当离开目标应用时，取消所有待处理的重试任务
                mHandler.removeCallbacksAndMessages(null);
            }
            return;
        }

        int eventType = event.getEventType();

        // 主要监听窗口变化事件，这是进入新界面的最可靠信号
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(AccessibilityConfig.TAG, "检测到窗口状态变化事件。");
            // 每次进入新界面时，都重置点击标志
            hasClickedOnThisScreen = false;
            mHandler.removeCallbacksAndMessages(null); // 取消上个界面可能遗留的重试任务

            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                findAndClickNode(rootNode, 0); // 启动查找和点击，初始尝试次数为0
                rootNode.recycle();
            }
        }
    }

    /**
     * 递归地查找并点击节点，增加了重试机制。
     * @param rootNode 根节点
     * @param attempt 当前尝试次数
     */
    private void findAndClickNode(AccessibilityNodeInfo rootNode, int attempt) {
        // 如果在本界面已经成功点击过，或者根节点为空，则停止后续所有操作
        if (hasClickedOnThisScreen || rootNode == null) {
            return;
        }

        if (attempt >= AccessibilityConfig.MAX_RETRY_ATTEMPTS) {
            Log.w(AccessibilityConfig.TAG, "超过最大重试次数，未能找到并点击目标节点。");
            return;
        }

        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(AccessibilityConfig.TARGET_FOR_INPUT_BUTTON);

        if (nodes != null && !nodes.isEmpty()) {
            AccessibilityNodeInfo targetNode = nodes.get(0); // ID通常是唯一的

            if (targetNode != null && targetNode.isVisibleToUser() && targetNode.isEnabled()) {
                Log.d(AccessibilityConfig.TAG, "节点已就绪 (尝试 " + (attempt + 1) + " 次)，准备执行点击。");

                // 设置标志位，防止重复点击。即使点击失败，也只在本界面尝试一次完整的流程。
                hasClickedOnThisScreen = true;

                boolean clickSuccess = false;

                // [关键修改] 1. 优先使用 performAction(ACTION_CLICK)
                // 这是最稳定、最推荐的点击方式，直接触发控件的点击事件。
                if (targetNode.isClickable()) {
                    Log.i(AccessibilityConfig.TAG, "节点可点击，尝试执行 performAction(ACTION_CLICK)。");
                    clickSuccess = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    if (clickSuccess) {
                        Log.i(AccessibilityConfig.TAG, "performAction(ACTION_CLICK) 成功！");
                    } else {
                        Log.w(AccessibilityConfig.TAG, "performAction(ACTION_CLICK) 返回 false，操作失败。");
                    }
                }

                // [关键修改] 2. 如果 performAction 失败或节点不可点击，回退到手势模拟
                if (!clickSuccess) {
                    Log.w(AccessibilityConfig.TAG, "performAction 失败或节点不可点击，回退到手势模拟。");
                    Rect bounds = new Rect();
                    targetNode.getBoundsInScreen(bounds);

                    if (bounds.width() > 0 && bounds.height() > 0) {
                        clickOnNodeByGesture(bounds.centerX(), bounds.centerY());
                    } else {
                        Log.e(AccessibilityConfig.TAG, "节点坐标无效，无法通过手势点击。Bounds: " + bounds);
                        hasClickedOnThisScreen = false; // 点击彻底失败，重置标志位允许重试
                    }
                }

            } else {
                // 节点未就绪，安排下一次重试
                Log.d(AccessibilityConfig.TAG, "节点未就绪 (尝试 " + (attempt + 1) + " 次)，将在 " + AccessibilityConfig.RETRY_DELAY_MS + "ms 后重试。");
                scheduleNextAttempt(attempt + 1);
            }
            // 回收所有找到的节点
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null) node.recycle();
            }
        } else {
            // 未找到节点，安排下一次重试
            Log.d(AccessibilityConfig.TAG, "根据ID未找到节点 (尝试 " + (attempt + 1) + " 次)，将在 " + AccessibilityConfig.RETRY_DELAY_MS + "ms 后重试。");
            scheduleNextAttempt(attempt + 1);
        }
    }

    /**
     * 安排下一次查找和点击的尝试
     * @param nextAttempt 下一次尝试的次数
     */
    private void scheduleNextAttempt(int nextAttempt) {
        mHandler.postDelayed(() -> {
            AccessibilityNodeInfo newRootNode = getRootInActiveWindow(); // 每次重试都重新获取最新的根节点
            findAndClickNode(newRootNode, nextAttempt);
            if (newRootNode != null) {
                newRootNode.recycle();
            }
        }, AccessibilityConfig.RETRY_DELAY_MS);
    }

    /**
     * [修改] 在指定屏幕坐标执行模拟手势点击（作为备用方案）
     * @param x X坐标
     * @param y Y坐标
     */
    private void clickOnNodeByGesture(int x, int y) {
        Log.i(AccessibilityConfig.TAG, "正在坐标 (" + x + ", " + y + ") 执行手势点击。");
        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        // [修改] 将点击持续时间从100ms缩短到20ms，更像一次真实的快速点击
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 20);
        gestureBuilder.addStroke(stroke);

        dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.i(AccessibilityConfig.TAG, "手势点击完成。");
                // 成功后无需任何操作，hasClickedOnThisScreen 已经是 true
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(AccessibilityConfig.TAG, "手势点击被取消。");
                // 如果手势被取消，说明点击没有成功，重置标志位以允许服务再次尝试
                hasClickedOnThisScreen = false;
            }
        }, null);
    }

    @Override
    public void onInterrupt() {
        Log.w(AccessibilityConfig.TAG, "无障碍服务被中断。");
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(AccessibilityConfig.TAG, "无障碍服务已解绑。");
        mHandler.removeCallbacksAndMessages(null);
        return super.onUnbind(intent);
    }
}
